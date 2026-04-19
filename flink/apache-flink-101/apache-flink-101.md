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

### Docker setup 

```bash
# Docker Compose to start up Kafka, Flink, and Flink's SQL CLI.
$ docker compose up --build -d
$ docker ps
CONTAINER ID   IMAGE                          COMMAND                  CREATED         STATUS         PORTS                      NAMES
e4a4fa26bcc5   apache-flink-101-taskmanager   "/docker-entrypoint.…"   8 seconds ago   Up 7 seconds   6123/tcp, 8081/tcp         apache-flink-101-taskmanager-1
17d9f13f0ad8   apache-flink-101-jobmanager    "/docker-entrypoint.…"   8 seconds ago   Up 7 seconds   127.0.0.1:8081->8081/tcp   apache-flink-101-jobmanager-1
f19538adeb19   apache/kafka:4.0.0             "/__cacert_entrypoin…"   8 seconds ago   Up 7 seconds   9092/tcp                   broker
657f78eac978   edenhill/kcat:1.7.1            "/bin/sh -c 'apk add…"   8 seconds ago   Up 7 seconds                              kcat


# Once the containers are running, start Flink SQL client
$ docker compose run sql-client
Flink SQL> help;

# stop the Flink SQL container
Flink SQL> quit;
```

Flink SQL https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/sql/overview/
Flink’s SQL support is based on [Apache Calcite](https://calcite.apache.org/) which implements the SQL standard.

Check the Flink http://localhost:8081/

```bash
# fixed-length (bounded) table with 500 rows 
> CREATE TABLE `bounded_pageviews` (
`url` STRING,
`ts` TIMESTAMP(3)
)
WITH (
'connector' = 'faker',
'number-of-rows' = '500',
'rows-per-second' = '100',
'fields.url.expression' = '/#{GreekPhilosopher.name}.html',
'fields.ts.expression' =  '#{date.past ''5'',''1'',''SECONDS''}'
);

> show tables;
+-------------------+
|        table name |
+-------------------+
| bounded_pageviews |
+-------------------+
  
> SELECT * FROM bounded_pageviews LIMIT 10;
# This takes 5 seconds to complete because the source is configured to produce 500 rows at a rate of 100 rows per second.
count
-----
  500
  
# switch to streaming, you will see the count increment from 100 to 200, etc, up to 500. Again, this will take 5 seconds.  
> SET 'execution.runtime-mode' = 'streaming';
> SELECT COUNT(*) AS `count` FROM bounded_pageviews;
# check under the hood, it won't affect the internal behavior of the runtime, it will change how the SQL Client displays the results
> SET 'sql-client.execution.result-mode' = 'changelog';
# In changelog mode, the SQL Client doesn't just update the count in place, 
# but instead displays each message in the stream of updates it's receiving from the Flink SQL runtime.
> SELECT COUNT(*) AS `count` FROM bounded_pageviews;
 op                count
 ...
 +U                  497
 -U                  497
 +U                  498
 -U                  498
 +U                  499
 -U                  499
 +U                  500 

# switch back to batch if needed
> SET 'execution.runtime-mode' = 'batch';
> SET 'sql-client.execution.result-mode' = 'changelog';

# create an unbounded table
# the faker connector is not using kafka topic, generates "fake" random data purely in-memory within the Flink job itself
CREATE TABLE `streaming_pageviews` (
  `url` STRING,
  `ts` TIMESTAMP(3)
)
WITH (
  'connector' = 'faker',
  'rows-per-second' = '100',
  'fields.url.expression' = '/#{GreekPhilosopher.name}.html',
  'fields.ts.expression' =  '#{date.past ''5'',''1'',''SECONDS''}'
);
> show tables;
+---------------------+
|          table name |
+---------------------+
|   bounded_pageviews |
| streaming_pageviews |
+---------------------+

# check the running jobs in the Flink UI http://localhost:8081/ 
> SELECT COUNT(*) AS `count` FROM streaming_pageviews;

# check jobs with command line
> show jobs;

# running slower per second
> ALTER TABLE `streaming_pageviews` SET ('rows-per-second' = '10');

# check topics
# generates "fake" random data purely in-memory within the Flink job itself
$ docker compose exec -it kcat kcat -b broker:9092 -L
Metadata for all topics (from broker 1: broker:9092/1):
 1 brokers:
  broker 1 at broker:9092 (controller)
 0 topics:

# cleanup
$ docker compose down -v
```

### Flink Runtime

- `Flink Client` - when you submit for example Flink SQL statement, the API submits the `Job Graph` to the `Job Manager` 
- `Job Manager`
   - it will find or create resources needed to run the job.
   - in a containerized environment will spin up as many `Task Managers` as needed to provide the desired parallelism
   - once the job is running the `Job Manager` will be responsible for coordinating the activities of Flink cluster
     - ex: checkpointing, restarting Task Manager if they fail
- `Task Manager`
  - each task manager provides some `task slots` - each of which can execute one `parallel instance` of a `job graph`
  - they run the jobs, 
    - they pull data from the sources, 
    - run transformations, 
    - send data to each other in case of rebalancing / repartitioning
    - push data out to the sink

More details about `Flink architecture`: https://nightlies.apache.org/flink/flink-docs-stable/docs/concepts/flink-architecture/

- When Flink Runtime is set for `Batch Mode` - Flink can run with bunch of optimizations to be more efficient
  - ex: pre sort the dataset to be easier to process (joining is easier), do the filtering first etc
- In `Streaming mode` 
  - input must be processed when it arrives
  - sometimes it must be buffered until necessary data has arrived, these buffered needs to be stored in a durable `state store` 
  - `state store` is important if the task manager fails or has to be restarted

Some use cases are only possible with stream processing (like where `low-latency` is critical requirement) 
  - `monitoring and alerting`
  - `fraud detection`
  - use case which require immediate response to every event, latency from batch processing make it unworkable

- Any usecase with `batch processing` can be implemented with `stream processing`. 
- However, is important to have runtime that supports both since if `batch processing` is needed that can be more efficient.


### The Flink Web UI - TODO

```bash
$ EXPLAIN PLAN FOR SELECT COUNT(*) AS `count` FROM streaming_pageviews;

| == Abstract Syntax Tree ==
LogicalAggregate(group=[{}], count=[COUNT()])
+- LogicalProject($f0=[0])
   +- LogicalTableScan(table=[[default_catalog, default_database, streaming_pageviews]])

== Optimized Physical Plan ==
GroupAggregate(select=[COUNT(*) AS count])
+- Exchange(distribution=[single])
   +- Calc(select=[0 AS $f0])
      +- TableSourceScan(table=[[default_catalog, default_database, streaming_pageviews]], fields=[url, ts])

== Optimized Execution Plan ==
GroupAggregate(select=[COUNT(*) AS count])
+- Exchange(distribution=[single])
   +- Calc(select=[0 AS $f0])
      +- TableSourceScan(table=[[default_catalog, default_database, streaming_pageviews]], fields=[url, ts])
 |
```

