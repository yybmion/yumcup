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
                        "https://yumcup.store",
                        "https://www.yumcup.store",
                        "http://localhost:3000",
                        "https://yumcup-omega.vercel.app"  // 끝의 슬래시 제거
                )
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // preflight 캐시 시간 설정
    }
}
