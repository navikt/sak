FROM nginx:stable-alpine

COPY files/run_nginx.sh /run_nginx.sh

RUN chmod +x /run_nginx.sh
RUN rm -rf /etc/nginx/conf.d/*

COPY files/conf.d/* /etc/nginx/conf.d/

RUN JSON_MESSAGE_FORMAT=$(awk '{printf "%s\\\\n", $0}' /etc/nginx/conf.d/proxy_message_format.txt) \
    envsubst < /etc/nginx/conf.d/json_log_format.txt \
    > /etc/nginx/conf.d/json_log_format.txt

RUN apk add --no-cache jq
RUN jq -c . < /etc/nginx/conf.d/json_log_format.txt > /etc/nginx/conf.d/json_log_format.txt

RUN PROXY_LOG_FORMAT=$(awk '{printf "%s\\\\n", $0}' /etc/nginx/conf.d/json_log_format.txt) \
    envsubst < /etc/nginx/conf.d/00_log.conf \
    > /etc/nginx/conf.d/00_log.conf

CMD "/run_nginx.sh"
