## Cluster Setup

```bash
$ docker compose -f up -d
```

JMX monitoring stacks
https://github.com/confluentinc/jmx-monitoring-stacks


### Simple consumer / producer demo

* Start up a single instance Zookeeper and single instance Kafka

```bash
$ docker-compose up -d
```

* Start up `simple-producer` and `simple-consumer`

Both Kafka clients interact with a topic which is created by the first client which starts up. 

* `simple-producer` exposes a REST endpoints where we can POST messages

```bash
$ echo "hello" | http post :8080/messages
$ echo 100 | http post :8080/many-messages
$ echo 5 | http post :8080/partition
```

* Notify in the logs of `simple-consumer` that the message was received

* View consumer groups 

```bash
$ docker exec -it broker bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --list

messages-dlt-group
_confluent-controlcenter-7-5-1-lastProduceTimeConsumer
messages-group
_confluent-controlcenter-7-5-1-1
_confluent-controlcenter-7-5-1-1-command
```

* Check `messages-group` consumer group (consumed with one instance but with 3 threads)

```java
@KafkaListener(groupId = "messages-group", topics = TOPIC,  clientIdPrefix="${spring.application.name}", concurrency = "3") 
```

https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/listener-annotation.html

```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --group messages-group --describe

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                              HOST            CLIENT-ID
messages-group  messages        4          10              10              0               simple-consumer-1-2-37bda5c8-f42f-4214-a75a-fe42dba18b1c /192.168.65.1   simple-consumer-1-2
messages-group  messages        5          23              23              0               simple-consumer-1-2-37bda5c8-f42f-4214-a75a-fe42dba18b1c /192.168.65.1   simple-consumer-1-2
messages-group  messages        0          11              11              0               simple-consumer-1-0-22efd730-ae74-4590-93ec-3d6e832ad68b /192.168.65.1   simple-consumer-1-0
messages-group  messages        1          18              18              0               simple-consumer-1-0-22efd730-ae74-4590-93ec-3d6e832ad68b /192.168.65.1   simple-consumer-1-0
messages-group  messages        2          23              23              0               simple-consumer-1-1-4a5ee839-d22b-4c32-afba-def8d0ea320c /192.168.65.1   simple-consumer-1-1
messages-group  messages        3          17              17              0               simple-consumer-1-1-4a5ee839-d22b-4c32-afba-def8d0ea320c /192.168.65.1   simple-consumer-1-1
```

* View topic:

```bash
$ kafka-topics --bootstrap-server localhost:9092 --topic messages -describe
```

```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --group messages-dlt-group --describe

GROUP              TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                              HOST            CLIENT-ID
messages-dlt-group messages.DLT    0          0               0               0               simple-consumer-1-0-9580fa23-fc72-4292-ab76-3289df56818b /192.168.65.1   simple-consumer-1-0
```

* Exposed `MessageListenerContainer`

```bash
$ http :9090/containers

[
"dlt-messages-container",
"messages-container",
]
```

* Start a new `SimpleConsumerApplication` instance with application arguments:

```bash
--spring.application.name=simple-consumer-2 --server.port=9091
```

```bash
$ kafka-consumer-groups --bootstrap-server localhost:29092 --group messages-group --describe

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                              HOST            CLIENT-ID
messages-group  messages        5          0               0               0               simple-consumer-2-2-f7b366e4-d8f0-470f-86fa-11195d48617a /192.168.65.1   simple-consumer-2-2
messages-group  messages        0          0               0               0               simple-consumer-1-0-68b95cff-c0eb-4086-8612-98c72f84193f /192.168.65.1   simple-consumer-1-0
messages-group  messages        1          0               0               0               simple-consumer-1-1-280f5e8b-b385-4f86-92e3-f78d0737a4cc /192.168.65.1   simple-consumer-1-1
messages-group  messages        3          0               0               0               simple-consumer-2-0-2d5074c6-158a-4d88-a5d6-9c837fbad79f /192.168.65.1   simple-consumer-2-0
messages-group  messages        2          0               0               0               simple-consumer-1-2-3926073d-ba38-43e6-bbe2-543ace9cfc81 /192.168.65.1   simple-consumer-1-2
messages-group  messages        4          0               0               0               simple-consumer-2-1-b847a9f3-9301-4d8c-a82c-2cf230f2ab97 /192.168.65.1   simple-consumer-2-1
```

`groupId` - identifies the `consumer groups`
`clientId` - really useful to determine via monitoring system what and where is consuming.

* Reset offset

What to do when there is no initial offset in Kafka or if the current offset no longer exists (for example the consumer group was deleted) 
on the server we set it to `earliest` with. (default is `latest`)

```yaml
spring:
  kafka:
    consumer:
      auto-offset-reset: earliest
``` 

* Reset offset (to-earliest, to-latest, to-offset <Long>) when the group is 'inactive' and only printing out what will be the change (--dry-run)

```bash
$ kafka-consumer-groups --bootstrap-server localhost:29092 --reset-offsets --group messages-group --to-earliest --topic messages --dry-run
$ kafka-consumer-groups --bootstrap-server localhost:29092 --reset-offsets --group messages-group --to-earliest --topic messages --execute
$ kafka-consumer-groups --bootstrap-server localhost:29092 --group messages-group --describe
```

### Poison pill

```bash
$ echo poison-pill | http :8080/messages
````

* Producer logs:

```bash
2023-11-09T11:44:48.767+01:00  INFO 60969 --- [nio-8080-exec-5] c.e.s.SimpleProducerApplication          : Sending payload poison-pill
2023-11-09T11:44:48.769+01:00  INFO 60969 --- [ad | producer-1] c.e.s.SimpleProducerApplication          : success, topic: messages, partition: 4, offset: 12
```

* Consumer logs:

```bash
2025-04-28T22:19:03.911+02:00 ERROR 2077 --- [simple-consumer-1] [ntainer#0-0-C-1] o.s.kafka.listener.DefaultErrorHandler   : Backoff FixedBackOff{interval=0, currentAttempts=10, maxAttempts=9} exhausted for messages-1@19
org.springframework.kafka.listener.ListenerExecutionFailedException: Listener method 'public void com.example.simpleconsumer.SimpleConsumerApplication.consume(java.lang.String)' threw exception
Caused by: java.lang.RuntimeException: failed processing message:poison-pill - 10 times
2023-11-09T11:46:37.671+01:00 DEBUG 61778 --- [ntainer#0-2-C-1] o.s.kafka.listener.DefaultErrorHandler   : Skipping seek of: messages-4@12
```

This is the default logic to `DefaultErrorHandler`. More details here: https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html

### Cleanup

* Delete a consumer group

```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --group group-id --delete
```

* Shut down cluster

```bash
$ docker-compose down
```

------------------------------------------------------------------------------------------------------------------------




Resources:
1. Spring for Apache Kafka Deep Dive 
https://www.confluent.io/blog/apache-kafka-spring-boot-application/

2. Spring Boot Apache Kafka Support
https://docs.spring.io/spring-boot/docs/2.2.6.RELEASE/reference/htmlsingle/#boot-features-kafka

3. Spring Apache Kafka documentation
https://docs.spring.io/spring-kafka/reference/html/

4. Spring Boot integration Kafka spring-kafka in-depth exploration
https://programming.vip/docs/spring-boot-integration-kafka-spring-kafka-in-depth-exploration.html

5. Exactly-once Semantics are Possible: Here’s How Kafka Does it 
https://www.confluent.io/blog/exactly-once-semantics-are-possible-heres-how-apache-kafka-does-it/

6. Kafka Streams Examples
https://github.com/confluentinc/kafka-streams-examples