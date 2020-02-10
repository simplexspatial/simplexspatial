#!/usr/bin/env bash

# Usage info
show_help() {
cat << EOF
Usage: ${0##*/} [-h] [-c cluster port] [-g gRPC port]
Start a new node.

    -h          display this help and exit
    -a PORT     Port used for Artery to communicate with the cluster.
    -g PORT     gRPC port exposed to clients.


EOF
}

if [[ "$#" -eq 0 ]]; then
    show_help
    exit 1
fi

cluster_port=0
grpc_port=8080
config=./conf/application.conf

while getopts ha:g: opt; do
    case ${opt} in
        h)
            show_help
            exit 0
            ;;
        a)  cluster_port=$OPTARG
            ;;
        g)  grpc_port=$OPTARG
            ;;
        *)
            show_help >&2
            exit 1
            ;;
    esac
done

cd "$(dirname "$0")"/..

echo "Starting node listening all interfaces and ports gRPC$ [${grpc_port}] and Artery [${cluster_port}]"
bin/simplexspatial-core \
    -Dakka.remote.artery.canonical.port=${cluster_port} \
    -Dsimplexportal.spatial.api.http.port=${grpc_port}