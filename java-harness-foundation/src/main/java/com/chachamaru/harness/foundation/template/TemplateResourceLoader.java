package com.chachamaru.harness.foundation.template;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class TemplateResourceLoader {

    private TemplateResourceLoader() {
    }

    public static String load(String resourcePath) {
        try (InputStream inputStream = TemplateResourceLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Template resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load template resource: " + resourcePath, e);
        }
    }
}
