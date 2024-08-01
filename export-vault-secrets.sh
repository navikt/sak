#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/db_creds/username;
then
    echo "Setting SPRING_DATASOURCE_USERNAME"
    export SPRING_DATASOURCE_USERNAME=$(cat /var/run/secrets/nais.io/db_creds/username)
fi

if test -f /var/run/secrets/nais.io/db_creds/password;
then
    echo "Setting SPRING_DATASOURCE_PASSWORD"
    export SPRING_DATASOURCE_PASSWORD=$(cat /var/run/secrets/nais.io/db_creds/password)
fi

if test -f /var/run/secrets/nais.io/db_config/jdbc_url;
then
    export SPRING_DATASOURCE_URL=$(cat /var/run/secrets/nais.io/db_config/jdbc_url)
    echo "Setting SPRING_DATASOURCE_URL=$SPRING_DATASOURCE_URL"
fi

if test -f /var/run/secrets/nais.io/db_config/ons_host;
then
    export DATABASE_ONSHOSTS=$(cat /var/run/secrets/nais.io/db_config/ons_host)
    echo "Setting DATABASE_ONSHOSTS=$DATABASE_ONSHOSTS"
fi

if test -f /var/run/secrets/nais.io/ldap/username;
then
    echo "Setting LDAP_USERNAME"
    export LDAP_USERNAME=$(cat /var/run/secrets/nais.io/ldap/username)
fi

if test -f /var/run/secrets/nais.io/ldap/password;
then
    echo "Setting LDAP_PASSWORD"
    export LDAP_PASSWORD=$(cat /var/run/secrets/nais.io/ldap/password)
fi

if test -f /var/run/secrets/nais.io/srvsak/username;
then
    echo "Setting SRVSAK_USERNAME"
    export SRVSAK_USERNAME=$(cat /var/run/secrets/nais.io/srvsak/username)
fi

if test -f /var/run/secrets/nais.io/srvsak/password;
then
    echo "Setting SRVSAK_PASSWORD"
    export SRVSAK_PASSWORD=$(cat /var/run/secrets/nais.io/srvsak/password)
fi
