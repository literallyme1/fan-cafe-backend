package com.example.fan_cafe.global.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JaksonTrimConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer addStringTrimmer() {
        return builder -> builder.deserializerByType(
                String.class,
                new StdScalarDeserializer<String>(String.class) {
                    @Override
                    public String deserialize(JsonParser p, DeserializationContext c) throws IOException {
                        String v = p.getValueAsString();
                        return v == null ? null : v.trim();
                    }
                    @Override
                    public String getEmptyValue(DeserializationContext c) {
                        return null; // "" -> null
                    }
                }
        );
    }
}
