Here we focus on how to encrypt the communication of data exchange.

### 1. Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
# Start a k8s cluster this time wwith with 3 agent nodes, 1 server node (control-plane), 
# we create a port mapping the port 30080-30083 range to from the 3 agent nodes to 8080-8083 on the host 
$ k3d cluster create mycluster -p "8080-8083:30080-30083@agent:0,1,2" --agents 3
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
```

### 2. Install the Strimzi operator

```bash
$ kubectl create ns kafka
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```


### 3. Create the Kafka cluster

```bash
$ kubectl apply -f kafka-tls.yaml -n kafka
```

### 4. Try to connect

It fails since TLS is enabled

```bash
$ kcat -L -b $(minikube ip):8080
%6|1665518123.452|FAIL|rdkafka#producer-1| [thrd:192.168.205.13:8080/bootstrap]: 192.168.205.13:8080/bootstrap: Disconnected while requesting ApiVersion: might be caused by incorrect security.protocol configuration (connecting to a SSL listener?) or broker version is < 0.10 (see api.version.request) (after 5ms in state APIVERSION_QUERY)
```

Ok, we set the security.protocol to SSL, now we get different error message:

```bash
$ kcat -L -b $(minikube ip):8080 -X security.protocol=SSL

%3|1665518763.819|FAIL|rdkafka#producer-1| [thrd:ssl://192.168.205.13:8080/bootstrap]: ssl://192.168.205.13:8080/bootstrap: SSL handshake failed: error:1416F086:SSL routines:tls_process_server_certificate:certificate verify failed: broker certificate could not be verified, verify that ssl.ca.location is correctly configured or root CA certificates are installed (brew install openssl) (after 8ms in state SSL_HANDSHAKE)
```

### 5. Check the created secrets and configmaps

```bash
$ kubectl get secret -n kafka

my-cluster-cluster-ca-cert               Opaque   3      5m12s
my-cluster-clients-ca-cert               Opaque   3      5m12s
my-cluster-clients-ca                    Opaque   1      5m12s
my-cluster-cluster-ca                    Opaque   1      5m12s
my-cluster-cluster-operator-certs        Opaque   4      5m11s
my-cluster-zookeeper-nodes               Opaque   4      5m11s
my-cluster-kafka-brokers                 Opaque   12     4m27s
my-cluster-entity-topic-operator-certs   Opaque   4      3m
my-cluster-entity-user-operator-certs    Opaque   4      3m


$ kubectl get cm -n kafka

kube-root-ca.crt                          1      8m23s
strimzi-cluster-operator                  1      8m1s
my-cluster-zookeeper-config               2      5m26s
my-cluster-kafka-0                        3      4m42s
my-cluster-kafka-1                        3      4m42s
my-cluster-kafka-2                        3      4m42s
my-cluster-entity-topic-operator-config   1      3m15s
my-cluster-entity-user-operator-config    1      3m15s
```

### 6. Extract the Cluster CA secret:

```bash
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt
```

Examine the certificate using `Keystore Explorer` or `openssl x509 -text -in ca.crt`

```bash
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            57:33:27:88:f5:28:e0:20:17:cd:f9:fe:62:81:44:24:fc:50:01:6b
    Signature Algorithm: sha512WithRSAEncryption
        Issuer: O=io.strimzi, CN=cluster-ca v0
        Validity
            Not Before: Oct 11 20:21:42 2022 GMT
            Not After : Oct 11 20:21:42 2023 GMT
        Subject: O=io.strimzi, CN=cluster-ca v0
        ...
```

### 7. Try again, setting now the `ssl.ca.location`

```bash
$ kcat -L -b $(minikube ip):8080 -X security.protocol=SSL -X ssl.ca.location=ca.crt

Metadata for all topics (from broker -1: ssl://192.168.205.13:8080/bootstrap):
 3 brokers:
  broker 0 at 192.168.205.11:8081 (controller)
  broker 2 at 192.168.205.11:8083
  broker 1 at 192.168.205.11:8082
...  
```

### Try with Producer / Consumer within the cluster:

Extract the `ca.p12` from the Cluster CA secret

```bash
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.p12}' | base64 -d > ca.p12
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.password}' | base64 -d > ca.password
```

Run an interactive pod:

```bash
$ kubectl run --restart=Never --image=quay.io/strimzi/kafka:0.31.1-kafka-3.2.3 producer-consumer -n kafka -- /bin/sh -c "sleep 3600"
```

and copy the ca.p12 and config file into it.

```bash
$ kubectl cp ca.p12 producer-consumer:/tmp -n kafka
# after you modified the `ssl.truststore.password` inside the config.properties
$ kubectl cp security-config.properties producer-consumer:/tmp -n kafka
```

Next try to run a producer and then a consumer within the interactive pod with security configuration:

```bash
$ kubectl exec -it producer-consumer -- sh
$ /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9093 --topic my-topic --producer.config=/tmp/security-config.properties
>one
>two
>three
^C

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

Resources:
- https://strimzi.io/docs/operators/latest/configuring.html#assembly-securing-access-str