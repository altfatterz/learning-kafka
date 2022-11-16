```bash
$ minikube start --driver=hyperkit --container-runtime=docker --memory 8192 --cpus 4 --no-kubernetes
$ minikube node list
$ minikube status
$ eval $(minikube docker-env)
$ k3d cluster create mycluster --agents 3
$ k3d cluster list
$ kubectl get nodes -o wide
$ docker ps
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule 
```

### Deployment

## Without Volume

```bash
$ kubectl apply -f deployment-demo-without-volume.yaml
```

```bash
$ kubectl get deploy
$ kubectl get rs
$ kubectl get svc
$ kubectl get ep
$ kubectl get pods -o wide
```

Check that the pod is provisioned in different nodes.

```bash
$ kubectl delete -f deployment-demo-without-volume.yaml
```

```bash
$ kubectl run -i --tty --image busybox:1.28 dns-test --restart=Never --rm
```

```bash
$ nslookup nginx-deploy

Server:    10.43.0.10
Address 1: 10.43.0.10 kube-dns.kube-system.svc.cluster.local

Name:      nginx-deploy
Address 1: 10.43.199.124 nginx-deploy.default.svc.cluster.local
```

# With Volume

```bash
$ kubectl apply -f deployment-demo-with-volume.yaml
```

Only one PVC / PV is provisioned even if we scale the deployment

```bash
$ kubectl scale deploy nginx-deployment --replicas=3
```

```bash
$ kubectl get pvc
$ kubectl get pv
```

Because we configured the PVC access mode to `ReadWriteOnce` notice that the pods are scheduled on the same node.

If you scale down the deployment the PVC is kept. (hence the PV is kept as well)

But if you remove the deployment the PVC is deleted. The PV is deleted based on RECLAIM_POLICY (which id Delete by default)

# StatefulSet

- For each new replica, the stateful set will create a separate volume.
- First thing you notice is that the pods don't have autogenerated IDs in their names
- To ensure a stable network ID, you need to define a headless service (does not have a cluster IP) for stateful applications.
- Benefit of headless service is to reach each pod directly (check nslookup)
- If the service is not headless then the service would act as a load balancer
- note that a `ReplicaSet` is not created
- the pods are starting one after another

StatefulSet Pods have a unique identity that consists of:
- an ordinal index (from 0 up through N-1)
- a stable network identity (headless service) - $(statefulset name)-$(ordinal)
- stable storage. 

- The identity sticks to the Pod, regardless of which node it's (re)scheduled on.

```bash
$ kubectl run -i --tty --image busybox:1.28 dns-test --restart=Never --rm
$ nslookup nginx-statefulset-0.headless-nginx
Server:    10.43.0.10
Address 1: 10.43.0.10 kube-dns.kube-system.svc.cluster.local

Name:      nginx-statefulset-0.headless-nginx
Address 1: 10.42.1.26 nginx-statefulset-0.headless-nginx.default.svc.cluster.local
```

```bash
$ nslookup headless-nginx
Server:    10.43.0.10
Address 1: 10.43.0.10 kube-dns.kube-system.svc.cluster.local

Name:      headless-nginx
Address 1: 10.42.0.27 nginx-statefulset-1.headless-nginx.default.svc.cluster.local
Address 2: 10.42.0.29 nginx-statefulset-2.headless-nginx.default.svc.cluster.local
Address 3: 10.42.1.26 nginx-statefulset-0.headless-nginx.default.svc.cluster.local
```

### Scaling a StatefulSet

```bash
$ kubectl scale sts nginx-statefulset --replicas=5
```
or 
```bash
$ kubectl patch sts web -p '{"spec":{"replicas":5}}'
```

As with StatefulSet creation, the StatefulSet controller created each Pod sequentially with respect to its ordinal index, 
and it waited for each Pod's predecessor to be Running and Ready before launching the subsequent Pod.

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
nginx-statefulset-1   1/1     Running   0          28m   10.42.0.27   k3d-mycluster-agent-1    <none>           <none>
nginx-statefulset-2   1/1     Running   0          28m   10.42.0.29   k3d-mycluster-agent-2    <none>           <none>
```

### PVCs and PVs

A `PersistentVolume` (PV) is a piece of storage in the cluster that either 
- has been manually provisioned by an administrator
- or dynamically provisioned by Kubernetes using a StorageClass.

Many cluster environments have a default StorageClass installed. 
When a StorageClass is not specified in the PersistentVolumeClaim, the cluster's default StorageClass is used instead.

A `PersistentVolumeClaim` (PVC) is a request for storage by a user that can be fulfilled by a PV.

These objects are independent of Pod lifecycles and preserve data through restarting, rescheduling, and even deleting Pods.


```bash
$ kubectl get pvc -o wide
NAME                      STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE   VOLUMEMODE
www-nginx-statefulset-0   Bound    pvc-c42898e9-640a-4358-a18b-a173f9de74da   1Gi        RWO            local-path     29m   Filesystem
www-nginx-statefulset-1   Bound    pvc-a8b8631f-39d1-464c-8896-2c18a8524d02   1Gi        RWO            local-path     29m   Filesystem
www-nginx-statefulset-2   Bound    pvc-8403d052-da13-4df2-876c-9c0ae09b3142   1Gi        RWO            local-path     29m   Filesystem
```

Deleting or scaling a StatefulSet down does not delete the volumes associated with the StatefulSet. This setting is for your safety because your data is more valuable than automatically purging all related StatefulSet resources.

However, depending on the storage class and reclaim policy, deleting the PersistentVolumeClaims may cause the associated volumes to also be deleted.
Never assume you'll be able to access data if its volume claims are deleted.

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