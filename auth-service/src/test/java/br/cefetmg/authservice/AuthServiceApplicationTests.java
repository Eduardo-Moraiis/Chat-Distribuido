package br.cefetmg.authservice;

import br.cefetmg.authservice.model.User;
import br.cefetmg.authservice.repository.UserRepository;
import br.cefetmg.authservice.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class AuthServiceApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
        // Teste básico: O Spring conseguiu subir?
    }

    @Test
    void deveCadastrarUsuarioComSenhaCriptografada() {
        // Criamos um usuário de teste
        User user = new User("teste_unitario", "Nome Teste", "senha123");

        // Executamos o registro
        authService.register(user);

        // Buscamos no banco para validar
        User userSalvo = userRepository.findById("teste_unitario").get();

        Assertions.assertNotNull(userSalvo);
        Assertions.assertEquals("Nome Teste", userSalvo.getName());

        // Valida que a senha NÃO é "senha123" (está criptografada)
        Assertions.assertNotEquals("senha123", userSalvo.getPassword());
        Assertions.assertTrue(passwordEncoder.matches("senha123", userSalvo.getPassword()));
    }
}