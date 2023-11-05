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
$ rm -r /tmp/kafka-volume
$ mkdir -p /tmp/kafka-volume 
$ k3d cluster create mycluster -p "8080-8083:30080-30083@agent:0,1,2" --agents 3 -v /tmp/kafka-volume:/var/lib/rancher/k3s/storage@all
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
# create the `kafka` namespace
$ kubectl create ns kafka
```

### 2. Verify our nodes:

```bash
$ kubectl get nodes -o wide
NAME                     STATUS   ROLES                  AGE     VERSION        INTERNAL-IP   EXTERNAL-IP   OS-IMAGE   KERNEL-VERSION    CONTAINER-RUNTIME
k3d-mycluster-server-0   Ready    control-plane,master   4m46s   v1.27.4+k3s1   172.21.0.3    <none>        K3s dev    6.4.16-linuxkit   containerd://1.7.1-k3s1
k3d-mycluster-agent-2    Ready    <none>                 4m42s   v1.27.4+k3s1   172.21.0.5    <none>        K3s dev    6.4.16-linuxkit   containerd://1.7.1-k3s1
k3d-mycluster-agent-0    Ready    <none>                 4m42s   v1.27.4+k3s1   172.21.0.4    <none>        K3s dev    6.4.16-linuxkit   containerd://1.7.1-k3s1
k3d-mycluster-agent-1    Ready    <none>                 4m41s   v1.27.4+k3s1   172.21.0.6    <none>        K3s dev    6.4.16-linuxkit   containerd://1.7.1-k3s1
```

```bash
$ docker ps
9dfb32fddb15   ghcr.io/k3d-io/k3d-tools:5.6.0   "/app/k3d-tools noop"    3 minutes ago   Up 3 minutes                                                                                                                                         k3d-mycluster-tools
3e4c46f6d571   ghcr.io/k3d-io/k3d-proxy:5.6.0   "/bin/sh -c nginx-pr…"   3 minutes ago   Up 3 minutes   80/tcp, 0.0.0.0:51515->6443/tcp, 0.0.0.0:8080->30080/tcp, 0.0.0.0:8081->30081/tcp, 0.0.0.0:8082->30082/tcp, 0.0.0.0:8083->30083/tcp   k3d-mycluster-serverlb
4bd75cf5a82e   rancher/k3s:v1.27.4-k3s1         "/bin/k3s agent"         3 minutes ago   Up 3 minutes                                                                                                                                         k3d-mycluster-agent-2
e85586562046   rancher/k3s:v1.27.4-k3s1         "/bin/k3s agent"         3 minutes ago   Up 3 minutes                                                                                                                                         k3d-mycluster-agent-1
040be0c2ad22   rancher/k3s:v1.27.4-k3s1         "/bin/k3s agent"         3 minutes ago   Up 3 minutes                                                                                                                                         k3d-mycluster-agent-0
3c9a5ee6d9f2   rancher/k3s:v1.27.4-k3s1         "/bin/k3s server --t…"   3 minutes ago   Up 3 minutes                                                                                                                                         k3d-mycluster-server-0
```

### 3. Install the Strimzi operator:

```bash
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 4. View the created pods / services 

Terminal:
```bash
$ watch kubectl get all -n kafka 
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
pod/strimzi-cluster-operator-95d88f6b5-r9f29      1/1     Running   0          3m45s
pod/my-cluster-zookeeper-0                        1/1     Running   0          2m3s
pod/my-cluster-kafka-0                            1/1     Running   0          53s
pod/my-cluster-kafka-1                            1/1     Running   0          53s
pod/my-cluster-kafka-2                            1/1     Running   0          53s
pod/my-cluster-entity-operator-64dc7c8844-dcn26   0/3     Running   0          5s

NAME                                          TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)                                        AGE
service/my-cluster-zookeeper-client           ClusterIP   10.43.53.199   <none>        2181/TCP                                       2m3s
service/my-cluster-zookeeper-nodes            ClusterIP   None           <none>        2181/TCP,2888/TCP,3888/TCP                     2m3s
service/my-cluster-kafka-brokers              ClusterIP   None           <none>        9090/TCP,9091/TCP,8443/TCP,9092/TCP,9093/TCP   54s
service/my-cluster-kafka-bootstrap            ClusterIP   10.43.133.78   <none>        9091/TCP,9092/TCP,9093/TCP                     54s
service/my-cluster-kafka-external-bootstrap   NodePort    10.43.86.210   <none>        9094:30080/TCP                                 54s
service/my-cluster-kafka-1                    NodePort    10.43.72.247   <none>        9094:30082/TCP                                 54s
service/my-cluster-kafka-0                    NodePort    10.43.242.81   <none>        9094:30081/TCP                                 54s
service/my-cluster-kafka-2                    NodePort    10.43.209.63   <none>        9094:30083/TCP                                 54s

NAME                                         READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/strimzi-cluster-operator     1/1     1            1           3m45s
deployment.apps/my-cluster-entity-operator   0/1     1            0           5s

NAME                                                    DESIRED   CURRENT   READY   AGE
replicaset.apps/strimzi-cluster-operator-95d88f6b5      1         1         1       3m45s
replicaset.apps/my-cluster-entity-operator-64dc7c8844   1         1         0       5s
```

Strimzi creates additional services - one for each Kafka broker. So in a Kafka cluster with N brokers we will have N+1 
node port services:
- One which can be used by the Kafka clients as the bootstrap service for the initial connection and for receiving 
  the metadata about the Kafka cluster
- Another N services - one for each broker - to address the brokers directly

### 7. Verify the advertised listener:

```bash
$ kubectl exec my-cluster-kafka-0 -c kafka -it -n kafka -- cat /tmp/strimzi.properties | grep advertised
```

```bash
advertised.listeners=CONTROLPLANE-9090://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9090,REPLICATION-9091://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9091,PLAIN-9092://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9092,TLS-9093://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9093,EXTERNAL-9094://localhost:8081
```

### 8. Verify that we can connect

```bash
$ telnet localhost 8080
$ telnet localhost 8081
$ telnet localhost 8082

Connected to localhost
```

Verify that from your host you can connect using [`kcat`](https://github.com/edenhill/kcat) (formerly `kafkacat`)

```bash
$ kcat -L -b localhost:8080 
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

### Produce and Consume via `kcat`

Produce:
```bash
$ echo 'Learning Strimzi' | kcat -P -b localhost:8080 -t my-topic
```

Consume:
```bash
$ kcat -C -b localhost:8080 -t my-topic
Learning Strimzi
% Reached end of topic my-topic [0] at offset 1
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
