### 1. Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
# Start a k8s cluster this time wwith with 3 agent nodes, 1 server node (control-plane), 
# we create a port mapping the port 30080-30083 range to from the 3 agent nodes to 8080-8083 on the host 
$ k3d cluster create mycluster -p "8080-8083:30080-30083@agent:0,1,2" --agents 3
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
```

### 2. Install the Strimzi operator

```bash
$ kubectl create ns kafka
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```


### 3. Create the Kafka cluster

```bash
$ kubectl apply -f kafka-tls.yaml -n kafka
```

### 4. Try to connect

It fails since TLS is enabled

```bash
$ kcat -L -b $(minikube ip):8080
%6|1665518123.452|FAIL|rdkafka#producer-1| [thrd:192.168.205.13:8080/bootstrap]: 192.168.205.13:8080/bootstrap: Disconnected while requesting ApiVersion: might be caused by incorrect security.protocol configuration (connecting to a SSL listener?) or broker version is < 0.10 (see api.version.request) (after 5ms in state APIVERSION_QUERY)
```

Ok, we set the security.protocol to SSL, now we get different error message:

```bash
$ kcat -L -b $(minikube ip):8080 -X security.protocol=SSL

%3|1665518763.819|FAIL|rdkafka#producer-1| [thrd:ssl://192.168.205.13:8080/bootstrap]: ssl://192.168.205.13:8080/bootstrap: SSL handshake failed: error:1416F086:SSL routines:tls_process_server_certificate:certificate verify failed: broker certificate could not be verified, verify that ssl.ca.location is correctly configured or root CA certificates are installed (brew install openssl) (after 8ms in state SSL_HANDSHAKE)
```

### 5. Check the created secrets and configmaps

```bash
$ kubectl get secret -n kafka

my-cluster-cluster-ca-cert               Opaque   3      5m12s
my-cluster-clients-ca-cert               Opaque   3      5m12s
my-cluster-clients-ca                    Opaque   1      5m12s
my-cluster-cluster-ca                    Opaque   1      5m12s
my-cluster-cluster-operator-certs        Opaque   4      5m11s
my-cluster-zookeeper-nodes               Opaque   4      5m11s
my-cluster-kafka-brokers                 Opaque   12     4m27s
my-cluster-entity-topic-operator-certs   Opaque   4      3m
my-cluster-entity-user-operator-certs    Opaque   4      3m


$ kubectl get cm -n kafka

kube-root-ca.crt                          1      8m23s
strimzi-cluster-operator                  1      8m1s
my-cluster-zookeeper-config               2      5m26s
my-cluster-kafka-0                        3      4m42s
my-cluster-kafka-1                        3      4m42s
my-cluster-kafka-2                        3      4m42s
my-cluster-entity-topic-operator-config   1      3m15s
my-cluster-entity-user-operator-config    1      3m15s
```

### 6. Extract the Cluster CA secret:

```bash
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt
```

### 7. Try again, setting now the `ssl.ca.location`

```bash
$ kcat -L -b $(minikube ip):8080 -X security.protocol=SSL -X ssl.ca.location=ca.crt

Metadata for all topics (from broker -1: ssl://192.168.205.13:8080/bootstrap):
 3 brokers:
  broker 0 at 192.168.205.11:8081 (controller)
  broker 2 at 192.168.205.11:8083
  broker 1 at 192.168.205.11:8082
...  
```

### Try with Producer / Consumer within the cluster:

Extract the `ca.p12` from the Cluster CA secret

```bash
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.p12}' | base64 -d > ca.p12
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.password}' | base64 -d > ca.password
```

Run an interactive pod:

```bash
$ kubectl run --restart=Never --image=quay.io/strimzi/kafka:0.31.1-kafka-3.2.3 producer-consumer -n kafka -- /bin/sh -c "sleep 3600"
```

and copy the ca.p12 and config file into it.

```bash
$ kubectl cp ca.p12 producer-consumer:/tmp -n kafka
# after you modified the `ssl.truststore.password` inside the config.properties
$ kubectl cp security-config.properties producer-consumer:/tmp -n kafka
```

next try to run a producer within the interactive pod:

```bash
$ /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9093 --topic my-topic --producer.config=/tmp/security-config.properties
>one
>two
>three
^C

$ /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9093 --topic my-topic --from-beginning --consumer.config=/tmp/security-config.properties
one
two
three
```
