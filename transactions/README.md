* Start the environment

```bash
$ docker compose up -d
```
* See the topics

```bash
$ docker exec -it tools bash
$ kafka-topics --bootstrap-server kafka:9092 --list
```

* Produce to the input topic

Produce into the `input-topic`
```bash
$ docker exec -it tools bash
$ kafkacat -P -b kafka:9092 -t input-topic
```

* Consume from the `output-topic`

```bash
$ docker exec -it tools bash
$ kafkacat -C -b kafka:9092 -K: -t output-topic
```

* Start the application

```bash
09:50:49.212 [main] DEBUG o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=consumer-my-group-id-1, groupId=my-group-id] Executing onJoinComplete with generation 1 and memberId consumer-my-group-id-1-7a9b71a7-ac79-41a4-8f7f-c48a0ff42b96
09:50:49.214 [main] INFO  o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=consumer-my-group-id-1, groupId=my-group-id] Adding newly assigned partitions: input-topic-2, input-topic-0, input-topic-1
09:50:49.220 [main] DEBUG o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=consumer-my-group-id-1, groupId=my-group-id] Fetching committed offsets for partitions: [input-topic-2, input-topic-0, input-topic-1]
09:50:49.224 [main] INFO  o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=consumer-my-group-id-1, groupId=my-group-id] Found no committed offset for partition input-topic-2
09:50:49.224 [main] INFO  o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=consumer-my-group-id-1, groupId=my-group-id] Found no committed offset for partition input-topic-0
09:50:49.224 [main] INFO  o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=consumer-my-group-id-1, groupId=my-group-id] Found no committed offset for partition input-topic-1
09:50:49.237 [main] INFO  o.a.k.c.c.i.SubscriptionState - [Consumer clientId=consumer-my-group-id-1, groupId=my-group-id] Resetting offset for partition input-topic-2 to offset 0.
09:50:49.237 [main] INFO  o.a.k.c.c.i.SubscriptionState - [Consumer clientId=consumer-my-group-id-1, groupId=my-group-id] Resetting offset for partition input-topic-0 to offset 0.
09:50:49.237 [main] INFO  o.a.k.c.c.i.SubscriptionState - [Consumer clientId=consumer-my-group-id-1, groupId=my-group-id] Resetting offset for partition input-topic-1 to offset 0.
```

* List the consumer groups:

```bash
$ docker exec -it tools bash
$ kafka-consumer-groups --bootstrap-server kafka:9092 --list
$ kafka-consumer-groups --bootstrap-server kafka:9092 --describe --group my-group-id

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                                 HOST            CLIENT-ID
my-group-id     input-topic     0          -               0               -               consumer-my-group-id-1-7a9b71a7-ac79-41a4-8f7f-c48a0ff42b96 /192.168.144.1  consumer-my-group-id-1
my-group-id     input-topic     1          -               0               -               consumer-my-group-id-1-7a9b71a7-ac79-41a4-8f7f-c48a0ff42b96 /192.168.144.1  consumer-my-group-id-1
my-group-id     input-topic     2          -               0               -               consumer-my-group-id-1-7a9b71a7-ac79-41a4-8f7f-c48a0ff42b96 /192.168.144.1  consumer-my-group-id-1
```



* Add the following to the console producer
```bash
Lorem ipsum dolor sit amet, consectetur adipiscing elit.
Vivamus sollicitudin sem ac enim viverra, in ultrices leo gravida.
Praesent eu purus posuere, sagittis metus a, tincidunt arcu.
Praesent mattis sem blandit fringilla venenatis.
Praesent sit amet mauris nec nibh feugiat interdum.
Ut id quam tempus, tristique nisl non, maximus lectus.
Aenean vitae turpis id orci auctor dictum.
Nunc semper arcu quis dui placerat, eu semper quam faucibus.
Aliquam sed quam elementum, tempus turpis quis, condimentum dui.
Praesent pellentesque purus egestas ex dictum, nec finibus arcu pulvinar.
Pellentesque aliquam risus sagittis, dictum urna vel, blandit augue.
Phasellus at risus suscipit, maximus elit et, dignissim nulla.
Mauris egestas neque nec libero convallis viverra.
Donec aliquam lacus eu dapibus condimentum.
Donec placerat mi et ligula cursus elementum.

```

In the logs we will see:

```bash
09:54:45.206 [main] INFO  com.example.TransactionalWordCount - Polled 10 records
09:54:45.206 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Transition from state READY to IN_TRANSACTION
09:54:45.218 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Begin adding new partition output-topic-0 to transaction
09:54:45.219 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Enqueuing transactional request (type=AddPartitionsToTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, partitions=[output-topic-0])
09:54:45.220 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Sending transactional request (type=AddPartitionsToTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, partitions=[output-topic-0]) to node localhost:19092 (id: 101 rack: null)
09:54:45.222 [main] INFO  com.example.TransactionalWordCount - Topic partition 2 last offset 4
09:54:45.223 [main] INFO  com.example.TransactionalWordCount - Topic partition 0 last offset 0
09:54:45.223 [main] INFO  com.example.TransactionalWordCount - Topic partition 1 last offset 3
09:54:45.223 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Begin adding offsets {input-topic-2=OffsetAndMetadata{offset=5, leaderEpoch=null, metadata=''}, input-topic-0=OffsetAndMetadata{offset=1, leaderEpoch=null, metadata=''}, input-topic-1=OffsetAndMetadata{offset=4, leaderEpoch=null, metadata=''}} for consumer group my-group-id to transaction
09:54:45.223 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Enqueuing transactional request (type=AddOffsetsToTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, consumerGroupId=my-group-id)
09:54:45.230 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Successfully added partitions [output-topic-0] to transaction
09:54:45.231 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Sending transactional request (type=AddOffsetsToTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, consumerGroupId=my-group-id) to node localhost:19092 (id: 101 rack: null)
09:54:45.233 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Successfully added partition for consumer group my-group-id to transaction
09:54:45.234 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Sending transactional request TxnOffsetCommitRequestData(transactionalId='transactional-wordcount-1', groupId='my-group-id', producerId=0, producerEpoch=0, topics=[TxnOffsetCommitRequestTopic(name='input-topic', partitions=[TxnOffsetCommitRequestPartition(partitionIndex=2, committedOffset=5, committedLeaderEpoch=-1, committedMetadata=''), TxnOffsetCommitRequestPartition(partitionIndex=0, committedOffset=1, committedLeaderEpoch=-1, committedMetadata=''), TxnOffsetCommitRequestPartition(partitionIndex=1, committedOffset=4, committedLeaderEpoch=-1, committedMetadata='')])]) to node localhost:19092 (id: 101 rack: null)
09:54:45.246 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Received TxnOffsetCommit response for consumer group my-group-id: {input-topic-2=NONE, input-topic-0=NONE, input-topic-1=NONE}
09:54:45.247 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Transition from state IN_TRANSACTION to COMMITTING_TRANSACTION
09:54:45.247 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Enqueuing transactional request (type=EndTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, result=COMMIT)
09:54:45.247 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.RecordAccumulator - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Assigned producerId 0 and producerEpoch 0 to batch with base sequence 0 being sent to partition output-topic-0
09:54:45.252 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] ProducerId: 0; Set last ack'd sequence number for topic-partition output-topic-0 to 71
09:54:45.253 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Sending transactional request (type=EndTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, result=COMMIT) to node localhost:19092 (id: 101 rack: null)
09:54:45.256 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Transition from state COMMITTING_TRANSACTION to READY
09:54:45.257 [main] INFO  com.example.TransactionalWordCount - Polled 5 records
09:54:45.257 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Transition from state READY to IN_TRANSACTION
09:54:45.257 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Begin adding new partition output-topic-0 to transaction
09:54:45.258 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Enqueuing transactional request (type=AddPartitionsToTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, partitions=[output-topic-0])
09:54:45.258 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Sending transactional request (type=AddPartitionsToTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, partitions=[output-topic-0]) to node localhost:19092 (id: 101 rack: null)
09:54:45.259 [main] INFO  com.example.TransactionalWordCount - Topic partition 0 last offset 3
09:54:45.259 [main] INFO  com.example.TransactionalWordCount - Topic partition 1 last offset 5
09:54:45.259 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Begin adding offsets {input-topic-0=OffsetAndMetadata{offset=4, leaderEpoch=null, metadata=''}, input-topic-1=OffsetAndMetadata{offset=6, leaderEpoch=null, metadata=''}} for consumer group my-group-id to transaction
09:54:45.259 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Enqueuing transactional request (type=AddOffsetsToTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, consumerGroupId=my-group-id)
09:54:45.261 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Enqueuing transactional request (type=AddPartitionsToTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, partitions=[output-topic-0])
09:54:45.261 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Sending transactional request (type=AddOffsetsToTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, consumerGroupId=my-group-id) to node localhost:19092 (id: 101 rack: null)
09:54:45.264 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Successfully added partition for consumer group my-group-id to transaction
09:54:45.289 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Sending transactional request (type=AddPartitionsToTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, partitions=[output-topic-0]) to node localhost:19092 (id: 101 rack: null)
09:54:45.292 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Successfully added partitions [output-topic-0] to transaction
09:54:45.292 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Sending transactional request TxnOffsetCommitRequestData(transactionalId='transactional-wordcount-1', groupId='my-group-id', producerId=0, producerEpoch=0, topics=[TxnOffsetCommitRequestTopic(name='input-topic', partitions=[TxnOffsetCommitRequestPartition(partitionIndex=0, committedOffset=4, committedLeaderEpoch=-1, committedMetadata=''), TxnOffsetCommitRequestPartition(partitionIndex=1, committedOffset=6, committedLeaderEpoch=-1, committedMetadata='')])]) to node localhost:19092 (id: 101 rack: null)
09:54:45.294 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Received TxnOffsetCommit response for consumer group my-group-id: {input-topic-0=NONE, input-topic-1=NONE}
09:54:45.294 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Transition from state IN_TRANSACTION to COMMITTING_TRANSACTION
09:54:45.295 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Enqueuing transactional request (type=EndTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, result=COMMIT)
09:54:45.295 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.RecordAccumulator - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Assigned producerId 0 and producerEpoch 0 to batch with base sequence 72 being sent to partition output-topic-0
09:54:45.298 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] ProducerId: 0; Set last ack'd sequence number for topic-partition output-topic-0 to 106
09:54:45.298 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Sending transactional request (type=EndTxnRequest, transactionalId=transactional-wordcount-1, producerId=0, producerEpoch=0, result=COMMIT) to node localhost:19092 (id: 101 rack: null)
09:54:45.301 [kafka-producer-network-thread | producer-transactional-wordcount-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-transactional-wordcount-1, transactionalId=transactional-wordcount-1] Transition from state COMMITTING_TRANSACTION to READY
```

* Try to run two instances of the TransactionWordCount and examine the logs


Resources
* https://www.baeldung.com/kafka-exactly-once
