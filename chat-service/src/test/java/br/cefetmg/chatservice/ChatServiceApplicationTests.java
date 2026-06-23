package br.cefetmg.chatservice;

import br.cefetmg.chatservice.model.ChatMessage;
import br.cefetmg.chatservice.model.MessageType;
import br.cefetmg.chatservice.repository.ChatMessageRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class ChatServiceApplicationTests {

    @Autowired
    private ChatMessageRepository repository;

    @Test
    void deveSalvarEMensagemNoMongo() {
        // 1. Criar mensagem
        ChatMessage msg = ChatMessage.builder()
                .sender("eduardo")
                .receiver("public")
                .content("Teste de Persistência")
                .type(MessageType.CHAT)
                .build();

        // 2. Salvar
        repository.save(msg);

        // 3. Buscar para validar usando o método exato da sua interface
        // Note o "OrderByTimestampAsc" no final
        List<ChatMessage> historico = repository.findByReceiverOrderByTimestampAsc("public");

        Assertions.assertFalse(historico.isEmpty(), "O histórico não deveria estar vazio");

        // Validamos se a mensagem salva é a mesma que buscamos
        boolean encontrou = historico.stream()
                .anyMatch(m -> m.getContent().equals("Teste de Persistência"));

        Assertions.assertTrue(encontrou, "A mensagem salva deveria estar no histórico");
    }
}