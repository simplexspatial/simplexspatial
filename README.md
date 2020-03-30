# SimplexSpatial
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fsimplexspatial%2Fsimplexspatial.svg?type=shield)](https://app.fossa.io/projects/git%2Bgithub.com%2Fsimplexspatial%2Fsimplexspatial?ref=badge_shield)


SimplexSpatial is a GeoSpatial server, focus in distributed algorithms execution. It is distributed, horizontally
scalable and fault tolerant system based in AKKA.

At the moment, to different APIs has been implemented as entry points:
- [JSON](#restful-examples): Easy to use restful API, ideally for testing or web.
- gRPC: Ideally to intensive requests, like load data. As good example, refer to [load-osm file project](https://github.com/simplexspatial/simplexspatial-loader-osm).


## Other documentation

- [Architecture and Design documentation](doc/architecture.md)
- [Performance documentation](doc/performance.md)


## Package and run

### Default configuration

The distributed generated package comes with a default configuration
into the `conf` folder.

#### SimplexSpatial configuration
SimplexSpatial is using the same configuration system that is used in
AKKA: [lightbend config](https://github.com/lightbend/config). It means
than you can set and overwrite configuration properties as it is
explained in the Lightbend Config site.

In the [published tar](#packaging), `conf/application.conf` contains the specific configuration for the
server. This is the config file used by default from the script used to
start a node.

As reference, this is the set of parameters that the server uses:
```
simplexportal.spatial {
  entrypoint {
    restful {
      interface = "0.0.0.0"
      port = 8080
    }
    grpc {
      interface = "0.0.0.0"
      port = 7080
    }
  }
  indexes {
    grid-index {
      partitions {
        nodes-lookup = 50
        ways-lookup = 50
        latitude = 10000
        longitude = 10000
      }
    }
  }
}
```

Remind that the server is base in [AKKA](https://akka.io/), so you can
set any parameters related to AKKA as well.

In relation to the AKKA cluster and in this stage of the project, it is
important to configure the way that the cluster is going to work. This
is the default configuration in the `application.conf`:
```
akka {

  // FIXME: Temporal for POC
  persistence {
    journal.plugin = "akka.persistence.journal.inmem"
    //    snapshot-store.plugin = "disable-snapshot-store"
  }

  cluster {
    seed-nodes = [
      "akka://SimplexSpatialSystem@127.0.1.1:2550",
      "akka://SimplexSpatialSystem@127.0.1.1:2551"
    ]
    sharding {
      number-of-shards = 100
    }
  }

}
```

This means that:
- It is using in memory persistence journal, so you can not restart the
  cluster at all. In that case, you will lose your data.
- It is using fixed seed nodes. Remind to update the IP (in this case it
  is the local IP for Ubuntu 19.10) and ports.

#### JVM and general configuration
`conf/application.ini` contains general information about the JVM, like
memory, JMX config, etc.

#### Logging configuration
AKKA is using [SLF4J](http://www.slf4j.org/) but SimplexSpatial adds
[logback](http://logback.qos.ch/) to the classpath, so that will be the
library to configure.

Important information about logging configuration:
- [AKKA SLF4J backend](https://doc.akka.io/docs/akka/current/typed/logging.html#slf4j-backend)
- [Internal logging by Akka](https://doc.akka.io/docs/akka/current/typed/logging.html#internal-logging-by-akka)
- [LOGBack configuration](http://logback.qos.ch/manual/configuration.html)

### Packaging

The following command will generate two distributable packages, one located
under `core/target/universal` and another under `osm-loader/target/universal`:

```bash
sbt clean universal:packageZipTarball
```

It will generate a 50M tar `{source_root}/core/target/universal/simplexspatial-core-<version>.tgz`
with all the necessary stuff to start a cluster node.

### Install Dependencies
Akka persistence needs a storge system to store the journal and snapshots.

For production, it is recommended to use a distributed storage, like Cassandra

For testing ( default configuration ) Postgresql is used. In the `conf` folder, there is a `docker-compose.yml` file to
be able to start all dependencies for testing.
```ssh
docker-compose up -d
```


### Running

It supposed that you have a JDK8 (recommended) or higher installed in your system.

#### Running thru CLI

Uncompress the package file and move to the generated folder:
```bash
tar -xvf simplexspatial-core-<version>.tgz
cd simplexspatial-core-<version>/
```

The default configuration is looking for seeds in `127.0.1.1:2550` and
`127.0.1.1:2551` It means that for the first two nodes to start, Artery
should listen ports 2550 and 2551. For other nodes, use port 0 to pickup
randomly one free port or set another free port.

Node 1:
```ssh
bin/simplexspatial-core \
    -java-home /usr/lib/jvm/java-8-openjdk-amd64 \
    -jvm-debug 9010 \
    -J-Xms1G \
    -J-Xmx4G  \
    -Dakka.remote.artery.canonical.port=2550  \
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
    -Dakka.remote.artery.canonical.port=2551  \
    -Dsimplexportal.spatial.entrypoint.grpc.port=7081 \
    -Dsimplexportal.spatial.entrypoint.restful.port=8081
```

Other nodes:
```ssh
bin/simplexspatial-core \
    -java-home /usr/lib/jvm/java-8-openjdk-amd64 \
    -J-Xms1G \
    -J-Xmx4G  \
    -Dakka.remote.artery.canonical.port=0  \
    -Dsimplexportal.spatial.entrypoint.grpc.port=0 \
    -Dsimplexportal.spatial.entrypoint.restful.port=0
```


#### Running thru sbt
To create a package with all necessary inside, execute the follow command:
```bash
sbt "core/runMain com.simplexportal.spatial.Main"
```


## Restful examples
Using [Httpi](https://httpie.org/)

### Nodes

- Add a node

    ```ssh
    http PUT http://localhost:8080/node/1 lon:=3.0001 lat:=-2.4444 attributes:={}
    ```
    
    or
    
    `node_file.json` content:
    ```json
    {
      "lon": 10.100001, "lat": -22.000002, "attributes": {}
    }
    ```
    Request to insert node:
    ```ssh
    http PUT http://localhost:8080/node/1 < node_file.json
    ```

- Get a Node
    ```ssh
    http GET http://localhost:8080/node/1
    ```

### Ways

- Add a Way

    ```ssh
    http PUT http://localhost:8080/way/10 nodes:=[1,2] attributes:={}
    ```
    
    or
    
    `way_file.json` content:
    ```json
    {
      "nodes": [1, 2],
      "attributes": { }
    }
    ```
    Request to insert way:
    ```ssh
    http PUT http://localhost:8080/node/10 < way_file.json
    ```

- Get a Way
    ```ssh
    http GET http://localhost:8080/way/10
    ```
  
### Batch mode
To execute a sequence of commands in one request.

`batch_commands.json` content:
```json
{
  "nodes": [
    {
      "id": 100001,
      "lon": 1.100001,
      "lat": -1.000002,
      "attributes": {}
    },
    {
      "id": 100002,
      "lon": 2.100001,
      "lat": -2.000002,
      "attributes": {}
    }
  ],
  "ways": [
    {
      "id": 100010,
      "nodes": [
        100001,
        100002
      ],
      "attributes": {
        "name": "Two points way."
      }
    }
  ]
}
```

Request to execute all commands in batch mode:
```ssh
http PUT http://localhost:8080/batch < batch_commands.json
```

### Nearest node
```ssh
http -v GET http://localhost:8080/algorithm/nearest/node lat==43.73819 lon==7.4269
```

## Notes

- Enable GRPC logs: -Djava.util.logging.config.file=/path/to/grpc-debug-logging.properties
- More information about Artery in the [AKKA documentation](https://doc.akka.io/docs/akka/current/remoting-artery.html).



## License
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fsimplexspatial%2Fsimplexspatial.svg?type=large)](https://app.fossa.io/projects/git%2Bgithub.com%2Fsimplexspatial%2Fsimplexspatial?ref=badge_large)
