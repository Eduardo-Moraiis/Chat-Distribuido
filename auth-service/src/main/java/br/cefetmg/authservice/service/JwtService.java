package br.cefetmg.authservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final String SECRET = "EraUmaVezUmaMeninaQueTodosChamavamDeChapeuzinhoVermelhoPorCausaDoCapuzDeVeludoVermelhoQueElaUsavaElaViviaFelizComSuaMaeEPaiQueAAmavamMuitoAssimComoTodosPorqueElaEraGentilEObedienteChapeuzinhoVermelhoTinhaAindaUmaVovoQueridaQueMoravaNaBeiraDaFloresta";
    private final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public String generateToken(String username){
        return Jwts.builder().subject(username).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 86400000)).signWith(KEY).compact();
    }
}
