### Confluent CLI with local cluster 

#### Start a local kafka cluster

Only one instance can be started

```bash
$ confluent local kafka start

Status: Image is up to date for confluentinc/confluent-local:7.6.0
+-----------------+-------+
| Kafka REST Port | 8082  |
| Plaintext Ports | 54026 |
+-----------------+-------+
```

The local kafka cluster is running via docker

```bash
$ docker ps

CONTAINER ID   IMAGE                                COMMAND                  CREATED         STATUS         PORTS                                                        NAMES
2ba119748868   confluentinc/confluent-local:7.6.0   "bash -c '/etc/confl…"   2 minutes ago   Up 2 minutes   0.0.0.0:8082->8082/tcp, 0.0.0.0:54026->54026/tcp, 9092/tcp   confluent-local-broker-1
```


#### List local broker

```bash
$ confluent local kafka broker list
```

#### List topics

```bash
$ confluent local kafka topic list
```

#### Create a topic

```bash
$ confluent local kafka topic create demo
$ confluent local kafka topic describe demo 
```

### Produce to topic

```bash
$ confluent local kafka topic produce demo
```

Or 

```bash
$ confluent kafka topic produce demo --protocol PLAINTEXT --bootstrap localhost:54026
```

### Consume from topic

```bash
$ confluent local kafka topic consume demo --group cli --from-beginning --print-key
```

Or

```bash
$ confluent kafka topic consume demo --protocol PLAINTEXT --bootstrap localhost:54026
```




### Stop local broker

```bash
$ confluent local kafka stop
```