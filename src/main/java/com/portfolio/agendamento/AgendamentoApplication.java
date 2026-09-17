package com.portfolio.agendamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;

@SpringBootApplication
public class AgendamentoApplication {

    public static void main(String[] args) {
        configurarAmbienteNuvem();
        SpringApplication.run(AgendamentoApplication.class, args);
    }

    /**
     * Detecta e adapta automaticamente o formato da variável DATABASE_URL do Render / Heroku / Neon,
     * removendo espaços em branco acidentais e configurando as credenciais e o driver PostgreSQL
     * de forma limpa e transparente.
     */
    private static void configurarAmbienteNuvem() {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl != null) {
            dbUrl = dbUrl.trim();
        }

        String dbUser = System.getenv("DB_USER");
        if (dbUser != null && !dbUser.isBlank()) {
            System.setProperty("spring.datasource.username", dbUser.trim());
        }

        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword != null && !dbPassword.isBlank()) {
            System.setProperty("spring.datasource.password", dbPassword.trim());
        }

        if (dbUrl != null && !dbUrl.isBlank()) {
            try {
                if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://")) {
                    String cleanUrl = dbUrl.replaceFirst("^postgres(ql)?://", "http://");
                    URI uri = URI.create(cleanUrl);

                    String host = uri.getHost();
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath();
                    String query = uri.getQuery();

                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                    if (query != null && !query.isBlank()) {
                        jdbcUrl += "?" + query;
                    }

                    System.setProperty("spring.datasource.url", jdbcUrl);

                    if (uri.getUserInfo() != null) {
                        String[] parts = uri.getUserInfo().split(":", 2);
                        System.setProperty("spring.datasource.username", parts[0]);
                        if (parts.length > 1) {
                            System.setProperty("spring.datasource.password", parts[1]);
                        }
                    }
                    System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
                } else if (!dbUrl.startsWith("jdbc:")) {
                    String jdbcUrl = "jdbc:" + dbUrl;
                    System.setProperty("spring.datasource.url", jdbcUrl);
                    if (dbUrl.contains("postgres")) {
                        System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
                    }
                } else {
                    System.setProperty("spring.datasource.url", dbUrl);
                    if (dbUrl.contains("postgresql")) {
                        System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
                    }
                }
            } catch (Exception e) {
                System.err.println("[Aviso] Falha ao processar DATABASE_URL: " + e.getMessage());
            }
        }
    }
}
