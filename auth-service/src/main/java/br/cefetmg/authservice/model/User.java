package br.cefetmg.authservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users") // "user" é palavra reservada no Postgres, usamos "users"
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(length = 50)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;
}