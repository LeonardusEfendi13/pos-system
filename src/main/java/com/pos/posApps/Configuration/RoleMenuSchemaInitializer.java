package com.pos.posApps.Configuration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
public class RoleMenuSchemaInitializer implements ApplicationRunner {
    private final DataSource dataSource;

    public RoleMenuSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String sql = new String(
                new ClassPathResource("sql/2026-10-09-role-menu.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
        List<String> statements = splitStatements(sql);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String item : statements) {
                statement.execute(item);
            }
        }
    }

    static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String statement = current.toString().trim();
                if (statement.endsWith(";")) {
                    statement = statement.substring(0, statement.length() - 1).trim();
                }
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                current.setLength(0);
            }
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }
        return statements;
    }
}
