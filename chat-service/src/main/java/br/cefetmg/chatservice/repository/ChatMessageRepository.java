package br.cefetmg.chatservice.repository;

import br.cefetmg.chatservice.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    // Para Grupos (1:N)
    List<ChatMessage> findByReceiverOrderByTimestampAsc(String receiver);

    // Para Chat Privado (1:1) - Forma otimizada
    @Query("{ '$or': [ { 'sender': ?0, 'receiver': ?1 }, { 'sender': ?1, 'receiver': ?0 } ] }")
    List<ChatMessage> findPrivateChatHistory(String user1, String user2);
}
