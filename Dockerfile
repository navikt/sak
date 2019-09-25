FROM navikt/java:8-appdynamics
ENV APPD_ENABLED=true
COPY target/sak.jar app.jar
