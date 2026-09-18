package br.cefetmg.chatservice.controller;

import br.cefetmg.chatservice.model.ChatMessage;
import br.cefetmg.chatservice.model.MessageType;
import br.cefetmg.chatservice.repository.ChatMessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatHistoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatMessageRepository repository;

    private ChatMessage groupMessage;
    private ChatMessage privateMessage1;
    private ChatMessage privateMessage2;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        groupMessage = ChatMessage.builder()
                .sender("alice")
                .receiver("public")
                .content("Olá grupo!")
                .type(MessageType.CHAT)
                .timestamp(LocalDateTime.now().minusMinutes(5))
                .build();

        privateMessage1 = ChatMessage.builder()
                .sender("alice")
                .receiver("bob")
                .content("Oi Bob, tudo bem?")
                .type(MessageType.CHAT)
                .timestamp(LocalDateTime.now().minusMinutes(3))
                .build();

        privateMessage2 = ChatMessage.builder()
                .sender("bob")
                .receiver("alice")
                .content("Oi Alice, tudo bem e você?")
                .type(MessageType.CHAT)
                .timestamp(LocalDateTime.now().minusMinutes(2))
                .build();

        repository.saveAll(List.of(groupMessage, privateMessage1, privateMessage2));
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void testGetGroupHistory() throws Exception {
        mockMvc.perform(get("/history/group/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sender").value("alice"))
                .andExpect(jsonPath("$[0].receiver").value("public"))
                .andExpect(jsonPath("$[0].content").value("Olá grupo!"));
    }

    @Test
    void testGetPrivateHistory() throws Exception {
        mockMvc.perform(get("/history/private/alice/bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sender").value("alice"))
                .andExpect(jsonPath("$[0].receiver").value("bob"))
                .andExpect(jsonPath("$[1].sender").value("bob"))
                .andExpect(jsonPath("$[1].receiver").value("alice"));
    }

    @Test
    void testGetPrivateConversations() throws Exception {
        mockMvc.perform(get("/history/conversations/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("bob"));
    }

    @Test
    void testCountMessages() throws Exception {
        mockMvc.perform(get("/history/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }
}
