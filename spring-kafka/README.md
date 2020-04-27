
1. Start up a single instance Zookeeper and single instance Kafka

```bash
$ docker-compose up -d
```

2. Start up `simple-producer` and `simple-consumer`

Both Kafka clients interact with a topic which is created by the first client which starts up. 

3. `simple-producer` exposes a REST endpoint `/messages` where we can POST messages

```bash
$ echo "hello" | http post :8080/messages
```

4. Notify in the logs of `simple-consumer` that the message was received

5. View consumer groups 

```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --list
group-id
```

6. Describe a group 

```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --group messages-group --describe

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                     HOST            CLIENT-ID
messages-group  messages        3          0               0               0               consumer-2-7bb8b80f-2674-4024-9089-d98901db1ee9 /172.20.0.1     consumer-2
messages-group  messages        2          0               0               0               consumer-2-7bb8b80f-2674-4024-9089-d98901db1ee9 /172.20.0.1     consumer-2
messages-group  messages        4          0               0               0               consumer-2-7bb8b80f-2674-4024-9089-d98901db1ee9 /172.20.0.1     consumer-2
messages-group  messages        1          0               0               0               consumer-2-7bb8b80f-2674-4024-9089-d98901db1ee9 /172.20.0.1     consumer-2
messages-group  messages        5          0               0               0               consumer-2-7bb8b80f-2674-4024-9089-d98901db1ee9 /172.20.0.1     consumer-2
messages-group  messages        0          0               0               0               consumer-2-7bb8b80f-2674-4024-9089-d98901db1ee9 /172.20.0.1     consumer-2
```

```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --group messages-dlt-group --describe

GROUP              TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                     HOST            CLIENT-ID
messages-dlt-group messages.DLT    0          0               0               0               consumer-1-251eb98f-7fb7-447b-bf70-c6016ce8b2b7 /172.19.0.1     consumer-1
```

Set the `concurrency` field to 3


```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --group messages-group --describe

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                     HOST            CLIENT-ID
messages-group  messages        4          0               0               0               consumer-4-acabb6ec-fb9b-4335-8942-592dd7893c4a /172.20.0.1     consumer-4
messages-group  messages        5          0               0               0               consumer-4-acabb6ec-fb9b-4335-8942-592dd7893c4a /172.20.0.1     consumer-4
messages-group  messages        0          0               0               0               consumer-2-10ff4d3e-5e01-4b4b-83ea-ccaa421301dc /172.20.0.1     consumer-2
messages-group  messages        1          0               0               0               consumer-2-10ff4d3e-5e01-4b4b-83ea-ccaa421301dc /172.20.0.1     consumer-2
messages-group  messages        2          0               0               0               consumer-3-068d849d-e31c-468d-ae2f-532418e4d332 /172.20.0.1     consumer-3
messages-group  messages        3          0               0               0               consumer-3-068d849d-e31c-468d-ae2f-532418e4d332 /172.20.0.1     consumer-3
```

For the `messages-dlt-group` is still 1 consumer

```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --group messages-dlt-group --describe

GROUP              TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                     HOST            CLIENT-ID
messages-dlt-group messages.DLT    0          0               0               0               consumer-1-251eb98f-7fb7-447b-bf70-c6016ce8b2b7 /172.19.0.1     consumer-1
```

Start a new `SimpleConsumerApplication` instance

```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --group messages-group --describe

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                     HOST            CLIENT-ID
messages-group  messages        5          0               0               0               consumer-4-acabb6ec-fb9b-4335-8942-592dd7893c4a /172.20.0.1     consumer-4
messages-group  messages        1          0               0               0               consumer-2-bd1b9d91-b53d-41f4-ada2-36d8ef05569c /172.20.0.1     consumer-2
messages-group  messages        4          0               0               0               consumer-4-15aca72e-e0bc-4cbb-8caf-063b1e057136 /172.20.0.1     consumer-4
messages-group  messages        0          0               0               0               consumer-2-10ff4d3e-5e01-4b4b-83ea-ccaa421301dc /172.20.0.1     consumer-2
messages-group  messages        2          0               0               0               consumer-3-04664b44-1ee2-4ce9-a245-414f12a0c594 /172.20.0.1     consumer-3
messages-group  messages        3          0               0               0               consumer-3-068d849d-e31c-468d-ae2f-532418e4d332 /172.20.0.1     consumer-3
```

TODO Fix CLIENT-ID

`groupId` - clear, with consumer groups
`id` - lookup a `MessageListenerContainer` from `KafkaListenerEndpointRegistry`
`clientId` - `client.id` for Kafka Consumer: https://kafka.apache.org/documentation/#consumerconfigs

The id is something internal for the Spring Application Context. This should not be unique globally. 
The groupId is indeed good option for scaling when you would like to have several competing consumers on the same topic. The clientIdPrefix is really useful to determine via monitoring system what and where is consuming.
That's like a global identificator for your application.


Set the `clientIdPrefix` to `"${spring.application.name}"`

```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --group messages-group --describe

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                       HOST            CLIENT-ID
messages-group  messages        4          0               0               0               consumer-2-1-0bada06f-d44f-4c9b-91ab-80e8c1c7b859 /172.20.0.1     consumer-2-1
messages-group  messages        3          0               0               0               consumer-2-0-ecca760a-f014-4bb8-b7cd-47bebac98159 /172.20.0.1     consumer-2-0
messages-group  messages        1          0               0               0               consumer-1-1-853d07bf-6ca0-46d7-b7ee-61ef249f08dc /172.20.0.1     consumer-1-1
messages-group  messages        2          0               0               0               consumer-1-2-8f5a7227-4353-4418-8a54-5fcc4bce8adc /172.20.0.1     consumer-1-2
messages-group  messages        0          0               0               0               consumer-1-0-26ade0cb-3e72-47cf-8907-d66fb12ef66f /172.20.0.1     consumer-1-0
messages-group  messages        5          0               0               0               consumer-2-2-c9dfa2b2-fb80-4a6c-9aea-214db365f20a /172.20.0.1     consumer-2-2
```

`autoStartup = "false"`

Expose the `KafkaListenerEndpointRegistry` through a controller and start it on both instances using:

```bash
$ http post :8081/containers/{id}
$ http post :8082/containers/{id}
```

Exposed `MessageListenerContainer`

```bash
$ http :8081/containers

[
"dlt-messages-container",
"messages-container",
]
```


7. Reset offset

What to do when there is no initial offset in Kafka or if the current offset no longer exists (for example the consumer group was deleted) 
on the server we set it to `earliest` with. (default is `latest`)

```yaml
spring:
  kafka:
    consumer:
      auto-offset-reset: earliest
``` 

Reset offset (to-earliest, to-latest, to-offset <Long>) when the group is 'inactive' and only printing out what will be the change (--dry-run)

```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --reset-offsets --group group-id --to-earliest --topic messages --dry-run
```

Execute the `--reset-offsets`

```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --reset-offsets --group group-id --to-earliest --topic messages --execute
```

8. Delete a consumer group

```bash
$ kafka-consumer-groups --bootstrap-server localhost:9092 --group group-id --delete
```

9. Shut down cluster

```bash
$ docker-compose down
```

`messages` topic crated with 3 partitions

2020-03-28 12:05:37.637  INFO 90844 --- [container-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : messages-group: partitions assigned: [messages-0, messages-1, messages-2]
2020-03-28 12:05:37.637  INFO 90844 --- [container-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : messages-dlt-group: partitions assigned: [messages.DLT-0]


------------------------------------------------------------------------------------------------------------------------

Transactions

spring.kafka.producer.transaction-id-prefix=tx.

```bash
Caused by: java.lang.IllegalStateException: No transaction is in process; possible solutions: 
- run the template operation within the scope of a template.executeInTransaction() operation,
- start a transaction with @Transactional before invoking the template method, 
- run in a transaction started by a listener container when consuming a record
```




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