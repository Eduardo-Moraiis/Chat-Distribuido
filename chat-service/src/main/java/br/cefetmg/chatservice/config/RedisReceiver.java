package br.cefetmg.chatservice.config;

import br.cefetmg.chatservice.model.ChatMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class RedisReceiver {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void receiveMessage(Object message) { // Mude para Object se der erro de cast
        try {
            ChatMessage chatMessage;

            if (message instanceof ChatMessage) {
                chatMessage = (ChatMessage) message;
            } else {
                // Caso o Redis envie como String, convertemos manualmente
                chatMessage = objectMapper.readValue(message.toString(), ChatMessage.class);
            }

            // Entrega via WebSocket
            if ("public".equalsIgnoreCase(chatMessage.getReceiver())) {
                messagingTemplate.convertAndSend("/topic/" + chatMessage.getReceiver(), chatMessage);
            } else {
                messagingTemplate.convertAndSendToUser(chatMessage.getReceiver(), "/queue/messages", chatMessage);
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem do Redis: " + e.getMessage());
        }
    }
}