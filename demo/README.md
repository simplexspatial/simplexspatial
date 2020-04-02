# Docker Compose environment

This is an environment that can be used to demo and testing.

It is using [Docker Compose 3.7](https://docs.docker.com/compose/), so it is necessary at least Docker v18.06.0+

The environment contains a Cassandra cluster to store Akka persistence journal and snapshots. The data is stored internally
in the container, so it will be a clean after remove the container.

## Start environment
```ssh
# Start
docker-compose  up -d

# Follow logs
docker-compose logs -f

# Shutdown
docker-compose down

# List services (docker containers) running
docker-compose ps

# Scale Cassandra cluster
docker-compose up -d --scale cassandra=2

# Access to the Cassandra CQL
docker exec -it demo_cassandra_1 cqlsh

# Check Cassandra cluster
docker exec -it demo_cassandra_1 nodetool status
```

## Start Simplexspatial nodes
ode 1:
```ssh
bin/simplexspatial-core \
    -java-home /usr/lib/jvm/java-8-openjdk-amd64 \
    -jvm-debug 9010 \
    -J-Xms1G \
    -J-Xmx4G  \
    -Dconfig.file=conf/application-cassandra.conf\
    -Dakka.remote.artery.canonical.port=2550  \
    -Dsimplexportal.spatial.entrypoint.grpc-web.port=6080 \
    -Dsimplexportal.spatial.entrypoint.grpc.port=7080 \
    -Dsimplexportal.spatial.entrypoint.restful.port=8080
```

Node 2:
```ssh
bin/simplexspatial-core \
    -java-home /usr/lib/jvm/java-8-openjdk-amd64 \
    -jvm-debug 9011 \
    -J-Xms1G \
    -J-Xmx4G  \
    -Dconfig.file=conf/application-cassandra.conf\
    -Dakka.remote.artery.canonical.port=2551  \
    -Dsimplexportal.spatial.entrypoint.grpc-web.port=6081 \
    -Dsimplexportal.spatial.entrypoint.grpc.port=7081 \
    -Dsimplexportal.spatial.entrypoint.restful.port=8081
```

For other nodes, we can set the port as 0, so Akka will use a random free port:
```ssh
bin/simplexspatial-core \
    -java-home /usr/lib/jvm/java-8-openjdk-amd64 \
    -J-Xms1G \
    -J-Xmx4G  \
    -Dconfig.file=conf/application-cassandra.conf\
    -Dakka.remote.artery.canonical.port=0  \
    -Dsimplexportal.spatial.entrypoint.grpc-web.port=0 \
    -Dsimplexportal.spatial.entrypoint.grpc.port=0 \
    -Dsimplexportal.spatial.entrypoint.restful.port=0
```