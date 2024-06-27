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
$ echo "Chuck Norris makes onions cry." | http post :8080/facts
$ http :8080/facts
```

```bash
$ mongosh --port <port>
$ db.version
7.0.11
```

### Try also Testcontainer Desktop running locally or on the Cloud

```bash
$ kcat -b localhost:52957 -L
 1 brokers:
  broker 1 at localhost:52957 (controller)
 0 topics:
$ kafka-topics --bootstrap-server localhost:<port> --version
7.6.1-ce
```

- easy to switch container runtimes
- config 
```bash
$ cat ~/.testcontainers.properties
$ ls ~/.config/testcontainers/services
created postgres.toml - to fix the port on host to 5432 - only worked for embedded runtime not for Docker Desktop
```

```bash
$ docker context list
desktop-linux *   Docker Desktop                            unix:///Users/altfatterz/.docker/run/docker.sock
tcd               Testcontainers Desktop                    tcp://127.0.0.1:51104

$ docker context use tcd
$ docker ps
```

Open terminal feature - you can connect to local worker or the cloud worker


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
{"value":"Chuck Norris makes onions cry."}
{"value":"Chuck Norris can kill two stones with one bird."}
{"value":"Chuck Norris does not get frostbite. Chuck Norris bites frost."}
{"value":"Death once had a near-Chuck-Norris experience."}
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
