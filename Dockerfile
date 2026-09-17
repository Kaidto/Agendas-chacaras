# -------------------------------------------------------------
# Estágio 1: Build da Aplicação com Maven e JDK 21
# -------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copiar pom.xml primeiro para cache eficiente de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fonte e gerar pacote final
COPY src ./src
RUN mvn clean package -DskipTests -B

# -------------------------------------------------------------
# Estágio 2: Imagem Final de Execução (Leve e Segura)
# -------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Criar usuário sem privilégios de root para segurança da aplicação
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar o JAR gerado no estágio de build
COPY --from=builder /build/target/agendamento-chacaras.jar app.jar

# Criar diretório para persistência H2 e uploads de fotos
RUN mkdir -p /app/data /app/uploads && chown -R appuser:appgroup /app

USER appuser

# Porta padrão lida pelo Render (variável $PORT)
ENV PORT=8080
EXPOSE ${PORT}

# Configuração de JVM otimizada para containers com baixo consumo de memória
ENTRYPOINT ["sh", "-c", "java -XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-8080} -jar app.jar"]
