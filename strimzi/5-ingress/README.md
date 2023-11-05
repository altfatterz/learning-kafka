### 1. Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
# Start a k8s cluster this time with with 3 agent nodes, 1 server node (control-plane), 
# Create a cluster mapping the port 30080-30083 range to from the 3 agent nodes to 8080-8083 on the host
# Note: Kubernetes’ default NodePort range is 30000-32767
$ rm -r /tmp/kafka-volume
$ mkdir -p /tmp/kafka-volume 
# Start a k8s cluster this time with with 3 agent nodes, 1 server node (control-plane), 
# We need to disable Traefik, since we would like to use Nginx Controller which is tested with Strimzi
$ k3d cluster create mycluster -p "443:443@agent:0,1,2" --agents 3 --k3s-arg "--disable=traefik@server:0" -v /tmp/kafka-volume:/var/lib/rancher/k3s/storage@all
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
# create the `kafka` namespace
$ kubectl create ns kafka
```

### 2. Install ingress-nginx

```bash
$ kubectl apply -f nginx-ingress.yaml
```

```bash
$ watch kubectl get all -n ingress-nginx
```

This script downloaded from [here](https://kubernetes.github.io/ingress-nginx/deploy/#quick-start) and was modified
only to enable `TLS passthrough`, (via `--enable-ssl-passthrough`) which means we are not using any certificates
configured in the Ingress we use the ones configured in Strimzi on Kafka level.

### 3. Install the Strimzi operator

```bash
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

```bash
$ watch kubectl get all -n kafka 
```

### 4. Create the Kafka cluster

```bash
$ kubectl apply -f kafka-ingress.yaml -n kafka
```

Verify the created ingresses:

```bash
$ kubectl get ingress -n kafka
NAME                                  CLASS   HOSTS                        ADDRESS      PORTS     AGE
my-cluster-kafka-external-bootstrap   nginx   bootstrap.127.0.0.1.nip.io   172.20.0.5   80, 443   42s
my-cluster-kafka-external-1           nginx   broker-1.127.0.0.1.nip.io    172.20.0.5   80, 443   42s
my-cluster-kafka-external-2           nginx   broker-2.127.0.0.1.nip.io    172.20.0.5   80, 443   42s
my-cluster-kafka-external-0           nginx   broker-0.127.0.0.1.nip.io    172.20.0.5   80, 443   42s
```

### 5. Create a topic using a strimzi resource definition:

After the services are up and running, create a topic:

```bash
$ kubectl apply -n kafka -f kafka-topic.yaml
```

Verify that the resource details:

```bash
$ kubectl get kt -n kafka
$ kubectl describe kt my-topic -n kafka
```


### 6. Try with a producer / consumer

Extract the `ca.p12` and `ca.crt` from the Cluster CA secret

```bash
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.p12}' | base64 -d > ca.p12
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.password}' | base64 -d > ca.password
```

```bash
$ kafka-console-producer --bootstrap-server bootstrap.127.0.0.1.nip.io:443 \
--producer-property security.protocol=SSL \
--producer-property ssl.truststore.password=tYKolG6XjFv3 \
--producer-property ssl.truststore.location=./ca.p12 \
--topic my-topic
````


```bash
$ kafka-console-consumer --bootstrap-server bootstrap.127.0.0.1.nip.io:443 \
--consumer-property security.protocol=SSL \
--consumer-property ssl.truststore.password=tYKolG6XjFv3 \
--consumer-property ssl.truststore.location=./ca.p12 \
--topic my-topic \
--from-beginning
```

### 7. Try with `kcat`

```bash
# extract the certificate in PEM format
$ kubectl get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt
# get metadata for all topics
$ kcat -L -b bootstrap.127.0.0.1.nip.io:443 -X security.protocol=SSL -X ssl.ca.location=ca.crt
# produce
$ echo "foo\nbar\nbaz" | kcat -P -b bootstrap.127.0.0.1.nip.io:443 -t my-topic -X security.protocol=SSL -X ssl.ca.location=ca.crt 
# consume 
$ kcat -C -b bootstrap.127.0.0.1.nip.io:443 -t my-topic -X security.protocol=SSL -X ssl.ca.location=ca.crt
foo
bar
baz 
```

### 7. Examine the certificate

Run it from your host:

```bash
$ openssl s_client -connect bootstrap.127.0.0.1.nip.io:443
CONNECTED(00000003)
depth=1 O = io.strimzi, CN = cluster-ca v0
verify error:num=19:self-signed certificate in certificate chain
verify return:1
depth=1 O = io.strimzi, CN = cluster-ca v0
verify return:1
depth=0 O = io.strimzi, CN = my-cluster-kafka
verify return:1
---
Certificate chain
 0 s:O = io.strimzi, CN = my-cluster-kafka
   i:O = io.strimzi, CN = cluster-ca v0
   a:PKEY: rsaEncryption, 2048 (bit); sigalg: RSA-SHA512
   v:NotBefore: Nov  5 15:55:49 2023 GMT; NotAfter: Nov  4 15:55:49 2024 GMT
 1 s:O = io.strimzi, CN = cluster-ca v0
   i:O = io.strimzi, CN = cluster-ca v0
   a:PKEY: rsaEncryption, 4096 (bit); sigalg: RSA-SHA512
   v:NotBefore: Nov  5 15:54:52 2023 GMT; NotAfter: Nov  4 15:54:52 2024 GMT
---
Server certificate
-----BEGIN CERTIFICATE-----
MIIF3TCCA8WgAwIBAgIUJwFJkUB0Ur/Lj4M1qrvvIKTYz9kwDQYJKoZIhvcNAQEN
BQAwLTETMBEGA1UECgwKaW8uc3RyaW16aTEWMBQGA1UEAwwNY2x1c3Rlci1jYSB2
MDAeFw0yMzExMDUxNTU1NDlaFw0yNDExMDQxNTU1NDlaMDAxEzARBgNVBAoMCmlv
LnN0cmltemkxGTAXBgNVBAMMEG15LWNsdXN0ZXIta2Fma2EwggEiMA0GCSqGSIb3
DQEBAQUAA4IBDwAwggEKAoIBAQCxiBE4FfRVt3th1AYouDfL/ve0H4ZkYGfG6PY1
nxn+3y9YqvzDi2VsI3i5urVttDA5+ixDnLMaV65IgA0ayQFGnf7nxNXBnnw3gSoU
e0Ktf6K6YxQivti3C4rbapRjdMxQ+2pfpXtouV5aBtItMQ8zND3Sy4QyovnbKS+7
UWrKiHVhqrm0cMSStGgL4aWIqwA+B62TYiH+BGWYN8i5svJsDXhSIbSyTQ3YxlV7
5l7q8Qwc0ZJdsgDpQUrTpXVUb+MQ11cl1ylNVdlxfMv3y9LpcVeFk4OMC1rJZ2VD
spZstYLhYq2YhVtYdprmH2r33CFtq/m49q/RnNIR9vuZ9LoZAgMBAAGjggHwMIIB
7DCCAegGA1UdEQSCAd8wggHbghpib290c3RyYXAuMTI3LjAuMC4xLm5pcC5pb4Ig
bXktY2x1c3Rlci1rYWZrYS1ib290c3RyYXAua2Fma2GCIm15LWNsdXN0ZXIta2Fm
a2EtYnJva2Vycy5rYWZrYS5zdmOCGWJyb2tlci0wLjEyNy4wLjAuMS5uaXAuaW+C
Mm15LWNsdXN0ZXIta2Fma2EtYm9vdHN0cmFwLmthZmthLnN2Yy5jbHVzdGVyLmxv
Y2Fsgh5teS1jbHVzdGVyLWthZmthLWJyb2tlcnMua2Fma2GCNW15LWNsdXN0ZXIt
a2Fma2EtMC5teS1jbHVzdGVyLWthZmthLWJyb2tlcnMua2Fma2Euc3ZjghhteS1j
bHVzdGVyLWthZmthLWJyb2tlcnOCMG15LWNsdXN0ZXIta2Fma2EtYnJva2Vycy5r
YWZrYS5zdmMuY2x1c3Rlci5sb2NhbIJDbXktY2x1c3Rlci1rYWZrYS0wLm15LWNs
dXN0ZXIta2Fma2EtYnJva2Vycy5rYWZrYS5zdmMuY2x1c3Rlci5sb2NhbIIkbXkt
Y2x1c3Rlci1rYWZrYS1ib290c3RyYXAua2Fma2Euc3ZjghpteS1jbHVzdGVyLWth
ZmthLWJvb3RzdHJhcDANBgkqhkiG9w0BAQ0FAAOCAgEA0RA7JTValSzvep40Yvp1
sGISxCZRYhX2WQExjGEyRncLzlbVmhm0QmRfrIktL9HacdWYJubIAvZdrdoaKgqE
Kx+J/6at/GiW+laVqSWNCs0DBy02jEXKrsbyWCqnu0C0jcDzg8qb1Bi1ZChiW3L5
QhLF1XjhrPAiejpYicak1S3tZJ/EVRku78pRe4zLLzaiN2mcxiWrSB3oqu4PYtF5
evEOP/nDVcEC/OV2LeVMifLwDjSzNTh6jdix2nSJ7K78x72Dnhv8iBYsu1sCLyhF
3ttQ05LmeWtKCVldwxzBFvHD4FAM1bc40LqwV8rDxwgXs6RED44F/ctdpzdXtMoy
0cDNpboOrssAPYGfC/RFAe6t2JdxjM4enYcJOwiISYial3fUIahPsjHD905ZP+PA
EH+OZ36UDl+98LaFN9aTmcG0ptQ8UzTrbkrvFKDWLy0wn/8K4A11KpJyT8lawSxN
gigKV9h3GQHqxoVAZ1E4hHXzFT7KeRGPkubj1hrXes+zQMYevEhK8m+IsAGA3NEl
WCW+8d0Kc2018jv4CceICe9xLeTfQmGFExJO0dfBImQBxectMKK91TnT+8mF6Ud7
UnjtqE8MRFSNMsSSXRQrDEK/J24CDARKUIhIYHle6eBTR9/x9EeDMpfhdhjD9+tq
oOIGiadfCSwMpbXoMQJT690=
-----END CERTIFICATE-----
subject=O = io.strimzi, CN = my-cluster-kafka
issuer=O = io.strimzi, CN = cluster-ca v0
---
No client certificate CA names sent
Peer signing digest: SHA256
Peer signature type: RSA-PSS
Server Temp Key: X25519, 253 bits
---
SSL handshake has read 3371 bytes and written 408 bytes
Verification error: self-signed certificate in certificate chain
---
New, TLSv1.3, Cipher is TLS_AES_256_GCM_SHA384
Server public key is 2048 bit
This TLS version forbids renegotiation.
Compression: NONE
Expansion: NONE
No ALPN negotiated
Early data was not sent
Verify return code: 19 (self-signed certificate in certificate chain)
```
