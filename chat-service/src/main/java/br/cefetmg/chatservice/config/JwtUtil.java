package br.cefetmg.chatservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    private final String SECRET = "EraUmaVezUmaMeninaQueTodosChamavamDeChapeuzinhoVermelhoPorCausaDoCapuzDeVeludoVermelhoQueElaUsavaElaViviaFelizComSuaMaeEPaiQueAAmavamMuitoAssimComoTodosPorqueElaEraGentilEObedienteChapeuzinhoVermelhoTinhaAindaUmaVovoQueridaQueMoravaNaBeiraDaFloresta";
    private final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}