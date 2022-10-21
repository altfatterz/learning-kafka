Deploying Kafka Connect

The `Cluster Operator` manages Kafka Connect clusters deployed using the `KafkaConnect` resource and connectors created using the `KafkaConnector` resource. [https://strimzi.io/docs/operators/latest/deploying.html#kafka-connect-str](https://strimzi.io/docs/operators/latest/deploying.html#kafka-connect-str)

### 1. Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
# Start a k8s cluster this time wwith with 3 agent nodes, 1 server node (control-plane), 
# we disable the loadbalancer in front of the server nodes and add a volume mapping (used for the PVCs demo from last time)
# we create a port mapping the port 30080-30083 range to from the 3 agent nodes to 8080-8083 on the host 
$ k3d cluster create mycluster -p "8080-8083:30080-30083@agent:0,1,2" --agents 3 -v /data:/var/lib/rancher/k3s/storage@all
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
# create the `kafka` namespace
$ kubectl create ns kafka
```

### 2. Install the [Strimzi](https://strimzi.io/) operator

```bash
$ kubectl create ns kafka
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 3. Deploy a simple Kafka cluster (1 broker/1 zookeeper)

```bash
$ kubectl apply -n kafka -f kafka-ephemeral-single.yaml 
```

### 4. Deploy Kafka Connect cluster (1 instance)
 
```bash
$ kubectl apply -f kafka-connect-with-source-connector.yaml -n kafka
```

Notice that first `my-connect-cluster-connect-build` job is create to create a custom image and upload it to the
`ttl.sh/altfatterz-strimzi-kafka-connect-3.2.3:2h` (valid for 2 hours)

Check in the logs what is doing:

```bash
$ logs -f my-connect-cluster-connect-build -n kafka
```

Then a `my-connect-cluster-connect-7d6b9b7c8c-5z5xb` pod is created which is the actual Connect Instance with the File Source Connector installed on it.

### 5. Create the `my-topic` via

```bash
$ kubectl apply -f kafka-topic.yaml -n kafka
$ kubectl get kt -n kafka
```

### 6. Start a consumer

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.31.1-kafka-3.2.3 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```

### 7. Deploy a `KafkaConnector` resource which represents the actual job

```bash
$ kubectl apply -f kafka-source-connector.yaml -n kafka
```

In the consumer logs you should see the messages from the topic

```bash
{"schema":{"type":"string","optional":false},"payload":"This product contains the dnsinfo.h header file, that provides a way to retrieve the system DNS configuration on MacOS."}
{"schema":{"type":"string","optional":false},"payload":"This private header is also used by Apple's open source"}
{"schema":{"type":"string","optional":false},"payload":" mDNSResponder (https://opensource.apple.com/tarballs/mDNSResponder/)."}
{"schema":{"type":"string","optional":false},"payload":""}
{"schema":{"type":"string","optional":false},"payload":" * LICENSE:"}
{"schema":{"type":"string","optional":false},"payload":"    * license/LICENSE.dnsinfo.txt (Apple Public Source License 2.0)"}
{"schema":{"type":"string","optional":false},"payload":"  * HOMEPAGE:"}
```

### 8. View the relevant resources:

```bash
$ kubectl api-resources | grep connect

kafkaconnectors                   kctr         kafka.strimzi.io/v1beta2               true         KafkaConnector
kafkaconnects                     kc           kafka.strimzi.io/v1beta2               true         KafkaConnect

$ kubectl get kc -n kafka

NAME                 DESIRED REPLICAS   READY
my-connect-cluster   1                  True


$ kubectl get kctr -n kafka

NAME                  CLUSTER              CONNECTOR CLASS                                           MAX TASKS   READY
my-source-connector   my-connect-cluster   org.apache.kafka.connect.file.FileStreamSourceConnector   2           True

$ kubectl describe kc my-connect-cluster -n kafka
$ kubectl describe kctr my-source-connector -n kafka 
```

