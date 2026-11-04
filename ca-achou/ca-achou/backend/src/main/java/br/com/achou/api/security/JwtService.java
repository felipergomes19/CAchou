package br.com.achou.api.security;

import br.com.achou.api.config.AchouProperties;
import br.com.achou.api.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final Duration validade;

    public JwtService(AchouProperties props) {
        String secret = props.jwt().secret();
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("achou.jwt.secret precisa ter pelo menos 32 caracteres.");
        }
        if (secret.startsWith("dev-only")) {
            log.warn("Usando segredo JWT de desenvolvimento. Defina ACHOU_JWT_SECRET em produção!");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.validade = Duration.ofHours(props.jwt().expiracaoHoras());
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("rm", usuario.getRm())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validade)))
                .signWith(key)
                .compact();
    }

    public Optional<UserPrincipal> validar(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new UserPrincipal(Long.valueOf(c.getSubject()), c.get("rm", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
