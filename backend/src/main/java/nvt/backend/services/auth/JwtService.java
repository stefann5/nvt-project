package nvt.backend.services.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import nvt.backend.exceptions.TokenExpiredException;
import nvt.backend.model.user.User;
import nvt.backend.repositories.auth.TokenRepository;
import nvt.backend.repositories.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.access-token-expiration}")
    private long accessTokenExpire;

    @Value("${application.security.jwt.refresh-token-expiration}")
    private long refreshTokenExpire;

    @Value("${application.front.address.login}")
    private String frontLoginAddress;

    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;

    // In-memory cache for token validation - TTL managed by token expiration
    private final Map<String, Boolean> tokenValidityCache = new ConcurrentHashMap<>();
    private final Map<String, Long> tokenCacheTimestamp = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60000; // 1 minute cache


    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    public boolean isValid(String token, UserDetails user) {
        String username = extractUsername(token);

        // Check cache first
        Boolean cached = getCachedTokenValidity(token);
        boolean validToken;
        if (cached != null) {
            validToken = cached;
        } else {
            validToken = tokenRepository
                    .findByAccessToken(token)
                    .map(t -> !t.isLoggedOut())
                    .orElse(false);
            cacheTokenValidity(token, validToken);
        }

        return (username.equals(user.getUsername())) && !isTokenExpired(token) && validToken;
    }
    
    private Boolean getCachedTokenValidity(String token) {
        Long timestamp = tokenCacheTimestamp.get(token);
        if (timestamp == null || System.currentTimeMillis() - timestamp > CACHE_TTL_MS) {
            tokenValidityCache.remove(token);
            tokenCacheTimestamp.remove(token);
            return null;
        }
        return tokenValidityCache.get(token);
    }
    
    private void cacheTokenValidity(String token, boolean valid) {
        tokenValidityCache.put(token, valid);
        tokenCacheTimestamp.put(token, System.currentTimeMillis());
    }
    
    public void invalidateTokenCache(String token) {
        tokenValidityCache.remove(token);
        tokenCacheTimestamp.remove(token);
    }

    public boolean isValidRefreshToken(String token, User user) {
        String username = extractUsername(token);

        try{
            boolean validRefreshToken = tokenRepository
                    .findByRefreshToken(token)
                    .map(t -> !t.isLoggedOut())
                    .orElse(false);

            return (username.equals(user.getUsername())) && !isTokenExpired(token) && validRefreshToken;
        }catch (ExpiredJwtException e){
            throw new TokenExpiredException("Token expired");
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSigninKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public String generateAccessToken(User user) {
        return generateToken(user, accessTokenExpire);
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, refreshTokenExpire);
    }

    private String generateToken(User user, long expireTime) {
        String token = Jwts
                .builder()
                .subject(user.getUsername())
                .claim("id", user.getId())
                .claim("role", user.getRole())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(getSigninKey())
                .compact();

        return token;
    }

    private SecretKey getSigninKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateActivationToken(User user, long activationTokenExpire) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + activationTokenExpire))
                .signWith(getSigninKey())
                .compact();
    }
}
