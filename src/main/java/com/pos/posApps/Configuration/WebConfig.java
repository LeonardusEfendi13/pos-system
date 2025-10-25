package com.pos.posApps.Configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map all /uploads/** URLs to the physical folder on disk
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/"); // this must match where you save the files
    }
}
