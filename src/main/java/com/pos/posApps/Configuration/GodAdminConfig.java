package com.pos.posApps.Configuration;

import com.pos.posApps.Service.GodAdminCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GodAdminConfig {
    @Bean
    public GodAdminCredentials godAdminCredentials(
            @Value("${god-admin.username:}") String username,
            @Value("${god-admin.password:}") String password
    ) {
        return new GodAdminCredentials(username, password);
    }
}
