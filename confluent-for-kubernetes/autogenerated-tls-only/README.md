### Confluent Platform with full TLS network encryption using auto-generated certs

In this workflow scenario, you'll set up a Confluent Platform cluster with
full TLS network encryption, using auto-generated certs.

### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent
$ kubectl cluster-info
$ kubectl create ns confluent
$ kubectl config set-context --current --namespace confluent
```

Confluent For Kubernetes provides auto-generated certificates for Confluent Platform
components to use for TLS network encryption. You'll need to generate and provide a
Certificate Authority (CA).

Generate a CA pair to use:

```
openssl genrsa -out ca-key.pem 2048

openssl req -new -key ca-key.pem -x509 \
  -days 1000 \
  -out ca.pem \
  -subj "/CN=ca1.mimacom.com/OU=development/O=mimacom/L=Zurich/C=CH"
```

Create a Kubernetes secret for the certificate authority:

```
$ kubectl create secret tls ca-pair-sslcerts --cert=ca.pem --key=ca-key.pem
$ kubectl get secret ca-pair-sslcerts -o yaml 
```

### Install Confluent For Kubernetes using Helm

```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes --set kRaftEnabled=true
## wait until the operator is up and running
$ kubectl get pods
```

### Deploy Confluent Platform

```bash
$ kubectl apply -f confluent-platform-tls-only.yaml
```

Wait until the pods are up

```bash
$ kubectl get pods
```

Expose control center using

```bash
$ kubectl confluent dashboard controlcenter
```

### Analyse

```bash
$ kubectl get secrets

ca-pair-sslcerts                           kubernetes.io/tls    2      57m
confluent-operator-licensing               Opaque               0      54m
sh.helm.release.v1.confluent-operator.v1   helm.sh/release.v1   1      54m
kraftcontroller-generated-jks              kubernetes.io/tls    8      30m
kafka-generated-jks                        kubernetes.io/tls    8      30m
schemaregistry-generated-jks               kubernetes.io/tls    8      26m
controlcenter-generated-jks                kubernetes.io/tls    8      6m7s
```

```bash
$ kubectl get secret kafka-generated-jks -o yaml
data:
  ca.crt
  csr.pem
  jksPassword.txt
  keystore.jks
  secretHash.json
  tls.crt
  tls.key
  truststore.jks
```

```bash 
$ kubectl describe kafka

    Internal:
      Client:  bootstrap.servers=kafka.confluent.svc.cluster.local:9071
security.protocol=SSL
ssl.truststore.location=/mnt/sslcerts/truststore.jks
ssl.truststore.password=<<jksPassword>>
```

```bash
$ kubectl get secret kafka-generated-jks -o yaml -o jsonpath='{.data.jksPassword\.txt}' | base64 -d
jksPassword=mystorepassword
```

### Create a configuration secret for client applications to use:

```bash
$ kubectl create secret generic kafka-client-config-secure --from-file=kafka.properties
$ kubectl get secret kafka-client-config-secure -o yaml
```

### Deploy producer application

Now that you've got the infrastructure set up, deploy the producer client app.

This app takes the above client configuration as a Kubernetes secret. The secret
is mounted to the app pod file system, and the client application reads the
configuration as a file.

```bash
$ kubectl apply -f secure-producer.yaml
```

Check the data in Control Center to verify that the `elastic-0` topic is populated.


### Cleanup

```bash
$ k3d cluster delete confluent
```