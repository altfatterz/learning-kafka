### 1. Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
# Start a k8s cluster this time with with 3 agent nodes, 1 server node (control-plane), 
$ rm -r /tmp/kafka-volume
$ mkdir -p /tmp/kafka-volume 
# Start a k8s cluster this time with with 3 agent nodes, 1 server node (control-plane), 
# We need to disable Traefik, since we would like to use Nginx Controller which is tested with Strimzi
$ k3d cluster create mycluster -p "443:443@agent:0,1,2" --agents 3 --k3s-arg "--disable=traefik@server:0" -v /tmp/kafka-volume:/var/lib/rancher/k3s/storage@all
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
# create the `kafka` namespace
$ kubectl create ns kafka
# set namespace to current context to `kafka`
$ kubectl config set-context --current --namespace=kafka
$ kubectl config get-contexts | grep k3d
CURRENT   NAME            CLUSTER         AUTHINFO                NAMESPACE
*         k3d-mycluster   k3d-mycluster   admin@k3d-mycluster     kafka 
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
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka'
```

```bash
$ watch kubectl get all  
```

### 4. Create the Kafka cluster

```bash
$ kubectl apply -f kafka-scram-authentication.yaml 
```

Verify the created ingresses:

```bash
$ kubectl get ingress
NAME                                  CLASS   HOSTS                        ADDRESS   PORTS     AGE
my-cluster-kafka-external-0           nginx   broker-0.127.0.0.1.nip.io              80, 443   1s
my-cluster-kafka-external-bootstrap   nginx   bootstrap.127.0.0.1.nip.io             80, 443   1s
my-cluster-kafka-external-2           nginx   broker-2.127.0.0.1.nip.io              80, 443   1s
my-cluster-kafka-external-1           nginx   broker-1.127.0.0.1.nip.io              80, 443   1s
```

### 5. Create a topic using a strimzi resource definition:

After the services are up and running, create a topic:

```bash
$ kubectl apply -f kafka-topic.yaml
```

### 6. Create a user

```bash
$ kubectl apply -f kafka-user.yaml

Check the created secret:

```bash
$ kubectl get secret my-user -o yaml
```

It has a `sasl.jaas.config` and `password` base64 encoded.

```bash
$ kubectl get secret my-user -o jsonpath="{.data['sasl\.jaas\.config']}" | base64 -d
$ kubectl get secret my-user -o jsonpath="{.data.password}" | base64 -d
```

### 7. Extract the cluster certificate:

```bash
$ kubectl get secret my-cluster-cluster-ca-cert -o jsonpath='{.data.ca\.p12}' | base64 -d > ca.p12
$ kubectl get secret my-cluster-cluster-ca-cert -o jsonpath='{.data.ca\.password}' | base64 -d > ca.password
```

### View the CA certificate

```bash
$ openssl pkcs12 -info -in ca.p12 -password pass:`cat ca.password`
```

### 9. Try to connect using kafka-console-producer / kafka-console-consumer

Start the producer:

```bash
$ kafka-console-producer --bootstrap-server bootstrap.127.0.0.1.nip.io:443 --topic my-topic \
--producer.config security-config.properties

```

Start the consumer:

With the following consumer you will get the below error message, since we enable also authorization

!!! - for some reason the kafka-console-consumer cannot connect, I get the following exception:

```bash
[2023-11-15 21:47:06,122] ERROR [Consumer clientId=console-consumer, groupId=my-group] Connection to node 0 (broker-0.127.0.0.1.nip.io/127.0.0.1:443) failed authentication due to: SSL handshake failed (org.apache.kafka.clients.NetworkClient)
[2023-11-15 21:47:06,122] ERROR Error processing message, terminating consumer process:  (kafka.tools.ConsoleConsumer$)
org.apache.kafka.common.errors.SslAuthenticationException: SSL handshake failed
Caused by: javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

```bash
$ kafka-console-consumer --bootstrap-server bootstrap.127.0.0.1.nip.io:443 --topic my-topic --consumer.config security-config.properties 
```

### 10. Try to connect using `kcat`

```bash
# extract the ca certificate in PEM format
$ kubectl get secret my-cluster-cluster-ca-cert -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt
# produce
$ echo "foo\nbar\nbaz" | kcat -P -b bootstrap.127.0.0.1.nip.io:443 \
-X ssl.ca.location=ca.crt \
-X security.protocol=SASL_SSL -X sasl.mechanisms=SCRAM-SHA-512 \
-X sasl.username=my-user -X sasl.password=KS50CNmZXDWs0cLIi1GCz92Uod3xIzWV -P -t my-topic
# consume 
$ kcat -C -b bootstrap.127.0.0.1.nip.io:443 -t my-topic \
-X ssl.ca.location=ca.crt \
-X security.protocol=SASL_SSL -X sasl.mechanisms=SCRAM-SHA-512 \
-X sasl.username=my-user -X sasl.password=KS50CNmZXDWs0cLIi1GCz92Uod3xIzWV
```

### Cleanup

```bash
$ k3d cluster delete mycluster
```
