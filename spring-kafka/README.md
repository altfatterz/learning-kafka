
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
$ kafka-consumer-groups --bootstrap-server localhost:9092 --group group-id --describe

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                     HOST            CLIENT-ID
group-id        messages        0          2               2               0               consumer-1-d3e99b2f-f4fd-4c9c-a07b-7a84ef002bc1 /172.27.0.1     consumer-1
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

Resources:
1. .https://www.confluent.io/blog/apache-kafka-spring-boot-application/