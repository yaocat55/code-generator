package cn.net.yao.generate.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer streamReadConstraints() {
        JsonFactory factory = new JsonFactory();
        factory.setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(100_000_000) // 100MB
                        .build());
        return builder -> builder.factory(factory);
    }
}
