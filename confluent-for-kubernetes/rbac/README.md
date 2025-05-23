### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent -p "9021:80@agent:0,1,2" -p "30000-30003:30000-30003@agent:0,1,2" --agents 3
$ kubectl cluster-info
$ kubectl get nodes

NAME                     STATUS   ROLES                  AGE   VERSION
k3d-confluent-server-0   Ready    control-plane,master   16s   v1.28.8+k3s1
k3d-confluent-agent-1    Ready    <none>                 13s   v1.28.8+k3s1
k3d-confluent-agent-2    Ready    <none>                 12s   v1.28.8+k3s1
k3d-confluent-agent-0    Ready    <none>                 12s   v1.28.8+k3s1

$  docker ps

CONTAINER ID   IMAGE                            COMMAND                  CREATED          STATUS          PORTS                                                                                                                      NAMES
481840cb55d8   ghcr.io/k3d-io/k3d-tools:5.6.3   "/app/k3d-tools noop"    46 seconds ago   Up 45 seconds                                                                                                                              k3d-confluent-tools
db8052df29db   ghcr.io/k3d-io/k3d-proxy:5.6.3   "/bin/sh -c nginx-pr…"   46 seconds ago   Up 34 seconds   0.0.0.0:9021->80/tcp, 0.0.0.0:50693->6443/tcp, 0.0.0.0:8000->30000/tcp, 0.0.0.0:8001->30001/tcp, 0.0.0.0:8002->30002/tcp   k3d-confluent-serverlb
4d4ce6a84871   rancher/k3s:v1.28.8-k3s1         "/bin/k3d-entrypoint…"   46 seconds ago   Up 39 seconds                                                                                                                              k3d-confluent-agent-2
1197f60a32f6   rancher/k3s:v1.28.8-k3s1         "/bin/k3d-entrypoint…"   46 seconds ago   Up 39 seconds                                                                                                                              k3d-confluent-agent-1
98a53806ed54   rancher/k3s:v1.28.8-k3s1         "/bin/k3d-entrypoint…"   46 seconds ago   Up 39 seconds                                                                                                                              k3d-confluent-agent-0
fcd5fe8433dc   rancher/k3s:v1.28.8-k3s1         "/bin/k3d-entrypoint…"   46 seconds ago   Up 43 seconds                                                                                                                              k3d-confluent-server-0

$ kubectl taint nodes k3d-confluent-server-0 key1=value1:NoSchedule
$ kubectl create ns confluent
$ kubectl config set-context --current --namespace confluent
```

### Deploy CFK from Confluent’s Helm repo

```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes --set kRaftEnabled=true
$ helm list
NAME              	NAMESPACE	REVISION	UPDATED                              	STATUS  	CHART                            	APP VERSION
confluent-operator	confluent	1       	2024-06-17 10:06:45.422168 +0200 CEST	deployed	confluent-for-kubernetes-0.921.20	2.8.2
$ kubectl get all
# wait until the confluent-operator pod is started 
```

### Deploy OpenLdap 

```bash
$ helm upgrade --install -f openldap/ldaps-rbac.yaml test-ldap openldap
$ kubectl exec -it ldap-0 -- bash
```

Run an LDAP search command

```bash
$ ldapsearch -LLL -x -H ldap://ldap.confluent.svc.cluster.local:389 -b 'dc=test,dc=com' -D "cn=mds,dc=test,dc=com" -w 'Developer!'
```


When RBAC is enabled (`spec.authorization.type: rbac`) CFK always uses the `Bearer` authentication for Confluent 
components, ignoring the `spec.authentication` setting.


The internal Kubernetes domain name depends on the namespace you deploy to. If you deploy to confluent namespace, 
then the internal domain names will be:
* .kraftcontroller.confluent.svc.cluster.local
* .kafka.confluent.svc.cluster.local
* .confluent.svc.cluster.local

### Create certificates

```bash
$ brew install cfssl
$ mkdir assets/certs/generated
// Create Certificate Authority 
$ cfssl gencert -initca assets/certs/ca-csr.json | cfssljson -bare assets/certs/generated/ca -
// validate Certificate Authority
// Certificate authority (CA) private key (`ca-key.pem`)
// Certificate authority (CA) certificate (`ca.pem`)
$ openssl x509 -in assets/certs/generated/ca.pem -text -noout
// create server certificates with the appropriate SANs (SANs listed in server-domain.json)
cfssl gencert -ca=assets/certs/generated/ca.pem \
-ca-key=assets/certs/generated/ca-key.pem \
-config=assets/certs/ca-config.json \
-profile=server assets/certs/server-domain.json | cfssljson -bare assets/certs/generated/server
// validate the server certificate and SANs
openssl x509 -in assets/certs/generated/server.pem -text -noout
```

## Provide component TLS certificates

```
$ kubectl create secret generic tls-group \
--from-file=fullchain.pem=assets/certs/generated/server.pem \
--from-file=cacerts.pem=assets/certs/generated/ca.pem \
--from-file=privkey.pem=assets/certs/generated/server-key.pem  

$ kubectl describe secret tls-group

Name:         tls-group
Namespace:    confluent
Data
====
cacerts.pem:    1306 bytes
fullchain.pem:  1749 bytes
privkey.pem:    1679 bytes
```

More info here: https://docs.confluent.io/operator/current/co-manage-certificates.html#rotate-user-provided-server-certificates


## Provide authentication credentials

* Create a Kubernetes secret object for Kafka and other Confluent Platform component.

This secret object contains file based properties. These files are in the
format that each respective Confluent component requires for authentication
credentials.

```
$ kubectl create secret generic credential \
--from-file=plain-users.json=assets/credentials/creds-kafka-sasl-users.json \
--from-file=ldap.txt=assets/credentials/ldap.txt

$ kubectl describe secret credential

Name:         credential
Namespace:    confluent
Type:  Opaque
Data
====
ldap.txt:          51 bytes
plain-users.json:  91 bytes 
```

## Provide RBAC principal credentials

* Create a Kubernetes secret object for MDS:

```bash
$ openssl x509 -in assets/mds/mds-publickey.txt -text -noout

$ kubectl create secret generic mds-token \
--from-file=mdsPublicKey.pem=assets/mds/mds-publickey.txt \
--from-file=mdsTokenKeyPair.pem=assets/mds/mds-tokenkeypair.txt

$ kubectl describe secret mds-token

Name:         mds-token
Namespace:    confluent
Type:  Opaque
Data
====
mdsPublicKey.pem:     450 bytes
mdsTokenKeyPair.pem:  1678 bytes 
```

* Create Kafka RBAC credential

```bash
$ kubectl create secret generic mds-client --from-file=bearer.txt=assets/credentials/bearer.txt 
$ kubectl describe secret mds-client

Name:         mds-client
Namespace:    confluent
Type:  Opaque
Data
====
bearer.txt:  37 bytes
```

* Create Kafka REST credential
 
```bash
$ kubectl create secret generic rest-credential --from-file=bearer.txt=assets/credentials/bearer.txt 
$ kubectl describe secret rest-credential

Name:         rest-credential
Namespace:    confluent
Type:  Opaque
Data
====
bearer.txt:  37 bytes 
```

### Deploy Confluent Platform

```bash
$ kubectl apply -f confluent-platform-base.yaml
```

### Access MDS Openapi

```bash
$ kubectl port-forward svc/kafka 8090:8090
```

Open browser

https://localhost:8090/security/openapi/swagger-ui/index.html


### Confluent Login

```bash
$ confluent login --url https://localhost:8090 --ca-cert-path assets/certs/generated/server.pem
```

Username: kafka
Password: kafka-secret

```bash
$ confluent cluster describe --url https://localhost:8090 --ca-cert-path assets/certs/generated/server.pem

Confluent Resource Name: b6c53b39-2274-43d7-96w

Scope:
      Type      |           ID
----------------+-------------------------
  kafka-cluster | b6c53b39-2274-43d7-96w
```

```bash
$ kubectl apply -f testadmin-rb.yaml
$ kubectl get confluentrolebindings

NAME           STATUS    KAFKACLUSTERID           PRINCIPAL        ROLE           KAFKARESTCLASS      AGE
testadmin-rb   CREATED   b6c53b39-2274-43d7-96w   User:testadmin   ClusterAdmin   confluent/default   13s
```

```bash
$ confluent iam rbac role-binding list --kafka-cluster b6c53b39-2274-43d7-96w --role ClusterAdmin

    Principal
------------------
  User:testadmin
```

```bash
$ confluent iam rbac role list
```

The MDS maintains a local cache of authorization data that is persisted to an internal Kafka topic named `_confluent-metadata-auth`.

### Validate

Verify broker properties

```bash
$ kubectl exec kafka-0 -- sh
cat /opt/confluentinc/etc/kafka/kafka.properties
```

```bash
$ kubectl describe kafka

    Internal:
      Authentication Type:  plain
      Client:               bootstrap.servers=kafka.confluent.svc.cluster.local:9071
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=<<sasl_username>> password=<<sasl_password>>;
sasl.mechanism=PLAIN
security.protocol=SASL_SSL
ssl.truststore.location=/mnt/sslcerts/truststore.p12
ssl.truststore.password=<<jksPassword>>
```

### Connect via a producer

```bash
$ kubectl get secret kafka-pkcs12 -o yaml -o jsonpath='{.data.jksPassword\.txt}' | base64 -d
jksPassword=mystorepassword
```

### Create a configuration secret for client applications to use:

```bash
$ kubectl create secret generic kafka-client-config-secure --from-file=kafka.properties
$ kubectl get secret kafka-client-config-secure -o yaml
```

```bash
$ kubectl apply -f secure-clients.yaml
```

### Nodeport

```bash
$ kafka-topics --bootstrap-server localhost:30000 --create --topic demo
$ kafka-topics --bootstrap-server localhost:30000 --list
```


### Configure authentication to access Kafka

#### SASL/PLAIN

#### SASL/PLAIN with LDAP authentication

- Confluent does not recommend using SASL/PLAIN with LDAP for interbroker communication because intermittent LDAP errors
  can cause significant broker performance issues, use instead mTLS, SASL/PLAIN

### ConfluentRolebinding

- CFK provides the ConfluentRolebinding CRD with which you can declaratively create and manage RBAC for users and groups
as CRs in Kubernetes

More info: https://docs.confluent.io/operator/current/co-manage-rbac.html

------------------------------------------------------------------------------------------------------------------------


------------------------------------------------------------------------------------------------------------------------


#### Resources:

- RBAC in Confluent Platform: https://docs.confluent.io/platform/current/security/rbac/index.html
- Configure RBAC for CFK: https://docs.confluent.io/operator/current/co-rbac.html


