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

### Install the Confluent Platform

```bash
$ kubectl apply -f confluent-platform-base.yaml
$ kubectl apply -f confluent-platform-schemaregistry.yaml
$ kubectl apply -f confluent-platform-controlcenter.yaml
```

## Create the schema config map

```
$ kubectl apply -f payment-value-schema-config.yaml
$ kubectl apply -f confluent-platform-schema.yaml
```

## Validation

```bash
$ kubectl exec schemaregistry-0 -it bash
$ curl http://schemaregistry.confluent.svc.cluster.local:8081/subjects
$ curl http://schemaregistry.confluent.svc.cluster.local:8081/subjects/payment-value/versions
$ curl http://schemaregistry.confluent.svc.cluster.local:8081/subjects/payment-value/versions/1/schema
```

### Cleanup 

```bash
$ k3d cluster delete confluent
```