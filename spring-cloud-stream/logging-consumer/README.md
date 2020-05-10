Quick start:

https://cloud.spring.io/spring-cloud-static/spring-cloud-stream/current/reference/html/spring-cloud-stream.html#_quick_start


Created topics:

```bash
$ kafka-topics --bootstrap-server localhost:9092 --list
```

Automatically created `log-in-0` topic.

```bash
$ kafka-topics --bootstrap-server localhost:9092 --topic log-in-0 --describe

Topic: log-in-0	PartitionCount: 1	ReplicationFactor: 1	Configs:
	Topic: log-in-0	Partition: 0	Leader: 1	Replicas: 1	Isr: 1	Offline:
```

Create some payload
```bash
$ kafka-console-producer --bootstrap-server localhost:9092 --topic log-in-0
> {"name":"John Doe"}
```

In the logs of the logging-consumer you should see

```bash
Received: John Doe
```

Now whenever you restart the application it will create a new `group id` for the 1 consumer contained `consumer group`

```bash
2020-05-10 13:13:18.013  INFO 25008 --- [           main] o.a.k.clients.consumer.KafkaConsumer     : [Consumer clientId=consumer-2, groupId=anonymous.f8fbf3d5-3464-4ff6-b052-bf836cc49cdd] Subscribed to topic(s): log-in-0
``` 

You can override this via

```bash
spring:
  cloud:
    stream:
      bindings:
        log-in-0:
          group: logging-consumer
```

and then the logs:

```bash
2020-05-10 13:17:05.509  INFO 25588 --- [           main] o.a.k.clients.consumer.KafkaConsumer     : [Consumer clientId=consumer-2, groupId=logging-consumer] Subscribed to topic(s): log-in-0
```

The name of the topic you can override via:

```bash
spring:
  cloud:
    stream:
      bindings:
        log-in-0:
          destination: people
```

With the `spring.cloud.function.definition` we can explicitly declare which function bean we want to be bound to binding destinations. In our case was not needed since we have only one such bean.

