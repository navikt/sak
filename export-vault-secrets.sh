#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/sakDS/username;
then
    echo "Setting SAKDS_USERNAME"
    export SAKDS_USERNAME=$(cat /var/run/secrets/nais.io/sakDS/username)
fi

if test -f /var/run/secrets/nais.io/sakDS/password;
then
    echo "Setting SAKDS_PASSWORD"
    export SAKDS_PASSWORD=$(cat /var/run/secrets/nais.io/sakDS/password)
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
