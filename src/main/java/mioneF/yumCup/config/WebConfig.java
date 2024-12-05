package mioneF.yumCup.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://yumcup.store",      // 프론트엔드 도메인
                        "http://localhost:3000"       // 개발 환경
                )
                .allowedMethods("GET", "POST", "OPTIONS")  // OPTIONS 메서드 추가
                .allowedHeaders("*")
                .exposedHeaders("*")              // 추가
                .allowCredentials(true);
    }
}
