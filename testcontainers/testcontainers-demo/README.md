```bash
$ docker ps 
```

```bash
$ psql -h localhost -p <port> -U postgres test
\d
SELECT version();
PostgreSQL 16.3 (Debian 16.3-1.pgdg120+1) on x86_64-pc-linux-gnu, compiled by gcc (Debian 12.2.0-14) 12.2.0, 64-bit
```

```bash
$ mongosh --port <port>
$ db.version
7.0.11
```

```bash
$ kcat -b localhost:52957 -L
 1 brokers:
  broker 1 at localhost:52957 (controller)
 0 topics:
$ kafka-topics --bootstrap-server localhost:<port> --version
7.6.1-ce
```

### Kafka with Schema Registry

```bash
$ docker ps 
$ export KAFKA_PORT=59480
$ export SCHEMA_REGISTRY_PORT=59536
```

```bash
$ docker network ls
```

```bash
$ docker ps --format '{{ .ID }} {{ .Names }} {{ json .Networks }}' 
```

```bash
$ docker network inspect <network-name>
```

```bash
$ kafka-topics --bootstrap-server localhost:$KAFKA_PORT --create --topic facts
$ kafka-topics --bootstrap-server localhost:$KAFKA_PORT --describe --topic facts
```

```bash
$ kafka-avro-console-producer \
  --topic facts \
  --bootstrap-server localhost:$KAFKA_PORT \
  --property schema.registry.url=http://localhost:$SCHEMA_REGISTRY_PORT \
  --property value.schema="$(< src/main/resources/avro/facts.avsc)" 
```

```bash
{"value":"Chuck Norris does not sleep. He waits."}
{"value":"On the 7th day, God rested ... Chuck Norris took over."}
{"value":"Chuck Norris can dribble a bowling ball."}
```

```bash
$ kafka-avro-console-consumer \
  --topic facts \
  --bootstrap-server localhost:$KAFKA_PORT \
  --property schema.registry.url=http://localhost:$SCHEMA_REGISTRY_PORT \
  --from-beginning
```

```bash
$ kafka-consumer-groups --bootstrap-server localhost:$KAFKA_PORT --list
$ kafka-consumer-groups --bootstrap-server localhost:$KAFKA_PORT --describe --list <group-id>
```

Resources:
- Part1: https://medium.com/@nuno.mt.sousa/kafka-schema-registry-junit-and-test-containers-part-i-082d0abfc804
- Part2: https://medium.com/@nuno.mt.sousa/part-ii-creating-a-kafka-cluster-test-extension-569ed750d137
- Part3: https://medium.com/@nuno.mt.sousa/part-iii-reducing-test-time-by-removing-all-topics-between-tests-ec9c829f3e9b