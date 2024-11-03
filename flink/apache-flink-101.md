### Apache Flink 101

- share-nothing architecture with local state,
- event-time processing
- state snapshots (for recovery)

4 Big ideas upon which Flink is based on:

- `Streaming`
- `State`
- `Time` 
- `Snapshots`

- supports both batch and stream processing
- SQL / Python / Java
- You use one of Flink APIs and Flink will execute your code in a Flink cluster.
- `Job`: a running Flink application
- `Job Graph` (or Topology) : the event data streaming through a data processing pipeline
  - `Nodes` represent the processing steps in the pipeline
  - Each processing step is executed by an `operator` which transform event streams
  - The operators are connected to one another with arrows
  - Is a directed acyclic graph
- Stream processing is done in parallel by partitioning is parallel `sub-streams`

`Flink SQL` 
    - batch and stream processing
    - ansi SQL
`Table API` - Java / Python (dynamic tables) - not implemented on DataStream API 
`DataStream API` (streams, windows) -  stream processing & analytics
`Process Functions` (events, state, time) - low-level stateful processing - part of the DataStream API

`Flink SQL`
- the data is not stored in Flink
- ex. if you create a table the data can be in a Kafka topic
- the table object is just a metadata describing the schema and the connector properties

- `change log stream` - contains stream of insert events (+I) 
- `update before` - retract and earlier result
- `update after` - update an earlier result
- `delete` - delete an earlier result