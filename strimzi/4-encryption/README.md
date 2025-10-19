Here we focus on how to encrypt the communication of data exchange. We secure the external `9094` port

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

### 2. Install the Strimzi operator

```bash
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 3. Create the Kafka cluster

Wait until the operator is up and running, then:

```bash
$ kubectl apply -f kafka-tls.yaml -n kafka
```

### 4. View the created pods / services

```bash
$ kubectl get all -n kafka 

NAME                                              READY   STATUS    RESTARTS   AGE
pod/my-cluster-dual-role-0                        1/1     Running   0          79s
pod/my-cluster-dual-role-1                        1/1     Running   0          79s
pod/my-cluster-dual-role-2                        1/1     Running   0          79s
pod/my-cluster-entity-operator-75f49f5f97-h5j6r   2/2     Running   0          26s
pod/strimzi-cluster-operator-7c88589497-tmlnm     1/1     Running   0          2m7s

NAME                                          TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                                        AGE
service/my-cluster-dual-role-0                NodePort    10.43.253.223   <none>        9094:30081/TCP                                 80s
service/my-cluster-dual-role-1                NodePort    10.43.225.237   <none>        9094:30082/TCP                                 80s
service/my-cluster-dual-role-2                NodePort    10.43.64.98     <none>        9094:30083/TCP                                 80s
service/my-cluster-kafka-bootstrap            ClusterIP   10.43.91.248    <none>        9091/TCP,9092/TCP,9093/TCP                     80s
service/my-cluster-kafka-brokers              ClusterIP   None            <none>        9090/TCP,9091/TCP,8443/TCP,9092/TCP,9093/TCP   80s
service/my-cluster-kafka-external-bootstrap   NodePort    10.43.81.217    <none>        9094:30080/TCP                                 80s

NAME                                         READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/my-cluster-entity-operator   1/1     1            1           26s
deployment.apps/strimzi-cluster-operator     1/1     1            1           2m7s

NAME                                                    DESIRED   CURRENT   READY   AGE
replicaset.apps/my-cluster-entity-operator-75f49f5f97   1         1         1       26s
replicaset.apps/strimzi-cluster-operator-7c88589497     1         1         1       2m7s
```

### 5. Verify the advertised listener:

```bash
$ kubectl exec my-cluster-dual-role-0 -c kafka -it -n kafka -- cat /tmp/strimzi.properties | grep advertised
advertised.listeners=CONTROLPLANE-9090://my-cluster-dual-role-0.my-cluster-kafka-brokers.kafka.svc:9090,REPLICATION-9091://my-cluster-dual-role-0.my-cluster-kafka-brokers.kafka.svc:9091,PLAIN-9092://my-cluster-dual-role-0.my-cluster-kafka-brokers.kafka.svc:9092,TLS-9093://my-cluster-dual-role-0.my-cluster-kafka-brokers.kafka.svc:9093,EXTERNAL-9094://localhost:8081

$ kubectl exec my-cluster-dual-role-1 -c kafka -it -n kafka -- cat /tmp/strimzi.properties | grep advertised
advertised.listeners=CONTROLPLANE-9090://my-cluster-dual-role-1.my-cluster-kafka-brokers.kafka.svc:9090,REPLICATION-9091://my-cluster-dual-role-1.my-cluster-kafka-brokers.kafka.svc:9091,PLAIN-9092://my-cluster-dual-role-1.my-cluster-kafka-brokers.kafka.svc:9092,TLS-9093://my-cluster-dual-role-1.my-cluster-kafka-brokers.kafka.svc:9093,EXTERNAL-9094://localhost:8082

$ kubectl exec my-cluster-dual-role-2 -c kafka -it -n kafka -- cat /tmp/strimzi.properties | grep advertised
advertised.listeners=CONTROLPLANE-9090://my-cluster-dual-role-2.my-cluster-kafka-brokers.kafka.svc:9090,REPLICATION-9091://my-cluster-dual-role-2.my-cluster-kafka-brokers.kafka.svc:9091,PLAIN-9092://my-cluster-dual-role-2.my-cluster-kafka-brokers.kafka.svc:9092,TLS-9093://my-cluster-dual-role-2.my-cluster-kafka-brokers.kafka.svc:9093,EXTERNAL-9094://localhost:8083
```

### 6. Try to connect

It fails since TLS is enabled

```bash
$ kcat -L -b localhost:8080
%6|1760865001.799|FAIL|rdkafka#producer-1| [thrd:localhost:8080/bootstrap]: localhost:8080/bootstrap: Disconnected: connection closed by peer: POLLHUP (after 1ms in state APIVERSION_QUERY)
%6|1760865002.024|FAIL|rdkafka#producer-1| [thrd:localhost:8080/bootstrap]: localhost:8080/bootstrap: Disconnected: connection closed by peer: POLLHUP (after 1ms in state APIVERSION_QUERY, 1 identical error(s) suppressed)
%6|1760865002.482|FAIL|rdkafka#producer-1| [thrd:localhost:8080/bootstrap]: localhost:8080/bootstrap: Disconnected: connection closed by peer: receive 0 after POLLIN (after 6ms in state APIVERSION_QUERY)
% ERROR: Failed to acquire metadata: Local: Broker transport failure (Are the brokers reachable? Also try increasing the metadata timeout with -m <timeout>?)
```

Ok, we set the `security.protocol` to SSL, now we get different error message:

```bash
$ kcat -L -b localhost:8080 -X security.protocol=SSL

%3|1760865020.028|FAIL|rdkafka#producer-1| [thrd:ssl://localhost:8080/bootstrap]: ssl://localhost:8080/bootstrap: SSL handshake failed: error:0A000086:SSL routines::certificate verify failed: broker certificate could not be verified, verify that ssl.ca.location is correctly configured or root CA certificates are installed (brew install openssl) (after 7ms in state SSL_HANDSHAKE)
%3|1760865020.083|FAIL|rdkafka#producer-1| [thrd:ssl://localhost:8080/bootstrap]: ssl://localhost:8080/bootstrap: SSL handshake failed: error:0A000086:SSL routines::certificate verify failed: broker certificate could not be verified, verify that ssl.ca.location is correctly configured or root CA certificates are installed (brew install openssl) (after 7ms in state SSL_HANDSHAKE, 1 identical error(s) suppressed)
% ERROR: Failed to acquire metadata: Local: Broker transport failure (Are the brokers reachable? Also try increasing the metadata timeout with -m <timeout>?)
```

### 5. Check the created secrets and configmaps

```bash
$ kubectl get secret -n kafka

NAME                                     TYPE     DATA   AGE
my-cluster-clients-ca                    Opaque   1      3m19s
my-cluster-clients-ca-cert               Opaque   3      3m19s
my-cluster-cluster-ca                    Opaque   1      3m19s
my-cluster-cluster-ca-cert               Opaque   3      3m19s
my-cluster-cluster-operator-certs        Opaque   2      3m19s
my-cluster-dual-role-0                   Opaque   2      3m19s
my-cluster-dual-role-1                   Opaque   2      3m19s
my-cluster-dual-role-2                   Opaque   2      3m19s
my-cluster-entity-topic-operator-certs   Opaque   2      2m25s
my-cluster-entity-user-operator-certs    Opaque   2      2m25s
```

- `my-cluster-cluster-ca-cert` contains:
  - `ca.crt` 
  - `ca.p12` 
  - `ca.password`
- `my-cluster-cluster-ca` contain:
  - `ca.key`

- `my-cluster-clients-ca-cert` contains:
  - `ca.crt`
  - `ca.p12`
  - `ca.password`
- `my-cluster-clients-ca` contains:
  - `ca.key`

- `my-cluster-cluster-operator-certs` contains:
  - `cluster-operator.crt` 
  - `cluster-operator.key`

- `my-cluster-dual-role-0` contains:
  - `my-cluster-dual-role-0.crt`
  - `my-cluster-dual-role-0.key`
- `my-cluster-dual-role-1` contains:
  - `my-cluster-dual-role-1.crt`
  - `my-cluster-dual-role-1.key`
- `my-cluster-dual-role-2` contains:
  - `my-cluster-dual-role-2.crt`
  - `my-cluster-dual-role-2.key`

- `my-cluster-entity-topic-operator-certs` contains: 
  - `entity-operator.crt` 
  - `entity-operator.key` 
- `my-cluster-entity-user-operator-certs` contains:
  - `entity-operator.key` 
  - `entity-operator.crt`
  
```bash
$ kubectl get cm -n kafka

NAME                                      DATA   AGE
kube-root-ca.crt                          1      12m
my-cluster-dual-role-0                    5      11m
my-cluster-dual-role-1                    5      11m
my-cluster-dual-role-2                    5      11m
my-cluster-entity-topic-operator-config   1      10m
my-cluster-entity-user-operator-config    1      10m
strimzi-cluster-operator                  1      12m
```

### 6 Extract CAs:

```bash
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.crt}' | base64 -d > cluster-ca.crt
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.p12}' | base64 -d > cluster-ca.p12
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.password}' | base64 -d > cluster-ca.password
$ kubectl get secret my-cluster-clients-ca-cert -n kafka -o jsonpath='{.data.ca\.crt}' | base64 -d > clients-ca.crt
$ kubectl get secret my-cluster-clients-ca-cert -n kafka -o jsonpath='{.data.ca\.p12}' | base64 -d > clients-ca.p12
$ kubectl get secret my-cluster-clients-ca-cert -n kafka -o jsonpath='{.data.ca\.password}' | base64 -d > clients-ca.password

$ openssl x509 -text -in cluster-ca.crt
$ openssl x509 -text -in clients-ca.crt
```

### 7. Try again, setting now the `ssl.ca.location`

```bash
$ kcat -L -b localhost:8080 -X security.protocol=SSL -X ssl.ca.location=cluster-ca.crt
Metadata for all topics (from broker -1: ssl://localhost:8080/bootstrap):
 3 brokers:
  broker 0 at localhost:8081 (controller)
  broker 2 at localhost:8083
  broker 1 at localhost:8082
 3 topics:
```

### 8. Try with Producer / Consumer within the cluster:

Run an interactive pod:

```bash
$ kubectl run --restart=Never --image=quay.io/strimzi/kafka:0.48.0-kafka-4.1.0 producer-consumer -n kafka -- /bin/sh -c "sleep 3600"
$ kubectl exec -it producer-consumer -n kafka -- sh
```

and copy the `cluseter-ca.p12` and config file into it.

```bash
$ kubectl cp cluster-ca.p12 producer-consumer:/tmp -n kafka
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
Connecting to 10.43.91.248
CONNECTED(00000003)
Can't use SSL_get_servername
depth=1 O=io.strimzi, CN=cluster-ca v0
verify error:num=19:self-signed certificate in certificate chain
verify return:1
depth=1 O=io.strimzi, CN=cluster-ca v0
verify return:1
depth=0 O=io.strimzi, CN=my-cluster-kafka
verify return:1
---
Certificate chain
 0 s:O=io.strimzi, CN=my-cluster-kafka
   i:O=io.strimzi, CN=cluster-ca v0
   a:PKEY: rsaEncryption, 2048 (bit); sigalg: RSA-SHA512
   v:NotBefore: Oct 19 09:07:19 2025 GMT; NotAfter: Oct 19 09:07:19 2026 GMT
 1 s:O=io.strimzi, CN=cluster-ca v0
   i:O=io.strimzi, CN=cluster-ca v0
   a:PKEY: rsaEncryption, 4096 (bit); sigalg: RSA-SHA512
   v:NotBefore: Oct 19 09:07:18 2025 GMT; NotAfter: Oct 19 09:07:18 2026 GMT
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

Present certificate for the externally 9094 port

```bash
$ openssl s_client -connect localhost:8080
Connecting to ::1
CONNECTED(00000005)
Can't use SSL_get_servername
depth=1 O=io.strimzi, CN=cluster-ca v0
verify error:num=19:self-signed certificate in certificate chain
verify return:1
depth=1 O=io.strimzi, CN=cluster-ca v0
verify return:1
depth=0 O=io.strimzi, CN=my-cluster-kafka
verify return:1
---
Certificate chain
 0 s:O=io.strimzi, CN=my-cluster-kafka
   i:O=io.strimzi, CN=cluster-ca v0
   a:PKEY: RSA, 2048 (bit); sigalg: sha512WithRSAEncryption
   v:NotBefore: Oct 19 09:07:19 2025 GMT; NotAfter: Oct 19 09:07:19 2026 GMT
 1 s:O=io.strimzi, CN=cluster-ca v0
   i:O=io.strimzi, CN=cluster-ca v0
   a:PKEY: RSA, 4096 (bit); sigalg: sha512WithRSAEncryption
   v:NotBefore: Oct 19 09:07:18 2025 GMT; NotAfter: Oct 19 09:07:18 2026 GMT
---
Server certificate
```

### 9. Cleanup

```bash
$ k3d cluster delete mycluster  
```
