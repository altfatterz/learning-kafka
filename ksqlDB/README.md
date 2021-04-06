[Quick Start for Apache Kafka using Confluent Platform (Local)](https://docs.confluent.io/platform/current/quickstart/ce-quickstart.html) 

Put the followings into `.zshrc`

```bash
export CONFLUENT_HOME=~/apps/confluent-6.1.1
export PATH="$CONFLUENT_HOME/bin:$PATH"

export CONFLUENT_CLI_HOME=~/apps/confluent-cli
export PATH="$CONFLUENT_CLI_HOME/bin:$PATH"
```

Manage a local Confluent Platform development environment.

```bash
$ confluent local current   // Get the path of the current Confluent run.
$ confluent local destroy   // Delete the data and logs for the current Confluent run.
$ confluent local version   // Print the Confluent Platform version.
```

Manage Confluent Platform services.

```bash
$ confluent local services list
$ confluent local services status
$ confluent local services start
$ confluent local services stop
```

Install the [Kafka Connect Datagen](https://www.confluent.io/hub/confluentinc/kafka-connect-datagen) source connector using the Confluent Hub client.

```bash
$ confluent-hub install --no-prompt confluentinc/kafka-connect-datagen:latest

Running in a "--no-prompt" mode
Implicit acceptance of the license below:
Apache License 2.0
https://www.apache.org/licenses/LICENSE-2.0
Downloading component Kafka Connect Datagen 0.4.0, provided by Confluent, Inc. from Confluent Hub and installing into /Users/altfatterz/apps/confluent-6.1.1/share/confluent-hub-components
Adding installation directory to plugin path in the following files:
  /Users/altfatterz/apps/confluent-6.1.1/etc/kafka/connect-distributed.properties
  /Users/altfatterz/apps/confluent-6.1.1/etc/kafka/connect-standalone.properties
  /Users/altfatterz/apps/confluent-6.1.1/etc/schema-registry/connect-avro-distributed.properties
  /Users/altfatterz/apps/confluent-6.1.1/etc/schema-registry/connect-avro-standalone.properties

Completed
```


KSQL CLI 

Default server is `http://localhost:8088`

```bash
$ ksql 
```

Useful KSQL CLI commands

```bash
$ output
$ version
$ exit
$ run script <path_to_sql_file>
$ server
$ server <server>
$ list topics;
$ list streams;
$ list tables;
$ list queries;
```

```bash
$ describe [extended] <table|stream>
```


