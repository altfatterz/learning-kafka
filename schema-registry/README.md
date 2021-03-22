Start up the environment:

```bash
$ docker-compose up -d
```

Create a message using the `ProducerApp` and consume it using `ConsumerApp`

Optionally start the `kafka-protobuf-console-consumer` consumer

```bash
$ docker exec -it tools bash
$ kafka-protobuf-console-consumer --bootstrap-server kafka:9092 --property schema.registry.url=http://schema-registry:8081 --topic demo-topic-protobuf --from-beginning
{"id":1,"firstName":"John","lastName":"Doe","email":"johndoe@gmail.com","phones":[{"number":"0761234678","type":"MOBILE"}]}
```


Check the consumer groups:

```bash
$ kafka-consumer-groups --bootstrap-server kafka:9092 --list
```

Note, the `kafka-<serialiser>-console-consumer` cli always creates a new consumer group with if you don't specify the group id using the `--group` option. 
With code the `ConsumerConfig.GROUP_ID_CONFIG` specifies the consumer group id. 



Resources:

Protobuf:
* https://developers.google.com/protocol-buffers

Confluent
* https://www.confluent.io/blog/confluent-platform-now-supports-protobuf-json-schema-custom-formats/
* https://docs.confluent.io/platform/current/schema-registry/serdes-develop/index.html#serializer-and-formatter


Third party:
* https://simon-aubury.medium.com/kafka-with-avro-vs-kafka-with-protobuf-vs-kafka-with-json-schema-667494cbb2af
* https://martin.kleppmann.com/2012/12/05/schema-evolution-in-avro-protocol-buffers-thrift.html
* https://zoltanaltfatter.com/2020/01/02/schema-evolution-with-confluent-registry/
