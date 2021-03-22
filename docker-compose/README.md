### Kafka with Docker

Uses `confluentinc` images

### Single Zookeeper and Kafka

```bash
$ docker-compose up -d
```

```bash
docker ps -a

CONTAINER ID   IMAGE                    COMMAND                  CREATED         STATUS         PORTS                                                                                                                                                  NAMES
f803ba38f0ba   lensesio/fast-data-dev   "/usr/bin/dumb-init …"   2 minutes ago   Up 2 minutes   0.0.0.0:2181->2181/tcp, 0.0.0.0:3030->3030/tcp, 0.0.0.0:8081-8083->8081-8083/tcp, 0.0.0.0:9092->9092/tcp, 0.0.0.0:9581-9585->9581-9585/tcp, 3031/tcp   avro-examples_kafka-cluster_1
```

Open in browser

http://localhost:3030/


```bash
$ docker exec -it fast-data-dev bash
```

#### Insomnia

Great HTTP client:
https://insomnia.rest/

```bash
$ brew install insomnia
```


### Resources:

https://github.com/confluentinc/examples/blob/5.3.1-post/cp-all-in-one/docker-compose.yml
https://docs.confluent.io/current/installation/docker/image-reference.html#image-reference
https://github.com/simplesteph/kafka-stack-docker-compose/