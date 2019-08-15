FROM nginx:stable-alpine

COPY files/run_nginx.sh /run_nginx.sh

RUN chmod +x /run_nginx.sh
RUN rm -rf /etc/nginx/conf.d/*

COPY files/env_config.nginx /etc/nginx/conf.d/env_config.nginx

CMD "/run_nginx.sh"
