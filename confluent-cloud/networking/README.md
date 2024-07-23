
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
  - if we add more VPC networks, peering connections grow exponentially
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

```bash
$ confluent network peering list
```

```bash
$ sudo apt update
$ sudo apt install nginx
```

### AWS Transit Gateway

- Connect the Confluent Cloud network with AWS Transit Gateway
- Connect AWS VPC-s with the AWS Transit Gateway
- Connect on premise networks with the AWS Transit Gateway

- AWS Transit Gateway - is a network transit hub that interconnects attachments (VPCs and VPNs) within the same AWS account or across AWS accounts.
- 

### Private Link

- this is unidirectional connection (from your cluster to confluent cloud)
- we cannot use fully managed connectors
- Private DNS Resolution - setting
  If checked, your cluster's endpoints will resolve solely through your private DNS zone. If disabled, you will require public DNS resolution.
- Only requires 3 IP addresses in your VPC or VNet
- On the Confluent Cloud side the is a customer layer of routing and proxies that is able to use the TLS SNI (Server Name Indication extension) on each packet to forward messages to the right broker.
- On your network you are required to setup wildcard DNS records to forward all traffic to the correct Private Link endpoint that exists
in your VPC (for AWS this is CNAME (translate domain name to domain name) record, for Azure is an A (translate domain name to ip address) record)
- In Route53 or Azure DNS you will need to create a custom private DNS zone.
- You need to setup a number of wildcard dns records to resolve all broker DNS names to the Private Link endpoint IPs
- If you provision a cluster like this you will get:

`Connection types`: PrivateLink
`Zone`:	euw1-az1, euw1-az3, euw1-az2
`Confluent Cloud VPC ID`: vpc-0f45eb5c84964f3f2
`Confluent Cloud AWS Account ID`:390327825978
`VPC Endpoint service name`: com.amazonaws.vpce.eu-west-1.vpce-svc-06bc29ea8ba3f64a2
`DNS resolution`:	Private
`DNS domain`: dom1w4jrl6w.eu-west-1.aws.confluent.cloud
`DNS subdomain`: euw1-az1: euw1-az1.dom1w4jrl6w.eu-west-1.aws.confluent.cloud
                 euw1-az2: euw1-az2.dom1w4jrl6w.eu-west-1.aws.confluent.cloud
                 euw1-az3: euw1-az3.dom1w4jrl6w.eu-west-1.aws.confluent.cloud

Then you add a `PrivateLink Access` providing:
- AWS account number
- and a name

You need to wait until the PrivateLink Access status is `Ready` (initiall is in `Provisioning` state)  

In AWS you will need to create an `Endpoint` using the `VPC Endpoint service name` using the `PrivateLink Ready partner service`

`Service Name`: com.amazonaws.vpce.eu-west-1.vpce-svc-06bc29ea8ba3f64a2
`DNS Names`:
vpce-0dcb6593dd7d29ac1-rabj5mnu.vpce-svc-06bc29ea8ba3f64a2.eu-west-1.vpce.amazonaws.com
vpce-0dcb6593dd7d29ac1-rabj5mnu-eu-west-1a.vpce-svc-06bc29ea8ba3f64a2.eu-west-1.vpce.amazonaws.com
vpce-0dcb6593dd7d29ac1-rabj5mnu-eu-west-1b.vpce-svc-06bc29ea8ba3f64a2.eu-west-1.vpce.amazonaws.com
vpce-0dcb6593dd7d29ac1-rabj5mnu-eu-west-1c.vpce-svc-06bc29ea8ba3f64a2.eu-west-1.vpce.amazonaws.com

`Subnets`:
subnet-0add76291d0d87641 (Simple Public Subnet 0) eu-west-1a (euw1-az3) 10.0.1.238
subnet-0b453c7911623ecc6 (Simple Public Subnet 2) eu-west-1c (euw1-az2) 10.0.3.138
subnet-05bf6759290c2f75d (Simple Public Subnet 1) eu-west-1b (euw1-az1) 10.0.2.236

In `Route53` you will need to create a `Private Hosted Zone` with 4 CNAME records




