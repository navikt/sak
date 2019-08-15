FROM navikt/common:0.1 AS navikt-common

COPY files/run_nginx.sh /run-script.sh

COPY --from=navikt-common /init-scripts /init-scripts
COPY --from=navikt-common /entrypoint.sh /entrypoint.sh
COPY --from=navikt-common /dumb-init /dumb-init

RUN chmod +x /entrypoint.sh
RUN rm -rf /etc/nginx/conf.d/*

COPY files/env_config.nginx /etc/nginx/conf.d/env_config.nginx

ENTRYPOINT ["/dumb-init", "--", "/entrypoint.sh"]
