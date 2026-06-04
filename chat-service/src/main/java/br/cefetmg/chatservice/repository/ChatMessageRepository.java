package br.cefetmg.chatservice.repository;

import br.cefetmg.chatservice.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findBySenderAndReceiver(String sender, String receiver);

    List<ChatMessage> findByreceiver(String receiver);
}
