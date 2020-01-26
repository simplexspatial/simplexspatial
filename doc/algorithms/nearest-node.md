# Nearest Node
Search the nearest node to a location in a distributed fashion.

The distance is calculated is the lenght of the straight line that
connect the `origin` with the `result`.

## Input
- Origin: Location from where calculate the distance.

## Output:
- Node: Full node information of the node found nearest of the point.

## Indexes used.
- Grid Index

## Algorithm

1. Start from the `tile` where the where the `origin` is located.
2. Calculate the nearest node in the selected tile. We will call `d` to
   the distance between the `origin` and the found node `n`
   > :warning: **Note:** At the moment, the grid index store internal
   data in a look up map, so will be necessary to iterate over all
   nodes.
3. If no node found, repeat from `step 2`, but using all tiles around.
4. If a node has been found, maybe there are nearer points in other
   tiles around, so we need to search for them.
3. Calculate the distance between the `origin` and any of the edges of
   the current tile. Let's name them as `d_n`, `d_s`, `d_e`, `d_w`,
   `d_ne`, `d_nw`, `d_se` and `d_sw`. Let's call `edge distance` and
   generalize as `d_edge`.
4. If an `edge distance` is lower or equal than `d`, then repeat the
   process from `step 2` using as tile the corresponding with the `edge`
   compared.
5. The process will finish when all `edge distance`s are longer than `d`

## Future improvements
- Optimize internal index: [ISSUE](https://github.com/angelcervera/simplexspatial/issues/41)
