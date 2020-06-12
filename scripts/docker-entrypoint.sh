#!/bin/bash

set -e

CGROUP_MEMORY_LIMIT_FILE="/sys/fs/cgroup/memory/memory.limit_in_bytes"
if [ -f $CGROUP_MEMORY_LIMIT_FILE ]; then
    MAXRAM=$(cat ${CGROUP_MEMORY_LIMIT_FILE})
else
    echo "This script is designed to run inside docker only, exiting..."
    exit 1
fi

TOTAL_MEMORY=$(($(cat /proc/meminfo  |head -n 1 |awk '{print $2}')*1024))

if [ "${MAXRAM}" -lt "${TOTAL_MEMORY}" ]; then
    XMXPERCENT="${XMXPERCENT:-80}"
    XMX=$(($MAXRAM-$MAXRAM/100*(100-$XMXPERCENT)))
    XMX_CONFIG="-J-Xmx${XMX} -J-Xms${XMX}"
fi

export SERVICE_INSTANCE_ID=$(tr -cd 'a-f0-9' < /dev/urandom | fold -w8 | head -n1)

# http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html#instancedata-user-data-retrieval
#export HOST_ID=$(curl -s --connect-timeout 5 http://169.254.169.254/latest/meta-data/instance-id)
#curl -s --connect-timeout 5 http://169.254.169.254/latest/meta-data/ |grep public-ipv4 && export PUBLIC_IPV4=$(curl -s --connect-timeout 5 http://169.254.169.254/latest/meta-data/public-ipv4)

# set default log level to INFO if not set already
DEFAULT_LOG_LEVEL="${DEFAULT_LOG_LEVEL:-INFO}"

# bind akka to external interface
export AKKA_BIND_HOST_NAME=${AKKA_BIND_HOST_NAME:-"0.0.0.0"}

# if $HOST variable exists we are on DCOS - use it as host IP. Otherwise assume we are on AWS ECS.
# DOCKER_HOST_IP=${HOST:-$(curl -s --connect-timeout 5 http://169.254.169.254/latest/meta-data/local-ipv4 || true)}

AKKA_HOST_NAME=${DOCKER_HOST_IP}
# if none of above worked - probably we are in local dev env, don't use clustering.
[ -z ${AKKA_HOST_NAME} ] && AKKA_HOST_NAME="127.0.0.1"
export AKKA_HOST_NAME

if [ ! -z "${LOG_HOST}" ] && [ ! -z "${LOG_PORT}" ]; then
    LOGGING_CONFIG="-DLOG_HOST=${LOG_HOST} -DLOG_PORT=${LOG_PORT} -DAPP_ENV=${APP_ENV} -DAPP_NAME=${APP_NAME} -DSERVICE_INSTANCE_ID=${SERVICE_INSTANCE_ID} -DHOST_ID=${HOST_ID} -DDEFAULT_LOG_LEVEL=${DEFAULT_LOG_LEVEL}"
fi

JAVA_OPTS=${JAVA_OPTS}" -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9010 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

[ ${RMI_EXTERNAL_IP} ] && JAVA_OPTS=${JAVA_OPTS}" -Djava.rmi.server.hostname=${DOCKER_HOST_IP}"
export JAVA_OPTS

echo "Calling the entry-point"
echo "Starting the JVM process"
# Start the JVM (passed as CMD)
command="$@"
exec $command ${LOGGING_CONFIG} ${XMX_CONFIG}
