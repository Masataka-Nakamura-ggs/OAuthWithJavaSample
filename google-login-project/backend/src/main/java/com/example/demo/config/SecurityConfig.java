package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Securityの設定クラス。
 * <p>
 * ・CSRF無効化
 * ・セッション管理をステートレス化
 * ・認証不要APIの指定
 * などを行う。
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Spring Securityのフィルタチェーン設定。
     * <p>
     * ・CSRF保護を無効化
     * ・セッション管理をステートレスに
     * ・/api/auth/** へのアクセスは認証不要
     * ・その他は認証必須
     * </p>
     * @param http HttpSecurityオブジェクト
     * @return SecurityFilterChain
     * @throws Exception 設定失敗時
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // CSRF保護を無効化
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS) // セッション管理をステートレス化
            .and()
            .authorizeRequests()
                .antMatchers("/api/auth/**").permitAll() // 認証不要API
                .anyRequest().authenticated(); // その他は認証必須

        return http.build();
    }
}
