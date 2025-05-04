### Multi broker with pure Kafka

```bash
docker compose up -d
docker ps

CONTAINER ID   IMAGE                            COMMAND                  CREATED          STATUS          PORTS                                NAMES
740d462aff2f   apache/kafka:4.0.0               "/__cacert_entrypoin…"   14 seconds ago   Up 13 seconds   9092/tcp, 0.0.0.0:19092->19092/tcp   broker-1
1002dba9f5c0   apache/kafka:4.0.0               "/__cacert_entrypoin…"   14 seconds ago   Up 13 seconds   9092/tcp, 0.0.0.0:19094->19092/tcp   broker-3
1977c1ba9929   apache/kafka:4.0.0               "/__cacert_entrypoin…"   14 seconds ago   Up 13 seconds   9092/tcp, 0.0.0.0:19093->19092/tcp   broker-2
218fdfd46358   obsidiandynamics/kafdrop:4.1.0   "/kafdrop.sh"            53 seconds ago   Up 13 seconds   0.0.0.0:9000->9000/tcp               multi-broker-pure-kafka-kafdrop-1
```

Check broker configuration:

```bash
docker exec broker-1 cat /opt/kafka/config/server.properties

advertised.listeners=INTERNAL://broker-1:9092,EXTERNAL://localhost:19092
listeners=INTERNAL://:9092,CONTROLLER://:9093,EXTERNAL://:19092
transaction.state.log.min.isr=2
controller.quorum.voters=101@broker-1:9093,102@broker-2:9093,103@broker-3:9093
transaction.state.log.replication.factor=3
node.id=101
process.roles=broker,controller
listener.security.protocol.map=INTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT
inter.broker.listener.name=INTERNAL
controller.listener.names=CONTROLLER
offsets.topic.replication.factor=3
log.dirs=/var/lib/kafka/data

docker exec broker-1 cat /var/lib/kafka/data/meta.properties

cluster.id=n9NwfzpvTDC1yqzy-rif3A
directory.id=RZprPxZybktQe8byVMKUWA
node.id=101
version=1
```

Check `Kafdrop` on http://localhost:9000/

### Create a topic

```bash
docker exec broker-1 ./opt/kafka/bin/kafka-topics.sh \
--create \
--topic products.prices.changelog.min-isr-2  \
--replication-factor 3 \
--partitions 3 \
--config min.insync.replicas=2 \
--bootstrap-server localhost:9092

```

### Describe a topic

```bash
docker exec broker-1 ./opt/kafka/bin/kafka-topics.sh \
--describe \
--topic products.prices.changelog.min-isr-2  \
--bootstrap-server localhost:9092

Topic: products.prices.changelog.min-isr-2	TopicId: y1lclbY-SEaYtCUCMiaPOg	PartitionCount: 3	ReplicationFactor: 3	Configs: min.insync.replicas=2
	Topic: products.prices.changelog.min-isr-2	Partition: 0	Leader: 101	Replicas: 101,102,103	Isr: 101,102,103	Elr: 	LastKnownElr:
	Topic: products.prices.changelog.min-isr-2	Partition: 1	Leader: 102	Replicas: 102,103,101	Isr: 102,103,101	Elr: 	LastKnownElr:
	Topic: products.prices.changelog.min-isr-2	Partition: 2	Leader: 103	Replicas: 103,101,102	Isr: 103,101,102	Elr: 	LastKnownElr:
```

### Stop broker-2 and broker3 and check topic configuration again

```bash
docker stop broker-2
docker stop broker-3
docker exec broker-1 ./opt/kafka/bin/kafka-topics.sh \
--describe \
--topic products.prices.changelog.min-isr-2  \
--bootstrap-server localhost:9092

Topic: products.prices.changelog.min-isr-2	TopicId: y1lclbY-SEaYtCUCMiaPOg	PartitionCount: 3	ReplicationFactor: 3	Configs: min.insync.replicas=2
	Topic: products.prices.changelog.min-isr-2	Partition: 0	Leader: 101	Replicas: 101,102,103	Isr: 101	Elr: 	LastKnownElr:
	Topic: products.prices.changelog.min-isr-2	Partition: 1	Leader: 101	Replicas: 102,103,101	Isr: 101	Elr: 	LastKnownElr:
	Topic: products.prices.changelog.min-isr-2	Partition: 2	Leader: 101	Replicas: 103,101,102	Isr: 101	Elr: 	LastKnownElr:
```

Also check http://localhost:9000/ 

### Start consumer:

```bash
docker exec broker-1 ./opt/kafka/bin/kafka-console-consumer.sh \
--topic products.prices.changelog.min-isr-2 \
--from-beginning \
--bootstrap-server localhost:9092
```

### Start producer acks=0

```bash
docker exec -it broker-1 bash
 
# no error is returned and we don't see in the consumer to be received
./opt/kafka/bin/kafka-console-producer.sh \
--topic products.prices.changelog.min-isr-2 \
--bootstrap-server localhost:9092 \
--producer-property acks=0

apple 1

# no error is returned and we don't see in the consumer to be received
./opt/kafka/bin/kafka-console-producer.sh \
--topic products.prices.changelog.min-isr-2 \
--bootstrap-server localhost:9092 \
--producer-property acks=1


# expected WARN and failure here but it does not happen
./opt/kafka/bin/kafka-console-producer.sh \
--topic products.prices.changelog.min-isr-2 \
--bootstrap-server localhost:9092 \
--producer-property acks=all
```

### Delete topic

```bash
docker exec broker-1 ./opt/kafka/bin/kafka-topics.sh \
--delete \
--topic products.prices.changelog.min-isr-2  \
--bootstrap-server localhost:9092
```


