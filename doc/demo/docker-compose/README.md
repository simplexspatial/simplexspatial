# Docker Compose environment

This is an environment that can be used to demo and testing.

It is using [Docker Compose 2.4](https://docs.docker.com/compose/), so it is necessary at least Docker v18.06.0+ and
Docker Compose 2.4

## Cassandra

The environment contains a Cassandra single node cluster to store Akka persistence journal and snapshots.
By default, Cassandra will store data internally, so it will be a clean after remove the container.

The environment assign 10GB of memory to the Cassandra node because [the default memory allocated to Cassandra is 8GB](https://cassandra.apache.org/doc/latest/operating/hardware.html).
Maybe will be necessary to change the Cassandra configuration to something lower, but at the moment, I suppose that
you have at least 16GB of memory in your system. Anyway, this environment is for testing and development.


Depending of your host, Cassandra will throw this warning:
```log
 WARN  [main] 2020-04-17 11:00:52,413 StartupChecks.java:311 - Maximum number of memory map areas per process (vm.max_map_count) 65530 is too low, recommended value: 1048575, you can change it with sysctl.
```

I did not find the way to fix it changing only the container affected. So, to fix it in Ubuntu or Debian bases OS hosts:
```shell script
# Check current value
$ sysctl vm.max_map_count
vm.max_map_count = 65530

# Append changes to /etc/sysctl.conf
$ echo "vm.max_map_count=1048575" | sudo tee -a /etc/sysctl.conf
vm.max_map_count=1048575

# Apply sysctl.conf updates without rebooting
$ sudo sysctl -p
vm.max_map_count = 1048575

# Check if new value has been applied
$ sysctl vm.max_map_count
vm.max_map_count = 1048575
```

## The Simplexspatial cluster.
The environment define 3 different nodes, all of them assigned with 2GB of memory and 2CPUs. `simplexspatial_seed_1` and
`simplexspatial_seed_2` are always up and they are used as seed porpoise. `simplexspatial` is the generic one, and It can
be used to scale the environment. 

## Posts exposed.

### Simplexspatial

| node  | gRPC  |  gRPC-Web | Restful  |
|-------|-------|-----------|----------|
| seed 1  | 7081  | 6081  | 8081  |
| seed 2  | 7082  | 6082  | 8082  |
| others  | Random  | Random  | Random  |

To check the list of nodes exposed randomly, use `docker-compose ps` and the column Ports:
```shell script
$ docker-compose ps
                 Name                               Command               State                                                       Ports                                                    
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
docker-compose_cassandra_1               docker-entrypoint.sh cassa ...   Up       7000/tcp, 7001/tcp, 7199/tcp, 0.0.0.0:9042->9042/tcp, 0.0.0.0:9160->9160/tcp                                
docker-compose_cassandra_initdb_1        /opt/initdb/entrypoint.sh  ...   Exit 0                                                                                                               
docker-compose_simplexspatial_1          /opt/simplexspatial/bin/si ...   Up       2550/tcp, 0.0.0.0:32882->6080/tcp, 0.0.0.0:32881->7080/tcp, 0.0.0.0:32880->8080/tcp, 0.0.0.0:32879->9010/tcp
docker-compose_simplexspatial_2          /opt/simplexspatial/bin/si ...   Up       2550/tcp, 0.0.0.0:32890->6080/tcp, 0.0.0.0:32889->7080/tcp, 0.0.0.0:32887->8080/tcp, 0.0.0.0:32885->9010/tcp
docker-compose_simplexspatial_3          /opt/simplexspatial/bin/si ...   Up       2550/tcp, 0.0.0.0:32888->6080/tcp, 0.0.0.0:32886->7080/tcp, 0.0.0.0:32884->8080/tcp, 0.0.0.0:32883->9010/tcp
docker-compose_simplexspatial_seed_1_1   /opt/simplexspatial/bin/si ...   Up       2550/tcp, 0.0.0.0:6081->6080/tcp, 0.0.0.0:7081->7080/tcp, 0.0.0.0:8081->8080/tcp, 0.0.0.0:9011->9010/tcp    
docker-compose_simplexspatial_seed_2_1   /opt/simplexspatial/bin/si ...   Up       2550/tcp, 0.0.0.0:6082->6080/tcp, 0.0.0.0:7082->7080/tcp, 0.0.0.0:8082->8080/tcp, 0.0.0.0:9012->9010/tcp   
```

## Start environment
As usually, list of useful commands.

```shell script
# Start
docker-compose up --build -d

# Follow logs
docker-compose logs -f

# Shutdown
docker-compose down

# Scale cluster
docker-compose up -d --scale simplexspatial=3

# List services (docker containers) running
docker-compose ps

# Check status of containes
docker stats

# Access to the Cassandra CQL
docker-compose exec cassandra cqlsh

# Check Cassandra cluster
docker-compose exec cassandra nodetool status
```

## Test environment

From the folder `doc/demo/scripts`:
```shell script
~/p/s/d/d/scripts $ http GET http://localhost:8082/way/100010
HTTP/1.1 404 Not Found
Content-Length: 83
Content-Type: text/plain; charset=UTF-8
Date: Fri, 17 Apr 2020 13:55:00 GMT
Server: akka-http/10.1.11

The requested resource could not be found but may be available again in the future.

~/p/s/d/d/scripts $ http PUT http://localhost:8081/batch < batch_small.json
HTTP/1.1 200 OK
Content-Length: 2
Content-Type: application/json
Date: Fri, 17 Apr 2020 13:55:07 GMT
Server: akka-http/10.1.11

{}

~/p/s/d/d/scripts $ http GET http://localhost:8082/way/100010
HTTP/1.1 200 OK
Content-Length: 185
Content-Type: application/json
Date: Fri, 17 Apr 2020 13:55:11 GMT
Server: akka-http/10.1.11

{
    "attributes": {
        "name": "Two points way."
    },
    "id": 100010,
    "nodes": [
        {
            "attributes": {},
            "id": 100001,
            "lat": 1.100001,
            "lon": -1.000002
        },
        {
            "attributes": {},
            "id": 100002,
            "lat": 2.100001,
            "lon": -2.000002
        }
    ]
}

```
