package br.cefetmg.chatservice.controller;

import br.cefetmg.chatservice.model.ChatMessage;
import br.cefetmg.chatservice.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/history")
public class ChatHistoryController {

    @Autowired
    private ChatMessageRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // 1:N - Histórico de Grupo ou Canal Público
    @GetMapping("/group/{groupId}")
    public List<ChatMessage> getGroupHistory(@PathVariable String groupId) {
        return repository.findByReceiverOrderByTimestampAsc(groupId);
    }

    // 1:1 - Histórico Privado entre duas pessoas
    @GetMapping("/private/{user1}/{user2}")
    public List<ChatMessage> getPrivateHistory(@PathVariable String user1, @PathVariable String user2) {
        return repository.findPrivateChatHistory(user1, user2);
    }

    @GetMapping("/conversations/{username}")
    public Set<String> getPrivateConversations(@PathVariable String username) {
        Query query = new Query(new Criteria().orOperator(
                Criteria.where("sender").is(username),
                Criteria.where("receiver").is(username)
        ));

        List<ChatMessage> messages = mongoTemplate.find(query, ChatMessage.class);
        Set<String> contacts = new LinkedHashSet<>();

        for (ChatMessage message : messages) {
            if (username.equals(message.getSender()) && message.getReceiver() != null && !isGroupReceiver(message.getReceiver())) {
                contacts.add(message.getReceiver());
            } else if (username.equals(message.getReceiver()) && message.getSender() != null) {
                contacts.add(message.getSender());
            }
        }

        return contacts;
    }

    @GetMapping("/count")
    public long countMessages() {
        return repository.count();
    }

    private boolean isGroupReceiver(String receiver) {
        return "public".equalsIgnoreCase(receiver)
                || "geral".equalsIgnoreCase(receiver)
                || "turma".equalsIgnoreCase(receiver);
    }
}
