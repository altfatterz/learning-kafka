Good videos:

https://www.learningjournal.guru/courses/kafka/kafka-foundation-training/

### max.in.flight.requests.per.connection

`max.in.flight.requests.per.connection` default is `5`

- The maximum number of unacknowledged requests the client will send on a single connection before blocking. 
- Note that if this setting is set to be greater than 1 and there are failed sends, there is a risk of message re-ordering due to retries

If order is important (transactional systems, for example banks) you can 2 options:

1. Use synchronous send.
2. set max.in.flight.requests.per.connection to 1

### retries

`retries` default `2147483647` - It defines how many times the producer will retry after getting an error
Users should generally prefer to leave this config unset and instead use `delivery.timeout.ms` to control retry behavior.

### retry.backoff.ms

Controls the time between two retries. The default value for this parameter is 100 milliseconds.

### compression.type

Applies to full batches of data.
The compression type used by a producer is noted as a header of the record batch. This allows multiple producers writing to
the same topic to use different compression types. Consumers will decompress batches of messages according to the compression type denoted in the header.


### EOS (Exactly Once Semantics)

https://www.confluent.io/blog/exactly-once-semantics-are-possible-heres-how-apache-kafka-does-it/

### Transactions in Apache Kafka

https://www.confluent.io/blog/transactions-apache-kafka/







### Static group membership

Avoid excessive re-balances (Consumers in Consumer Groups)

`group.instance.id` - `Consumer Group` as a `StatefulSet` and assign `group.instance.id` as the pod name of the Consumer.
- Consumers do not send LeaveGroupRequest
- Rejoin doesn’t trigger rebalance for known group.instance.id

https://kubernetes.io/docs/tasks/inject-data-application/environment-variable-expose-pod-information/ 