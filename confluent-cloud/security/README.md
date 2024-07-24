### Confluent Cloud Security

https://developer.confluent.io/courses/cloud-security/
https://www.confluent.io/confluent-cloud/security/
https://docs.confluent.io/cloud/current/overview.html
https://www.confluent.io/blog/

### Authenticate Users

- username / password
- SSO (single sign on) - Okta, Google, enable consistent policy layer for MFA, password enforcement, user termination

### Authenticate Apps

- Api Keys
- OAuth access tokens

### API Keys

two types:
- `Resource specific keys` 
  - give access to individual resources in your environment like a specific kafka cluster, schema registry, ksqldb etc
- `Cloud API keys`
  - give you manage the entire confluent cloud organisation's environments or single environment depending how you configure it with RBAC

- there is an `api key` and `api secret`
- API keys can be created for `user` and `service accounts`
- create a policy to always use service account api keys in production
- manage and delete unneeded keys ans service accounts
- api keys can be created and destroyed without affecting the service account ACLs or RBAC role bindings
- rotate the API keys in this order:
  - create a new API key
  - update the resources and apps to use your newly created API key
  - verify the connection via your audit logs
  - delete the old API key
- Use `audit logs` to track the usage of API keys 


### OAuth

- API keys allow you to control access on a per-application and service basis, but you might run out of API keys
- with OAuth you use access tokens to authenticate applications or services
- OAuth allows to group your identities into `identity pools` and manage access at the pool level.
- API keys are specific for Confluent Cloud, OAuth allows you to manage authentication accross your entire organisation

### Authorization

#### ACL
Principal,     Permission, Operation, Resource Type, Resource Name, Pattern Type
User:u-ljyvz6   ALLOW       READ        TOPIC           CLients       Literal  

- the permission is tied to the identity
- if the number of identities increases this could be hard to manage
- ACLs are specific to Kafka resources, don't extend to Confluent Cloud concepts like `environments` or `organisations`

#### RBAC

- Identities are assigned to a role
Pre-defined roles: https://docs.confluent.io/cloud/current/security/access-control/rbac/predefined-rbac-roles.html

#### Administration roles
AccountAdmin
BillingAdmin
CloudClusterAdmin
DataDiscovery
DataSteward
EnvironmentAdmin
FlinkAdmin
KsqlAdmin
NetworkAdmin
OrganizationAdmin
ResourceKeyAdmin
ResourceOwner

### Application development roles
DeveloperManage
DeveloperRead
DeveloperWrite
FlinkDeveloper
MetricsViewer
Operator
ResourceOwner

- RBAC for Kafka is available only on `Standard` and `Dedicated` clusters
- To create service accounts, you must be granted the `OrganizationAdmin` role.
- All the other cluster administration roles (EnvironmentAdmin, CloudClusterAdmin, and ResourceOwner) can grant or revoke permissions for existing service accounts on the resources that they control.

- If you use OAuth for authentication you will be creating identity pools for your principals.
- There are two parts to every identity pool:
  - who can use the pool: - the "who" is a set of conditions that the identity needs to satisfy in order to use the pool
  - what the pool can access: - the “what” is defined by ACLs and RBAC roles.

- You can use a mix of ACLs and RBACs, 
- order of precedence: https://docs.confluent.io/cloud/current/security/access-control/rbac/use-acls-with-rbac.html

### Encryption

- `data in motion` - TLS 1.2
- `data at rest` - by default

#### BYOK - bring your own key, also supported

Benefits:
- It allows for encryption at the storage layer and lets a user revoke access to the key at will
- It helps secure data in a scenario where a server’s drive might be subpoenaed
- It checks a box for security compliance.

Limitations
- It will still allow access to the data by the Confluent and cloud platform provider and site reliability engineers (SREs) and engineering teams as long as access to the key is granted.
- It’s only after the key authorization is revoked that access is lost. If you need to ensure that neither Confluent nor the cloud provider has access to your data, you’ll need to use `end-to-end encryption`.
- With BYOK, key management relies on the customer
- Losing access to your self-managed key means the data is essentially gone too.
- BYOK can only be implemented when a dedicated cluster is created, and not available on basic or standard cluster

### Connect log events

Connect Log Events provide log information from the operation of connectors on Confluent Cloud. As of now, the log events are limited to `Connector` and `Task failure` events, capturing the reasons of these failures

```bash
$ confluent connect event describe

+-----------------+-------------------------------+
| Cluster         | lkc-y3wj2k                   |
| Environment     | env-vn2zxp                   |
| Service Account | sa-8p0j2m                    |
| Topic Name      | confluent-connect-log-events |
+-----------------+-------------------------------+
```

This is a different environment / cluster

```bash
$ confluent environment use env-vn2zxp 
$ confluent kafka cluster use lkc-y3wj2k
```

List the api-keys for the audit log cluster

```bash
$ confluent api-key list --resource lkc-y3wj2k
None found
```

Create an API key and secret. This is required to consume from the connect log events topic.

```bash
$ confluent api-key create --service-account sa-8p0j2m --resource lkc-y3wj2k

+------------+------------------------------------------------------------------+
| API Key    | LMOLWTICZWLS7NXV                                                 |
| API Secret | exuXMoSR9zNXQXzBkGG2YsYFwQlOwSz1alNemnDClxIlD0VF/I7Nhr6nlXB/k8bt |
+------------+------------------------------------------------------------------+
```

```bash
$ confluent api-key list --resource lkc-y3wj2k

  Current |       Key        | Description |   Owner   |        Owner Email         | Resource Type |  Resource  |       Created
----------+------------------+-------------+-----------+----------------------------+---------------+------------+-----------------------
  *       | LMOLWTICZWLS7NXV |             | sa-8p0j2m | <auditlog service account> | kafka         | lkc-y3wj2k | 2024-07-23T09:20:22Z
```

```bash
$ confluent kafka topic consume confluent-connect-log-events
```

### Audit log events

```bash
$ confluent audit-log describe

+-----------------+-------------------------------+
| Cluster         | lkc-y3wj2k                    |
| Environment     | env-vn2zxp                    |
| Service Account | sa-8p0j2m                     |
| Topic Name      | confluent-audit-log-events    |
+-----------------+-------------------------------+
```

```bash
$ confluent kafka topic consume confluent-audit-log-events
```

- retained for 7 days on a different cluster in a different organisation
  - can be replicated to another cluster
  - use SIEM (Security Information and Event Management) tool for management and alerts  
- uses cloudevents for format
- does not contain the content fo the event, only that the event has happened

#### Advantages:

- Provide Insight
- Improve Security
- Help you understand Impact
- Compliance

#### Types

- Authentication Events
- Authorization Events
- Organisation Events

#### Security 


https://confluent.safebase.us/