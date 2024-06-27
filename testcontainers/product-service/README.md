Start via `TestProductServiceApplication`

```bash
$ docker ps 
```

```bash
$ docker ps
CONTAINER ID   IMAGE                          COMMAND                  CREATED         STATUS         PORTS                                                        NAMES
4539dbdc8440   confluentinc/cp-kafka:latest   "sh -c 'while [ ! -f…"   4 minutes ago   Up 4 minutes   9092/tcp, 0.0.0.0:57323->2181/tcp, 0.0.0.0:57324->9093/tcp   pensive_knuth
2cc7ebb842b6   postgres:latest                "docker-entrypoint.s…"   5 minutes ago   Up 5 minutes   0.0.0.0:57275->5432/tcp                                      beautiful_albattani
3daca18b7129   testcontainers/ryuk:0.5.1      "/bin/ryuk"              5 minutes ago   Up 5 minutes   0.0.0.0:57234->8080/tcp                                      testcontainers-ryuk-edcee0a1-1052-4ddd-bf28-01a876243b8e
```

Create a product:

```bash
$ psql -h localhost -p <check> -U postgres test
$ Password for user postgres: secret
$ insert into products(code, name, price) values ('T800', 'first cybernetic organism', 10.50);
```

Check the topics:

```bash
$ kafka-topics --bootstrap-server localhost:<check> --list

__consumer_offsets
product-price-changes
```


Create a product price change event

```bash
$ kafka-console-producer --bootstrap-server localhost:<check> --topic product-price-changes \
--property parse.key=true \
--property key.separator="|" \
--property value.serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

```bash
T800|{"productCode":"T800", "price":"800.50"}
```

```bash
$ kafka-console-consumer --bootstrap-server localhost:<check> --topic product-price-changes \
--property value.deserializer=org.apache.kafka.connect.json.JsonDeserializer --from-beginning


T800|{"productCode":"T800", "price":"800.50"}
```


Verify that the database is updated:

```bash
$ select * from products where code = 'T800';
```