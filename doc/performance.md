# Performance



## One unique tile actor

This test is the performance using one Tile actor.

**At the moment, it is not possible to reproduce this performance test because
the starting script is creating a cluster and sharding all requests.**

```
sudo update-alternatives --config java
java --version
sbt clean assembly

java \
    -Xms20G  -Xmx24G  \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=9010 \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -jar core-assembly-0.0.1-SNAPSHOT.jar

java \
    -Xms5G -Xmx10G \
    -jar load_osm/target/scala-2.12/loadOSM-assembly-0.0.1-SNAPSHOT.jar \
    --block-size=300 \
    /home/angelcc/Downloads/osm/ireland-and-northern-ireland-latest.osm.pbf
    
 
```

To load, I used osm4scala to read the osm file and pull data into the server.

Using always the Irish OSM network (150MB, 19.426.617 nodes and 2.096.455 ways = 21.523.072 entities):

```
// Compiled with Java X and without optimizations
Java8 -Xms5G  -Xmx10G => 19426617 nodes and 2096455 ways loaded in 152.88 seconds (142K/second)
Java11 -Xms5G  -Xmx10G => 19426617 nodes and 2096455 ways loaded in 107.22 seconds (201K/second)
Java12 -Xms5G  -Xmx10G => 19426617 nodes and 2096455 ways loaded in 106.93 seconds (203K/second)
Java13 -Xms5G  -Xmx10G => 19426617 nodes and 2096455 ways loaded in 111.45 seconds (194K/second)


// From here, always using -Xms5G  -Xmx10G

// With dictionary for tags (Less memory and GC used)
Java8  => 19426617 nodes and 2096455 ways loaded in 129.35 seconds (167K/second)
Java13 => 19426617 nodes and 2096455 ways loaded in 107.20 seconds (201K/second)

// Loading in blocks 2000 directly (**same JavaVM**) 
Java13 => 19426617 nodes and 2096455 ways loaded in 87 seconds (247K/second)
```
// From here, always using -Xms20G  -Xmx24G

### gRPC
- Data : Ireland (Republic of Ireland and North Ireland)
- Size: 19426617 nodes and 2096455 ways = 21523072 entities / 153MB

#### Sequential unary calls.
```
Java8  => Blocks of  275 => 106 seconds => 203K/second
Java8  => Blocks of  300 => 100 seconds => 215K/second
Java8  => Blocks of  350 => 119 seconds
Java8  => Blocks of  400 => 125 seconds
Java8  => Blocks of  500 => 135 seconds
Java8  => Blocks of 1000 => 225 seconds => 96K/second
Java11 => Blocks of  300 => 128 seconds
```

#### Streaming Ireland, Java8
```
No blocks => 10 minutes (600 seconds) => 36K/second
Blocks of 300 in server => 3.4 minutes (205 seconds) => 105K/second
Blocks of 300 in client =>
Blocks of 300 in both => 1.2 minutes (74 second) => 291K/second (Tell pattern)
Blocks of 300 in both => 1.5 minutes (93 second) => 231K/second (Ask pattern)
```

#### Streaming Ireland, java8, Akka Typed 2.6
```
Added 19426617/19426617 nodes and 2096455/2096455 ways in 77 seconds. 71744 blocks sent. => 280K/second
```

- With Ask patter, worst performance than with Tell pattern, but more
  stable.
- TODO: Replace Flow.ask with Sink.actorReffWithAck
