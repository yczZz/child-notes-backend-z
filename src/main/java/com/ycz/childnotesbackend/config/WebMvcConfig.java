package com.ycz.childnotesbackend.config;

import com.ycz.childnotesbackend.interceptor.ApiRateLimitInterceptor;
import com.ycz.childnotesbackend.interceptor.AdminAuthInterceptor;
import com.ycz.childnotesbackend.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    private final AdminAuthInterceptor adminAuthInterceptor;

    private final ApiRateLimitInterceptor apiRateLimitInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor,
                        AdminAuthInterceptor adminAuthInterceptor,
                        ApiRateLimitInterceptor apiRateLimitInterceptor) {
        this.authInterceptor = authInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.apiRateLimitInterceptor = apiRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiRateLimitInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/wx-login");
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/api/**")
                .excludePathPatterns("/admin/api/auth/login");
    }
}
