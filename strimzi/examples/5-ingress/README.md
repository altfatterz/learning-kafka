Here we focus on how to encrypt the communication of data exchange.

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
$ kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.4.0/deploy/static/provider/cloud/deploy.yaml
```
More details here: [https://kubernetes.github.io/ingress-nginx/deploy/#quick-start](https://kubernetes.github.io/ingress-nginx/deploy/#quick-start)

### 3. Install the Strimzi operator

```bash
$ kubectl create ns kafka
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 4. Create the Kafka cluster

```bash
$ kubectl apply -f kafka-ingress.yaml -n kafka
```

### 5. Try with Producer / Consumer

Extract the `ca.p12` from the Cluster CA secret

```bash
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.p12}' | base64 -d > ca.p12
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.password}' | base64 -d > ca.password
```

Run an interactive pod:

```bash
$ kubectl run --restart=Never --image=quay.io/strimzi/kafka:0.31.1-kafka-3.2.3 producer-consumer -n kafka -- /bin/sh -c "sleep 3600"
```

```bash
$ kubectl exec -it producer-consumer -n kafka -- sh
$ openssl s_client -connect 192.168.205.14:443
```


```bash
$ kafka-console-producer --bootstrap-server bootstrap.192.168.205.14.nip.io:443 \
--producer-property security.protocol=SSL \
--producer-property ssl.truststore.password=neN0fT6opdLb \
--producer-property ssl.truststore.location=./ca.p12 --topic my-topic

[2022-10-12 22:17:03,679] ERROR [Producer clientId=console-producer] Connection to node -1 (bootstrap.192.168.205.14.nip.io/192.168.205.14:443) failed authentication due to: SSL handshake failed (org.apache.kafka.clients.NetworkClient)
[2022-10-12 22:17:03,679] WARN [Producer clientId=console-producer] Bootstrap broker bootstrap.192.168.205.14.nip.io:443 (id: -1 rack: null) disconnected (org.apache.kafka.clients.NetworkClient)
```
