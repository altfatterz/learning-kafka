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

### Create client-side Bearer credentials for MDS

The expected key is `bearer.txt`.

```bash
$ kubectl create secret generic kafka-mds-client --from-file=bearer.txt=credentials/kafka-mds-client.txt
$ kubectl create secret generic c3-mds-client --from-file=bearer.txt=credentials/c3-mds-client.txt
$ kubectl create secret generic rest-client --from-file=bearer.txt=credentials/rest-client.txt
```

Creat the `mds-token`
```bash
kubectl create secret generic mds-token \
--from-file=mdsPublicKey.pem=mds/mds-publickey.txt \
--from-file=mdsTokenKeyPair.pem=mds/mds-tokenkeypair.txt 
```


------------------------------------------------------------------------------------------------------------------------

------------------------------------------------------------------------------------------------------------------------


#### Resources:

- RBAC in Confluent Platform: https://docs.confluent.io/platform/current/security/rbac/index.html
- Configure RBAC for CFK: https://docs.confluent.io/operator/current/co-rbac.html


