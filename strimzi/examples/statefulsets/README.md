# StatefulSet

StatefulSets are valuable:

- Stable, unique network identifiers.
- Stable, persistent storage.
- Ordered, graceful deployment and scaling.
- Ordered, automated rolling updates.

When using `StatefulSets` note that a `ReplicaSet` is not created.

### Pod Identity

StatefulSet Pods have a unique identity that consists of:
- an ordinal, 
- a stable network identity (headless service)
- stable storage. 
The identity sticks to the Pod, regardless of which node it's (re)scheduled on.

### Ordinal Index

For a StatefulSet with N replicas, each Pod in the StatefulSet will be assigned an integer ordinal, from 0 up through N-1, that is unique over the Set.

### Stable Network ID

Each pod within the StatefulSet creates the hostname:

```bash
$(statefulset name)-$(ordinal)
```

```bash
$ kubectl run -i --tty --image busybox:1.28 dns-test --restart=Never --rm
$ nslookup nginx-statefulset-0.headless-nginx
Server:    10.43.0.10
Address 1: 10.43.0.10 kube-dns.kube-system.svc.cluster.local

Name:      nginx-statefulset-0.headless-nginx
Address 1: 10.42.1.26 nginx-statefulset-0.headless-nginx.default.svc.cluster.local
```

If you need to find and connect to the active members of a StatefulSet, you should query the CNAME of the headless Service (headless-nginx.default.svc.cluster.local). The SRV records associated with the CNAME will contain only the Pods in the StatefulSet that are Running and Ready.

```bash
$ nslookup headless-nginx
Server:    10.43.0.10
Address 1: 10.43.0.10 kube-dns.kube-system.svc.cluster.local

Name:      headless-nginx
Address 1: 10.42.0.27 nginx-statefulset-1.headless-nginx.default.svc.cluster.local
Address 2: 10.42.0.29 nginx-statefulset-2.headless-nginx.default.svc.cluster.local
Address 3: 10.42.1.26 nginx-statefulset-0.headless-nginx.default.svc.cluster.local
```

If your application already implements connection logic that tests for liveness and readiness, you can use the SRV records of the Pods ( `nginx-statefulset-0.headless-nginx.default.svc.cluster.local`, `nginx-statefulset-1.headless-nginx.default.svc.cluster.local`), as they are stable, and your application will be able to discover the Pods' addresses when they transition to Running and Ready.

### Scaling a StatefulSet

```bash
$ kubectl scale sts nginx-statefulset --replicas=5
```
or 
```bash
$ kubectl patch sts web -p '{"spec":{"replicas":5}}'
```

As with StatefulSet creation, the StatefulSet controller created each Pod sequentially with respect to its ordinal index, and it waited for each Pod's predecessor to be Running and Ready before launching the subsequent Pod.

### Scaling Down

```bash
$ kubectl scale sts nginx-statefulset --replicas=2
```
The controller deleted one Pod at a time, in reverse order with respect to its ordinal index, and it waited for each to be completely shutdown before deleting the next.

```bash
$ kubectl get sts
```

```bash
$ kubectl describe sts nginx-statefulset

Name:               nginx-statefulset
Namespace:          default
CreationTimestamp:  Thu, 03 Nov 2022 21:34:20 +0100
Selector:           app=nginx
Labels:             <none>
Annotations:        <none>
Replicas:           3 desired | 3 total
Update Strategy:    RollingUpdate
  Partition:        0
Pods Status:        3 Running / 0 Waiting / 0 Succeeded / 0 Failed
Pod Template:
  Labels:  app=nginx
  Containers:
   nginx:
    Image:        nginx:1.23.2-alpine
    Port:         80/TCP
    Host Port:    0/TCP
    Environment:  <none>
    Mounts:
      /usr/share/nginx/html from www (rw)
  Volumes:  <none>
Volume Claims:
  Name:          www
  StorageClass:
  Labels:        <none>
  Annotations:   <none>
  Capacity:      1Gi
  Access Modes:  [ReadWriteOnce]
Events:
  Type    Reason            Age   From                    Message
  ----    ------            ----  ----                    -------
  Normal  SuccessfulCreate  27m   statefulset-controller  create Claim www-nginx-statefulset-0 Pod nginx-statefulset-0 in StatefulSet nginx-statefulset success
  Normal  SuccessfulCreate  27m   statefulset-controller  create Pod nginx-statefulset-0 in StatefulSet nginx-statefulset successful
  Normal  SuccessfulCreate  27m   statefulset-controller  create Claim www-nginx-statefulset-1 Pod nginx-statefulset-1 in StatefulSet nginx-statefulset success
  Normal  SuccessfulCreate  27m   statefulset-controller  create Pod nginx-statefulset-1 in StatefulSet nginx-statefulset successful
  Normal  SuccessfulCreate  27m   statefulset-controller  create Claim www-nginx-statefulset-2 Pod nginx-statefulset-2 in StatefulSet nginx-statefulset success
  Normal  SuccessfulCreate  27m   statefulset-controller  create Pod nginx-statefulset-2 in StatefulSet nginx-statefulset successful
```

```bash
$ kubectl get pods -o wide
NAME                  READY   STATUS    RESTARTS   AGE   IP           NODE                     NOMINATED NODE   READINESS GATES
nginx-statefulset-0   1/1     Running   0          28m   10.42.1.26   k3d-mycluster-agent-0    <none>           <none>
nginx-statefulset-1   1/1     Running   0          28m   10.42.0.27   k3d-mycluster-server-0   <none>           <none>
nginx-statefulset-2   1/1     Running   0          28m   10.42.0.29   k3d-mycluster-server-0   <none>           <none>
```

PersistentVolumes mounted to the Pods of a StatefulSet are not deleted when the StatefulSet's Pods are deleted.

```bash
$ kubectl get pvc -o wide
NAME                      STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE   VOLUMEMODE
www-nginx-statefulset-0   Bound    pvc-c42898e9-640a-4358-a18b-a173f9de74da   1Gi        RWO            local-path     29m   Filesystem
www-nginx-statefulset-1   Bound    pvc-a8b8631f-39d1-464c-8896-2c18a8524d02   1Gi        RWO            local-path     29m   Filesystem
www-nginx-statefulset-2   Bound    pvc-8403d052-da13-4df2-876c-9c0ae09b3142   1Gi        RWO            local-path     29m   Filesystem
```

```bash
$ kubectl get pv -o wide
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                             STORAGECLASS   REASON   AGE   VOLUMEMODE
pvc-c42898e9-640a-4358-a18b-a173f9de74da   1Gi        RWO            Delete           Bound    default/www-nginx-statefulset-0   local-path              29m   Filesystem
pvc-a8b8631f-39d1-464c-8896-2c18a8524d02   1Gi        RWO            Delete           Bound    default/www-nginx-statefulset-1   local-path              29m   Filesystem
pvc-8403d052-da13-4df2-876c-9c0ae09b3142   1Gi        RWO            Delete           Bound    default/www-nginx-statefulset-2   local-path              29m   Filesystem
```

### Updating StatefulSets

Handled by `spec.updateStrategy`, default is `RollingUpdate` 
Can be used to update
- update the container images of the Pods in a StatefulSet
- resource requests and/or limits of the Pods in a StatefulSet
- labels of the Pods in a StatefulSet
- annotations of the Pods in a StatefulSet

The `RollingUpdate` will update all Pods in a StatefulSet, in reverse ordinal order while respecting the StatefulSet guarantees
There is also `OnDelete` but is legacy and is not recommended.

### Pod Management Policy

`.spec.podManagementPolicy` value to `OrderedReady` or `Parallel`

`OrderedReady` - is the default for StatefulSets. It tells the StatefulSet controller to respect the ordering guarantees demonstrated above.
`Parallel` - tells the StatefulSet controller to launch or terminate all Pods in parallel, and not to wait for Pods to become Running and Ready


Resources:
1. [https://kubernetes.io/docs/tutorials/stateful-application/basic-stateful-set/](https://kubernetes.io/docs/tutorials/stateful-application/basic-stateful-set/)
2. [https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/)
