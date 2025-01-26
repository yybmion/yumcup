package mioneF.yumCup.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final long MAX_AGE = 3600L;

    private static final String[] ALLOWED_ORIGINS = {
            "https://yumcup.store",
            "https://www.yumcup.store",
            "http://localhost:3000",
            "https://yumcup-omega.vercel.app"
    };

    private static final String[] ALLOWED_METHODS = {
            "GET", "POST", "OPTIONS"
    };

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(ALLOWED_ORIGINS)
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(MAX_AGE); // preflight 캐시 시간 설정
    }
}
