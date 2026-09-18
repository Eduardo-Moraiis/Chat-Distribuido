package br.cefetmg.chatservice.config;

import br.cefetmg.chatservice.model.ChatMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

@Service
public class RedisReceiver {

    private static final Set<String> GROUP_RECEIVERS = Set.of("public", "geral", "turma");

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void receiveMessage(Object message) { // Mude para Object se der erro de cast
        System.out.println("RedisReceiver receiveMessage chamado com: " + message);
        try {
            ChatMessage chatMessage;

            if (message instanceof ChatMessage) {
                chatMessage = (ChatMessage) message;
            } else {
                // Caso o Redis envie como String, convertemos manualmente
                chatMessage = objectMapper.readValue(message.toString(), ChatMessage.class);
            }

            // Entrega via WebSocket
            String receiver = chatMessage.getReceiver();

            if (receiver != null && GROUP_RECEIVERS.contains(receiver.toLowerCase())) {
                messagingTemplate.convertAndSend("/topic/" + chatMessage.getReceiver(), chatMessage);
            } else if (receiver != null) {
                messagingTemplate.convertAndSendToUser(chatMessage.getReceiver(), "/queue/messages", chatMessage);
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem do Redis: " + e.getMessage());
        }
    }
}
