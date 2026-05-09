package com.homemakers.homemakers.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/providers/**")
                .addResourceLocations("file:uploads/providers/");

        registry.addResourceHandler("/provider-documents/**")
                .addResourceLocations("file:uploads/provider-documents/");
    }
}