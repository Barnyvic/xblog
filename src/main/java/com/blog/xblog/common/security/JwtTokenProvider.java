package com.blog.xblog.common.security;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long validityInMillis;

    public JwtTokenProvider(
            @Value("${security.jwt.private-key:}") String base64PrivateKey,
            @Value("${security.jwt.public-key:}") String base64PublicKey,
            @Value("${security.jwt.validity-ms:86400000}") long validityInMillis) {
        KeyPair pair = loadOrGenerateKeys(base64PrivateKey, base64PublicKey);
        this.privateKey = pair.getPrivate();
        this.publicKey = pair.getPublic();
        this.validityInMillis = validityInMillis;
    }

    private KeyPair loadOrGenerateKeys(String base64PrivateKey, String base64PublicKey) {
        String cleanPrivate = base64PrivateKey == null ? "" : base64PrivateKey.replaceAll("\\s+", "").trim();
        String cleanPublic = base64PublicKey == null ? "" : base64PublicKey.replaceAll("\\s+", "").trim();

        if (cleanPrivate.length() > 0 && cleanPublic.length() > 0) {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                byte[] privateKeyBytes = Decoders.BASE64.decode(cleanPrivate);
                byte[] publicKeyBytes = Decoders.BASE64.decode(cleanPublic);
                PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
                PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
                return new KeyPair(publicKey, privateKey);
            } catch (Exception ex) {
                log.warn("Invalid or incomplete JWT keys in config, using generated in-memory key (dev only): {}", ex.getMessage());
            }
        }

        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            log.warn("JWT keys not set or invalid. Using in-memory RSA key (tokens invalid after restart). Set JWT_PRIVATE_KEY and JWT_PUBLIC_KEY for production.");
            return pair;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load or generate RSA keys for JWT", ex);
        }
    }

    public String generateToken(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String username = principal instanceof UserDetails
                ? ((UserDetails) principal).getUsername()
                : principal.toString();

        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMillis);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
