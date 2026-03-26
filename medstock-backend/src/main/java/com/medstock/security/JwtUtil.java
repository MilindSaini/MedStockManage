package com.medstock.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.medstock.entity.User;
import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtUtil {

    @Value("${medstock.security.jwt.private-key:}")
    private String privateKeyValue;

    @Value("${medstock.security.jwt.public-key:}")
    private String publicKeyValue;

    @Value("${medstock.security.jwt.issuer:medstock}")
    private String issuer;

    @Value("${medstock.security.jwt.access-token-minutes:15}")
    private long accessTokenMinutes;

    @Value("${medstock.security.jwt.refresh-token-days:7}")
    private long refreshTokenDays;

    @Value("${medstock.security.jwt.allow-ephemeral-keys:false}")
    private boolean allowEphemeralKeys;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    void initialize() {
        try {
            RSAPrivateKey privateKey;
            RSAPublicKey publicKey;

            if (privateKeyValue == null || privateKeyValue.isBlank() || publicKeyValue == null || publicKeyValue.isBlank()) {
                if (!allowEphemeralKeys) {
                    throw new IllegalStateException("JWT RSA keys are required. Set medstock.security.jwt.private-key and medstock.security.jwt.public-key.");
                }
                log.warn("JWT RSA keys are not configured. Using generated ephemeral keys because allow-ephemeral-keys=true.");
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                privateKey = (RSAPrivateKey) keyPair.getPrivate();
                publicKey = (RSAPublicKey) keyPair.getPublic();
            } else {
                privateKey = parsePrivateKey(privateKeyValue);
                publicKey = parsePublicKey(publicKeyValue);
            }

            this.algorithm = Algorithm.RSA256(publicKey, privateKey);
            this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize JWT utility", ex);
        }
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roles = RoleUtils.parseRoles(user.getRole());
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(userIdentity(user))
            .withClaim("uid", user.getId())
            .withClaim("sid", user.getStoreId())
            .withClaim("role", RoleUtils.primaryRole(roles))
            .withClaim("roles", roles)
            .withClaim("type", "ACCESS")
            .withIssuedAt(now)
            .withExpiresAt(now.plus(accessTokenMinutes, ChronoUnit.MINUTES))
            .sign(algorithm);
    }

    public String generateRefreshToken(User user, String tokenId) {
        Instant now = Instant.now();
        List<String> roles = RoleUtils.parseRoles(user.getRole());
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(userIdentity(user))
            .withJWTId(tokenId)
            .withClaim("uid", user.getId())
            .withClaim("sid", user.getStoreId())
            .withClaim("role", RoleUtils.primaryRole(roles))
            .withClaim("roles", roles)
            .withClaim("type", "REFRESH")
            .withIssuedAt(now)
            .withExpiresAt(now.plus(refreshTokenDays, ChronoUnit.DAYS))
            .sign(algorithm);
    }

    public boolean validateToken(String token, String expectedType) {
        try {
            DecodedJWT decoded = verifier.verify(token);
            String type = decoded.getClaim("type").asString();
            return expectedType.equals(type);
        } catch (JWTVerificationException ex) {
            return false;
        }
    }

    public DecodedJWT extractClaims(String token) {
        return verifier.verify(token);
    }

    public long getAccessTokenExpiresInSeconds() {
        return accessTokenMinutes * 60;
    }

    private RSAPrivateKey parsePrivateKey(String rawValue) throws Exception {
        String normalized = normalizePem(rawValue)
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGINPRIVATEKEY-----", "")
            .replace("-----ENDPRIVATEKEY-----", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private RSAPublicKey parsePublicKey(String rawValue) throws Exception {
        String normalized = normalizePem(rawValue)
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGINPUBLICKEY-----", "")
            .replace("-----ENDPUBLICKEY-----", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private String normalizePem(String value) {
        return value
            .replace("\\n", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
            .trim();
    }

    private String userIdentity(User user) {
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }
}
