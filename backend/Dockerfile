# ========================
# ArchiGenTN Backend - Dockerfile (Multi-stage)
# ========================
# Build: docker build -t archigentn-backend .
# Run:   docker run -p 8080:8080 archigentn-backend

# --- Stage 1: Build ---
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copier Maven wrapper et pom.xml d'abord (cache des dependances)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Telecharger les dependances (cache Docker layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copier le code source
COPY src src

# Build le JAR (sans tests - ils sont executes dans la CI)
RUN ./mvnw package -DskipTests -B

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Utilisateur non-root pour securite
RUN addgroup -S archigen && adduser -S archigen -G archigen

# Copier le JAR depuis le builder
COPY --from=builder /app/target/*.jar app.jar

# Changer le proprietaire
RUN chown archigen:archigen app.jar

USER archigen

# Port expose
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Lancer l'application avec profil docker
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
