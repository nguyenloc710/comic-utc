# Build nhiều tầng: tầng 1 đóng gói jar bằng Maven, tầng 2 chỉ chứa JRE + jar.
# Image mặc định lấy qua mirror ECR Public vì Docker Hub có thể bị chặn tải blob trên mạng nội bộ;
# mạng nào không vào được ECR thì dựng với --build-arg IMAGE_REGISTRY=docker.io/library
ARG IMAGE_REGISTRY=public.ecr.aws/docker/library

FROM ${IMAGE_REGISTRY}/maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Tải thư viện trước khi chép mã nguồn để lớp cache này giữ nguyên khi chỉ sửa code
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Test cần Docker (Testcontainers) nên chạy ở bước verify riêng, không chạy trong lúc dựng image
RUN mvn -q -B -DskipTests package

FROM ${IMAGE_REGISTRY}/eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 comic && mkdir -p /app/uploads /app/logs && chown -R comic /app
COPY --from=build /workspace/target/comic-utc-*.jar app.jar
USER comic
ENV SPRING_PROFILES_ACTIVE=prod STORAGE_ROOT=/app/uploads LOG_FILE=/app/logs/app.log TZ=Asia/Ho_Chi_Minh
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
