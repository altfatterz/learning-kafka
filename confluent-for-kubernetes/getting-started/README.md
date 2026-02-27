# Confluent for Kubernetes (CFK)  

https://docs.confluent.io/operator/current/overview.html

### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent -p "9021:80@loadbalancer"
$ kubectl cluster-info
$ kubectl get nodes
NAME                     STATUS   ROLES                  AGE   VERSION
k3d-confluent-server-0   Ready    control-plane,master   13s   v1.31.5+k3s1

$ kubectl create ns confluent
# set the default the confluent namespace
$ kubectl config set-context --current --namespace confluent
```

### Deploy CFK from Confluent’s Helm repo

CFK 3.1.1 Release Notes -> https://docs.confluent.io/operator/current/release-notes.html#co-long-3-1-1-release-notes
- CFK 3.1.1 allows you to deploy and manage Confluent Platform versions from 7.4.x to 8.1.x on Kubernetes versions 1.26 - 1.34 (OpenShift 4.13 -4.20).

```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes
$ helm list
NAME              	NAMESPACE	REVISION	UPDATED                             	STATUS  	CHART                             	APP VERSION
confluent-operator	confluent	1       	2026-02-27 20:11:28.749759 +0100 CET	deployed	confluent-for-kubernetes-0.1351.59	3.1.1

$ kubectl get all
# wait until the confluent-operator pod is started 
```

### Analyse

```bash
$ kubectl get crds | grep confluent

clusterlinks.platform.confluent.io            2026-02-27T19:11:28Z
cmfrestclasses.platform.confluent.io          2026-02-27T19:11:28Z
confluentrolebindings.platform.confluent.io   2026-02-27T19:11:28Z
connectors.platform.confluent.io              2026-02-27T19:11:28Z
connects.platform.confluent.io                2026-02-27T19:11:28Z
controlcenters.platform.confluent.io          2026-02-27T19:11:28Z
flinkapplications.platform.confluent.io       2026-02-27T19:11:28Z
flinkenvironments.platform.confluent.io       2026-02-27T19:11:28Z
gateways.platform.confluent.io                2026-02-27T19:11:28Z
kafkarestclasses.platform.confluent.io        2026-02-27T19:11:28Z
kafkarestproxies.platform.confluent.io        2026-02-27T19:11:28Z
kafkas.platform.confluent.io                  2026-02-27T19:11:28Z
kafkatopics.platform.confluent.io             2026-02-27T19:11:28Z
kraftcontrollers.platform.confluent.io        2026-02-27T19:11:28Z
kraftmigrationjobs.platform.confluent.io      2026-02-27T19:11:28Z
ksqldbs.platform.confluent.io                 2026-02-27T19:11:28Z
schemaexporters.platform.confluent.io         2026-02-27T19:11:28Z
schemaimporters.platform.confluent.io         2026-02-27T19:11:28Z
schemaregistries.platform.confluent.io        2026-02-27T19:11:28Z
schemas.platform.confluent.io                 2026-02-27T19:11:28Z
usmagents.platform.confluent.io               2026-02-27T19:11:28Z
zookeepers.platform.confluent.io              2026-02-27T19:11:28Z
```

```bash
$ kubectl api-resources --api-group=platform.confluent.io
NAME                    SHORTNAMES                  APIVERSION                      NAMESPACED   KIND
clusterlinks            cl,clusterlink,clink        platform.confluent.io/v1beta1   true         ClusterLink
cmfrestclasses          cmfrestclass                platform.confluent.io/v1beta1   true         CMFRestClass
confluentrolebindings   cfrb,confluentrolebinding   platform.confluent.io/v1beta1   true         ConfluentRolebinding
connectors              ctr,connector               platform.confluent.io/v1beta1   true         Connector
connects                connect                     platform.confluent.io/v1beta1   true         Connect
controlcenters          controlcenter,c3            platform.confluent.io/v1beta1   true         ControlCenter
flinkapplications       flinkapplication            platform.confluent.io/v1beta1   true         FlinkApplication
flinkenvironments       flinkenvironment            platform.confluent.io/v1beta1   true         FlinkEnvironment
gateways                                            platform.confluent.io/v1beta1   true         Gateway
kafkarestclasses        krc,kafkarestclass          platform.confluent.io/v1beta1   true         KafkaRestClass
kafkarestproxies        kafkarestproxy,krp          platform.confluent.io/v1beta1   true         KafkaRestProxy
kafkas                  kafka,broker                platform.confluent.io/v1beta1   true         Kafka
kafkatopics             kt,topic                    platform.confluent.io/v1beta1   true         KafkaTopic
kraftcontrollers        kraftcontroller,kraft       platform.confluent.io/v1beta1   true         KRaftController
kraftmigrationjobs      kraftmigrationjob,kmj       platform.confluent.io/v1beta1   true         KRaftMigrationJob
ksqldbs                 ksqldb,ksql                 platform.confluent.io/v1beta1   true         KsqlDB
schemaexporters         se,schemaexporter           platform.confluent.io/v1beta1   true         SchemaExporter
schemaimporters         si,schemaimporter           platform.confluent.io/v1beta1   true         SchemaImporter
schemaregistries        schemaregistry,sr           platform.confluent.io/v1beta1   true         SchemaRegistry
schemas                 schema                      platform.confluent.io/v1beta1   true         Schema
usmagents                                           platform.confluent.io/v1beta1   true         USMAgent
zookeepers              zookeeper,zk                platform.confluent.io/v1beta1   true         Zookeeper
```

For storage we will use the default (local-path) storage class 

```bash
$ kubectl get sc
NAME                   PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
local-path (default)   rancher.io/local-path   Delete          WaitForFirstConsumer   false                  2m29s
```

For production is recommended:

`volumeBindingMode: WaitForFirstConsumer`
`reclaimPolicy: Retain`
`allowVolumeExpansion: true`

### Install the Confluent Platform

```bash
$ kubectl apply -f confluent-platform-base.yaml
$ kubectl apply -f confluent-platform-schemaregistry.yaml
$ kubectl apply -f confluent-platform-restproxy.yaml
$ kubectl apply -f confluent-platform-connect.yaml
$ kubectl apply -f confluent-platform-controlcenter.yaml
```

Verify the created pods

```bash
$ kubectl get pods
```

Notice that `connect` and `schemaregistry` start early and don't wait until broker and controller nodes are up causing
this way they restart couple of times.

### Create the pageviews topic

```bash
$ kubectl apply -f topic.yaml
```

### Install the Datagen Source Connector

```bash
$ kubectl apply -f datagen-source-connector.yaml
```

### Show the topics and connector 

```bash
$ kubectl get topic
NAME        REPLICAS   PARTITION   STATUS    CLUSTERID                AGE
pageviews   3          1           CREATED   MTgxYmE2MzctYWRhYS00Yw   17s

$ kubectl get connector
NAME        STATUS    CONNECTORSTATUS   TASKS-READY   AGE
pageviews   CREATED   RUNNING           4/4           21s
```

### Consume from the topic

```bash
$ kubectl exec -it kafka-0 -c kafka -- bash 
$ kafka-console-consumer --from-beginning --topic pageviews --bootstrap-server  kafka.confluent.svc.cluster.local:9071
```

#### Install Confluent plugin using Krew

https://docs.confluent.io/operator/current/co-deploy-cfk.html#install-confluent-plugin-using-krew

```bash
$ cd ~/temp
$ export CFK_VERSION=confluent-for-kubernetes-3.1.1
$ curl -O https://confluent-for-kubernetes.s3-us-west-1.amazonaws.com/$CFK_VERSION.tar.gz
$ tar -xvf $CFK_VERSION.tar.gz
$ cd $CFK_VERSION.tar.gz/kubectl-plugin
# If you are upgrading from an older version of the Confluent plugin, delete the old plugin:
$ kubectl krew uninstall confluent
$ kubectl krew  install --manifest=confluent-platform.yaml --archive=kubectl-confluent-darwin-arm64.tar.gz

Installing plugin: confluent
Installed plugin: confluent
\
 | Use this plugin:
 | 	kubectl confluent
 | Documentation:
 | 	https://github.com/confluentinc/kubectl-plugin/
/
```

```bash
# operator version
$ kubectl confluent --version
confluent version v0.1351.59
```

```bash
$ kubectl confluent version
COMPONENT        NAME             VERSION  OPERATOR-VERSION
Kafka            kafka            8.1.1    v0.1351.59
Connect          connect          8.1.1    v0.1351.59
SchemaRegistry   schemaregistry   8.1.1    v0.1351.59
ControlCenter    controlcenter    8.1.1    v0.1351.59
KafkaRestProxy   kafkarestproxy   8.1.1    v0.1351.59
KRaftController  kraftcontroller  8.1.1    v0.1351.59
```

```bash
$ kubectl confluent status
COMPONENT        NAME             READY  STATUS   AGE
Kafka            kafka            3/3    RUNNING  50m
Connect          connect          1/1    RUNNING  37m
SchemaRegistry   schemaregistry   1/1    RUNNING  39m
ControlCenter    controlcenter    1/1    RUNNING  37m
KafkaRestProxy   kafkarestproxy   1/1    RUNNING  37m
KRaftController  kraftcontroller  1/1    RUNNING  50m
```

```bash
$ kubectl confluent cluster kafka listeners
COMPONENT  NAME   LISTENER-NAME  ACCESS    ADDRESS                                 TLS    AUTH  AUTHORIZATION
Kafka      kafka  replication    INTERNAL  kafka.confluent.svc.cluster.local:9072  false
Kafka      kafka  controller     INTERNAL  kafka.confluent.svc.cluster.local:9074  false
Kafka      kafka  external       INTERNAL  kafka.confluent.svc.cluster.local:9092  false
Kafka      kafka  internal       INTERNAL  kafka.confluent.svc.cluster.local:9071  false
```

```bash
$ kubectl confluent connector list
$ kubectl confluent connector pause
$ kubectl confluent connector resume
$ kubectl confluent connector restart
```

### Check ConfigMaps and Secrets

```bash
$ kubectl get cm

NAME                            DATA   AGE
connect-init-config             4      19m
connect-shared-config           7      19m
controlcenter-init-config       4      19m
controlcenter-shared-config     6      19m
kafka-init-config               4      29m
kafka-shared-config             7      29m
kafkarestproxy-init-config      4      19m
kafkarestproxy-shared-config    5      19m
kraftcontroller-init-config     4      32m
kraftcontroller-shared-config   7      32m
kube-root-ca.crt                1      47m
schemaregistry-init-config      4      21m
schemaregistry-shared-config    5      21m
```

```bash
$ kubectl get secret

NAME                                       TYPE                 DATA   AGE
confluent-operator-licensing               Opaque               0      46m
sh.helm.release.v1.confluent-operator.v1   helm.sh/release.v1   1      46m
```

### Control Center

Expose: 

```bash
$ kubectl port-forward pod/controlcenter-0 9021:9021
```

or

```bash
$ kubectl confluent dashboard controlcenter
```

### Expose Control Center via Ingress using Traefik Controller (built in using k3d) - no need for port-forward

```bash
~ $ traefik version
Version:      2.10.5
Codename:     saintmarcelin
Go version:   go1.21.3
Built:        2023-10-11T13:54:02Z
OS/Arch:      linux/amd64
```

```bash
$ kubectl apply -f ingress.yaml
```

Access http://localhost:9021

### Rest Proxy

```bash
$ kubectl port-forward svc/kafkarestproxy 8082:8082
$ http :8082/topics
$ http :8082/v3/clusters
```


### Storage

```bash
$ kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                               STORAGECLASS   VOLUMEATTRIBUTESCLASS   REASON   AGE
pvc-1ddd7a6b-73db-4d76-a1f2-8cdf99a383c1   10Gi       RWO            Delete           Bound    confluent/data0-kraftcontroller-0   local-path     <unset>                          44m
pvc-2b6f5664-11bf-4061-a3ef-2fbeb80eb46a   10Gi       RWO            Delete           Bound    confluent/data0-kafka-1             local-path     <unset>                          41m
pvc-5602e6ef-2dd9-4ada-b72b-403e4cd281ad   10Gi       RWO            Delete           Bound    confluent/data0-kafka-2             local-path     <unset>                          41m
pvc-96f7baa1-6090-49d8-9389-528167e05c1b   10Gi       RWO            Delete           Bound    confluent/data0-controlcenter-0     local-path     <unset>                          31m
pvc-e30b388c-43c5-4be1-9f0b-10167a56b81f   10Gi       RWO            Delete           Bound    confluent/data0-kafka-0             local-path     <unset>                          41m

$ kubectl get pvc
NAME                      STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   VOLUMEATTRIBUTESCLASS   AGE
data0-controlcenter-0     Bound    pvc-96f7baa1-6090-49d8-9389-528167e05c1b   10Gi       RWO            local-path     <unset>                 31m
data0-kafka-0             Bound    pvc-e30b388c-43c5-4be1-9f0b-10167a56b81f   10Gi       RWO            local-path     <unset>                 41m
data0-kafka-1             Bound    pvc-2b6f5664-11bf-4061-a3ef-2fbeb80eb46a   10Gi       RWO            local-path     <unset>                 41m
data0-kafka-2             Bound    pvc-5602e6ef-2dd9-4ada-b72b-403e4cd281ad   10Gi       RWO            local-path     <unset>                 41m
data0-kraftcontroller-0   Bound    pvc-1ddd7a6b-73db-4d76-a1f2-8cdf99a383c1   10Gi       RWO            local-path     <unset>                 44m
```

Notice that for the volumes the RECLAIM_POLICY is 'Delete', this is not a production setup.

### CFK pulls down images from DockerHub from `confluentinc` repositories

https://docs.confluent.io/operator/current/co-custom-registry.html

```bash
docker.io/confluentinc/confluent-init-container:<init-tag>
docker.io/confluentinc/confluent-operator:<cfk-tag>
docker.io/confluentinc/cp-enterprise-control-center-next-gen:<c3-tag>
docker.io/confluentinc/cp-enterprise-replicator:<tag>
docker.io/confluentinc/cp-kafka-rest:<tag>
docker.io/confluentinc/cp-ksqldb-server:<tag>
docker.io/confluentinc/cp-schema-registry:<tag>
docker.io/confluentinc/cp-server:<tag>
docker.io/confluentinc/cp-server-connect:<tag>
docker.io/confluentinc/cp-zookeeper:<tag>
```

Current versions:

```bash
tag=8.1.1
c3-tag=2.3.0
init-tag=3.1.1
cfk-tag=0.1351.59
```

Use custom docker registry: https://docs.confluent.io/operator/current/co-custom-registry.html


### Tear down:

```bash
$ kubectl delete -f datagen-source-connector.yaml
$ kubectl delete -f topic.yaml

$ kubectl delete -f confluent-platform-controlcenter.yaml
$ kubectl delete -f confluent-platform-connect.yaml
$ kubectl delete -f confluent-platform-restproxy.yaml
$ kubectl delete -f confluent-platform-schemaregistry.yaml
$ kubectl delete -f confluent-platform-base.yaml

$ helm uninstall confluent-operator 
```

Resources:
1. https://docs.confluent.io/operator/current/co-quickstart.html
2. https://docs.confluent.io/operator/current/overview.html
3. https://docs.confluent.io/operator/current/co-deploy-cfk.html#co-deploy-operator