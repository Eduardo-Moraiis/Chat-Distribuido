package br.cefetmg.authservice.service;

import br.cefetmg.authservice.model.User;
import br.cefetmg.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public void register(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Erro: Este nome de usuário já está em uso.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);
    }

    public String login(String username, String password){
        Optional<User> userOptional = userRepository.findByUsername(username);

        if(userOptional.isPresent()){
            User user = userOptional.get();

            if(passwordEncoder.matches(password, user.getPassword())){
                return jwtService.generateToken(user.getUsername());
            }
        }

        throw new RuntimeException("Usuário ou senha inválido.");
    }

}
