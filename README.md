# simplex-spatial

Comming soon!!

## Notes

- Enable GRPC logs: -Djava.util.logging.config.file=/path/to/grpc-debug-logging.properties

## Performance Loading

```
sudo update-alternatives --config java
java --version
sbt clean assembly
java -Xms5G -Xmx10G -jar load_osm/target/scala-2.12/loadOSM-assembly-0.0.1-SNAPSHOT.jar \
    local \
    /home/angelcerveraclaudio/Downloads/osm/ireland-and-northern-ireland-latest.osm.pbf 
 
```

To load, I used osm4scala to read the osm file and pull data into the server.

Using always the Irish OSM network (150MB, 19.426.617 nodes and 2.096.455 ways):

```
// Compiled with Java X and without optimizations
Java8 -Xms5G  -Xmx10G => 19426617 nodes and 2096455 ways loaded in 152.88 seconds
Java11 -Xms5G  -Xmx10G => 19426617 nodes and 2096455 ways loaded in 107.22 seconds
Java12 -Xms5G  -Xmx10G => 19426617 nodes and 2096455 ways loaded in 106.93 seconds
Java13 -Xms5G  -Xmx10G => 19426617 nodes and 2096455 ways loaded in 111.45 seconds


// From here, always using -Xms5G  -Xmx10G

// With dictionary for tags (Less memory and GC used)
Java8  => 19426617 nodes and 2096455 ways loaded in 129.35 seconds
Java13 => 19426617 nodes and 2096455 ways loaded in 107.20 seconds

// Loading in blocks 2000 directly (same JavaVM) 
Java13 => 19426617 nodes and 2096455 ways loaded in 87 seconds

// From here, always using -Xms20G  -Xmx24G

// Remote server using gRPC sequential unary calls.
Java8  => Blocks of  275 => 106 seconds
Java8  => Blocks of  300 => 100 seconds
Java8  => Blocks of  350 => 119 seconds
Java8  => Blocks of  400 => 125 seconds
Java8  => Blocks of  500 => 135 seconds
Java8  => Blocks of 1000 => 225 seconds
Java11 => Blocks of  300 => 128 seconds

```

## Thru sbt
```bash
sbt "core/runMain com.simplexportal.spatial.Main"
```

```bash
sbt "loadOSM/runMain com.simplexportal.spatial.loadosm.Main /home/angelcerveraclaudio/Downloads/osm/andorra-latest.osm.pbf"
```
