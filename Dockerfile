
FROM nginx:stable-alpine

RUN HTTP_PROXY=http://webproxy-utvikler.nav.no:8088 apk add --no-cache gettext
RUN HTTP_PROXY=http://webproxy-utvikler.nav.no:8088 apk add --no-cache python3

COPY files/*.py /

RUN mkdir /conf.d
COPY files/conf.d/* /conf.d/

RUN PROXY_MESSAGE_FORMAT=$(python3 escape_string.py /conf.d/proxy_message_format.txt) \
python3 envsubst.py /conf.d/json_log_format.json

RUN JSON_LOG_FORMAT=$(python3 escape_string.py /conf.d/json_log_format.json) \
python3 envsubst.py /conf.d/00_log.conf

RUN rm -rf /etc/nginx/conf.d/*
RUN cp /conf.d/*.conf /etc/nginx/conf.d/
RUN cp /conf.d/*.nginx /etc/nginx/conf.d/

COPY files/run_nginx.sh /run_nginx.sh
RUN chmod +x /run_nginx.sh

CMD "/run_nginx.sh"
