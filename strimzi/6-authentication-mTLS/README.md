### 1. Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
# Start a k8s cluster this time with with 3 agent nodes, 1 server node (control-plane), 
# Create a cluster mapping the port 30080-30083 range to from the 3 agent nodes to 8080-8083 on the host
# Note: Kubernetes’ default NodePort range is 30000-32767
$ rm -r /tmp/kafka-volume
$ mkdir -p /tmp/kafka-volume 
# Start a k8s cluster this time with with 3 agent nodes, 1 server node (control-plane), 
# We need to disable Traefik, since we would like to use Nginx Controller which is tested with Strimzi
$ k3d cluster create mycluster -p "443:443@agent:0,1,2" --agents 3 --k3s-arg "--disable=traefik@server:0" -v /tmp/kafka-volume:/var/lib/rancher/k3s/storage@all
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
# create the `kafka` namespace
$ kubectl create ns kafka
```

### 2. Install ingress-nginx

```bash
$ kubectl apply -f nginx-ingress.yaml
```

```bash
$ watch kubectl get all -n ingress-nginx
```

This script downloaded from [here](https://kubernetes.github.io/ingress-nginx/deploy/#quick-start) and was modified
only to enable `TLS passthrough`, (via `--enable-ssl-passthrough`) which means we are not using any certificates
configured in the Ingress we use the ones configured in Strimzi on Kafka level.

### 3. Install the Strimzi operator

```bash
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

```bash
$ watch kubectl get all -n kafka 
```

### 4. Create the Kafka cluster

```bash
$ kubectl apply -f kafka-security.yaml -n kafka
```

Verify the created ingresses:

```bash
$ kubectl get ingress -n kafka
NAME                                  CLASS   HOSTS                        ADDRESS      PORTS     AGE
my-cluster-kafka-external-bootstrap   nginx   bootstrap.127.0.0.1.nip.io   172.20.0.5   80, 443   42s
my-cluster-kafka-external-1           nginx   broker-1.127.0.0.1.nip.io    172.20.0.5   80, 443   42s
my-cluster-kafka-external-2           nginx   broker-2.127.0.0.1.nip.io    172.20.0.5   80, 443   42s
my-cluster-kafka-external-0           nginx   broker-0.127.0.0.1.nip.io    172.20.0.5   80, 443   42s
```

### 5. Create a topic using a strimzi resource definition:

After the services are up and running, create a topic:

```bash
$ kubectl apply -f kafka-topic.yaml -n kafka
```

### 6. Create a user

```bash
$ kubectl apply -f kafka-user.yaml -n kafka
```

When the user is created by the User Operator, it creates a new secret with the same name as the KafkaUser resource. 
The secret contains a private and public key for TLS client authentication.
The public key is contained in a user certificate, which is signed by the client Certificate Authority (CA).

```bash
$ kubectl get secret my-user -n kafka -o yaml
```

- `ca.crt`
- `user.crt`
- `user.key`
- `user.p12`
- `user.password`

### 7. Let's check the secret generated for the KafkaUser resource

A new secret is created with the same name as the KafkaUser resource. 
The secret contains a private and public key for TLS client authentication. 
The public key is contained in a user certificate, which is signed by the client Certificate Authority (CA).

```bash
$ kubectl get secret my-user -n kafka -o jsonpath='{.data.user\.p12}' | base64 -d > user.p12
$ kubectl get secret my-user -n kafka -o jsonpath='{.data.user\.password}' | base64 -d > user.password
```

### 8. Extract the cluster certificate:

```bash
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.p12}' | base64 -d > ca.p12
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.password}' | base64 -d > ca.password
```

### 9. Try to connect using kafka-console-producer / kafka-console-consumer

Start the producer:

```bash
$ kafka-console-producer --bootstrap-server bootstrap.127.0.0.1.nip.io:443 \
--producer-property security.protocol=SSL \
--producer-property ssl.truststore.password=ljMeaX8HpZWN \
--producer-property ssl.truststore.location=./ca.p12 \
--producer-property ssl.keystore.password=m8r7n6lK812UsH35zwEkpFcCn9N7yLt3 \
--producer-property ssl.keystore.location=./user.p12 \
--topic my-topic
```

Start the consumer:

With the following consumer you will get the below error message, since we enable also authorization
`
```bash
$ kafka-console-consumer --bootstrap-server bootstrap.127.0.0.1.nip.io:443 \
--consumer-property security.protocol=SSL \
--consumer-property ssl.truststore.password=ljMeaX8HpZWN \
--consumer-property ssl.truststore.location=./ca.p12 \
--consumer-property ssl.keystore.password=m8r7n6lK812UsH35zwEkpFcCn9N7yLt3 \
--consumer-property ssl.keystore.location=./user.p12 \
--topic my-topic \
--from-beginning
```

[2022-10-15 14:52:56,754] ERROR Error processing message, terminating consumer process:  (kafka.tools.ConsoleConsumer$)
org.apache.kafka.common.errors.GroupAuthorizationException: Not authorized to access group: console-consumer-31101
Processed a total of 0 messages

```bash
$ kafka-console-consumer --bootstrap-server bootstrap.127.0.0.1.nip.io:443 \
--consumer-property group.id=my-group \
--consumer-property security.protocol=SSL \
--consumer-property ssl.truststore.password=ljMeaX8HpZWN \
--consumer-property ssl.truststore.location=./ca.p12 \
--consumer-property ssl.keystore.password=m8r7n6lK812UsH35zwEkpFcCn9N7yLt3 \
--consumer-property ssl.keystore.location=./user.p12 \
--topic my-topic \
--from-beginning
```

### 10. Try to connect using `kcat`

```bash
# extract the ca certificate in PEM format
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt
# extract the my-user certificate in PEM format
$ kubectl get secret my-user -n kafka -o jsonpath='{.data.user\.crt}' | base64 -d > user.crt
$ kubectl get secret my-user -n kafka -o jsonpath='{.data.user\.key}' | base64 -d > user.key
# produce
$ echo "foo\nbar\nbaz" | kcat -P -b bootstrap.127.0.0.1.nip.io:443 -t my-topic -X security.protocol=SSL -X ssl.ca.location=./ca.crt -X ssl.certificate.location=./user.crt -X ssl.key.location=./user.key 
# consume 
$ kcat -C -b bootstrap.127.0.0.1.nip.io:443 -t my-topic -X security.protocol=SSL -X ssl.ca.location=./ca.crt -X ssl.certificate.location=./user.crt -X ssl.key.location=./user.key 
foo
bar
baz 
```


Resources:

- Deploying Kafka with Let's Encrypt certificates: https://strimzi.io/blog/2021/05/07/deploying-kafka-with-lets-encrypt-certificates/

