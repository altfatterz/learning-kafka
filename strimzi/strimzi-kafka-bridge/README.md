Create a Kafka cluster:

```bash
$ kubectl apply -f kafka-ephemeral.yaml
```

Create a topic:

```bash
$ kubectl apply -f my-topic.yaml
```

Create a Kafka Bridge:

```bash
$ kubectl apply -f kafka-bridge.yaml
```

Check the services:

```bash
$ kubectl get svc
```

```bash
$ kubectl port-forward svc/my-bridge-bridge-service 8080:8080
```

```bash
$ curl -v localhost:8080/topics | jq .

HTTP/1.1 200 OK
content-length: 94
content-type: application/vnd.kafka.v2+json

[
    "__strimzi_store_topic",
    "__strimzi-topic-operator-kstreams-topic-store-changelog",
    "my-topic"
]
```

Check examples with `HTTPie`

More resources [https://strimzi.io/docs/bridge/latest/full](https://strimzi.io/docs/bridge/latest/full)