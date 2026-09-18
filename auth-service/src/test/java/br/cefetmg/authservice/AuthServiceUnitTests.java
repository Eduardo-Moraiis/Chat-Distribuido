package br.cefetmg.authservice;

import br.cefetmg.authservice.model.User;
import br.cefetmg.authservice.repository.UserRepository;
import br.cefetmg.authservice.service.AuthService;
import br.cefetmg.authservice.service.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

class AuthServiceUnitTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegister_Sucesso() {
        User user = new User("username_teste", "Nome Teste", "senha123");
        
        Mockito.when(userRepository.findByUsername("username_teste")).thenReturn(Optional.empty());
        Mockito.when(passwordEncoder.encode("senha123")).thenReturn("senhaCripto");
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);

        authService.register(user);

        Assertions.assertEquals("senhaCripto", user.getPassword());
        Mockito.verify(userRepository, Mockito.times(1)).save(user);
    }

    @Test
    void testRegister_UsuarioJaExistente() {
        User user = new User("username_teste", "Nome Teste", "senha123");
        
        Mockito.when(userRepository.findByUsername("username_teste")).thenReturn(Optional.of(user));

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            authService.register(user);
        });

        Assertions.assertEquals("Erro: Este nome de usuário já está em uso.", exception.getMessage());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void testLogin_Sucesso() {
        User user = new User("username_teste", "Nome Teste", "senhaCripto");
        
        Mockito.when(userRepository.findByUsername("username_teste")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("senha123", "senhaCripto")).thenReturn(true);
        Mockito.when(jwtService.generateToken("username_teste")).thenReturn("token_mockado");

        String token = authService.login("username_teste", "senha123");

        Assertions.assertEquals("token_mockado", token);
    }

    @Test
    void testLogin_SenhaIncorreta() {
        User user = new User("username_teste", "Nome Teste", "senhaCripto");
        
        Mockito.when(userRepository.findByUsername("username_teste")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("senha_errada", "senhaCripto")).thenReturn(false);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            authService.login("username_teste", "senha_errada");
        });

        Assertions.assertEquals("Usuário ou senha inválido.", exception.getMessage());
    }

    @Test
    void testLogin_UsuarioNaoEncontrado() {
        Mockito.when(userRepository.findByUsername("username_teste")).thenReturn(Optional.empty());

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            authService.login("username_teste", "senha123");
        });

        Assertions.assertEquals("Usuário ou senha inválido.", exception.getMessage());
    }
}
