# simplex-spatial

Comming soon!!

## Notes

### Performance

```
sudo update-alternatives --config java
java --version
sbt clean assembly
java -Xms5G -Xmx10G -jar load_osm/target/scala-2.12/loadOSM-assembly-0.0.1-SNAPSHOT.jar \
    local \
    /home/angelcerveraclaudio/Downloads/osm/ireland-and-northern-ireland-latest.osm.pbf 
 
```

```
// Compiled with Java X and without optimizations
Java8 -Xms5G  -Xmx10G =>19426617 nodes and 2096455 ways loaded in 152.88 seconds
Java11 -Xms5G  -Xmx10G => 19426617 nodes and 2096455 ways loaded in 107.22 seconds
Java12 -Xms5G  -Xmx10G => 19426617 nodes and 2096455 ways loaded in 106.93 seconds
Java13 -Xms5G  -Xmx10G => 19426617 nodes and 2096455 ways loaded in 111.45 seconds
```
