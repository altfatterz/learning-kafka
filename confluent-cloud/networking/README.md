
### Confluent Cloud Networking

https://developer.confluent.io/courses/confluent-cloud-networking

### What connects to Confluent Cloud?

#### Confluent Cloud Data Plane
 
- Confluent Network
  - over TCP 
  - fully managed connectors
  - fully managed ksqlDB apps
  - confluent cloud cluster

- Confluent Schema Registry
  - over HTTPS

#### Confluent Cloud Control Plane (Internet)

- Confluent Cloud UI
- Confluent Management API
- Confluent Metrics API
- Confluent Audit Logs

### Confluent Cloud Connectivity Options

1. Secured Public Endpoints
   - public ip address accessed over the internet, everything encrypted in transit with TLS 1.2
   - is the default and is easier to use
2. VPC / VNet Peering
   - directly connect the Confluent Network with your network
   - traffic will never travers over the public internet and will use the cloud providers backbone
   - common on all cloud providers, but come with some drawbacks
3. AWS Transit Gateway
   - extension of peering where the Confluent Network is peered to one of your transit gateways
   - this effectly acts a Cloud router, you can connect multiple VPCs with a single Transit Gateway
4. Private Link
   - one way communication, only allows connections to be initiated from customer VPC / VNet toward Confluent Cloud

### Secured Public Endpoints

- the only option for basic and standard clusters
- is secure enough for most usecases
- consider choosing it unless you have a specific InfoSec or compliance requirement
- data is encrypted in transit
- authentication with api key and api secret 
- once you are authenticated then authorization with ACLs or RBAC
- clients can be anywhere only they need to be able to access internet with a proxy
- kafka uses a TCP wire protocol, not HTTP or HTTPS, access through an HTTPS proxy is not supported
- fully managed connectors access data in your cluster, so your data needs to be accessible over the internet
- if the data is store in a cloud provider (AWS, Snowflake) then data can be exposed to the internet
- when data cannot be exposed over the internet you can use a self managed connector
- self managed connector: you run a connect cluster on your environment which pushes (source) or producers (sink) to or from Confluent Cloud cluster 

### VPC / VNet Peering

##### Create a network of type `VPC Peering` (other types: `Private Link` or `Transit Gateway`)
  - Provide the CIDR range
  - The CIDR block must be in one of the following private networks:
    - Private IP Range (RFC 1918)
      - 10.0.0.0/8
      - 172.16.0.0/12 
      - 192.168.0.0/16 
    - Carrier Grade NAT Range (RFC 6598)
      - 100.64.0.0/10
    - Benchmark Address Range (RFC 2544)
      - 198.18.0.0/15
    - Must be a /16 network
    - Disallowed ranges:
      - 10.100.0.0/16
      - 10.255.0.0/16
      - 172.17.0.0/16
      - 172.20.0.0/16
    - Must not overlap with any ranges in use by the customer. Peering can not be set up to any VPC that overlaps with Confluent Cloud CIDR.
    - Cannot overlap with an existing Confluent Cloud CIDR.
- DNS servers must allow upstream DNS resolution for confluent cloud domain
- Networks without clusters will be deleted after 60 days.
- Benefits: 
  - traffic is not on public internet, communication with private IPs
  - supports multiple Confluent Cloud clusters in a single network
  - managed connectors can access customer hosted data sources and sinks
- Drawbacks:
  - Cross-region peering setup is not supported through CC console. (reach out to Confluent Support team to set this up)
  - no transitive connectivity
- AWS VPC / Azure VNet peerings are regional
- Google VPCs are global and can span multiple regions - despite this Confluent Cloud on GCP only accepts connection from the same region
- 

##### Add a Peering Connection

Providing the followings:
- AWS account id
- AWS VPC id
- AWS VPC CIDR range

In AWS you need to accept the peering request (you have 7 days to accept the request before it expires.)

##### Add a Route

In the public route table:

Destination: 10.1.0.0/16 (Confluent Cloud Network)
Target: The Peering Connection

##### Next provision a Dedicated cluster 

#### Setup https://getfoxyproxy.org/ to access the cluster in Confluent Cloud Console 



### AWS Transit Gateway

### Private Link