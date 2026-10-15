package com.pos.posApps.Configuration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

@Component
public class StaffHrSchemaInitializer implements ApplicationRunner {
    private final DataSource dataSource;

    public StaffHrSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String sql = new String(
                new ClassPathResource("sql/2026-10-13-staff-hr.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
        List<String> statements = RoleMenuSchemaInitializer.splitStatements(sql);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String item : statements) {
                statement.execute(item);
            }
        }
    }
}
