
Useful examples:

```bash
$ ccloud login --save
...
Wrote credentials to netrc file "~/.netrc"
Using environment "t829" ("default").
```

```bash
$ ccloud environment list 
```

```bash
$ ccloud kafka cluster list
      Id      |         Name         | Type  | Provider |    Region    | Availability | Status
+-------------+----------------------+-------+----------+--------------+--------------+--------+
    lkc-3kj32 | kafka-cluster-on-gcp | BASIC | gcp      | europe-west6 | single-zone  | UP
```

Set the `ClUSTER_ID` environment variable 

```bash
export CLUSTER_ID=<cluster-id> 
```

```bash
$ ccloud kafka topic list --cluster $CLUSTER_ID
     Name
+------------+
  demo-topic
```

Create the API Key/Secret using http://confluent.cloud

Store an API key/secret locally to use in the CLI.

```bash
$ ccloud api-key store --resource $CLUSTER_ID
Key: MHW7UJ2KN6F53T5N

Secret: ****************************************************************
Stored API secret for API key "MHW7UJ2KN6F53T5N".
```
```bash
$ ccloud api-key list
         Key         | Description | Owner |     Owner Email      | Resource Type | Resource ID |       Created
+--------------------+-------------+-------+----------------------+---------------+-------------+----------------------+
    MHW7UJ2KN6F53T5N |             |  1160 | altfatterz@gmail.com |               | lkc-3kj32   | 2021-03-21T20:00:19Z
```

```bash
export KEY=<key>
```

Make an API key active for use in other commands.

```bash
$ ccloud api-key use $KEY --resource $CLUSTER_ID
Set API Key "MHW7UJ2KN6F53T5N" as the active API key for "lkc-3kj32".
```

Consumer:

```bash
$ ccloud kafka topic consume -b demo-topic --cluster $CLUSTER_ID
```

Producer:
```bash
$ ccloud kafka topic produce demo-topic --cluster $CLUSTER_ID
```

