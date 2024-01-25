1. Start the application

2. Check the created topics and consumer groups:

```bash
$ kafka-consumer-groups --bootstrap-server localhost:29092 --group words --describe
$ kafka-consumer-groups --bootstrap-server localhost:29092 --group uppercase_words --describe
$ kafka-topics --bootstrap-server localhost:29092 --describe --topic words
$ kafka-topics --bootstrap-server localhost:29092 --describe --topic uppercase_words
```

3. Trigger the producer:

```bash
$ echo "The quick brown fox jumps over a lazy dog"  | http post :8080/messages1
```

In the logs we see that the consumer is receiving uncommitted messages, not what we expect.

```bash
2023-11-10T09:42:46.310+01:00  INFO 74612 --- [ntainer#1-0-C-1] o.a.k.clients.producer.KafkaProducer     : [Producer clientId=producer-tx-2, transactionalId=tx-2] Instantiated a transactional producer.
2023-11-10T09:42:46.312+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:46.312+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@5e730434]]
2023-11-10T09:42:46.312+01:00  INFO 74612 --- [ntainer#0-0-C-1] c.e.t.TransactionDemoApplication         : Received: [quick]
2023-11-10T09:42:46.312+01:00  INFO 74612 --- [ntainer#1-0-C-1] o.a.kafka.common.utils.AppInfoParser     : Kafka version: 3.4.1
2023-11-10T09:42:46.312+01:00  INFO 74612 --- [ntainer#1-0-C-1] o.a.kafka.common.utils.AppInfoParser     : Kafka commitId: 8a516edc2755df89
2023-11-10T09:42:46.313+01:00  INFO 74612 --- [ntainer#1-0-C-1] o.a.kafka.common.utils.AppInfoParser     : Kafka startTimeMs: 1699605766312
2023-11-10T09:42:46.313+01:00  INFO 74612 --- [ntainer#1-0-C-1] o.a.k.c.p.internals.TransactionManager   : [Producer clientId=producer-tx-2, transactionalId=tx-2] Invoking InitProducerId for the first time in order to acquire a producer ID
2023-11-10T09:42:46.317+01:00  INFO 74612 --- [| producer-tx-2] org.apache.kafka.clients.Metadata        : [Producer clientId=producer-tx-2, transactionalId=tx-2] Cluster ID: MkU3OEVBNTcwNTJENDM2Qg
2023-11-10T09:42:46.317+01:00  INFO 74612 --- [| producer-tx-2] o.a.k.c.p.internals.TransactionManager   : [Producer clientId=producer-tx-2, transactionalId=tx-2] Discovered transaction coordinator localhost:29092 (id: 1 rack: null)
2023-11-10T09:42:46.319+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:46.427+01:00  INFO 74612 --- [| producer-tx-2] o.a.k.c.p.internals.TransactionManager   : [Producer clientId=producer-tx-2, transactionalId=tx-2] ProducerId set to 23 with epoch 2
2023-11-10T09:42:46.428+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@55d78440]]
2023-11-10T09:42:46.428+01:00  INFO 74612 --- [ntainer#1-0-C-1] c.e.t.TransactionDemoApplication         : Received: [THE]
2023-11-10T09:42:46.431+01:00  INFO 74612 --- [| producer-tx-2] o.a.k.c.p.internals.TransactionManager   : [Producer clientId=producer-tx-2, transactionalId=tx-2] Discovered group coordinator localhost:29092 (id: 1 rack: null)
2023-11-10T09:42:46.471+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:46.471+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@5e730434]]
2023-11-10T09:42:46.471+01:00  INFO 74612 --- [ntainer#0-0-C-1] c.e.t.TransactionDemoApplication         : Received: [brown]
2023-11-10T09:42:46.478+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:46.538+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:46.541+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:46.541+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@5e730434]]
2023-11-10T09:42:46.541+01:00  INFO 74612 --- [ntainer#1-0-C-1] c.e.t.TransactionDemoApplication         : Received: [QUICK, BROWN]
2023-11-10T09:42:46.543+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:46.674+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:46.674+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@55d78440]]
2023-11-10T09:42:46.674+01:00  INFO 74612 --- [ntainer#0-0-C-1] c.e.t.TransactionDemoApplication         : Received: [fox]
2023-11-10T09:42:46.676+01:00  INFO 74612 --- [| producer-tx-2] org.apache.kafka.clients.Metadata        : [Producer clientId=producer-tx-2, transactionalId=tx-2] Resetting the last seen epoch of partition uppercase_words-0 to 0 since the associated topicId changed from null to FspFmJoITvyZFB4a0puiOg
2023-11-10T09:42:46.685+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:46.688+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:46.688+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@5e730434]]
2023-11-10T09:42:46.688+01:00  INFO 74612 --- [ntainer#1-0-C-1] c.e.t.TransactionDemoApplication         : Received: [FOX]
2023-11-10T09:42:46.722+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:46.877+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:46.878+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@55d78440]]
2023-11-10T09:42:46.878+01:00  INFO 74612 --- [ntainer#0-0-C-1] c.e.t.TransactionDemoApplication         : Received: [jumps]
2023-11-10T09:42:46.882+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:46.886+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:46.887+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@5e730434]]
2023-11-10T09:42:46.887+01:00  INFO 74612 --- [ntainer#1-0-C-1] c.e.t.TransactionDemoApplication         : Received: [JUMPS]
2023-11-10T09:42:46.892+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:47.081+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:47.081+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@55d78440]]
2023-11-10T09:42:47.081+01:00  INFO 74612 --- [ntainer#0-0-C-1] c.e.t.TransactionDemoApplication         : Received: [over]
2023-11-10T09:42:47.086+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:47.091+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:47.091+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@5e730434]]
2023-11-10T09:42:47.091+01:00  INFO 74612 --- [ntainer#1-0-C-1] c.e.t.TransactionDemoApplication         : Received: [OVER]
2023-11-10T09:42:47.095+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:47.285+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:47.285+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@55d78440]]
2023-11-10T09:42:47.285+01:00  INFO 74612 --- [ntainer#0-0-C-1] c.e.t.TransactionDemoApplication         : Received: [a]
2023-11-10T09:42:47.291+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:47.295+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:47.295+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@5e730434]]
2023-11-10T09:42:47.296+01:00  INFO 74612 --- [ntainer#1-0-C-1] c.e.t.TransactionDemoApplication         : Received: [A]
2023-11-10T09:42:47.302+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:47.488+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:47.488+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@55d78440]]
2023-11-10T09:42:47.488+01:00  INFO 74612 --- [ntainer#0-0-C-1] c.e.t.TransactionDemoApplication         : Received: [lazy]
2023-11-10T09:42:47.493+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:47.495+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:47.496+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@5e730434]]
2023-11-10T09:42:47.496+01:00  INFO 74612 --- [ntainer#1-0-C-1] c.e.t.TransactionDemoApplication         : Received: [LAZY]
2023-11-10T09:42:47.503+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:47.691+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:47.692+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@55d78440]]
2023-11-10T09:42:47.692+01:00  INFO 74612 --- [ntainer#0-0-C-1] c.e.t.TransactionDemoApplication         : Received: [dog]
2023-11-10T09:42:47.697+01:00 DEBUG 74612 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:42:47.702+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:42:47.702+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@5e730434]]
2023-11-10T09:42:47.703+01:00  INFO 74612 --- [ntainer#1-0-C-1] c.e.t.TransactionDemoApplication         : Received: [DOG]
2023-11-10T09:42:47.710+01:00 DEBUG 74612 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit

```

4. Set `isolation.level: read_committed` restart the application and trigger the producer again.

```bash
$ echo "The quick brown fox jumps over a lazy dog"  | http post :8080/messages1
```

Now, all the messages have been received after the producer committed the transaction

```bash
2023-11-10T09:49:43.421+01:00  INFO 74847 --- [nio-8080-exec-1] o.a.k.clients.producer.KafkaProducer     : [Producer clientId=producer-tx-0, transactionalId=tx-0] Instantiated a transactional producer.
2023-11-10T09:49:43.427+01:00  INFO 74847 --- [nio-8080-exec-1] o.a.kafka.common.utils.AppInfoParser     : Kafka version: 3.4.1
2023-11-10T09:49:43.427+01:00  INFO 74847 --- [nio-8080-exec-1] o.a.kafka.common.utils.AppInfoParser     : Kafka commitId: 8a516edc2755df89
2023-11-10T09:49:43.427+01:00  INFO 74847 --- [nio-8080-exec-1] o.a.kafka.common.utils.AppInfoParser     : Kafka startTimeMs: 1699606183427
2023-11-10T09:49:43.428+01:00  INFO 74847 --- [nio-8080-exec-1] o.a.k.c.p.internals.TransactionManager   : [Producer clientId=producer-tx-0, transactionalId=tx-0] Invoking InitProducerId for the first time in order to acquire a producer ID
2023-11-10T09:49:43.431+01:00  INFO 74847 --- [| producer-tx-0] org.apache.kafka.clients.Metadata        : [Producer clientId=producer-tx-0, transactionalId=tx-0] Cluster ID: MkU3OEVBNTcwNTJENDM2Qg
2023-11-10T09:49:43.431+01:00  INFO 74847 --- [| producer-tx-0] o.a.k.c.p.internals.TransactionManager   : [Producer clientId=producer-tx-0, transactionalId=tx-0] Discovered transaction coordinator localhost:29092 (id: 1 rack: null)
2023-11-10T09:49:43.540+01:00  INFO 74847 --- [| producer-tx-0] o.a.k.c.p.internals.TransactionManager   : [Producer clientId=producer-tx-0, transactionalId=tx-0] ProducerId set to 21 with epoch 11
2023-11-10T09:49:43.547+01:00  INFO 74847 --- [| producer-tx-0] org.apache.kafka.clients.Metadata        : [Producer clientId=producer-tx-0, transactionalId=tx-0] Resetting the last seen epoch of partition words-0 to 0 since the associated topicId changed from null to wfgWxUyVQvaDaqc7_t2XSQ
2023-11-10T09:49:45.397+01:00 DEBUG 74847 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:49:45.399+01:00 DEBUG 74847 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@15bb0a4b]]
2023-11-10T09:49:45.402+01:00  INFO 74847 --- [ntainer#0-0-C-1] c.e.t.TransactionDemoApplication         : Received: [The, quick, brown, fox, jumps, over, a, lazy, dog]
2023-11-10T09:49:45.404+01:00  INFO 74847 --- [| producer-tx-0] org.apache.kafka.clients.Metadata        : [Producer clientId=producer-tx-0, transactionalId=tx-0] Resetting the last seen epoch of partition uppercase_words-0 to 0 since the associated topicId changed from null to FspFmJoITvyZFB4a0puiOg
2023-11-10T09:49:45.413+01:00  INFO 74847 --- [| producer-tx-0] o.a.k.c.p.internals.TransactionManager   : [Producer clientId=producer-tx-0, transactionalId=tx-0] Discovered group coordinator localhost:29092 (id: 1 rack: null)
2023-11-10T09:49:45.523+01:00 DEBUG 74847 --- [ntainer#0-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
2023-11-10T09:49:45.528+01:00 DEBUG 74847 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2023-11-10T09:49:45.528+01:00 DEBUG 74847 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Created Kafka transaction on producer [CloseSafeProducer [delegate=org.apache.kafka.clients.producer.KafkaProducer@15bb0a4b]]
2023-11-10T09:49:45.528+01:00  INFO 74847 --- [ntainer#1-0-C-1] c.e.t.TransactionDemoApplication         : Received: [THE, QUICK, BROWN, FOX, JUMPS, OVER, A, LAZY, DOG]
2023-11-10T09:49:45.531+01:00 DEBUG 74847 --- [ntainer#1-0-C-1] o.s.k.t.KafkaTransactionManager          : Initiating transaction commit
```

5. Test rollback

```bash
echo "The quick brown fail fox jumps over a lazy dog"  | http post :8080/messages1
```

As expected, the consumer app does not receive any messages.
The rollback is only for unchecked exceptions by default. To rollback checked exceptions we need to specify the `rollbackFor` on the `@Transactional` annotation

```bash
2023-11-10T09:55:32.529+01:00  INFO 75082 --- [nio-8080-exec-1] o.a.k.clients.producer.KafkaProducer     : [Producer clientId=producer-tx-0, transactionalId=tx-0] Instantiated a transactional producer.
2023-11-10T09:55:32.534+01:00  INFO 75082 --- [nio-8080-exec-1] o.a.kafka.common.utils.AppInfoParser     : Kafka version: 3.4.1
2023-11-10T09:55:32.534+01:00  INFO 75082 --- [nio-8080-exec-1] o.a.kafka.common.utils.AppInfoParser     : Kafka commitId: 8a516edc2755df89
2023-11-10T09:55:32.534+01:00  INFO 75082 --- [nio-8080-exec-1] o.a.kafka.common.utils.AppInfoParser     : Kafka startTimeMs: 1699606532534
2023-11-10T09:55:32.535+01:00  INFO 75082 --- [nio-8080-exec-1] o.a.k.c.p.internals.TransactionManager   : [Producer clientId=producer-tx-0, transactionalId=tx-0] Invoking InitProducerId for the first time in order to acquire a producer ID
2023-11-10T09:55:32.538+01:00  INFO 75082 --- [| producer-tx-0] org.apache.kafka.clients.Metadata        : [Producer clientId=producer-tx-0, transactionalId=tx-0] Cluster ID: MkU3OEVBNTcwNTJENDM2Qg
2023-11-10T09:55:32.539+01:00  INFO 75082 --- [| producer-tx-0] o.a.k.c.p.internals.TransactionManager   : [Producer clientId=producer-tx-0, transactionalId=tx-0] Discovered transaction coordinator localhost:29092 (id: 1 rack: null)
2023-11-10T09:55:32.645+01:00  INFO 75082 --- [| producer-tx-0] o.a.k.c.p.internals.TransactionManager   : [Producer clientId=producer-tx-0, transactionalId=tx-0] ProducerId set to 21 with epoch 14
2023-11-10T09:55:32.651+01:00  INFO 75082 --- [| producer-tx-0] org.apache.kafka.clients.Metadata        : [Producer clientId=producer-tx-0, transactionalId=tx-0] Resetting the last seen epoch of partition words-0 to 0 since the associated topicId changed from null to wfgWxUyVQvaDaqc7_t2XSQ
2023-11-10T09:55:33.268+01:00  INFO 75082 --- [nio-8080-exec-1] o.a.k.clients.producer.KafkaProducer     : [Producer clientId=producer-tx-0, transactionalId=tx-0] Aborting incomplete transaction
2023-11-10T09:55:33.276+01:00 ERROR 75082 --- [nio-8080-exec-1] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: java.lang.RuntimeException] with root cause

java.lang.RuntimeException: null
	at com.example.transactiondemo.Producer.lambda$process1$0(Producer.java:20) ~[classes/:na]
	at org.springframework.kafka.core.KafkaTemplate.executeInTransaction(KafkaTemplate.java:634) ~[spring-kafka-3.0.12.jar:3.0.12]
	...
```

Things to understand:

- The transactional producer sends messages to the Kafka cluster even before committing the transaction. 
- You could see it in the previous section, where the listener was continuously receiving messages if the isolation level was `read_uncommited.`
- If we roll back a transaction on the producer side, the messages sent before the rollback occurs reach the Kafka broker. 
- But of course the messages the message are rolled back, but how you might ask. The transaction coordinator changes the values of Kafka offsets.

6. Start another instance with program arguments

```bash
--server.port=8081 --spring.kafka.consumer.client-id=consumer2
```

You will see the two consumer groups are still from the `consumer1`, since the topics have 1 partiton

```bash
$ kafka-consumer-groups --bootstrap-server localhost:29092 --group uppercase_words --describe

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                      HOST            CLIENT-ID
uppercase_words uppercase_words 0          350             351             1               consumer1-0-2f31ef11-535d-4df4-91ad-db2e3028494f /192.168.65.1   consumer1-0
```

```bash
$ kafka-consumer-groups --bootstrap-server localhost:29092 --group words --describe

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                      HOST            CLIENT-ID
words           words           0          290             291             1               consumer1-0-81b5f64c-900e-4495-a398-9d2c91585138 /192.168.65.1   consumer1-0
```

```bash
```