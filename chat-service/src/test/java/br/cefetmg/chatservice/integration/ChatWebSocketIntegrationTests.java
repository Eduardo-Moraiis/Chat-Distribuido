package br.cefetmg.chatservice.integration;

import br.cefetmg.chatservice.config.JwtUtil;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private ChatMessageRepository repository;

    @Autowired
    private JwtUtil jwtUtil;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        jwtToken = generateDummyToken();
    }

    private String generateDummyToken() {
        return io.jsonwebtoken.Jwts.builder()
                .subject("alice")
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

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void testWebSocketConnectionAndMessageExchange() throws Exception {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        converter.setObjectMapper(mapper);
        stompClient.setMessageConverter(converter);

        String wsUrl = "ws://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwtToken);

        BlockingQueue<ChatMessage> blockingQueue = new LinkedBlockingDeque<>();

        org.springframework.web.socket.WebSocketHttpHeaders handshakeHeaders = new org.springframework.web.socket.WebSocketHttpHeaders();
        
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                System.err.println("STOMP CLIENT EXCEPTION: " + exception.getMessage());
                exception.printStackTrace();
            }
            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                System.err.println("STOMP CLIENT TRANSPORT ERROR: " + exception.getMessage());
                exception.printStackTrace();
            }
        };

        CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync(wsUrl, handshakeHeaders, connectHeaders, sessionHandler);
        StompSession session = sessionFuture.get(10, TimeUnit.SECONDS);

        Assertions.assertTrue(session.isConnected(), "WebSocket session should be connected");

        session.subscribe("/topic/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                blockingQueue.offer((ChatMessage) payload);
            }
        });

        // Aguarda o processamento da inscrição no servidor
        Thread.sleep(1000);

        ChatMessage message = ChatMessage.builder()
                .sender("alice")
                .receiver("public")
                .content("Teste WebSocket")
                .type(MessageType.CHAT)
                .build();

        session.send("/app/chat.sendMessage", message);

        ChatMessage receivedMessage = blockingQueue.poll(10, TimeUnit.SECONDS);
        Assertions.assertNotNull(receivedMessage, "Broadcasted message should not be null");
        Assertions.assertEquals("alice", receivedMessage.getSender());
        Assertions.assertEquals("public", receivedMessage.getReceiver());
        Assertions.assertEquals("Teste WebSocket", receivedMessage.getContent());

        // Validate persistence in MongoDB
        // Since WebSocket saves asynchronously, wait up to 2 seconds for persistence
        boolean saved = false;
        for (int i = 0; i < 20; i++) {
            if (repository.count() > 0) {
                saved = true;
                break;
            }
            Thread.sleep(100);
        }
        Assertions.assertTrue(saved, "Message should be saved in MongoDB");
    }
}
