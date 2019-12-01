#!/usr/bin/env bash

# Usage info
show_help() {
cat << EOF
Usage: ${0##*/} [-h] [-c cluster port] [-g gRPC port] [-j core jar location]
Start a new node.

    -h          display this help and exit
    -c PORT     Port used for Artery to communicate with the cluster.
    -g PORT     gRPC port exposed to clients.
    -j          jar location. Default core-assembly-0.0.1-SNAPSHOT.jar


EOF
}

if [[ "$#" -eq 0 ]]; then
    show_help
    exit 1
fi

cluster_port=0
grpc_port=8080
jar=core-assembly-0.0.1-SNAPSHOT.jar

while getopts hc:g:j: opt; do
    case ${opt} in
        h)
            show_help
            exit 0
            ;;
        c)  cluster_port=$OPTARG
            ;;
        g)  grpc_port=$OPTARG
            ;;
        j)  jar=$OPTARG
            ;;
        *)
            show_help >&2
            exit 1
            ;;
    esac
done

java \
    -Xms4G  -Xmx6G  \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=0 \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dakka.remote.artery.canonical.port=${cluster_port} \
    -Dsimplexportal.spatial.api.http.port=${grpc_port} \
    -Dakka.loglevel=INFO \
    -jar ${jar}