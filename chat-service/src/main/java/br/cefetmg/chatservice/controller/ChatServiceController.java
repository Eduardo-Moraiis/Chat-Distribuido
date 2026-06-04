package br.cefetmg.chatservice.controller;

import br.cefetmg.chatservice.model.ChatMessage;
import br.cefetmg.chatservice.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatServiceController {

    @Autowired
    private ChatMessageRepository repository;

    @Autowired
    private RedisTemplate<String, ChatMessage> redisTemplate;

    @Autowired
    private ChannelTopic topic;

    @MessageMapping("/chat.sendMessage")
    public void processMessage(@Payload ChatMessage message) {

        message.setTimestamp(LocalDateTime.now());

        repository.save(message);

        redisTemplate.convertAndSend(topic.getTopic(), message);

        System.out.println("Mensagem de " + message.getSender() + " enviada para o Redis.");
    }
}