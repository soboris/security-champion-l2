package com.secchamp.officedemo.config;

import dev.openfga.sdk.api.client.OpenFgaClient;
import dev.openfga.sdk.api.configuration.ClientConfiguration;
import dev.openfga.sdk.errors.FgaInvalidParameterException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenFgaConfig {

    @Value("${openfga.api.url}")
    private String openFgaApiUrl;

    @Bean
    public OpenFgaClient openFgaClient() throws FgaInvalidParameterException {
        ClientConfiguration config = new ClientConfiguration()
                .apiUrl(openFgaApiUrl);
        
        return new OpenFgaClient(config);
    }
}