package wiki.kana.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import wiki.kana.repository.UserRepository;
import wiki.kana.util.JwtTokenUtil;

import java.io.IOException;
import java.util.List;

/**
 * JWT认证过滤器
 * 负责从HTTP请求中提取JWT Token并验证用户身份
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        try {
            // 从请求头中获取Token
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenUtil.validateToken(jwt)) {
                // 从Token中获取用户信息
                String username = jwtTokenUtil.getUsernameFromToken(jwt);
                Long userId = jwtTokenUtil.getUserIdFromToken(jwt);
                String role = jwtTokenUtil.getRoleFromToken(jwt);

                // 验证用户是否存在且活跃
                userRepository.findByUsername(username).ifPresent(user -> {
                    if (user.getIsActive()) {
                        // 创建认证对象
                        List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + role)
                        );

                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                username + ":" + userId, // principal: 包含用户名和ID
                                null, // credentials: 不需要密码
                                authorities
                            );

                        // 设置到Security上下文中
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("Set authentication for user: {}", username);
                    } else {
                        log.warn("Inactive user attempted access with valid token: {}", username);
                    }
                });
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            // 不抛出异常，让请求继续处理
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取JWT Token
     *
     * @param request HTTP请求
     * @return JWT Token字符串，如果不存在则返回null
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return jwtTokenUtil.extractTokenFromHeader(bearerToken);
    }

    /**
     * 判断是否需要跳过过滤的路径
     * 这些路径不需要JWT验证
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 跳过不需要认证的路径
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/validate") ||
               path.startsWith("/api/posts/") ||
               path.startsWith("/api/categories/") ||
               path.startsWith("/api/tags/") ||
               path.startsWith("/api/settings/public") ||
               path.startsWith("/error") ||
               path.startsWith("/actuator");
    }
}