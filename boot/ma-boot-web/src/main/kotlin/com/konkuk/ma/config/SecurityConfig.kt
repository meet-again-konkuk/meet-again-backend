package com.konkuk.ma.config

import com.konkuk.ma.support.security.JwtAuthenticationFilter
import com.konkuk.ma.support.security.RoutingAwareEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,

    private val entryPoint: RoutingAwareEntryPoint
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(entryPoint) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(HttpMethod.POST,"/api/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST,"/api/auth/find-email").permitAll()
                    .requestMatchers(HttpMethod.POST,"/api/auth/find-password").permitAll()
                    .requestMatchers(HttpMethod.POST,"/api/auth/refresh-token").permitAll()
                    .requestMatchers(HttpMethod.POST,"/api/sms/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/sign-up").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/members/nickname/exists").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/members/email/exists").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/members/withdrawal/cancellation").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/members/regions").permitAll()
                    .requestMatchers(HttpMethod.GET, "/files/memory/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/files/community/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/files/member/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/points").authenticated()
                    .requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
            }
            .anonymous { it.disable() }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
