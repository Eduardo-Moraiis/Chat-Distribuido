package br.cefetmg.authservice.controller;

import br.cefetmg.authservice.model.User;
import br.cefetmg.authservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/auth")
public class AuthServiceController {
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user){
        try {
            authService.register(user);
            return ResponseEntity.status(201).body("Usuário criado com sucesso.");
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user){
        try{
            String token = authService.login(user.getUsername(), user.getPassword());
            return ResponseEntity.ok(Collections.singletonMap("token", token));
        }catch (RuntimeException e){
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
