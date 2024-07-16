### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent -p "9021:80@loadbalancer"
$ kubectl cluster-info
$ kubectl get nodes
NAME                     STATUS   ROLES                  AGE   VERSION
k3d-confluent-server-0   Ready    control-plane,master   73s   v1.27.4+k3s1
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
$ kubectl create secret generic tls-group1 \
--from-file=fullchain.pem=assets/certs/generated/server.pem \
--from-file=cacerts.pem=assets/certs/generated/ca.pem \
--from-file=privkey.pem=assets/certs/generated/server-key.pem 

$ kubectl get secret tls-group1 -o yaml
```

## Provide authentication credentials

* Create a Kubernetes secret object for Kafka and other Confluent Platform component.

This secret object contains file based properties. These files are in the
format that each respective Confluent component requires for authentication
credentials.

```
$ kubectl create secret generic credential \
--from-file=plain-users.json=assets/credentials/creds-kafka-sasl-users.json \
--from-file=digest-users.json=assets/credentials/creds-zookeeper-sasl-digest-users.json \
--from-file=digest.txt=assets/credentials/creds-kafka-zookeeper-credentials.txt \
--from-file=plain.txt=assets/credentials/creds-client-kafka-sasl-user.txt \
--from-file=ldap.txt=assets/credentials/ldap.txt

$ kubectl get secret credential -o yaml 
```

## Provide RBAC principal credentials

* Create a Kubernetes secret object for MDS:
```
$ kubectl create secret generic mds-token \
--from-file=mdsPublicKey.pem=assets/mds/mds-publickey.txt \
--from-file=mdsTokenKeyPair.pem=assets/mds/mds-tokenkeypair.txt

$ kubectl get secret mds-token 
```

* Create Kafka RBAC credential
```
$ kubectl create secret generic mds-client --from-file=bearer.txt=assets/credentials/bearer.txt 
$ kubectl get secret mds-client -o yaml
```

* Create Kafka REST credential
* 
```
$ kubectl create secret generic rest-credential \
--from-file=bearer.txt=assets/credentials/bearer.txt \
--from-file=basic.txt=assets/credentials/bearer.txt
$ kubectl get secret rest-credential -o yaml 
```


### Deploy Confluent Platform

```bash
$ kubectl apply -f confluent-platform-base.yaml
```

### Validate

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



------------------------------------------------------------------------------------------------------------------------


#### Resources:

- RBAC in Confluent Platform: https://docs.confluent.io/platform/current/security/rbac/index.html
- Configure RBAC for CFK: https://docs.confluent.io/operator/current/co-rbac.html


