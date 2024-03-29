package com.xuni.api.auth.config;

import com.xuni.api.auth.presentation.*;
import com.xuni.api.auth.presentation.filter.ActuatorFilter;
import com.xuni.api.auth.presentation.filter.AdminInterceptor;
import com.xuni.api.auth.presentation.filter.JwtAuthInterceptor;
import com.xuni.api.auth.application.jwt.JwtTokenManager;
import jakarta.servlet.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
    import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Slf4j
//@Profile("default")
@Configuration
public class AuthInterceptorConfig implements WebMvcConfigurer{

    private final JwtTokenManager jwtTokenManager;

    public AuthInterceptorConfig(JwtTokenManager jwtTokenManager) {
        this.jwtTokenManager = jwtTokenManager;
    }

    @Bean
    public FilterRegistrationBean actuatorFilter() {
        FilterRegistrationBean<Filter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
        filterFilterRegistrationBean.setFilter(new ActuatorFilter(jwtTokenManager));
        return filterFilterRegistrationBean;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("[Register JwtAuthInterceptor]");
        registry.addInterceptor(new JwtAuthInterceptor(jwtTokenManager))
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/**")
                .excludePathPatterns("/h2-console/**")
                .order(1);

        log.info("[Register AdminInterceptor]");
        registry.addInterceptor(new AdminInterceptor(jwtTokenManager))
                .addPathPatterns("/**")
                .excludePathPatterns("/docs/**")
                .excludePathPatterns("/favicon.ico")
                .excludePathPatterns("/auth/**")
                .excludePathPatterns("/h2-console/**")
                .order(2);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthenticatedMemberArgumentResolver(jwtTokenManager));
        resolvers.add(new OptionalAuthenticationArgumentResolver(jwtTokenManager));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3030")
                .exposedHeaders("Authorization")
                .allowCredentials(true) // 클라이언트에서 Authorization 헤더를 보낼 때는 붙이는게 규정같음
                .allowedMethods("GET","POST","PATCH","PUT","DELETE","OPTIONS");
    }
}