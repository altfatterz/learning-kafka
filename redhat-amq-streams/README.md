### RedHat AMQ Streams

# 2.0
- AMQ Streams version 2.0 is based on [Strimzi 0.26.x](https://www.youtube.com/watch?v=886Nw_ECREQ)
- The AMQ Streams 2.0.1 patch release is now available.
- AMQ Streams 2.0 is supported on OpenShift Container Platform 4.6 to 4.9
- AMQ Streams now supports Apache Kafka version 3.0.0.
- Strimzi 0.26.x
  - Apache Kafka 2.8.1, 3.0.0
    - Kafka Connect Build - `maven` type connector
    - JMX can be now exposed in Zookeeper (`jmxOptions` field)
    - Secure connection between Cruise Control and Cluster Operator
    - Anomaly detection feature can be enabled in Cruise Control configuration

# 2.1
- AMQ Streams version 2.1 is based on [Strimzi 0.28.x](https://www.youtube.com/watch?v=PZKbrDUU1zo)
- AMQ Streams 2.1 is supported on OpenShift Container Platform 4.6 to 4.10.
- AMQ Streams now supports Apache Kafka version 3.1.0.
- You must upgrade the Cluster Operator to AMQ Streams version 2.1 before you can upgrade brokers and client applications to Kafka 3.1.0.
- Strimzi 0.28.x
  - introduces `StrimziPodSets` to use instead of `StatefulSets`
  - allow new features, like stretched kafka cluster across kubernetes clusters, different configurations for each broker
  - introduces custom authentication beside (mTLS, SCRAM-SHA-512, OAuth Authentication)
  - `KafkaRebalance` - can be used to balance the data between different disks of the same broker 

# 2.2
- AMQ Streams 2.2 on OpenShift is based on Kafka 3.2.3 and [Strimzi 0.29.x](https://www.youtube.com/watch?v=lUsIoFTZr00).
- AMQ Streams 2.2 is supported on OpenShift Container Platform 4.6 to 4.11.
- Strimzi 0.29.x
  - `KafkaRebalance` modes:
    - `full` - default which rebalances the whole cluster (used in previous releases by default)
    - `add-brokers` - adds new brokers to a cluster after scale-up (faster than rebalancing the whole cluster)
    - `remove-brokers` - removes brokers from the cluster before scale-down (moves all topics from the removed nodes to the rest of the Kafka cluster)


### Local setup

OpenShift Red Hat Local (former CodeReady Containers) [https://developers.redhat.com/articles/2022/05/12/developer-tools-rebrand-say-farewell-codeready-name](
https://developers.redhat.com/articles/2022/05/12/developer-tools-rebrand-say-farewell-codeready-name)

Install from [https://console.redhat.com/openshift](https://console.redhat.com/openshift)

```bash
crc version
CRC version: 2.10.1+7e7f6b2d
OpenShift version: 4.11.7
Podman version: 4.2.0
```

```bash
$ crc config set memory 20480
$ crc config set cpus 4
$ crc config set enable-cluster-monitoring true 
```

```bash
$ crc setup
```

```bash
$ crc start

Started the OpenShift cluster.

The server is accessible via web console at:
  https://console-openshift-console.apps-crc.testing

Log in as administrator:
  Username: kubeadmin
  Password: zS7fT-MKsVU-LCbCK-gnFRW

Log in as user:
  Username: developer
  Password: developer

Use the 'oc' command line interface:
$ eval $(crc oc-env)
$ oc login -u developer https://api.crc.testing:6443
```

```bash
$ crc console --credentials
To login as a regular user, run 'oc login -u developer -p developer https://api.crc.testing:6443'.
To login as an admin, run 'oc login -u kubeadmin -p zS7fT-MKsVU-LCbCK-gnFRW https://api.crc.testing:6443'
```

Useful commands:
```bash
$ oc whomai
$ oc config get-contexts
```

Access the console:

```bash
crc console
Opening the OpenShift Web Console in the default browser...
```

Install the `Red Hat Integration AMQ Streams operator` operator and the `Apicurio Registry Operator`



```bash
$ oc get pods -n openshift-operators
NAME                                                     READY   STATUS    RESTARTS   AGE
amq-streams-cluster-operator-v2.2.0-2-785bc7f5fc-gdmf4   1/1     Running   0          2m45s
apicurio-registry-operator-65b74cc7f8-gzlpr              1/1     Running   1          3m38s
```

Create a Kafka cluster:

```bash
$ oc new-project kafka
$ oc apply -f kafka.yaml
```

```bash
$ oc get pods -n demo
my-cluster-entity-operator-85878859c-st8sm   3/3     Running   0          104s
my-cluster-kafka-0                           1/1     Running   0          2m34s
my-cluster-kafka-1                           1/1     Running   0          2m34s
my-cluster-kafka-2                           1/1     Running   0          2m33s
my-cluster-zookeeper-0                       1/1     Running   0          4m10s
my-cluster-zookeeper-1                       1/1     Running   0          4m10s
my-cluster-zookeeper-2                       1/1     Running   0          4m10s
```

### Encryption

Extracting bootstrap information:

```bash
$ export KAFKA_BOOTSTRAP=`oc get routes my-cluster-kafka-external-bootstrap -o=jsonpath='{.status.ingress[0].host}{"\n"}'`
```

Check the served certificate

```bash
$ openssl s_client -connect $KAFKA_BOOTSTRAP:443
$ openssl s_client -connect $KAFKA_BOOTSTRAP:443 -servername $KAFKA_BOOTSTRAP
```

Extracting the public certificate:

```bash
$ oc get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt
```

# List topics with `kcat`

```bash
$ kcat -L -b $KAFKA_BOOTSTRAP:443 -X security.protocol=SSL -X ssl.ca.location=ca.crt
```

# Try with kafka-console-producer / kafka-console-consumer

Extract the `ca.p12` from the Cluster CA secret

```bash
$ oc get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.p12}' | base64 -d > ca.p12
$ oc get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.password}' | base64 -d > ca.password
```

List the certificates in the CA PKCS12 keystore
```bash
$ keytool -list -v -keystore ca.p12 -storepass `cat ca.password`
```

Producer:

```bash
$ kafka-console-producer --bootstrap-server $KAFKA_BOOTSTRAP:443 \
--producer-property security.protocol=SSL \
--producer-property ssl.truststore.password=`cat ca.password` \
--producer-property ssl.truststore.location=./ca.p12 \
--topic my-topic
```

Consumer:

```bash
$ kafka-console-consumer --bootstrap-server $KAFKA_BOOTSTRAP:443 \
--consumer-property security.protocol=SSL \
--consumer-property ssl.truststore.location=./ca.p12 \
--consumer-property ssl.truststore.password=`cat ca.password` \
--topic my-topic \
--from-beginning
```

# Install Apicurio Registry with the Apicurio Registry Operator

Create an instance
```bash
$ oc apply -f apicurio-registry.yaml
```

Expose the UI
```bash
$ oc port-forward svc/apicurio-registry-service 8080:8080
```

```bash
$ crc status
CRC VM:          Running
OpenShift:       Running (v4.11.7)
RAM Usage:       11.61GB of 21.03GB
Disk Usage:      16.43GB of 32.74GB (Inside the CRC VM)
Cache Usage:     64.46GB
Cache Directory: /Users/altfatterz/.crc/cache
```

Cleanup
```bash
$ crc delete
```


Resources:

1. OpenShift Local or Single Node OpenShift: [https://www.opensourcerers.org/2022/09/13/openshift-local-or-single-node-openshift/](https://www.opensourcerers.org/2022/09/13/openshift-local-or-single-node-openshift/)
2. Openshift Local (formerly Red Hat CodeReady Containers) [https://developers.redhat.com/products/openshift-local/overview](https://developers.redhat.com/products/openshift-local/overview)
3. Getting Started with AMQ Streams on OpenShift [https://access.redhat.com/documentation/en-us/red_hat_amq_streams/2.2/html/getting_started_with_amq_streams_on_openshift/index](https://access.redhat.com/documentation/en-us/red_hat_amq_streams/2.2/html/getting_started_with_amq_streams_on_openshift/index)