Start

```bash
$ docker-compose up -d
```

Run the application and check the logs

```bash
21:03:47.385 [main] DEBUG o.a.k.clients.producer.KafkaProducer - [Producer clientId=producer-producer-1, transactionalId=producer-1] Kafka producer started
21:03:47.386 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] Transition from state UNINITIALIZED to INITIALIZING
21:03:47.386 [main] INFO  o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] ProducerId set to -1 with epoch -1
21:03:47.493 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] Enqueuing transactional request InitProducerIdRequestData(transactionalId='producer-1', transactionTimeoutMs=60000)
21:03:47.494 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] Enqueuing transactional request FindCoordinatorRequestData(key='producer-1', keyType=1)
21:03:47.494 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] Enqueuing transactional request InitProducerIdRequestData(transactionalId='producer-1', transactionTimeoutMs=60000)
21:03:47.610 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-producer-1, transactionalId=producer-1] Sending transactional request FindCoordinatorRequestData(key='producer-1', keyType=1) to node localhost:19092 (id: -1 rack: null)
21:03:47.618 [kafka-producer-network-thread | producer-producer-1] INFO  org.apache.kafka.clients.Metadata - [Producer clientId=producer-producer-1, transactionalId=producer-1] Cluster ID: 8d8vCAmrTTKtsDW2u9-N4Q
21:03:47.726 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-producer-1, transactionalId=producer-1] Sending transactional request InitProducerIdRequestData(transactionalId='producer-1', transactionTimeoutMs=60000) to node localhost:19092 (id: 101 rack: null)
21:03:47.728 [kafka-producer-network-thread | producer-producer-1] INFO  o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] ProducerId set to 0 with epoch 7
21:03:47.729 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] Transition from state INITIALIZING to READY
21:03:47.729 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] Transition from state READY to IN_TRANSACTION
21:03:47.761 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] Begin adding new partition demo-topic-0 to transaction
21:03:47.762 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] Transition from state IN_TRANSACTION to COMMITTING_TRANSACTION
21:03:47.764 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] Enqueuing transactional request (type=AddPartitionsToTxnRequest, transactionalId=producer-1, producerId=0, producerEpoch=7, partitions=[demo-topic-0])
21:03:47.764 [main] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] Enqueuing transactional request (type=EndTxnRequest, transactionalId=producer-1, producerId=0, producerEpoch=7, result=COMMIT)
21:03:47.765 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-producer-1, transactionalId=producer-1] Sending transactional request (type=AddPartitionsToTxnRequest, transactionalId=producer-1, producerId=0, producerEpoch=7, partitions=[demo-topic-0]) to node localhost:19092 (id: 101 rack: null)
21:03:47.771 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] Successfully added partitions [demo-topic-0] to transaction
21:03:47.772 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.p.i.RecordAccumulator - [Producer clientId=producer-producer-1, transactionalId=producer-1] Assigned producerId 0 and producerEpoch 7 to batch with base sequence 0 being sent to partition demo-topic-0
21:03:47.781 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] ProducerId: 0; Set last ack'd sequence number for topic-partition demo-topic-0 to 1
21:03:47.783 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-producer-1, transactionalId=producer-1] Sending transactional request (type=EndTxnRequest, transactionalId=producer-1, producerId=0, producerEpoch=7, result=COMMIT) to node localhost:19092 (id: 101 rack: null)
21:03:47.787 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.p.i.TransactionManager - [Producer clientId=producer-producer-1, transactionalId=producer-1] Transition from state COMMITTING_TRANSACTION to READY
Closing producer.
21:03:47.789 [Thread-0] INFO  o.a.k.clients.producer.KafkaProducer - [Producer clientId=producer-producer-1, transactionalId=producer-1] Closing the Kafka producer with timeoutMillis = 9223372036854775807 ms.
21:03:47.789 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-producer-1, transactionalId=producer-1] Beginning shutdown of Kafka producer I/O thread, sending remaining records.
21:03:47.797 [kafka-producer-network-thread | producer-producer-1] DEBUG o.a.k.c.producer.internals.Sender - [Producer clientId=producer-producer-1, transactionalId=producer-1] Shutdown of Kafka producer I/O thread has completed.
21:03:47.798 [Thread-0] DEBUG o.a.k.clients.producer.KafkaProducer - [Producer clientId=producer-producer-1, transactionalId=producer-1] Kafka producer has been closed
```

```bash
$ kafka-console-consumer --bootstrap-server localhost:19092 --topic demo-topic
Put any space separated data here for count
Output will contain count of every word in the message
```


TransactionalWordCount does not work yet.



Resources
* https://www.baeldung.com/kafka-exactly-once
