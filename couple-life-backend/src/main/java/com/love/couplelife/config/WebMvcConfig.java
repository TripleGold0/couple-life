package com.love.couplelife.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置。
 * <ul>
 *     <li>开启全局 CORS（与 SecurityConfig 中的 CORS 配置互补，便于本地联调）</li>
 *     <li>把上传目录映射到 {@code /uploads/**} 静态路径，前端可直接通过 URL 访问图片</li>
 * </ul>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 上传根目录，由 application.yml 中的 app.upload-dir 配置 */
    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * 把磁盘上的 {@code ${app.upload-dir}} 目录映射成 HTTP 路径 {@code /uploads/**}。
     * 注意：location 必须以 "file:" 协议前缀 + 末尾 "/" 结尾，否则不生效。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
