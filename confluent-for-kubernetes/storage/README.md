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

Check the default (local-path) storage class

```bash
$ kubectl get sc
NAME                   PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
local-path (default)   rancher.io/local-path   Delete          WaitForFirstConsumer   false                  3h14m
```

For production is recommended:

`volumeBindingMode: WaitForFirstConsumer`
`reclaimPolicy: Retain`
`allowVolumeExpansion: true`


### Create a storage class

```bash
$ kubectl apply -f production-storage-class.yaml
$ kubectl get sc
```

### Install the Confluent Platform

```bash
$ kubectl apply -f confluent-platform-base.yaml
$ kubectl apply -f confluent-platform-controlcenter.yaml
```

Only the brokers, ksqldb and controlcenter use persistent storage volumes, the other confluent components do not need a storage class. 

Check in other terminals the created PVs and PVCs

```bash
$ watch kubectl get pv

NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                               STORAGECLASS               REASON   AGE
pvc-cca16bc5-0109-42e0-9c02-161405404d10   2Gi        RWO            Retain           Bound      confluent/data0-kraftcontroller-0   production-storage-class            21m
pvc-b2d0adc7-4197-4b28-949b-34e0ddf44c27   2Gi        RWO            Retain           Bound      confluent/data0-kafka-1             production-storage-class            20m
pvc-a754b0f6-600e-490b-a742-ea2111c65b69   2Gi        RWO            Retain           Bound      confluent/data0-kafka-0             production-storage-class            20m
pvc-5c7c2ac1-377d-4182-b901-fe8c0e42057a   2Gi        RWO            Retain           Bound      confluent/data0-kafka-2             production-storage-class            20m
pvc-e301e718-dcbd-4a32-a17d-34d2c5891259   2Gi        RWO            Retain           Bound      confluent/data0-controlcenter-0     production-storage-class            37s
```

```bash
$ watch kubectl get pvc
NAME                      STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS               AGE
data0-kraftcontroller-0   Bound    pvc-cca16bc5-0109-42e0-9c02-161405404d10   2Gi        RWO            production-storage-class   21m
data0-kafka-1             Bound    pvc-b2d0adc7-4197-4b28-949b-34e0ddf44c27   2Gi        RWO            production-storage-class   21m
data0-kafka-0             Bound    pvc-a754b0f6-600e-490b-a742-ea2111c65b69   2Gi        RWO            production-storage-class   21m
data0-kafka-2             Bound    pvc-5c7c2ac1-377d-4182-b901-fe8c0e42057a   2Gi        RWO            production-storage-class   21m
data0-controlcenter-0     Bound    pvc-e301e718-dcbd-4a32-a17d-34d2c5891259   2Gi        RWO            production-storage-class   59s
```

### Check the directory

Get a PV description

```bash
$ kubectl describe pv pvc-6a630352-c1d4-45ed-9f42-64e0a866fd46
...
Source:
    Type:          HostPath (bare host directory volume)
    Path:          /var/lib/rancher/k3s/storage/pvc-6a630352-c1d4-45ed-9f42-64e0a866fd46_confluent_data0-kafka-0
    HostPathType:  DirectoryOrCreate
```

List the content of that directory

```bash
$ docker exec -it k3d-confluent-server-0 sh
$ cd /var/lib/rancher/k3s/storage/pvc-6a630352-c1d4-45ed-9f42-64e0a866fd46_confluent_data0-kafka-0
$ ls logs
```

With k3d the storage folder is set to `/var/lib/rancher/k3s/storage`

```bash
$ ls -l /var/lib/rancher/k3s/storage
drwxrwxrwx 3 0 0 4096 Jul 11 09:37 pvc-429b9a63-566c-4adf-aff2-a050d40dbbfd_confluent_data0-kafka-2
drwxrwxrwx 3 0 0 4096 Jul 11 09:38 pvc-5492524f-3c1c-44fd-9176-cc6d2f1966e8_confluent_data0-kafka-1
drwxrwxrwx 3 0 0 4096 Jul 11 09:38 pvc-6a630352-c1d4-45ed-9f42-64e0a866fd46_confluent_data0-kafka-0
drwxrwxrwx 3 0 0 4096 Jul 11 09:37 pvc-a7e4b99b-9da5-4e41-9d39-d4cb27d14d3e_confluent_data0-kraftcontroller-0
drwxrwxrwx 3 0 0 4096 Jul 11 10:12 pvc-e301e718-dcbd-4a32-a17d-34d2c5891259_confluent_data0-controlcenter-0
```

### Scale down and up again the kafka StateFul Set

Verify that the PVs are still there after removal since the reclaim policy is set to `Retain`

```bash
$ kubectl scale sts kafka --replicas=0
```

Note that the PVCs and PVs are still there.

Stateful set has this configuration: https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/#persistentvolumeclaim-retention

```bash
spec:
  persistentVolumeClaimRetentionPolicy:
    whenDeleted: Retain
    whenScaled: Retain
```

Scale back up the broker

```bash
$ kubectl scale sts kafka --replicas=3
```

### Remove the kafka broker

```bash
$ kubectl get kafka

NAME    REPLICAS   READY   STATUS    AGE
kafka   3          3       RUNNING   39m
```

```bash
$ kubectl delete kafka
```

In this case the PVCs are gone, and the PVs will be there with `Released` status. 

TODO, how to reuse the already existing PVCs? Solution: We need to keep the PVCs somehow.

### More info

- Configure Storage for Confluent Platform Using Confluent for Kubernetes: https://docs.confluent.io/operator/current/co-storage.html


