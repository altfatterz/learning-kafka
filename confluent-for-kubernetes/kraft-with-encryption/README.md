# Setup a Kafka cluster in Kraft mode and enable encryption using cert-manager 

### The `tls.key` and `tls.crt` where generated via 

```bash
$ openssl genrsa -out security/tls.key 2048
$ openssl req -x509 -new -nodes -key security/tls.key -sha256 -days 1825 -out security/tls.crt \
  -subj '/CN=ca1.mimacom.com/OU=development/O=mimacom/L=Zurich/C=CH' 
```

### View certificate:

```bash
$ openssl x509 -in security/tls.crt -text -noout
```

### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent
$ kubectl cluster-info
$ kubectl create ns confluent
$ kubectl config set-context --current --namespace confluent
```

### Install `cert-manager`

It will be installed in the `cert-manager` namespace

```bash
$ kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.2/cert-manager.yaml
```

### Check the running pods for `cert-manager`

```bash
$ kubectl get pods -n cert-manager

NAME                                      READY   STATUS    RESTARTS   AGE
cert-manager-cainjector-9795f664f-x6p88   1/1     Running   0          27s
cert-manager-594b84b49d-mdjdv             1/1     Running   0          27s
cert-manager-webhook-64d9455f47-b5rld     1/1     Running   0          27s
```

### Check the new CRDs

```bash
$ kubectl get crds | grep cert-manager

certificaterequests.cert-manager.io     2024-06-17T14:51:47Z
certificates.cert-manager.io            2024-06-17T14:51:47Z
challenges.acme.cert-manager.io         2024-06-17T14:51:47Z
clusterissuers.cert-manager.io          2024-06-17T14:51:47Z
issuers.cert-manager.io                 2024-06-17T14:51:47Z
orders.acme.cert-manager.io             2024-06-17T14:51:47Z
```

Cert Manager supports different modes for certificate authorities:
- Using a CA Issuer - https://cert-manager.io/docs/configuration/ca/ - we are using this one
- Using a Self-Signed Issuer - https://cert-manager.io/docs/configuration/selfsigned/
- Using a Let's Encrypt Issuer - https://cert-manager.io/docs/configuration/acme/
- Using Vault - https://cert-manager.io/docs/configuration/vault/
- Using Venafi https://cert-manager.io/docs/configuration/venafi/

### Create the certificates with kustomize

```bash
$ kubectl apply --dry-run=client -k `pwd`/security -o yaml 
$ kubectl apply -k  `pwd`/security

secret/ca-key-pair created
certificate.cert-manager.io/ca-c3-cert created
certificate.cert-manager.io/ca-connect-cert created
certificate.cert-manager.io/ca-controller-cert created
certificate.cert-manager.io/ca-kafka-cert created
certificate.cert-manager.io/ca-schemaregistry-cert created
issuer.cert-manager.io/ca-issuer create
```

### Verify the created certificates / issuers

```bash
$ kubectl get certificates -o wide

NAME                     READY   SECRET               ISSUER      STATUS                                          AGE
ca-c3-cert               True    controlcenter-tls    ca-issuer   Certificate is up to date and has not expired   85m
ca-controller-cert       True    controller-tls       ca-issuer   Certificate is up to date and has not expired   85m
ca-connect-cert          True    connect-tls          ca-issuer   Certificate is up to date and has not expired   85m
ca-schemaregistry-cert   True    schemaregistry-tls   ca-issuer   Certificate is up to date and has not expired   85m
ca-kafka-cert            True    kafka-tls            ca-issuer   Certificate is up to date and has not expired   85m
```

```bash
$ kubectl get issuer

NAME        READY   AGE
ca-issuer   True    55s
```

### View the created secrets

```bash
$ kubectl get secret

ca-key-pair          kubernetes.io/tls   2      111s
controlcenter-tls    kubernetes.io/tls   3      111s
kafka-tls            kubernetes.io/tls   3      109s
controller-tls       kubernetes.io/tls   3      107s
connect-tls          kubernetes.io/tls   3      107s
schemaregistry-tls   kubernetes.io/tls   3      107s
```

```bash
$ kubectl describe secret ca-key-pair  
$ kubectl describe secret controlcenter-tls
$ kubectl describe secret kafka-tls
$ kubectl describe secret controller-tls
$ kubectl describe secret connect-tls 
$ kubectl describe secret schemaregistry-tls
```

Better inspect with the [cmctl](https://cert-manager.io/docs/reference/cmctl/#renew tool
```bash
$ brew install cmctl
$ cmctl inspect secret kafka-tls
```

### Install Confluent For Kubernetes using Helm

```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes --set kRaftEnabled=true
```

```bash
$ kubectl get crds | grep confluent

clusterlinks.platform.confluent.io            2024-02-12T20:03:58Z
confluentrolebindings.platform.confluent.io   2024-02-12T20:03:58Z
connectors.platform.confluent.io              2024-02-12T20:03:58Z
connects.platform.confluent.io                2024-02-12T20:03:58Z
controlcenters.platform.confluent.io          2024-02-12T20:03:58Z
kafkarestclasses.platform.confluent.io        2024-02-12T20:03:58Z
kafkarestproxies.platform.confluent.io        2024-02-12T20:03:58Z
kafkas.platform.confluent.io                  2024-02-12T20:03:58Z
kafkatopics.platform.confluent.io             2024-02-12T20:03:58Z
kraftcontrollers.platform.confluent.io        2024-02-12T20:03:58Z
ksqldbs.platform.confluent.io                 2024-02-12T20:03:58Z
schemaexporters.platform.confluent.io         2024-02-12T20:03:58Z
schemaregistries.platform.confluent.io        2024-02-12T20:03:59Z
schemas.platform.confluent.io                 2024-02-12T20:03:59Z
zookeepers.platform.confluent.io              2024-02-12T20:03:59Z
```

Check that the Confluent For Kubernetes operator pod comes up and is running:

```bash
$ watch kubectl get pods

NAME                                  READY   STATUS    RESTARTS   AGE
confluent-operator-6c7bb75484-k294m   1/1     Running   0          22s
```

### Deploy the Confluent Platform

```bash
$ kubectl apply -f confluent-platform-core.yaml
$ kubectl apply -f confluent-platform-schemaregistry.yaml
$ kubectl apply -f confluent-platform-connect.yaml
$ kubectl apply -f confluent-platform-controlcenter.yaml
```

### Control Center

Expose control center using

```bash
$ kubectl confluent dashboard controlcenter
```

Access the https://localhost:9021 and check the certificate in the browser


### Analyse config how to connect a client 

```bash
$ kubectl describe kafka kafka
...
Client:  
bootstrap.servers=kafka.confluent.svc.cluster.local:9071
security.protocol=SSL
ssl.truststore.location=/mnt/sslcerts/truststore.p12
ssl.truststore.password=<<jksPassword>>
```


```bash
Common Name (CN)	controlcenter
Organisation (O)	<Not part of certificate>
Organisational Unit (OU)	<Not part of certificate>
Common Name (CN)	ca1.mimacom.com
Organisation (O)	mimacom
Organisational Unit (OU)	development
Issued On	Monday 17 June 2024 at 16:53:04
Expires On	Sunday 15 September 2024 at 16:53:04
```

```
$ kubectl get secret
$ kubectl describe secret kafka-pkcs12
...
Data
====
truststore.p12:   1186 bytes
cacerts.pem:      1318 bytes
fullchain.pem:    1375 bytes
jksPassword.txt:  27 bytes
keystore.p12:     3555 bytes
privkey.pem:      1679 bytes
secretHash.json:  55 bytes
```

```bash
$ kubectl get secret kafka-pkcs12 -o jsonpath='{.data.jksPassword\.txt}' | base64 -d
```

### Create a configuration secret for client applications to use:

```bash
$ kubectl create secret generic kafka-client-config-secure --from-file=kafka.properties
$ kubectl get secret kafka-client-config-secure -o jsonpath='{.data.kafka\.properties}' | base64 -d 
```

### Install the sample producer app and topic.

```bash
$ kubectl apply -f producer-app-data.yaml
```

Check the logs for the created demo and view the Controll Center demo how the messages are flowing in

```bash
$ kubectl logs -f elastic-0
```

Analyse the mount directory

```bash
$ kubectl exec -it elastic-0 -- sh
ls -l /mnt
lrwxrwxrwx 1 root root  23 Jul 12 09:02 kafka.properties -> ..data/kafka.properties
drwxrwxrwt 3 root root 220 Jul 12 09:02 sslcerts
```


### Re-issuance triggered by user actions. By default, the private key won't be rotated automatically.


```bash
$ cmctl renew ca-kafka-cert
$ kubectl describe cert ca-kafka-cert

Events:
  Normal  Reused     5m50s                cert-manager-certificates-key-manager      Reusing private key stored in existing Secret resource "kafka-tls"
  Normal  Requested  5m50s                cert-manager-certificates-request-manager  Created new CertificateRequest resource "ca-kafka-cert-2"
  Normal  Issuing    5m50s (x2 over 87m)  cert-manager-certificates-issuing          The certificate has been successfully issued
  
$ kubectl get certificaterequests

ca-c3-cert-1               True                True    ca-issuer   system:serviceaccount:cert-manager:cert-manager   88m
ca-kafka-cert-1            True                True    ca-issuer   system:serviceaccount:cert-manager:cert-manager   88m
ca-controller-cert-1       True                True    ca-issuer   system:serviceaccount:cert-manager:cert-manager   88m
ca-connect-cert-1          True                True    ca-issuer   system:serviceaccount:cert-manager:cert-manager   88m
ca-schemaregistry-cert-1   True                True    ca-issuer   system:serviceaccount:cert-manager:cert-manager   88m
ca-kafka-cert-2            True                True    ca-issuer   system:serviceaccount:cert-manager:cert-manager   7m2s
```

Notice that the kafka brokers are restarted.

Using the setting `rotationPolicy: Always`, the private key Secret associated with a Certificate
object can be configured to be rotated as soon as an the Certificate is reissued


[wave](https://github.com/wave-k8s/wave) is a Secret controller that makes sure deployments get restarted
whenever a mounted Secret changes.

------------------------------------------------------------------------------------------------------------------------

### Cleanup

```bash
$ k3d cluster delete confluent
```
