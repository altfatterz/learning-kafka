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

### Analyse

```bash
$ kubectl get crds | grep confluent

clusterlinks.platform.confluent.io            2024-06-17T08:35:49Z
confluentrolebindings.platform.confluent.io   2024-06-17T08:35:49Z
connectors.platform.confluent.io              2024-06-17T08:35:49Z
connects.platform.confluent.io                2024-06-17T08:35:49Z
controlcenters.platform.confluent.io          2024-06-17T08:35:49Z
kafkarestclasses.platform.confluent.io        2024-06-17T08:35:49Z
kafkarestproxies.platform.confluent.io        2024-06-17T08:35:49Z
kafkas.platform.confluent.io                  2024-06-17T08:35:49Z
kafkatopics.platform.confluent.io             2024-06-17T08:35:49Z
kraftcontrollers.platform.confluent.io        2024-06-17T08:35:49Z
kraftmigrationjobs.platform.confluent.io      2024-06-17T08:35:49Z
ksqldbs.platform.confluent.io                 2024-06-17T08:35:50Z
schemaexporters.platform.confluent.io         2024-06-17T08:35:50Z
schemaregistries.platform.confluent.io        2024-06-17T08:35:50Z
schemas.platform.confluent.io                 2024-06-17T08:35:50Z
zookeepers.platform.confluent.io              2024-06-17T08:35:50Z
```

```bash
$ kubectl api-resources --api-group=platform.confluent.io
NAME                    SHORTNAMES                  APIVERSION                      NAMESPACED   KIND
clusterlinks            cl,clusterlink,clink        platform.confluent.io/v1beta1   true         ClusterLink
confluentrolebindings   cfrb,confluentrolebinding   platform.confluent.io/v1beta1   true         ConfluentRolebinding
connectors              ctr,connector               platform.confluent.io/v1beta1   true         Connector
connects                connect                     platform.confluent.io/v1beta1   true         Connect
controlcenters          controlcenter,c3            platform.confluent.io/v1beta1   true         ControlCenter
kafkarestclasses        krc,kafkarestclass          platform.confluent.io/v1beta1   true         KafkaRestClass
kafkarestproxies        kafkarestproxy,krp          platform.confluent.io/v1beta1   true         KafkaRestProxy
kafkas                  kafka,broker                platform.confluent.io/v1beta1   true         Kafka
kafkatopics             kt,topic                    platform.confluent.io/v1beta1   true         KafkaTopic
kraftcontrollers        kraftcontroller,kraft       platform.confluent.io/v1beta1   true         KRaftController
kraftmigrationjobs      kraftmigrationjob,kmj       platform.confluent.io/v1beta1   true         KRaftMigrationJob
ksqldbs                 ksqldb,ksql                 platform.confluent.io/v1beta1   true         KsqlDB
schemaexporters         se,schemaexporter           platform.confluent.io/v1beta1   true         SchemaExporter
schemaregistries        schemaregistry,sr           platform.confluent.io/v1beta1   true         SchemaRegistry
schemas                 schema                      platform.confluent.io/v1beta1   true         Schema
zookeepers              zookeeper,zk                platform.confluent.io/v1beta1   true         Zookeeper
```

For storage we will use the default (local-path) storage class 

```bash
$ kubectl get sc
NAME                   PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
local-path (default)   rancher.io/local-path   Delete          WaitForFirstConsumer   false                  3h14m
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
pageviews   3          1           CREATED   8688bde1-9bf6-4c53-b5Q   131m
$ kubectl get connector
NAME        STATUS    CONNECTORSTATUS   TASKS-READY   AGE
pageviews   CREATED   RUNNING           4/4           131m
```

### Consume from the topic

```bash
$ kubectl exec kafka-0 -it -- bash 
$ kafka-console-consumer --from-beginning --topic pageviews --bootstrap-server  kafka.confluent.svc.cluster.local:9071
```

#### Install Confluent plugin using Krew

https://docs.confluent.io/operator/current/co-deploy-cfk.html#install-confluent-plugin-using-krew

```bash
$ cd ~/temp
$ curl -O https://confluent-for-kubernetes.s3-us-west-1.amazonaws.com/confluent-for-kubernetes-2.8.2.tar.gz
$ tar -xvf confluent-for-kubernetes-2.8.2.tar.gz
$ cd confluent-for-kubernetes-2.8.2.tar.gz/kubectl-plugin
# If you are upgrading from an older version of the Confluent plugin, delete the old plugin:
$ kubectl krew uninstall confluent
$ kubectl krew install --manifest=confluent-platform.yaml --archive=kubectl-confluent-darwin-amd64.tar.gz

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
$ kubectl confluent --version
confluent version v0.921.20
```

```bash
$ kubectl confluent version
COMPONENT        NAME             VERSION  OPERATOR-VERSION
Kafka            kafka            7.6.1    v0.921.20
Connect          connect          7.6.1    v0.921.20
SchemaRegistry   schemaregistry   7.6.1    v0.921.20
ControlCenter    controlcenter    7.6.1    v0.921.20
KRaftController  kraftcontroller  7.6.1    v0.921.20
```

```bash
$ kubectl confluent status
COMPONENT        NAME             READY  STATUS        AGE
Kafka            kafka            3/3    RUNNING       2m29s
Connect          connect          1/1    RUNNING       2m29s
SchemaRegistry   schemaregistry   0/1    PROVISIONING  2m28s
ControlCenter    controlcenter    0/1    PROVISIONING  2m28s
KRaftController  kraftcontroller  3/3    RUNNING       2m29s
```

```bash
$ kubectl confluent  cluster kafka listeners
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

kube-root-ca.crt                1      16h
kraftcontroller-shared-config   6      8m12s
kraftcontroller-init-config     3      8m12s
kafka-shared-config             6      7m27s
kafka-init-config               3      7m27s
schemaregistry-shared-config    4      5m21s
schemaregistry-init-config      3      5m21s
kafkarestproxy-shared-config    4      3m51s
kafkarestproxy-init-config      3      3m51s
connect-shared-config           6      2m21s
connect-init-config             3      2m21s
controlcenter-shared-config     5      12s
controlcenter-init-config       3      12s
```

```bash
$ kubectl get secret

NAME                                       TYPE                 DATA   AGE
confluent-operator-licensing               Opaque               0      16h
sh.helm.release.v1.confluent-operator.v1   helm.sh/release.v1   1      16h
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

### Expose Control Center via Ingress using Traefik Controller (built in using k3d)

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
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                               STORAGECLASS   REASON   AGE
pvc-a0bcdaa9-3774-44b2-b43d-868d52613262   10Gi       RWO            Delete           Bound    confluent/data0-kraftcontroller-0   local-path              139m
pvc-f33907ac-97ec-473d-892a-743a82cb8f94   10Gi       RWO            Delete           Bound    confluent/data0-kraftcontroller-2   local-path              139m
pvc-ab22f3c1-5f2a-4334-ba0a-1be089b91ee3   10Gi       RWO            Delete           Bound    confluent/data0-kraftcontroller-1   local-path              139m
pvc-80d64c99-1867-44f3-9893-41b2b3305816   10Gi       RWO            Delete           Bound    confluent/data0-kafka-0             local-path              138m
pvc-6b42a803-1533-4433-a014-a2b683497a08   10Gi       RWO            Delete           Bound    confluent/data0-kafka-2             local-path              138m
pvc-2b417886-27fd-4983-8f03-c65789ed15f7   10Gi       RWO            Delete           Bound    confluent/data0-kafka-1             local-path              138m
pvc-6b5c78da-a39c-42fd-8f6b-f988c9a2c08c   10Gi       RWO            Delete           Bound    confluent/data0-controlcenter-0     local-path              137m

$ kubectl get pvc
NAME                      STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
data0-kraftcontroller-0   Bound    pvc-a0bcdaa9-3774-44b2-b43d-868d52613262   10Gi       RWO            local-path     139m
data0-kraftcontroller-2   Bound    pvc-f33907ac-97ec-473d-892a-743a82cb8f94   10Gi       RWO            local-path     139m
data0-kraftcontroller-1   Bound    pvc-ab22f3c1-5f2a-4334-ba0a-1be089b91ee3   10Gi       RWO            local-path     139m
data0-kafka-0             Bound    pvc-80d64c99-1867-44f3-9893-41b2b3305816   10Gi       RWO            local-path     138m
data0-kafka-2             Bound    pvc-6b42a803-1533-4433-a014-a2b683497a08   10Gi       RWO            local-path     138m
data0-kafka-1             Bound    pvc-2b417886-27fd-4983-8f03-c65789ed15f7   10Gi       RWO            local-path     138m
data0-controlcenter-0     Bound    pvc-6b5c78da-a39c-42fd-8f6b-f988c9a2c08c   10Gi       RWO            local-path     137m
```

Notice that for the volumes the RECLAIM_POLICY is 'Delete', this is not a production setup.

### CFK pulls down images from DockerHub from `confluentinc` repositories

https://docs.confluent.io/operator/current/co-custom-registry.html

```bash
docker.io/confluentinc/confluent-init-container:<init-tag>
docker.io/confluentinc/confluent-operator:<cfk-tag>
docker.io/confluentinc/cp-enterprise-control-center:<tag>
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
tag=7.6.1
init-tag=2.8.0
cfk-tag=0.921.20
```

Use custom docker registry: https://docs.confluent.io/operator/current/co-custom-registry.html


### Tear down:

```bash
$ kubectl delete -f datagen-source-connector.yaml
$ kubectl delete -f topic.yaml
$ kubectl delete -f confluent-platform.yaml
```

Resources:
1. https://docs.confluent.io/operator/current/co-quickstart.html
2. https://docs.confluent.io/operator/current/overview.html
3. https://docs.confluent.io/operator/current/co-deploy-cfk.html#co-deploy-operator