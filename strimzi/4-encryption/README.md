Here we focus on how to encrypt the communication of data exchange.

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

### 2. Install the Strimzi operator

```bash
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 3. Create the Kafka cluster

```bash
$ kubectl apply -f kafka-tls.yaml -n kafka
```

### 4. View the created pods / services

```bash
$ watch kubectl get all -n kafka 
```

### 5. Verify the advertised listener:

```bash
$ kubectl exec my-cluster-kafka-0 -c kafka -it -n kafka -- cat /tmp/strimzi.properties | grep advertised
```

```bash
advertised.listeners=CONTROLPLANE-9090://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9090,REPLICATION-9091://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9091,PLAIN-9092://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9092,TLS-9093://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9093,EXTERNAL-9094://localhost:8081
```

### 6. Try to connect

It fails since TLS is enabled

```bash
$ kcat -L -b localhost:8080
%6|1665518123.452|FAIL|rdkafka#producer-1| [thrd:192.168.205.13:8080/bootstrap]: 192.168.205.13:8080/bootstrap: Disconnected while requesting ApiVersion: might be caused by incorrect security.protocol configuration (connecting to a SSL listener?) or broker version is < 0.10 (see api.version.request) (after 5ms in state APIVERSION_QUERY)
```

Ok, we set the `security.protocol` to SSL, now we get different error message:

```bash
$ kcat -L -b localhost:8080 -X security.protocol=SSL

%3|1665518763.819|FAIL|rdkafka#producer-1| [thrd:ssl://192.168.205.13:8080/bootstrap]: ssl://192.168.205.13:8080/bootstrap: SSL handshake failed: error:1416F086:SSL routines:tls_process_server_certificate:certificate verify failed: broker certificate could not be verified, verify that ssl.ca.location is correctly configured or root CA certificates are installed (brew install openssl) (after 8ms in state SSL_HANDSHAKE)
```

### 5. Check the created secrets and configmaps

```bash
$ kubectl get secret -n kafka

my-cluster-cluster-ca-cert               Opaque   3      5m17s
my-cluster-clients-ca-cert               Opaque   3      5m17s
my-cluster-clients-ca                    Opaque   1      5m17s
my-cluster-cluster-ca                    Opaque   1      5m17s
my-cluster-cluster-operator-certs        Opaque   4      5m17s
my-cluster-zookeeper-nodes               Opaque   4      5m17s
my-cluster-kafka-brokers                 Opaque   12     3m25s
my-cluster-entity-topic-operator-certs   Opaque   4      96s
my-cluster-entity-user-operator-certs    Opaque   4      96s
```

- `my-cluster-cluster-ca-cert` and `my-cluster-clients-ca-cert` contain
  - `ca.crt` 
  - `ca.p12` 
  - `ca.password`
- `my-cluster-clients-ca` and `my-cluster-cluster-ca` contain 
  - `ca.crt`
- `my-cluster-cluster-operator-certs` contains
  - `cluster-operator.crt` 
  - `cluster-operator.key` 
  - `cluster-operator.p12` 
  - `cluster-operator.password` 
- `my-cluster-entity-topic-operator-certs` and `my-cluster-entity-user-operator-certs` contains 
  - `entity-operator.crt` 
  - `entity-operator.key` 
  - `entity-operator.p12` 
  - `entity-operator.password`
- `my-cluster-zookeeper-nodes` contains
  - `my-cluster-zookeeper-0.crt`
  - `my-cluster-zookeeper-0.key`
  - `my-cluster-zookeeper-0.p12`
  - `my-cluster-zookeeper-0.password`
- `my-cluster-kafka-brokers` contains
  - `my-cluster-kafka-0.crt`
  - `my-cluster-kafka-0.key`
  - `my-cluster-kafka-0.p12`
  - `my-cluster-kafka-0.password`
  - `my-cluster-kafka-1.crt`
  - `my-cluster-kafka-1.key`
  - `my-cluster-kafka-1.p12`
  - `my-cluster-kafka-1.password`
  - `my-cluster-kafka-2.crt`
  - `my-cluster-kafka-2.key`
  - `my-cluster-kafka-2.p12`
  - `my-cluster-kafka-2.password`

```bash
$ kubectl get cm -n kafka

NAME                                      DATA   AGE
kube-root-ca.crt                          1      21m
strimzi-cluster-operator                  1      21m
my-cluster-zookeeper-config               2      18m
my-cluster-kafka-0                        3      16m
my-cluster-kafka-1                        3      16m
my-cluster-kafka-2                        3      16m
my-cluster-entity-topic-operator-config   1      14m
my-cluster-entity-user-operator-config    1      14m
```

### 6. Extract the Cluster CA secret:

```bash
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt
```

Examine the certificate 

```bash
$ openssl x509 -text -in ca.crt
```

```bash
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            76:64:b5:c3:b9:b5:3c:fa:b5:fe:32:84:d2:e5:58:e4:f8:d0:a3:97
        Signature Algorithm: sha512WithRSAEncryption
        Issuer: O = io.strimzi, CN = cluster-ca v0
        Validity
            Not Before: Nov  5 15:01:28 2023 GMT
            Not After : Nov  4 15:01:28 2024 GMT
        Subject: O = io.strimzi, CN = cluster-ca v0
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
                Public-Key: (4096 bit)
                ...
```

### 7. Try again, setting now the `ssl.ca.location`

```bash
$ kcat -L -b localhost:8080 -X security.protocol=SSL -X ssl.ca.location=ca.crt
Metadata for all topics (from broker -1: ssl://localhost:8080/bootstrap):
 3 brokers:
  broker 0 at localhost:8081 (controller)
  broker 2 at localhost:8083
  broker 1 at localhost:8082
 3 topics:
```

### 8. Try with Producer / Consumer within the cluster:

Extract the `ca.p12` from the Cluster CA secret

```bash
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.p12}' | base64 -d > ca.p12
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.password}' | base64 -d > ca.password
```

Run an interactive pod:

```bash
$ kubectl run --restart=Never --image=quay.io/strimzi/kafka:0.38.0-kafka-3.6.0 producer-consumer -n kafka -- /bin/sh -c "sleep 3600"
```

and copy the ca.p12 and config file into it.

```bash
$ kubectl cp ca.p12 producer-consumer:/tmp -n kafka
# after you modified the `ssl.truststore.password` inside the config.properties
$ kubectl cp security-config.properties producer-consumer:/tmp -n kafka
```

Next try to run a producer and then a consumer within the interactive pod with security configuration:

```bash
$ kubectl exec -it producer-consumer -n kafka -- sh
$ /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9093 --topic my-topic --producer.config=/tmp/security-config.properties
>one
>two
>three
^C
```

```bash
$ kubectl exec -it producer-consumer -n kafka -- sh
$ /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9093 --topic my-topic --from-beginning --consumer.config=/tmp/security-config.properties
one
two
three
```

Within the interactive pod we can verify what certificate is presented:

```bash
$ openssl s_client -connect my-cluster-kafka-bootstrap:9093

CONNECTED(00000003)
Can't use SSL_get_servername
depth=1 O = io.strimzi, CN = cluster-ca v0
verify error:num=19:self signed certificate in certificate chain
verify return:1
depth=1 O = io.strimzi, CN = cluster-ca v0
verify return:1
depth=0 O = io.strimzi, CN = my-cluster-kafka
verify return:1
---
Certificate chain
 0 s:O = io.strimzi, CN = my-cluster-kafka
   i:O = io.strimzi, CN = cluster-ca v0
 1 s:O = io.strimzi, CN = cluster-ca v0
   i:O = io.strimzi, CN = cluster-ca v0
---
Server certificate
-----BEGIN CERTIFICATE-----
...
-----END CERTIFICATE-----
subject=O = io.strimzi, CN = my-cluster-kafka

issuer=O = io.strimzi, CN = cluster-ca v0

---
No client certificate CA names sent
Peer signing digest: SHA256
Peer signature type: RSA-PSS
Server Temp Key: X25519, 253 bits
---
SSL handshake has read 3322 bytes and written 369 bytes
Verification error: self signed certificate in certificate chain
---
New, TLSv1.3, Cipher is TLS_AES_256_GCM_SHA384
Server public key is 2048 bit
Secure Renegotiation IS NOT supported
Compression: NONE
Expansion: NONE
No ALPN negotiated
Early data was not sent
Verify return code: 19 (self signed certificate in certificate chain)
---
---
Post-Handshake New Session Ticket arrived:
SSL-Session:
    Protocol  : TLSv1.3
    Cipher    : TLS_AES_256_GCM_SHA384
    Session-ID: F50CF825CD3AC02030A35CDFFFB828B6DB1A7F9BB9FA6B50D3BCF90C29373A64
    Session-ID-ctx:
    Resumption PSK: 47C87572987221ABA9E708B7A312719DC680838058323DB04E737094A6BCF1D6B75F9757E6FD32DDFF3189D188DBD7E4
    PSK identity: None
    PSK identity hint: None
    SRP username: None
    TLS session ticket lifetime hint: 86400 (seconds)
    TLS session ticket:
    0000 - b7 e1 ef 70 f4 04 77 70-db 11 bf 71 14 3a 3f 27   ...p..wp...q.:?'
    0010 - 09 28 34 37 0e 30 97 08-6e fc 75 2f ef ec 68 ec   .(47.0..n.u/..h.

    Start Time: 1665521790
    Timeout   : 7200 (sec)
    Verify return code: 19 (self signed certificate in certificate chain)
    Extended master secret: no
    Max Early Data: 0
---
```

### 9. Cleanup

```bash
$ k3d cluster delete mycluster  
```
