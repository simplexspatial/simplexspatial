# simplex-spatial

Comming soon!!

## Other documentation

- [Architecture and Design documentation](doc/architecture.md)
- [Performance documentation](doc/performance.md)


## Package and run
The following command will generate two distributable packages, one located
under `core/target/universal` and another under `osm-loader/target/universal`:

```bash
sbt clean universal:packageZipTarball
```

It will generate a 50M tar `{source_root}/core/target/universal/simplex-spatial-core-<version>.tgz`
with all the necessary stuff to start a cluster node.

It is supposed that you have a JDK8 or higher installed in your system.

To run a node, uncompress the tar file, and move into the new folder:
```bash
tar -xvf simplex-spatial-core-<version>.tgz
cd simplex-spatial-core-<version>/
bin/simple_start_node.sh -a 2550 -g 8080
```

## Running thru sbt
To create a package with all necessary inside, execute the follow command:
```bash
sbt "core/runMain com.simplexportal.spatial.Main"
```

```bash
sbt "loadOSM/runMain com.simplexportal.spatial.loadosm.Main --block-size=300 /home/angelcerveraclaudio/Downloads/osm/ireland-and-northern-ireland-latest.osm.pbf"
```

## Running thru CLI
Using the previous zip, uncompress it and from the folder where you
uncompressed:

### Running core

```bash
bin/simplex-spatial-core \
    -java-home /usr/lib/jvm/java-8-openjdk-amd64 \
    -jvm-debug 9010 \
    -J-Xms1G \
    -J-Xmx4G  \
    -Dakka.remote.artery.canonical.port=2550  \
    -Dsimplexportal.spatial.api.http.port=8080

bin/simplex-spatial-core \
    -java-home /usr/lib/jvm/java-8-openjdk-amd64 \
    -jvm-debug 9011 \
    -J-Xms1G \
    -J-Xmx4G  \
    -Dakka.remote.artery.canonical.port=2551  \
    -Dsimplexportal.spatial.api.http.port=8081

bin/simplex-spatial-core \
    -java-home /usr/lib/jvm/java-8-openjdk-amd64 \
    -jvm-debug 9012 \
    -J-Xms1G \
    -J-Xmx4G  \
    -Dakka.remote.artery.canonical.port=2552  \
    -Dsimplexportal.spatial.api.http.port=8082

```

### Running osm loader

```bash
bin/simplex-spatial-osm-loader \
    -java-home /usr/lib/jvm/java-8-openjdk-amd64 \
    -jvm-debug 9009 \
    -J-Xms1G \
    -J-Xmx4G  \
    --block-size=300 \
    /home/angelcc/Downloads/osm/ireland-and-northern-ireland-latest.osm.pbf
```




## Notes

- Enable GRPC logs: -Djava.util.logging.config.file=/path/to/grpc-debug-logging.properties
