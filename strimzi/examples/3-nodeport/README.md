### Nodeport

So far we could only connect to our Kafka cluster from within the k8s cluster.
Let's try to do it connect to it from outside using a [Nodeport](https://kubernetes.io/docs/concepts/services-networking/service/#type-nodeport) service

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

### 2. Verify our nodes:

```bash
$ kubectl get nodes -o wide
NAME                     STATUS   ROLES                  AGE     VERSION        INTERNAL-IP   EXTERNAL-IP   OS-IMAGE   KERNEL-VERSION   CONTAINER-RUNTIME
k3d-mycluster-agent-0    Ready    <none>                 31s   v1.24.4+k3s1   172.19.0.4    <none>        K3s dev    5.10.57          containerd://1.6.6-k3s1
k3d-mycluster-server-0   Ready    control-plane,master   37s   v1.24.4+k3s1   172.19.0.2    <none>        K3s dev    5.10.57          containerd://1.6.6-k3s1
k3d-mycluster-agent-1    Ready    <none>                 35s   v1.24.4+k3s1   172.19.0.5    <none>        K3s dev    5.10.57          containerd://1.6.6-k3s1
k3d-mycluster-agent-2    Ready    <none>                 31s   v1.24.4+k3s1   172.19.0.3    <none>        K3s dev    5.10.57          containerd://1.6.6-k3s1
```

### 3. Install the Strimzi operator:

```bash
$ kubectl create ns kafka
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 4. View the operator 

Open in new terminal:
```bash
$ watch kubectl get pods -n kafka 
```

Open in new terminal:
```bash
$ watch kubectl get svc -n kafka 
```

### 5. Deploy Kafka with Nodeport config:

Before deploying the Kafka cluster with Nodeport configuration edit the `advertisedHost` value in the `kafka-nodeport.yaml` file with the value from your `minikube ip`

```bash
$ kubectl apply -f kafka-nodeport.yaml -n kafka
```

### 6. Verify in the created services and pods:

```bash
$ kubectl get svc -n kafka 
my-cluster-zookeeper-client           ClusterIP   10.43.106.210   <none>        2181/TCP                     81s
my-cluster-zookeeper-nodes            ClusterIP   None            <none>        2181/TCP,2888/TCP,3888/TCP   81s
my-cluster-kafka-external-2           NodePort    10.43.118.217   <none>        9093:30083/TCP               36s
my-cluster-kafka-brokers              ClusterIP   None            <none>        9090/TCP,9091/TCP,9092/TCP   36s
my-cluster-kafka-bootstrap            ClusterIP   10.43.1.232     <none>        9091/TCP,9092/TCP            36s
my-cluster-kafka-external-bootstrap   NodePort    10.43.39.52     <none>        9093:30080/TCP               36s
my-cluster-kafka-external-0           NodePort    10.43.98.105    <none>        9093:30081/TCP               36s
my-cluster-kafka-external-1           NodePort    10.43.128.103   <none>        9093:30082/TCP               36s

$ kubectl get pods -n kafka
strimzi-cluster-operator-54cb64cfdd-v8x6s     1/1     Running   0          4m15s
my-cluster-zookeeper-0                        1/1     Running   0          2m50s
my-cluster-kafka-1                            1/1     Running   0          2m5s
my-cluster-kafka-0                            1/1     Running   0          2m5s
my-cluster-kafka-2                            1/1     Running   0          2m5s
my-cluster-entity-operator-6b9c8fb54f-57hd9   3/3     Running   0          58s
```

Strimzi creates additional services - one for each Kafka broker. So in a Kafka cluster with N brokers we will have N+1 
node port services:
- One which can be used by the Kafka clients as the bootstrap service for the initial connection and for receiving 
  the metadata about the Kafka cluster
- Another N services - one for each broker - to address the brokers directly

### 7. Verify the advertised listener:

```bash
$ kubectl exec my-cluster-kafka-0 -c kafka -it -n kafka -- cat /tmp/strimzi.properties | grep advertised
advertised.listeners=CONTROLPLANE-9090://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9090,REPLICATION-9091://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9091,PLAIN-9092://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9092,EXTERNAL-9093://192.168.205.5:8081
```

### 8. Verify that within the Minikube VM we can connect to the NodePort

```bash
$ minikube ssh
$ telnet <a-node-ip> 30080
Connected to <a-node-ip>
```

Verify that from your host you can connect using [`kcat`](https://github.com/edenhill/kcat) (formerly `kafkacat`)

```bash
  $ kcat -L -b $(minikube ip):8080 
```

Produce:
```bash
$ echo 'Learning Strimzi' | kcat -P -b $(minikube ip):8080 -t nodeport-demo
```

Consume:
```bash
$ kcat -C -b $(minikube ip):8080 -t nodeport-demo
Learning Strimzi
% Reached end of topic nodeport-demo [0] at offset 1
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
