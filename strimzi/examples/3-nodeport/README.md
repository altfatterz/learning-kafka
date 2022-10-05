### Nodeport

Connecting to our cluster using Nodeport

```bash
$ k3d cluster create mycluster -p "8080-8083:30080-30083@agent:0,1,2" --agents 3
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
```

Verify our nodes:
```bash
$ kubectl get nodes -o wide
NAME                     STATUS   ROLES                  AGE     VERSION        INTERNAL-IP   EXTERNAL-IP   OS-IMAGE   KERNEL-VERSION   CONTAINER-RUNTIME
k3d-mycluster-agent-2    Ready    <none>                 3m11s   v1.24.4+k3s1   172.20.0.3    <none>        K3s dev    5.10.57          containerd://1.6.6-k3s1
k3d-mycluster-server-0   Ready    control-plane,master   3m15s   v1.24.4+k3s1   172.20.0.2    <none>        K3s dev    5.10.57          containerd://1.6.6-k3s1
k3d-mycluster-agent-0    Ready    <none>                 3m9s    v1.24.4+k3s1   172.20.0.4    <none>        K3s dev    5.10.57          containerd://1.6.6-k3s1
k3d-mycluster-agent-1    Ready    <none>                 3m10s   v1.24.4+k3s1   172.20.0.5    <none>        K3s dev    5.10.57          containerd://1.6.6-k3s1
```

Install the Strimzi operator:
```bash
$ kubectl create ns kafka
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

Open in new terminal:
```bash
$ watch kubectl get pods -n kafka 
```

Open in new terminal:
```bash
$ watch kubectl get svc -n kafka 
```

Deploy Kafka with Nodeport config:

```bash
$ kubectl apply -f kafka-nodeport.yaml -n kafka
```

Verify in the created services and pods:

```bash
$ kubectl get svc -n kafka 
my-cluster-zookeeper-client           ClusterIP   10.43.111.157   <none>        2181/TCP                     84s
my-cluster-zookeeper-nodes            ClusterIP   None            <none>        2181/TCP,2888/TCP,3888/TCP   84s
my-cluster-kafka-brokers              ClusterIP   None            <none>        9090/TCP,9091/TCP,9092/TCP   60s
my-cluster-kafka-external-bootstrap   NodePort    10.43.14.99     <none>        9093:30080/TCP               60s
my-cluster-kafka-bootstrap            ClusterIP   10.43.178.131   <none>        9091/TCP,9092/TCP            60s
my-cluster-kafka-external-1           NodePort    10.43.48.160    <none>        9093:30082/TCP               60s
my-cluster-kafka-external-0           NodePort    10.43.207.35    <none>        9093:30081/TCP               60s
my-cluster-kafka-external-2           NodePort    10.43.147.8     <none>        9093:30083/TCP               59s

$ kubectl get pods -n kafka
strimzi-cluster-operator-54cb64cfdd-hlf9m     1/1     Running   0          9m44s
my-cluster-zookeeper-0                        1/1     Running   0          2m15s
my-cluster-kafka-0                            1/1     Running   0          109s
my-cluster-kafka-1                            1/1     Running   0          109s
my-cluster-kafka-2                            1/1     Running   0          109s
my-cluster-entity-operator-6b9c8fb54f-2l2fh   3/3     Running   0          49s
```

Verify the advertised listener:

```bash
$ kubectl exec my-cluster-kafka-0 -c kafka -it -n kafka -- cat /tmp/strimzi.properties | grep advertised
advertised.listeners=CONTROLPLANE-9090://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9090,REPLICATION-9091://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9091,PLAIN-9092://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9092,EXTERNAL-9093://192.168.205.5:8081
```

Verify that within the Minikube VM we can connect to the nodeport

```bash
$ minikube ssh
$ telnet 172.20.0.4 30080
Connected to 172.20.0.4
```

Verify that from your host you can connect using [`kcat`](https://github.com/edenhill/kcat) (formerly `kafkacat`)

```bash
$ kcat -L -b 192.168.205.5:8080 
```

Produce:
```bash
$ echo 'Learning Strimzi' | kcat -P -b 192.168.205.5:8080 -t nodeport-demo
```

Consume:
```bash
$ kcat -C -b 192.168.205.5:8080 -t nodeport-demo
Learning Strimzi
% Reached end of topic nodeport-demo [0] at offset 1
```

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
