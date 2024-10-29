FROM ghcr.io/navikt/baseimages/temurin:21

COPY export-vault-secrets.sh /init-scripts/10-export-vault-secrets.sh
COPY app/target/app.jar /app/app.jar
