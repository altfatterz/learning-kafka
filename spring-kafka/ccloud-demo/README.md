
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

```bash
$ ccloud kafka topic list --cluster lkc-3kj32
     Name
+------------+
  demo-topic
```


Use an API key and secret in the CLI
```bash
$ ccloud api-key store --resource lkc-3kj32
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
$ ccloud api-key use MHW7UJ2KN6F53T5N --resource lkc-3kj32
Set API Key "MHW7UJ2KN6F53T5N" as the active API key for "lkc-3kj32".
```

Consumer:

```bash
$ ccloud kafka topic consume -b demo-topic --cluster lkc-3kj32
```

Producer:
```bash
$ ccloud kafka topic produce demo-topic --cluster lkc-3kj32
```


TODO include Schema Registry into the mix and use Protobuf

