--- WORK IN PROGRESS ---

1. Schema Registry is licensed under the Confluent Community License.
2. Schema Registry uses Kafka to persist schemas, and so it acts as a client to write data to the Kafka cluster.
3. If Kafka is configured with TLS/SSL encryption and is using SCRAM authentication, the Schema Registry config options:

Connects to Kafka as user `schemaregistry`

```bash
kafkastore.bootstrap.servers=kafka1:9093
# Configure SASL_SSL if TLS/SSL encryption is enabled, otherwise configure SASL_PLAINTEXT
kafkastore.security.protocol=SASL_SSL
kafkastore.sasl.mechanism=SCRAM-SHA-512
kafkastore.sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required \
  username="schemaregistry" \
  password="schemaregistry-secret";
```








```bash
$ helm repo add bitnami https://charts.bitnami.com/bitnami
$ helm repo update
$ helm install my-schema-registry bitnami/schema-registry --version 16.2.2
```


Schema Registry can be accessed through the following DNS name from within your cluster:

    my-schema-registry.kafka.svc.cluster.local (port )

To access Schema Registry from outside the cluster execute the following commands:

1. Get the Schema Registry URL by running these commands:

   export SERVICE_PORT=$(kubectl get --namespace kafka -o jsonpath="{.spec.ports[0].port}" services my-schema-registry)
   kubectl port-forward --namespace kafka svc/my-schema-registry ${SERVICE_PORT}:${SERVICE_PORT} &
   echo "http://127.0.0.1:${SERVICE_PORT}"

2. Access Schema Registry using the obtained URL.


### Schema Registry

```bash
$ kubectl apply -f schemaregistry-user.yaml
$ kubectl get secret schemaregistry -o jsonpath="{.data['sasl\.jaas\.config']}" | base64 -d
$ kubectl get secret schemaregistry -o jsonpath="{.data.password}" | base64 -d
```

```bash
$ keytool -importkeystore -srckeystore ca.p12 -srcstoretype PKCS12 -destkeystore schema-registry.truststore.jks -deststoretype jks -deststorepass verysecure
$ keytool -v -list -keystore schema-registry.truststore.jks
```

```bash
$ kubectl create secret generic schemaregistry-truststore --from-file=./schema-registry.truststore.jks
```

```bash
$ helm repo add bitnami https://charts.bitnami.com/bitnami
$ helm repo update
$ helm install my-schema-registry bitnami/schema-registry --version 16.2.2 -f values.yaml
```




Resources:

1. https://docs.confluent.io/platform/current/schema-registry/index.html
2. https://artifacthub.io/packages/helm/bitnami/schema-registry
3. https://raw.githubusercontent.com/confluentinc/confluent-platform-security-tools/master/kafka-generate-ssl.sh
