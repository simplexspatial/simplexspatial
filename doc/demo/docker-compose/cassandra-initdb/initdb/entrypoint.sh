#!/bin/bash
set -e

host="$1"

while ! cqlsh -e 'describe cluster' $host ; do
    sleep 3
done
sleep 3

echo "Cassandra is ready - Creating schema for akka persistence"
cqlsh -f ./schema.cql $host
echo "Akka persistence schema created."

