set -e
cd /etc/nginx/conf.d

envsubst '$DOKARKIV_SAK_REST_URL' < env_config.nginx > /etc/nginx/conf.d/default.conf

nginx -g 'daemon off;'
