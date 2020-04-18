```bash
$ kafka-console-consumer --bootstrap-server localhost:9092 --property print.key=true --property key.separator=":" --topic word-count-output --from-beginning
```

```bash
$ kafka-console-producer --broker-list localhost:9092 --topic word-count-input
```

```bash
$ kafka-topics --bootstrap-server localhost:9092 --list
```

DSL API:
https://kafka.apache.org/20/documentation/streams/developer-guide/dsl-api.html
