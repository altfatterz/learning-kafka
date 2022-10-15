### 1. Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
# Start a k8s cluster this time with with 3 agent nodes, 1 server node (control-plane), 
# We need to disable Traefik, since we would like to use Nginx Controller which is tested with Strimzi
$ k3d cluster create mycluster -p "443:443@agent:0,1,2" --agents 3 --k3s-arg "--disable=traefik@server:0" 
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
```

### 2. Install ingress-nginx

```bash
$ kubectl apply -f nginx-ingress.yaml
```

This script downloaded from [here](https://kubernetes.github.io/ingress-nginx/deploy/#quick-start) and was modified only to enable `TLS passthrough`, which means we are not using any certificates configured in the Ingress, we use the ones configured in Strimzi on Kafka level.

### 3. Install the Strimzi operator

```bash
$ kubectl create ns kafka
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 4. Create the Kafka cluster

Modify the `host` to value `minikube ip`

```bash
$ kubectl apply -f kafka-security.yaml -n kafka
```

### 5. Create a topic

```bash
$ kubectl apply -f topic.yaml -n kafka
```

### 6. Create a user

```bash
$ kubectl apply -f user.yaml -n kafka
```

When the user is created by the User Operator, it creates a new secret with the same name as the KafkaUser resource. 
The secret contains a private and public key for TLS client authentication.
The public key is contained in a user certificate, which is signed by the client Certificate Authority (CA).

### 7. Let's check the secret generated for the KafkaUser resource

The User Operator creates a new secret with the same name as the KafkaUser resource. 
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

### 9. Try to connect:

```bash
$ kafka-console-producer --bootstrap-server bootstrap.192.168.205.14.nip.io:443 \
--producer-property security.protocol=SSL \
--producer-property ssl.truststore.password=gxN9ifvrhmsY \
--producer-property ssl.truststore.location=./ca.p12 \
--producer-property ssl.keystore.password=YzGuVmQwjZjM \
--producer-property ssl.keystore.location=./user.p12 \
--topic my-topic
````

With the following consumer you will get the below error message, since we enable also authorization
`
```bash
$ kafka-console-consumer --bootstrap-server bootstrap.192.168.205.14.nip.io:443 \
--consumer-property security.protocol=SSL \
--consumer-property ssl.truststore.password=gxN9ifvrhmsY \
--consumer-property ssl.truststore.location=./ca.p12 \
--consumer-property ssl.keystore.password=YzGuVmQwjZjM \
--consumer-property ssl.keystore.location=./user.p12 \
--topic my-topic \
--from-beginning
```

[2022-10-15 14:52:56,754] ERROR Error processing message, terminating consumer process:  (kafka.tools.ConsoleConsumer$)
org.apache.kafka.common.errors.GroupAuthorizationException: Not authorized to access group: console-consumer-31101
Processed a total of 0 messages

```bash
$ kafka-console-consumer --bootstrap-server bootstrap.192.168.205.14.nip.io:443 \
--consumer-property group.id=my-group \
--consumer-property security.protocol=SSL \
--consumer-property ssl.truststore.password=gxN9ifvrhmsY \
--consumer-property ssl.truststore.location=./ca.p12 \
--consumer-property ssl.keystore.password=YzGuVmQwjZjM \
--consumer-property ssl.keystore.location=./user.p12 \
--topic my-topic \
--from-beginning
```


