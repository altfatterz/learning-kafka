### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent -p "9021:80@loadbalancer"
$ kubectl cluster-info
$ kubectl create ns confluent
$ kubectl config set-context --current --namespace confluent
# install ingress to expose control center easily at http://localhost:9021
$ kubectl apply -f ingress.yaml
```

### Import images and verify imported images

```bash
$ ./import-images
$ docker exec k3d-confluent-server-0 crictl images | grep 7.6.1
$ docker exec k3d-confluent-server-0 crictl images | grep 2.8.0
```

### Install the CFK operator

```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes --set kRaftEnabled=true
```

### Verify running pods

```bash
# wait until the operator is up and running
$ watch kubectl get pods --all-namespaces
```


### Generate a CA pair to use:

```
$ openssl genrsa -out ca-key.pem 2048

$ openssl req -new -key ca-key.pem -x509 \
  -days 1000 \
  -out ca.pem \
  -subj "/CN=ca1.mimacom.com/OU=development/O=mimacom/L=Zurich/C=CH"
```

### Create a Kubernetes secret for the certificate authority:

```
$ kubectl create secret tls ca-pair-sslcerts --cert=ca.pem --key=ca-key.pem
$ kubectl get secret ca-pair-sslcerts -o yaml

data:
 tls.crt:
 tls.key:
```

### Deploy Kafka cluster

```bash
$ kubectl apply -f confluent-platform-base.yaml
$ kubectl apply -f confluent-platform-controlcenter.yaml
```

### Create a configuration secret for client applications to use:

```bash
$ kubectl create secret generic kafka-client-config-secure --from-file=kafka.properties
$ kubectl get secret kafka-client-config-secure -o yaml
```

### Create topic

```bash
$ kubectl apply -f topic.yaml
```

### Deploy perf producer

```bash
$ kubectl logs -f perf-producer.yaml

251281 records sent, 50256.2 records/sec (49.08 MB/sec), 460.2 ms avg latency, 980.0 ms max latency.
609645 records sent, 121929.0 records/sec (119.07 MB/sec), 258.3 ms avg latency, 507.0 ms max latency.
645690 records sent, 129138.0 records/sec (126.11 MB/sec), 236.6 ms avg latency, 284.0 ms max latency.
542685 records sent, 108537.0 records/sec (105.99 MB/sec), 281.8 ms avg latency, 452.0 ms max latency.
512880 records sent, 102576.0 records/sec (100.17 MB/sec), 298.2 ms avg latency, 394.0 ms max latency.
3000000 records sent, 101102.011930 records/sec (98.73 MB/sec), 291.27 ms avg latency, 980.00 ms max latency, 275 ms 50th, 425 ms 95th, 614 ms 99th, 916 ms 99.9th.
```

### 