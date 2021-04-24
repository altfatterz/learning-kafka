```bash
$ docker exec -it tools bash
$ curl rest-proxy:8082/brokers
$ curl rest-proxy:8082/topics | jq
$ curl rest-proxy:8082/topics/jsontest | jq
```


# Produce a message using JSON with the value '{ "foo": "bar" }' to the topic `jsontest`

```bash
$ curl -X POST -H "Content-Type: application/vnd.kafka.json.v2+json" \
  --data '{"records":[{"value":{"foo":"bar"}}]}' "http://rest-proxy:8082/topics/jsontest"
  
{ "offsets":[{"partition":0,"offset":0,"error_code":null,"error":null}],"key_schema_id":null,"value_schema_id":null }
```

# Create a consumer for JSON data, starting at the beginning of the topic's
```bash
$ curl -X POST -H "Content-Type: application/vnd.kafka.v2+json" \
  --data '{"name": "my_consumer_instance", "format": "json", "auto.offset.reset": "earliest"}' \
  http://rest-proxy:8082/consumers/my_json_consumer
  
{"instance_id":"my_consumer_instance","base_uri":"http://rest-proxy:8082/consumers/my_json_consumer/instances/my_consumer_instance"}   
```

# Subscribe to a topic
```bash
$ curl -X POST -H "Content-Type: application/vnd.kafka.v2+json" --data '{"topics":["jsontest"]}' \
  http://rest-proxy:8082/consumers/my_json_consumer/instances/my_consumer_instance/subscription

```

# Get the records
```bash
$ curl -X GET -H "Accept: application/vnd.kafka.json.v2+json" \
  http://rest-proxy:8082/consumers/my_json_consumer/instances/my_consumer_instance/records

[{"topic":"jsontest","key":null,"value":{"foo":"bar"},"partition":0,"offset":0}]
```

# Get consumer offsets

```bash
$ curl -X GET -H "Accept: application/vnd.kafka.json.v2+json" -H "Content-Type: application/vnd.kafka.v2+json"  \
  -d '{"partitions":[ {"topic":"jsontest", "partition":0}]}' \
  http://rest-proxy:8082/consumers/my_json_consumer/instances/my_consumer_instance/offsets

{"offsets":[{"topic":"jsontest","partition":0,"offset":1,"metadata":""}]}
```

# TO delete a consumer instance to clean up resources

```bash
$ curl -X DELETE http://rest-proxy:8082/consumers/my_json_consumer/instances/my_consumer_instance
```


More resources here:
* https://rmoff.net/2019/05/02/reading-kafka-connect-offsets-via-the-rest-proxy/
* https://docs.confluent.io/platform/current/kafka-rest/index.html
