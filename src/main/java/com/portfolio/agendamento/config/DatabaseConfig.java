package com.portfolio.agendamento.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Configuração flexível de DataSource:
 * 1. Converte automaticamente DATABASE_URL nos formatos postgres:// e postgresql:// (usados por Render, Neon, Supabase)
 *    para o formato exigido pelo driver JDBC (jdbc:postgresql://...).
 * 2. Suporta conexão direta via jdbc:postgresql://.
 * 3. Faz fallback automático para banco H2 local caso DATABASE_URL não seja fornecida.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:}")
    private String configuredUrl;

    @Value("${spring.datasource.username:sa}")
    private String defaultUsername;

    @Value("${spring.datasource.password:}")
    private String defaultPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // Obtém a URL do application.yml ou diretamente da variável de ambiente DATABASE_URL
        String rawUrl = (configuredUrl != null && !configuredUrl.isBlank()) 
                ? configuredUrl 
                : System.getenv("DATABASE_URL");

        if (rawUrl != null && !rawUrl.isBlank()) {
            String trimmedUrl = rawUrl.trim();

            // Render / Neon / Supabase fornecem URLs no formato:
            // postgres://user:password@host:port/database ou postgresql://...
            if (trimmedUrl.startsWith("postgres://") || trimmedUrl.startsWith("postgresql://")) {
                try {
                    log.info("[DatabaseConfig] Detectada URL PostgreSQL de nuvem (Render/Neon). Formatando para JDBC...");
                    // Substitui o protocolo por algo que java.net.URI parseie confiavelmente
                    String normalized = trimmedUrl.replaceFirst("^postgres(ql)?://", "http://");
                    URI uri = new URI(normalized);

                    String userInfo = uri.getUserInfo();
                    String username = defaultUsername;
                    String password = defaultPassword;

                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        username = parts[0];
                        password = parts[1];
                    } else if (userInfo != null) {
                        username = userInfo;
                    }

                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath();
                    String dbName = (path != null && path.length() > 1) ? path.substring(1) : "";

                    String query = uri.getQuery();
                    StringBuilder jdbcUrl = new StringBuilder();
                    jdbcUrl.append("jdbc:postgresql://").append(uri.getHost()).append(":").append(port).append("/").append(dbName);

                    if (query != null && !query.isBlank()) {
                        jdbcUrl.append("?").append(query);
                    } else {
                        jdbcUrl.append("?sslmode=require");
                    }

                    config.setJdbcUrl(jdbcUrl.toString());
                    config.setUsername(username);
                    config.setPassword(password);
                    config.setDriverClassName("org.postgresql.Driver");
                    
                    // Otimizações para plano gratuito Render (baixo consumo de conexões)
                    config.setMaximumPoolSize(5);
                    config.setMinimumIdle(1);
                    config.setIdleTimeout(30000);
                    config.setMaxLifetime(60000);
                    config.setConnectionTimeout(30000);

                    log.info("[DatabaseConfig] Conexão PostgreSQL configurada para o host: {}", uri.getHost());
                    return new HikariDataSource(config);
                } catch (Exception e) {
                    log.error("[DatabaseConfig] Erro ao converter URL da nuvem ({}): {}. Tentando conexão como URL direta.", trimmedUrl, e.getMessage());
                }
            } else if (trimmedUrl.startsWith("jdbc:postgresql:")) {
                log.info("[DatabaseConfig] Usando URL JDBC PostgreSQL direta.");
                config.setJdbcUrl(trimmedUrl);
                config.setUsername(defaultUsername);
                config.setPassword(defaultPassword);
                config.setDriverClassName("org.postgresql.Driver");
                config.setMaximumPoolSize(5);
                return new HikariDataSource(config);
            }
        }

        // Fallback para H2 (desenvolvimento local sem configurações adicionais)
        String h2Url = (rawUrl != null && rawUrl.startsWith("jdbc:h2:"))
                ? rawUrl
                : "jdbc:h2:file:./data/agendamento;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE";

        log.info("[DatabaseConfig] Inicializando banco H2 local: {}", h2Url);
        config.setJdbcUrl(h2Url);
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }
}
