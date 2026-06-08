package br.cefetmg.chatservice.controller;

import br.cefetmg.chatservice.model.ChatMessage;
import br.cefetmg.chatservice.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/history")
public class ChatHistoryController {

    @Autowired
    private ChatMessageRepository repository;

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
}