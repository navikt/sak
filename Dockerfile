FROM navikt/java:11-appdynamics
ENV APPD_ENABLED=true
COPY export-vault-secrets.sh /init-scripts/10-export-vault-secrets.sh
COPY target/sak.jar app.jar
