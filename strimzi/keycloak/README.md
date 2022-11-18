### Create a 3 node Kubernetes cluster:

```bash
$ k3d cluster create my-k8s-cluster --agents 3
# view our k8s cluster 
$ k3d cluster list
# kubectl is automatically will be set to the context
$ kubectl cluster-info
# verify that we have 1 agent nodes and 1 server node
$ kubectl get nodes -o wide
# check with docker that the nodes are running in a docker container
$ docker ps
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-my-k8s-cluster-server-0 key1=value1:NoSchedule
```

Following guide from here: [https://operatorhub.io/operator/keycloak-operator](https://operatorhub.io/operator/keycloak-operator)

1. Install Operator Lifecycle Manager (OLM), a tool to help manage the Operators running on your cluster.
```bash
$ curl -sL https://github.com/operator-framework/operator-lifecycle-manager/releases/download/v0.22.0/install.sh | bash -s v0.22.0

customresourcedefinition.apiextensions.k8s.io/catalogsources.operators.coreos.com created
customresourcedefinition.apiextensions.k8s.io/clusterserviceversions.operators.coreos.com created
customresourcedefinition.apiextensions.k8s.io/installplans.operators.coreos.com created
customresourcedefinition.apiextensions.k8s.io/olmconfigs.operators.coreos.com created
customresourcedefinition.apiextensions.k8s.io/operatorconditions.operators.coreos.com created
customresourcedefinition.apiextensions.k8s.io/operatorgroups.operators.coreos.com created
customresourcedefinition.apiextensions.k8s.io/operators.operators.coreos.com created
customresourcedefinition.apiextensions.k8s.io/subscriptions.operators.coreos.com created
customresourcedefinition.apiextensions.k8s.io/catalogsources.operators.coreos.com condition met
customresourcedefinition.apiextensions.k8s.io/clusterserviceversions.operators.coreos.com condition met
customresourcedefinition.apiextensions.k8s.io/installplans.operators.coreos.com condition met
customresourcedefinition.apiextensions.k8s.io/olmconfigs.operators.coreos.com condition met
customresourcedefinition.apiextensions.k8s.io/operatorconditions.operators.coreos.com condition met
customresourcedefinition.apiextensions.k8s.io/operatorgroups.operators.coreos.com condition met
customresourcedefinition.apiextensions.k8s.io/operators.operators.coreos.com condition met
customresourcedefinition.apiextensions.k8s.io/subscriptions.operators.coreos.com condition met
namespace/olm created
namespace/operators created
serviceaccount/olm-operator-serviceaccount created
clusterrole.rbac.authorization.k8s.io/system:controller:operator-lifecycle-manager created
clusterrolebinding.rbac.authorization.k8s.io/olm-operator-binding-olm created
olmconfig.operators.coreos.com/cluster created
deployment.apps/olm-operator created
deployment.apps/catalog-operator created
clusterrole.rbac.authorization.k8s.io/aggregate-olm-edit created
clusterrole.rbac.authorization.k8s.io/aggregate-olm-view created
operatorgroup.operators.coreos.com/global-operators created
operatorgroup.operators.coreos.com/olm-operators created
clusterserviceversion.operators.coreos.com/packageserver created
catalogsource.operators.coreos.com/operatorhubio-catalog created
Waiting for deployment "olm-operator" rollout to finish: 0 of 1 updated replicas are available...
deployment "olm-operator" successfully rolled out
deployment "catalog-operator" successfully rolled out
Package server phase: InstallReady
Package server phase: Succeeded
deployment "packageserver" successfully rolled out
```

You can verify that the OLM components have been successfully deployed by running

```bash
$ kubectl -n olm get deployments
catalog-operator   1/1     1            1           5m24s
olm-operator       1/1     1            1           5m24s
packageserver      2/2     2            2           5m4s
```

2. Install the operator by running the following command:

```bash
$ kubectl create -f https://operatorhub.io/install/keycloak-operator.yaml
namespace/my-keycloak-operator created
operatorgroup.operators.coreos.com/operatorgroup created
subscription.operators.coreos.com/my-keycloak-operator created
```

It created a job and the `keycloak-operator` is installed in the `my-keycloak-operator` namespace and is usable from this namespace only.

```bash
olm                    981a1d7b751c9fa9af2e2a2fc3c5387b4b205cd5ce796cf772ce1e8fa2bltsr   0/1     Completed   0          60s
my-keycloak-operator   keycloak-operator-64db49949b-98rdn                                1/1     Running     0          36s
```

```bash
$ kubectl get csv -n my-keycloak-operator
NAME                        DISPLAY             VERSION   REPLACES                    PHASE
keycloak-operator.v20.0.1   Keycloak Operator   20.0.1    keycloak-operator.v20.0.0   Succeeded
```

```bash
$ kubeclt get crd | grep keycloak
keycloakrealmimports.k8s.keycloak.org         2022-11-18T20:25:58Z
keycloaks.k8s.keycloak.org                    2022-11-18T20:25:58Z
```

```bash
$ kubectl api-resources | grep keycloak
keycloakrealmimports                           k8s.keycloak.org/v2alpha1              true         KeycloakRealmImport
keycloaks                         kc           k8s.keycloak.org/v2alpha1              true         Keycloak
```

------------------------------------------------------------------------------------------------------------------------
### Basic Keycloak Deployment

```bash
$ kubectl apply -f postgres.yaml
```

For development purposes we will use from now on `test.keycloak.org`.
For development purposes you can use this command to obtain a self-signed certificate:

```bash
$ openssl req -subj '/CN=test.keycloak.org/O=Test Keycloak./C=US' -newkey rsa:2048 -nodes -keyout key.pem -x509 -days 365 -out certificate.pem
```
and we install it in the cluster namespace as a Secret by running:

```bash
$ kubectl create secret tls example-tls-secret --cert certificate.pem --key key.pem -n my-keycloak-operator
```

```bash
$ kubectl create secret generic keycloak-db-secret \
--from-literal=username=postgres \
--from-literal=password=testpassword \
-n my-keycloak-operator -o yaml --dry-run=client > keycloak-db-secret.yaml
```

```bash
$ kubectl apply -f keycloak-db-secret.yaml
```

```bash
$ kubectl apply -f keycloak.yaml
```

```bash
$ kubectl get pods -n my-keycloak-operator
NAME                                 READY   STATUS    RESTARTS   AGE
keycloak-operator-64db49949b-98rdn   1/1     Running   0          48m
postgresql-db-0                      1/1     Running   0          19m
my-keycloak-0                        1/1     Running   0          60s
```

```bash
$ kubectl get keycloaks/my-keycloak -n my-keycloak-operator -o go-template='{{range .status.conditions}}CONDITION: {{.type}}{{"\n"}}  STATUS: {{.status}}{{"\n"}}  MESSAGE: {{.message}}{{"\n"}}{{end}}'

CONDITION: Ready
  STATUS: true
  MESSAGE:
CONDITION: HasErrors
  STATUS: false
  MESSAGE:
CONDITION: RollingUpdate
  STATUS: false
  MESSAGE:
```

```bash
$ kubectl get svc -n my-keycloak-operator
NAME                    TYPE           CLUSTER-IP     EXTERNAL-IP                                   PORT(S)          AGE
postgres-db             LoadBalancer   10.43.240.89   172.25.0.2,172.25.0.3,172.25.0.4,172.25.0.5   5432:31428/TCP   25m
my-keycloak-service     ClusterIP      10.43.237.29   <none>                                        8443/TCP         6m17s
my-keycloak-discovery   ClusterIP      None           <none>                                        7800/TCP         6m17s
```

```bash
$ kubectl port-forward svc/my-keycloak-service 8443:8443 -n my-keycloak-operator
```

https://localhost:8443/admin/master/console/

"Loading the admin console" spinning

trying to load the resource: https://test.keycloak.org/realms/master/protocol/openid-connect/3p-cookies/step1.html



```bash
$ kubectl get secret my-keycloak-initial-admin -n my-keycloak-operator -o jsonpath='{.data.username}' | base64 --decode
admin

$ kubectl get secret my-keycloak-initial-admin -n my-keycloak-operator -o jsonpath='{.data.password}' | base64 --decode
85e23d0c56ea4ac1b143dbafb04164f5
```


------------------------------------------------------------------------------------------------------------------------
Note, that this repo is not supported anymore https://github.com/keycloak/keycloak-operator (read-only mode)
New Docs: --> https://www.keycloak.org/guides#operator
New Operator sources: --> https://github.com/keycloak/keycloak/tree/main/operator


