FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

COPY . .

RUN apt-get update && apt-get install -y --no-install-recommends \
    texlive-latex-base \
    texlive-fonts-recommended \
    texlive-science \
    dvisvgm \
    && rm -rf /var/lib/apt/lists/*

EXPOSE 8081

ENTRYPOINT ["java","--add-opens=java.base/java.lang=ALL-UNNAMED","-jar","protterServer.jar"]