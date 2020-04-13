# Ubiquitous Languages.
- Node: Represent a location in the map, with attributes attached. In a Graph database, it would be a *Vertex*.
- Way: Represent a street, road or another way in the map. It is represented for a set of attributes and an ordered list
  of nodes. In a Graph database, it's equivalent to an *edge*.

# Subdomains

## Grid Network
// TODO

## Node LookUp
Nodes are distributed in other indexes, usually per location.
If we want access to a Node per Id, we need to access to this LookUp index.
This is a distributed index accessible per id. It will return the id of the shard
that contains the Node. 

## Way LookUp
Ways are distributed and split in other indexes, usually per location.
If we want access to a Way per Id, we need to access to this LookUp index.
This is a distributed index accessible per id. It will return a set of id of the shards
that contain parts of the way. 

# Feeding data

