1. Setup the infrastructure using `spring-kafka/docker-compose.yml` file

```bash
$ docker compse up -d
```

2. Start the `WordCountApp` application. The application creates automatically the followings: 
- the input topic
- the output topic
- a producer generating input
- a stream processing the input and generating output

3. List topics:

```bash
$ kafka-topics --bootstrap-server localhost:19092 --list | grep word-count

__consumer_offsets
word-count-input
word-count-output
wordcount-KSTREAM-AGGREGATE-STATE-STORE-0000000003-changelog
wordcount-KSTREAM-AGGREGATE-STATE-STORE-0000000003-repartition
```

4. Start consumer:

```bash
$ kafka-console-consumer --topic word-count-output --from-beginning \
   --bootstrap-server localhost:19092 \
   --property print.key=true \
   --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

5. Metrics exposed via actuator / prometheus registry. The `kafka.stream.state` might appear a few seconds later.

```bash
$ http :8080/actuator/metrics | jq . | grep kafka.stream

    "kafka.stream.alive.stream.threads",
    "kafka.stream.failed.stream.threads",
    "kafka.stream.processor.node.process.rate",
    "kafka.stream.processor.node.process.total",
    "kafka.stream.processor.node.record.e2e.latency.avg",
    "kafka.stream.processor.node.record.e2e.latency.max",
    "kafka.stream.processor.node.record.e2e.latency.min",
    "kafka.stream.state.all.latency.avg",
    "kafka.stream.state.all.latency.max",
    "kafka.stream.state.all.rate",
    "kafka.stream.state.background.errors",
    "kafka.stream.state.block.cache.capacity",
    "kafka.stream.state.block.cache.data.hit.ratio",
    "kafka.stream.state.block.cache.filter.hit.ratio",
    "kafka.stream.state.block.cache.index.hit.ratio",
    "kafka.stream.state.block.cache.pinned.usage",
    "kafka.stream.state.block.cache.usage",
    "kafka.stream.state.bytes.read.compaction.rate",
    "kafka.stream.state.bytes.read.rate",
    "kafka.stream.state.bytes.read.total",
    "kafka.stream.state.bytes.written.compaction.rate",
    "kafka.stream.state.bytes.written.rate",
    "kafka.stream.state.bytes.written.total",
    "kafka.stream.state.compaction.pending",
    "kafka.stream.state.compaction.time.avg",
    "kafka.stream.state.compaction.time.max",
    "kafka.stream.state.compaction.time.min",
    "kafka.stream.state.cur.size.active.mem.table",
    "kafka.stream.state.cur.size.all.mem.tables",
    "kafka.stream.state.delete.latency.avg",
    "kafka.stream.state.delete.latency.max",
    "kafka.stream.state.delete.rate",
    "kafka.stream.state.estimate.num.keys",
    "kafka.stream.state.estimate.pending.compaction.bytes",
    "kafka.stream.state.estimate.table.readers.mem",
    "kafka.stream.state.flush.latency.avg",
    "kafka.stream.state.flush.latency.max",
    "kafka.stream.state.flush.rate",
    "kafka.stream.state.get.latency.avg",
    "kafka.stream.state.get.latency.max",
    "kafka.stream.state.get.rate",
    "kafka.stream.state.live.sst.files.size",
    "kafka.stream.state.mem.table.flush.pending",
    "kafka.stream.state.memtable.bytes.flushed.rate",
    "kafka.stream.state.memtable.bytes.flushed.total",
    "kafka.stream.state.memtable.flush.time.avg",
    "kafka.stream.state.memtable.flush.time.max",
    "kafka.stream.state.memtable.flush.time.min",
    "kafka.stream.state.memtable.hit.ratio",
    "kafka.stream.state.num.deletes.active.mem.table",
    "kafka.stream.state.num.deletes.imm.mem.tables",
    "kafka.stream.state.num.entries.active.mem.table",
    "kafka.stream.state.num.entries.imm.mem.tables",
    "kafka.stream.state.num.immutable.mem.table",
    "kafka.stream.state.num.live.versions",
    "kafka.stream.state.num.running.compactions",
    "kafka.stream.state.num.running.flushes",
    "kafka.stream.state.number.file.errors.total",
    "kafka.stream.state.number.open.files",
    "kafka.stream.state.prefix.scan.latency.avg",
    "kafka.stream.state.prefix.scan.latency.max",
    "kafka.stream.state.prefix.scan.rate",
    "kafka.stream.state.put.all.latency.avg",
    "kafka.stream.state.put.all.latency.max",
    "kafka.stream.state.put.all.rate",
    "kafka.stream.state.put.if.absent.latency.avg",
    "kafka.stream.state.put.if.absent.latency.max",
    "kafka.stream.state.put.if.absent.rate",
    "kafka.stream.state.put.latency.avg",
    "kafka.stream.state.put.latency.max",
    "kafka.stream.state.put.rate",
    "kafka.stream.state.range.latency.avg",
    "kafka.stream.state.range.latency.max",
    "kafka.stream.state.range.rate",
    "kafka.stream.state.record.e2e.latency.avg",
    "kafka.stream.state.record.e2e.latency.max",
    "kafka.stream.state.record.e2e.latency.min",
    "kafka.stream.state.restore.latency.avg",
    "kafka.stream.state.restore.latency.max",
    "kafka.stream.state.restore.rate",
    "kafka.stream.state.size.all.mem.tables",
    "kafka.stream.state.total.sst.files.size",
    "kafka.stream.state.write.stall.duration.avg",
    "kafka.stream.state.write.stall.duration.total",
    "kafka.stream.task.active.buffer.count",
    "kafka.stream.task.active.process.ratio",
    "kafka.stream.task.dropped.records.rate",
    "kafka.stream.task.dropped.records.total",
    "kafka.stream.task.enforced.processing.rate",
    "kafka.stream.task.enforced.processing.total",
    "kafka.stream.task.process.latency.avg",
    "kafka.stream.task.process.latency.max",
    "kafka.stream.task.process.rate",
    "kafka.stream.task.process.total",
    "kafka.stream.task.punctuate.latency.avg",
    "kafka.stream.task.punctuate.latency.max",
    "kafka.stream.task.punctuate.rate",
    "kafka.stream.task.punctuate.total",
    "kafka.stream.task.record.lateness.avg",
    "kafka.stream.task.record.lateness.max",
    "kafka.stream.task.restore.rate",
    "kafka.stream.task.restore.remaining.records.total",
    "kafka.stream.task.restore.total",
    "kafka.stream.thread.blocked.time.ns.total",
    "kafka.stream.thread.commit.latency.avg",
    "kafka.stream.thread.commit.latency.max",
    "kafka.stream.thread.commit.rate",
    "kafka.stream.thread.commit.ratio",
    "kafka.stream.thread.commit.total",
    "kafka.stream.thread.poll.latency.avg",
    "kafka.stream.thread.poll.latency.max",
    "kafka.stream.thread.poll.rate",
    "kafka.stream.thread.poll.ratio",
    "kafka.stream.thread.poll.records.avg",
    "kafka.stream.thread.poll.records.max",
    "kafka.stream.thread.poll.total",
    "kafka.stream.thread.process.latency.avg",
    "kafka.stream.thread.process.latency.max",
    "kafka.stream.thread.process.rate",
    "kafka.stream.thread.process.ratio",
    "kafka.stream.thread.process.records.avg",
    "kafka.stream.thread.process.records.max",
    "kafka.stream.thread.process.total",
    "kafka.stream.thread.punctuate.latency.avg",
    "kafka.stream.thread.punctuate.latency.max",
    "kafka.stream.thread.punctuate.rate",
    "kafka.stream.thread.punctuate.ratio",
    "kafka.stream.thread.punctuate.total",
    "kafka.stream.thread.task.closed.rate",
    "kafka.stream.thread.task.closed.total",
    "kafka.stream.thread.task.created.rate",
    "kafka.stream.thread.task.created.total",
    "kafka.stream.thread.thread.start.time",
    "kafka.stream.topic.bytes.consumed.total",
    "kafka.stream.topic.bytes.produced.total",
    "kafka.stream.topic.records.consumed.total",
    "kafka.stream.topic.records.produced.total",
```

Info about metrics: https://docs.confluent.io/platform/current/streams/monitoring.html#


### Metrics in prometheus format

```bash
$ http :8080/actuator/prometheus
```

### Read from the state store

```bash
kafka-console-consumer \
    --bootstrap-server localhost:19092 \
    --topic word-count-KSTREAM-AGGREGATE-STATE-STORE-0000000005-changelog \
    --property "print.key=true" \
    --value-deserializer "org.apache.kafka.common.serialization.LongDeserializer" \
    --from-beginning
```
