### Confluent Cloud Security

https://developer.confluent.io/courses/cloud-security/ 


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