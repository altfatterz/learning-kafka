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

```bash
$ kubectl apply -f kafka-ingress.yaml -n kafka
```

Verify the created ingresses:

```bash
$ kubectl get ingress -n kafka
NAME                                  CLASS   HOSTS                             ADDRESS                            PORTS     AGE
my-cluster-kafka-external-2           nginx   broker-2.192.168.205.14.nip.io    172.19.0.3,172.19.0.4,172.19.0.5   80, 443   2m28s
my-cluster-kafka-external-bootstrap   nginx   bootstrap.192.168.205.14.nip.io   172.19.0.3,172.19.0.4,172.19.0.5   80, 443   2m28s
my-cluster-kafka-external-0           nginx   broker-0.192.168.205.14.nip.io    172.19.0.3,172.19.0.4,172.19.0.5   80, 443   2m28s
my-cluster-kafka-external-1           nginx   broker-1.192.168.205.14.nip.io    172.19.0.3,172.19.0.4,172.19.0.5   80, 443   2m28s
```

### 5. Try with a producer / consumer

Extract the `ca.p12` and `ca.crt` from the Cluster CA secret

```bash
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.p12}' | base64 -d > ca.p12
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.password}' | base64 -d > ca.password
```

```bash
$ kafka-console-producer --bootstrap-server bootstrap.192.168.205.14.nip.io:443 \
--producer-property security.protocol=SSL \
--producer-property ssl.truststore.password=aO0M18RW5Oe1 \
--producer-property ssl.truststore.location=./ca.p12 \
--topic my-topic
```

```bash
$ kafka-console-consumer --bootstrap-server bootstrap.192.168.205.14.nip.io:443 \
--consumer-property security.protocol=SSL \
--consumer-property ssl.truststore.password=aO0M18RW5Oe1 \
--consumer-property ssl.truststore.location=./ca.p12 \
--topic my-topic \
--from-beginning
```

### 6. Try with `kcat` 

```bash
# extract the certificate in PEM format
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt
# get metadata for all topics
$ kcat -L -b bootstrap.192.168.205.14.nip.io:443 -X security.protocol=SSL -X ssl.ca.location=ca.crt
# produce
$ echo "foo\nbar\nbaz" | kcat -P -b bootstrap.192.168.205.14.nip.io:443 -t new-topic -X security.protocol=SSL -X ssl.ca.location=ca.crt 
# consume 
$ kcat -C -b bootstrap.192.168.205.14.nip.io:443 -t my-topic -X security.protocol=SSL -X ssl.ca.location=ca.crt
foo
bar
baz 
```

