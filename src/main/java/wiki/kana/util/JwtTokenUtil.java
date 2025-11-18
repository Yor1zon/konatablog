package wiki.kana.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * JWT Token 工具类
 * 负责JWT Token的生成、解析和验证
 */
@Slf4j
@Component
public class JwtTokenUtil {

    @Value("${app.jwt.secret:konatablog-secret-key-for-jwt-token-generation}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400}") // 24小时（秒）
    private int jwtExpirationInSeconds;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     用户角色
     * @return JWT Token字符串
     */
    public String generateToken(Long userId, String username, String role) {
        log.debug("Generating JWT token for user: {} (ID: {}, Role: {})", username, userId, role);

        Date expirationDate = Date.from(
            LocalDateTime.now()
                .plusSeconds(jwtExpirationInSeconds)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        );

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 从Token中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Failed to extract username from token", e);
            return null;
        }
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            log.error("Failed to extract userId from token", e);
            return null;
        }
    }

    /**
     * 从Token中获取用户角色
     *
     * @param token JWT Token
     * @return 用户角色
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.error("Failed to extract role from token", e);
            return null;
        }
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 检查Token是否过期
     *
     * @param token JWT Token
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("Failed to check token expiration", e);
            return true;
        }
    }

    /**
     * 解析Token
     *
     * @param token JWT Token
     * @return Claims对象
     * @throws JwtException 解析异常
     */
    private Claims parseToken(String token) {
        Jwt<?, ?> jwt = Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parse(token);

        if (jwt.getBody() instanceof Claims) {
            return (Claims) jwt.getBody();
        }

        throw new JwtException("Invalid JWT token: claims not found");
    }

    /**
     * 获取Token过期时间
     *
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("Failed to extract expiration date from token", e);
            return null;
        }
    }

    /**
     * 从请求头中提取Token
     *
     * @param authHeader Authorization header值
     * @return Token字符串，如果无效则返回null
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // 去掉"Bearer "前缀
        }
        return null;
    }
}