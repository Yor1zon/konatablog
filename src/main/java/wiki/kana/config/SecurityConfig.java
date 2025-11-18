package wiki.kana.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 配置类
 * 配置密码编码器、JWT认证和API访问控制
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置密码编码器
     * 使用BCrypt算法 - 安全性高，自带盐值
     *
     * @return BCryptPasswordEncoder实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder是最安全的密码编码器
        // 支持自适应哈希，自带盐值，防止彩虹表攻击
        return new BCryptPasswordEncoder(12); // 强度为12，越高越安全但越慢
    }

    /**
     * 配置CORS
     * 允许前端跨域访问
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // 生产环境应该配置具体域名
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 配置安全过滤链
     * 设置访问权限和JWT认证
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（JWT应用通常不需要）
                .csrf(csrf -> csrf.disable())

                // 启用CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 设置会话管理为无状态
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置访问权限
                .authorizeHttpRequests(authz -> authz
                        // 允许访问的公共接口
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/validate",
                                "/api/posts/**",
                                "/api/categories/**",
                                "/api/tags/**",
                                "/api/settings/public"
                        ).permitAll()

                        // 需要认证的接口
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/media/**",
                                "/api/settings/**",
                                "/api/themes/**"
                        ).authenticated()

                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                )

                // 配置异常处理
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=utf-8");
                            response.getWriter().write("""
                                    {
                                        "success": false,
                                        "error": {
                                            "code": "UNAUTHORIZED",
                                            "message": "未认证或Token已过期"
                                        }
                                    }
                                    """);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=utf-8");
                            response.getWriter().write("""
                                    {
                                        "success": false,
                                        "error": {
                                            "code": "FORBIDDEN",
                                            "message": "权限不足"
                                        }
                                    }
                                    """);
                        })
                )

                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
