### Apache Flink 101

- `share-nothing architecture` with local state,
- `event-time processing`
- `state snapshots` (for recovery)

4 Big ideas upon which Flink is based on:

- `Streaming`
- `State`
- `Time` 
- `Snapshots`

### Streaming

- A `stream` is a sequence of `events`
- A `stream` can be `bounded` or `unbounded`
- For Flink batch processing is just a `special case` in the runtime, the stream is bounded
- `sources` --> real-time stream processing --> `sinks`
- as a Flink developer you will define business logic using one of Flink APIs and Flink will execute your code in a Flink cluster
- Supported: SQL / Python / Java
- `Job`: a running Flink application
- `Job Graph` (or Topology) : the event data streaming through a data processing pipeline
  - `Nodes` represent the `processing steps` in the pipeline
  - Each processing step is executed by an `operator` which transform event streams
  - The operators are connected to one another with arrows
  - Is a `directed acyclic graph`, event data is flowing from the `sources` to `sinks`, being processed along the way by the `operators`
- `Stream processing` is done in 
  - `parallel` by partitioning is parallel `sub-streams`, 
  - `forward` - `operator` forwarding from the source to downstream
  - `repartition` - when filtering a repartition is necessary, when events are serialized and maybe be sent over the network to next operator downstream
  - `rebalance` - changing parallelism 

```bash
# example Flink SQL application
INSERT INTO results
SELECT color, count(*)
FROM events
where color <> orange
GROUP BY color;
```

### Flink SQL

`Flink SQL` 
    - `batch processing` and `stream processing`
    - ansi SQL
`Table API` - Java / Python (dynamic tables) - not implemented on DataStream API 
`DataStream API` (streams, windows) -  stream processing & analytics
`Process Functions` (events, state, time) - low-level stateful processing - part of the DataStream API

- a single Flink application could use all these APIs together
- How the code is organized:
--------------------------------------------------------------------
-- DataStream API (Process Functions) --- |-- Table / SQL API ------  
------------------------------------------|-- Optimizer / Planner --
------------- Low-Level Stream Operator API ------------------------
--------------------- DataFlow Runtime  ----------------------------

- with `Flink SQL` it feels like you are using a database, but is not a database, the data is not stored in `Flink`

```bash
# will be append-only table
CREATE TABLE shipments (item STRING, count INT) WITH (...)
# will be updating table 
# when processing updates the Flink SQL runtime needs to keep some internal state 
CREATE TABLE inventory (item STRING, stock INT) WITH (...)

# shipments from our suppliers increase our stock level, outbound shipments to our customers reduce our stock level
# flink sql planner compiles this insert statement into a Flink job
# shipment table is the source, and the inventory table is the sink
# the aggregation operator is in the middle
# after the source the events are shuffled to group them by item 
# the aggregation creates separate sums for each item
# the result ends up in the inventory table 
INSERT INTO inventory 
SELECT item, SUM(count) AS stock
FROM shipments 
GROUP BY item
```

- when you create a `Flink table` you are just describing data that lives somewhere else

```bash
# describe schema and connector properties
CREATE TABLE shipments (item STRING, count INT) WITH (
'connector' = 'kafka',
'topic' = 'shipments',
'value.format' = 'json',
'properties.group.id' = 'myGroup',
'scan.startup.mode' = 'latest-offset',
'properties.bootstrap.servers' = 'XXX',
'properties.security.protocol' = 'SASL_SSL',
'properties.sasl.mechanism' = 'PLAIN',
'properties.sasl.jaas.config' = 'XXX'
)
```

- `Flink SQL tables` are dynamic, change over time 
- `Flink SQL tables` is equivalent to a `changelog stream`
- 4 event types
  - +I - insertion, default semantics
  - -U - update before, retract an earlier result 
  - +U - update after, update an earlier result
  - -D - delete, delete an earlier result 
- -U / +U pair of events that work together to update an earlier result


```bash
## Streaming Only
ORDER BY time ascending (only)

### Batch Only
ORDER BY anything
```
