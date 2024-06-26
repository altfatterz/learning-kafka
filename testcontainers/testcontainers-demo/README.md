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
$ docker network ls
```

```bash
$ docker ps --format '{{ .ID }} {{ .Names }} {{ json .Networks }}' 
```

```bash
$ docker network inspect <network-name>
```

An example with redis:

https://github.com/redis-developer/testcontainers-redis