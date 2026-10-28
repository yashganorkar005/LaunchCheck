package com.launchcheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchcheck.audit.safety.AddressResolver;
import com.launchcheck.audit.safety.GlobalAddressPolicy;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import java.net.InetAddress;
import java.util.Arrays;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class LaunchCheckApplication {
    public static void main(String[] args) {
        SpringApplication.run(LaunchCheckApplication.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public UrlSafetyPolicy urlSafetyPolicy() {
        AddressResolver defaultResolver = host -> Arrays.asList(InetAddress.getAllByName(host));
        return new UrlSafetyPolicy(new GlobalAddressPolicy(), defaultResolver);
    }
}

