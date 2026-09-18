package br.cefetmg.chatservice.integration;

import br.cefetmg.chatservice.model.ChatMessage;
import br.cefetmg.chatservice.model.MessageType;
import br.cefetmg.chatservice.repository.ChatMessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatConcurrencyLoadTests {

    @LocalServerPort
    private int port;

    @Autowired
    private ChatMessageRepository repository;

    private static final int CONCURRENT_USERS = 10;
    private final ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_USERS);

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
        executorService.shutdownNow();
    }

    private String generateToken(String username) {
        return io.jsonwebtoken.Jwts.builder()
                .subject(username)
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 86400000))
                .signWith(javax.crypto.spec.SecretKeySpec.class.cast(
                        io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                                "EraUmaVezUmaMeninaQueTodosChamavamDeChapeuzinhoVermelhoPorCausaDoCapuzDeVeludoVermelhoQueElaUsavaElaViviaFelizComSuaMaeEPaiQueAAmavamMuitoAssimComoTodosPorqueElaEraGentilEObedienteChapeuzinhoVermelhoTinhaAindaUmaVovoQueridaQueMoravaNaBeiraDaFloresta"
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)
                        )
                ))
                .compact();
    }

    @Test
    void testConcurrentUsersExchangingMessages() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/ws";
        CountDownLatch connectionLatch = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch receiveLatch = new CountDownLatch(CONCURRENT_USERS * CONCURRENT_USERS);
        AtomicInteger totalReceivedMessages = new AtomicInteger(0);

        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        List<StompSession> sessions = new CopyOnWriteArrayList<>();

        for (int i = 1; i <= CONCURRENT_USERS; i++) {
            final String username = "user_" + i;
            final String token = generateToken(username);

            executorService.submit(() -> {
                try {
                    SockJsClient sockJsClient = new SockJsClient(transports);
                    WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
                    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    mapper.findAndRegisterModules();
                    mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    converter.setObjectMapper(mapper);
                    stompClient.setMessageConverter(converter);

                    StompHeaders connectHeaders = new StompHeaders();
                    connectHeaders.add("Authorization", "Bearer " + token);

                    org.springframework.web.socket.WebSocketHttpHeaders handshakeHeaders = new org.springframework.web.socket.WebSocketHttpHeaders();
                    
                    StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
                        @Override
                        public void handleException(StompSession s, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                            System.err.println("STOMP CLIENT CONCURRENT EXCEPTION: " + exception.getMessage());
                            exception.printStackTrace();
                        }
                        @Override
                        public void handleTransportError(StompSession s, Throwable exception) {
                            System.err.println("STOMP CLIENT CONCURRENT TRANSPORT ERROR: " + exception.getMessage());
                            exception.printStackTrace();
                        }
                    };

                    CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync(wsUrl, handshakeHeaders, connectHeaders, sessionHandler);
                    StompSession session = sessionFuture.get(10, TimeUnit.SECONDS);

                    if (session.isConnected()) {
                        sessions.add(session);
                        connectionLatch.countDown();

                        session.subscribe("/topic/public", new StompFrameHandler() {
                            @Override
                            public Type getPayloadType(StompHeaders headers) {
                                return ChatMessage.class;
                            }

                            @Override
                            public void handleFrame(StompHeaders headers, Object payload) {
                                totalReceivedMessages.incrementAndGet();
                                receiveLatch.countDown();
                            }
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Erro na thread do usuario " + username + ": " + e.getMessage());
                }
            });
        }

        boolean allConnected = connectionLatch.await(15, TimeUnit.SECONDS);
        Assertions.assertTrue(allConnected, "Todas as 10 conexões de usuários concorrentes deveriam ter se conectado");
        Assertions.assertEquals(CONCURRENT_USERS, sessions.size(), "Devem haver 10 sessões ativas");

        // Aguarda as inscrições se estabelecerem no servidor
        Thread.sleep(1000);

        CountDownLatch sendLatch = new CountDownLatch(CONCURRENT_USERS);
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final StompSession session = sessions.get(i);
            final String senderName = "user_" + (i + 1);
            executorService.submit(() -> {
                try {
                    ChatMessage msg = ChatMessage.builder()
                            .sender(senderName)
                            .receiver("public")
                            .content("Mensagem concorrente de " + senderName)
                            .type(MessageType.CHAT)
                            .build();
                    session.send("/app/chat.sendMessage", msg);
                    sendLatch.countDown();
                } catch (Exception e) {
                    System.err.println("Erro ao enviar mensagem concorrente: " + e.getMessage());
                }
            });
        }

        boolean allSent = sendLatch.await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(allSent, "Todos os envios de mensagens deveriam ter completado");

        boolean allReceived = receiveLatch.await(15, TimeUnit.SECONDS);
        System.out.println("Total de mensagens recebidas em broadcast: " + totalReceivedMessages.get());
        Assertions.assertTrue(totalReceivedMessages.get() >= CONCURRENT_USERS, "Cada usuário deveria ter recebido pelo menos a sua própria mensagem");

        boolean dbSavedAll = false;
        for (int i = 0; i < 30; i++) {
            if (repository.count() == CONCURRENT_USERS) {
                dbSavedAll = true;
                break;
            }
            Thread.sleep(200);
        }
        
        System.out.println("Quantidade total de mensagens gravadas no Mongo: " + repository.count());
        Assertions.assertEquals(CONCURRENT_USERS, repository.count(), "Deveriam existir exatamente 10 mensagens persistidas no MongoDB");

        for (StompSession session : sessions) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
