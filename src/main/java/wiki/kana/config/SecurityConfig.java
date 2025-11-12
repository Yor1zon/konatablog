package wiki.kana.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security 配置类
 * 配置密码加密器和其他安全相关组件
 */
@Configuration
public class SecurityConfig {

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
}
