### Kafka with Docker

Uses `confluentinc` images

### Usage

```bash
$ docker-compose -f single-zookeeper-and-kafka.yml up
```

```bash
docker ps -a

CONTAINER ID        IMAGE                             COMMAND                  CREATED             STATUS              PORTS                                        NAMES
1ea8f49bc2ae        confluentinc/cp-kafka:5.3.1       "/etc/confluent/dock…"   2 minutes ago       Up 2 minutes        0.0.0.0:9092->9092/tcp                       kafka
3ffd6983fe4a        confluentinc/cp-zookeeper:5.3.1   "/etc/confluent/dock…"   2 minutes ago       Up 2 minutes        2888/tcp, 0.0.0.0:2181->2181/tcp, 3888/tcp   zookeeper
```

```bash
$ docker-compose -f single-zookeeper-and-kafka.yml down
```

### Resources:

https://github.com/confluentinc/examples/blob/5.3.1-post/cp-all-in-one/docker-compose.yml
https://docs.confluent.io/current/installation/docker/image-reference.html#image-reference
https://github.com/simplesteph/kafka-stack-docker-compose/