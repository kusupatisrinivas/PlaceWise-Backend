package com.placewise.security;
import com.placewise.entity.User;
import com.placewise.dto.ApiDtos.AuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import java.time.*;
import java.util.List;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final long minutes;
    private final Clock clock;
    public JwtService(JwtEncoder encoder, @Value("${app.jwt-minutes}") long minutes, Clock clock) {
        this.encoder = encoder; this.minutes = minutes; this.clock = clock;
    }
    public AuthResponse issue(User user) {
        Instant now = clock.instant(), expires = now.plusSeconds(minutes * 60);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("placewise").subject(user.id.toString())
            .issuedAt(now).expiresAt(expires).claim("roles", List.of(user.role.name())).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(token, expires, user.role.name(), user.id, user.fullName, user.email);
    }
}
