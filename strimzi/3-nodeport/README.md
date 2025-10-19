### Nodeport

So far we could only connect to our Kafka cluster from within the k8s cluster.
Let's try to do it connect to it from outside using a [Nodeport](https://kubernetes.io/docs/concepts/services-networking/service/#type-nodeport) service

### 1. Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
# Start a k8s cluster this time with with 3 agent nodes, 1 server node (control-plane), 
# Create a cluster mapping the port 30080-30083 range to from the 3 agent nodes to 8080-8083 on the host
# Note: Kubernetes’ default NodePort range is 30000-32767 
$ k3d cluster create mycluster -p "8080-8083:30080-30083@agent:0,1,2" --agents 3
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
# create the `kafka` namespace
$ kubectl create ns kafka
```

### 2. Verify our nodes:

```bash
$ kubectl get nodes -o wide
NAME                     STATUS   ROLES                  AGE   VERSION        INTERNAL-IP   EXTERNAL-IP   OS-IMAGE           KERNEL-VERSION     CONTAINER-RUNTIME
k3d-mycluster-agent-0    Ready    <none>                 21s   v1.31.5+k3s1   172.18.0.4    <none>        K3s v1.31.5+k3s1   6.10.14-linuxkit   containerd://1.7.23-k3s2
k3d-mycluster-agent-1    Ready    <none>                 24s   v1.31.5+k3s1   172.18.0.5    <none>        K3s v1.31.5+k3s1   6.10.14-linuxkit   containerd://1.7.23-k3s2
k3d-mycluster-agent-2    Ready    <none>                 21s   v1.31.5+k3s1   172.18.0.6    <none>        K3s v1.31.5+k3s1   6.10.14-linuxkit   containerd://1.7.23-k3s2
k3d-mycluster-server-0   Ready    control-plane,master   33s   v1.31.5+k3s1   172.18.0.3    <none>        K3s v1.31.5+k3s1   6.10.14-linuxkit   containerd://1.7.23-k3s2
```

```bash
$ docker ps
9dfb32fddb15   ghcr.io/k3d-io/k3d-tools:5.6.0   "/app/k3d-tools noop"    3 minutes ago   Up 3 minutes                                                                                                                                         k3d-mycluster-tools
IMAGE                            COMMAND                  CREATED          STATUS          PORTS                                                                                                                                                                                                                 NAMES
a39022a380e1   ghcr.io/k3d-io/k3d-tools:5.8.3   "/app/k3d-tools noop"    46 seconds ago   Up 45 seconds                                                                                                                                                                                                                         k3d-mycluster-tools
ce6c757e64f4   ghcr.io/k3d-io/k3d-proxy:5.8.3   "/bin/sh -c nginx-pr…"   46 seconds ago   Up 31 seconds   0.0.0.0:63624->6443/tcp, 0.0.0.0:8080->30080/tcp, [::]:8080->30080/tcp, 0.0.0.0:8081->30081/tcp, [::]:8081->30081/tcp, 0.0.0.0:8082->30082/tcp, [::]:8082->30082/tcp, 0.0.0.0:8083->30083/tcp, [::]:8083->30083/tcp   k3d-mycluster-serverlb
cf88bb0e8fba   rancher/k3s:v1.31.5-k3s1         "/bin/k3d-entrypoint…"   46 seconds ago   Up 42 seconds                                                                                                                                                                                                                         k3d-mycluster-agent-2
907c9b66e48f   rancher/k3s:v1.31.5-k3s1         "/bin/k3d-entrypoint…"   46 seconds ago   Up 42 seconds                                                                                                                                                                                                                         k3d-mycluster-agent-1
ab299c8ecc74   rancher/k3s:v1.31.5-k3s1         "/bin/k3d-entrypoint…"   46 seconds ago   Up 42 seconds                                                                                                                                                                                                                         k3d-mycluster-agent-0
701dbb8bd922   rancher/k3s:v1.31.5-k3s1         "/bin/k3d-entrypoint…"   46 seconds ago   Up 44 seconds                                                                                                                                                                                                                         k3d-mycluster-server-0
```

### 3. Install the Strimzi operator:

```bash
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 4. View the created pods / services 

Wait until the operator is up and running

```bash
$ kubectl get pods -n kafka
NAME                                        READY   STATUS    RESTARTS   AGE
strimzi-cluster-operator-7c88589497-xfb2t   1/1     Running   0          70s
```

### 5. Deploy Kafka with Nodeport config:

By default, the port numbers used for the bootstrap and broker services are automatically assigned by Kubernetes. 
You can override the assigned node ports for nodeport listeners by specifying the requested port numbers via `nodePort`

```bash
$ kubectl apply -f kafka-nodeport.yaml -n kafka
```

### 6. Verify in the created services and pods:

```bash
$ kubectl get all -n kafka
NAME                                              READY   STATUS    RESTARTS   AGE
pod/my-cluster-dual-role-0                        1/1     Running   0          3m7s
pod/my-cluster-dual-role-1                        1/1     Running   0          3m7s
pod/my-cluster-dual-role-2                        1/1     Running   0          3m7s
pod/my-cluster-entity-operator-687d45c589-7gnpz   2/2     Running   0          25s
pod/strimzi-cluster-operator-7c88589497-xfb2t     1/1     Running   0          5m14s

NAME                                          TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                                        AGE
service/my-cluster-dual-role-0                NodePort    10.43.233.34    <none>        9094:30081/TCP                                 3m7s
service/my-cluster-dual-role-1                NodePort    10.43.76.167    <none>        9094:30082/TCP                                 3m7s
service/my-cluster-dual-role-2                NodePort    10.43.145.191   <none>        9094:30083/TCP                                 3m7s
service/my-cluster-kafka-bootstrap            ClusterIP   10.43.146.143   <none>        9091/TCP,9092/TCP,9093/TCP                     3m7s
service/my-cluster-kafka-brokers              ClusterIP   None            <none>        9090/TCP,9091/TCP,8443/TCP,9092/TCP,9093/TCP   3m7s
service/my-cluster-kafka-external-bootstrap   NodePort    10.43.230.59    <none>        9094:30080/TCP                                 3m7s

NAME                                         READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/my-cluster-entity-operator   1/1     1            1           25s
deployment.apps/strimzi-cluster-operator     1/1     1            1           5m14s

NAME                                                    DESIRED   CURRENT   READY   AGE
replicaset.apps/my-cluster-entity-operator-687d45c589   1         1         1       25s
replicaset.apps/strimzi-cluster-operator-7c88589497     1         1         1       5m14s
```

`Strimzi` creates additional services - one for each Kafka broker. So in a Kafka cluster with N brokers we will have N+1 
node port services:
- One which can be used by the Kafka clients as the bootstrap service for the initial connection and for receiving 
  the metadata about the Kafka cluster
- Another N services - one for each broker - to address the brokers directly

### 7. Verify the advertised listener:

```bash
$ kubectl exec my-cluster-dual-role-0 -c kafka -it -n kafka -- cat /tmp/strimzi.properties | grep advertised
advertised.listeners=CONTROLPLANE-9090://my-cluster-dual-role-0.my-cluster-kafka-brokers.kafka.svc:9090,REPLICATION-9091://my-cluster-dual-role-0.my-cluster-kafka-brokers.kafka.svc:9091,PLAIN-9092://my-cluster-dual-role-0.my-cluster-kafka-brokers.kafka.svc:9092,TLS-9093://my-cluster-dual-role-0.my-cluster-kafka-brokers.kafka.svc:9093,EXTERNAL-9094://localhost:8081

$ kubectl exec my-cluster-dual-role-1 -c kafka -it -n kafka -- cat /tmp/strimzi.properties | grep advertised
advertised.listeners=CONTROLPLANE-9090://my-cluster-dual-role-1.my-cluster-kafka-brokers.kafka.svc:9090,REPLICATION-9091://my-cluster-dual-role-1.my-cluster-kafka-brokers.kafka.svc:9091,PLAIN-9092://my-cluster-dual-role-1.my-cluster-kafka-brokers.kafka.svc:9092,TLS-9093://my-cluster-dual-role-1.my-cluster-kafka-brokers.kafka.svc:9093,EXTERNAL-9094://localhost:8082

$ kubectl exec my-cluster-dual-role-2 -c kafka -it -n kafka -- cat /tmp/strimzi.properties | grep advertised
advertised.listeners=CONTROLPLANE-9090://my-cluster-dual-role-2.my-cluster-kafka-brokers.kafka.svc:9090,REPLICATION-9091://my-cluster-dual-role-2.my-cluster-kafka-brokers.kafka.svc:9091,PLAIN-9092://my-cluster-dual-role-2.my-cluster-kafka-brokers.kafka.svc:9092,TLS-9093://my-cluster-dual-role-2.my-cluster-kafka-brokers.kafka.svc:9093,EXTERNAL-9094://localhost:8083
```

### 8. Verify that we can connect

```bash
$ brew install telnet

$ telnet localhost 8080
$ telnet localhost 8081
$ telnet localhost 8082
$ telnet localhost 8083

Connected to localhost
```

Verify that from your host you can connect using [`kcat`](https://github.com/edenhill/kcat) (formerly `kafkacat`)

```bash
$ kcat -b localhost:8080 -L 
Metadata for all topics (from broker 2: localhost:8083/2):
 3 brokers:
  broker 0 at localhost:8081
  broker 1 at localhost:8082
  broker 2 at localhost:8083 (controller)
  
# get it in json  
$ kcat -b localhost:8080 -L -J | jq .
```

### 8. Create a topic using a strimzi resource definition:

After the services are up and running, create a topic:

```bash
$ kubectl apply -n kafka -f kafka-topic.yaml
```

Verify that the resource details:

```bash
$ kubectl get kt -n kafka
$ kubectl describe kt my-topic -n kafka
```

### Produce and Consume via [kcat](https://github.com/edenhill/kcat)

Produce:

```bash
$ echo 'Learning Strimzi' | kcat -b localhost:8080 -P -t my-topic
```

Consume:
```bash
$ kcat -b localhost:8080 -C -t my-topic
Learning Strimzi
% Reached end of topic my-topic [0] at offset 1
```

Produce: 

```bash
kcat -b localhost:8080 -t my-topic -K: -P <<EOF
1:{"order_id":1,"order_ts":1534772501276,"total_amount":10.50,"customer_name":"Bob Smith"}
2:{"order_id":2,"order_ts":1534772605276,"total_amount":3.32,"customer_name":"Sarah Black"}
3:{"order_id":3,"order_ts":1534772742276,"total_amount":21.00,"customer_name":"Emma Turner"}
EOF
```

Consume:
```bash
kcat -b localhost:8080 -C \
-f '\nKey (%K bytes): %k\t\nValue (%S bytes): %s\n\Partition: %p\tOffset: %o\n--\n' \
-t my-topic

Key (-1 bytes):
Value (16 bytes): Learning Strimzi
Partition: 0	Offset: 0
--

Key (-1 bytes):
Value (3 bytes): foo
Partition: 0	Offset: 1
--

Key (1 bytes): 1
Value (88 bytes): {"order_id":1,"order_ts":1534772501276,"total_amount":10.50,"customer_name":"Bob Smith"}
Partition: 0	Offset: 2
--

Key (1 bytes): 2
Value (89 bytes): {"order_id":2,"order_ts":1534772605276,"total_amount":3.32,"customer_name":"Sarah Black"}
Partition: 0	Offset: 3
--

Key (1 bytes): 3
Value (90 bytes): {"order_id":3,"order_ts":1534772742276,"total_amount":21.00,"customer_name":"Emma Turner"}
Partition: 0	Offset: 4
```


# 9092 internal port is not secure 

Run an interactive pod:

```bash
$ kubectl run --restart=Never --image=quay.io/strimzi/kafka:0.48.0-kafka-4.1.0 producer-consumer -n kafka -- /bin/sh -c "sleep 3600"
$ kubectl exec -it producer-consumer -n kafka -- sh
sh-5.1$ /opt/kafka/bin/kafka-console-producer.sh --version
4.1.0
sh-5.1$ /opt/kafka/bin/kafka-console-consumer.sh --version
4.1.0

$ /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
$ /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic
```

# 9093 internal port is secure

Run an interactive pod:

```bash
$ kubectl run --restart=Never --image=quay.io/strimzi/kafka:0.48.0-kafka-4.1.0 producer-consumer -n kafka -- /bin/sh -c "sleep 3600"
$ kubectl exec -it producer-consumer -n kafka -- sh
sh-5.1$ /opt/kafka/bin/kafka-console-producer.sh --version
4.1.0
sh-5.1$ /opt/kafka/bin/kafka-console-consumer.sh --version
4.1.0

$ /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9093 --topic my-topic --from-beginning
[2025-10-19 08:57:49,816] WARN [Consumer clientId=console-consumer, groupId=console-consumer-52269] Bootstrap broker my-cluster-kafka-bootstrap:9093 (id: -1 rack: null isFenced: false) disconnected (org.apache.kafka.clients.NetworkClient)
...

$ /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9093 --topic my-topic
>[2025-10-19 08:58:24,856] WARN [Producer clientId=console-producer] Bootstrap broker my-cluster-kafka-bootstrap:9093 (id: -1 rack: null isFenced: false) disconnected (org.apache.kafka.clients.NetworkClient)
...
```


### 9. Conclusions:

Pros:

- Exposing your Kafka cluster to the outside using node ports can give you a lot of flexibility.
- It is also able to deliver very good performance.
- Compared to other solutions such as load-balancers, routes or ingress there is no middleman to be a bottleneck or add latency.
- Your client connections will go to your Kafka broker in the most direct way possible.

Cons:
- But there is also a price you have to pay for this. Node ports are a very low level solution.
- Often you will run into problems with the detection of the advertised addresses as described in the sections above.
- Another problem might be that node ports expect you to expose your Kubernetes nodes to the clients. And that is often seen as security risk by the administrators.


### Cleanup

```bash
$ k3d cluster delete mycluster  
```
