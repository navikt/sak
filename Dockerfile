FROM navikt/java:11
ENV APPD_ENABLED=true
COPY export-vault-secrets.sh /init-scripts/10-export-vault-secrets.sh
COPY target/sak-1.0-SNAPSHOT.jar app.jar
