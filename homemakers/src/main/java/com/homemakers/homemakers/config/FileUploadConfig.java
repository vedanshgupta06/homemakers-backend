package com.homemakers.homemakers.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String providerPath = Paths.get("uploads/providers")
                .toFile()
                .getAbsolutePath();

        String documentPath = Paths.get("uploads/provider-documents")
                .toFile()
                .getAbsolutePath();

        registry.addResourceHandler("/providers/**")
                .addResourceLocations("file:" + providerPath + "/");

        registry.addResourceHandler("/provider-documents/**")
                .addResourceLocations("file:" + documentPath + "/");
    }
}