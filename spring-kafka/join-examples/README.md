### Stream-To-Table

Start the application, the input and output topics will be created.

1. Produce to the user-regions topic

```bash
$ kafka-console-producer \
  --topic user-regions \
  --bootstrap-server localhost:19092 \
  --property parse.key=true \
  --property key.separator=":" \
  --property key.serializer=org.apache.kafka.common.serialization.StringSerializer \
  --property value.serializer=org.apache.kafka.common.serialization.StringSerializer

alice:asia
bob:americas
chao:asia
dave:europe
```

```bash
echo "alice:asia\nbob:americas\nchao:asia\ndave:europe" | kcat -b localhost:19092 -t user-regions -P -K:
```

2. Produce to user-clicks topic:

Not working currently, cannot send with command line a value of Long
With the example I always get this exception:

Caused by: org.apache.kafka.common.errors.SerializationException: Size of data received by LongDeserializer is not 8
at org.apache.kafka.common.serialization.LongDeserializer.deserialize(LongDeserializer.java:30) ~[kafka-clients-3.7.0.jar:na]


```bash

https://stackoverflow.com/questions/44803392/kafka-console-producer-ignores-value-serializer

$ kafka-console-producer \
  --topic user-clicks \
  --bootstrap-server localhost:19092 \
  --property parse.key=true \
  --property key.separator=":" \
  --property key.serializer=org.apache.kafka.common.serialization.StringSerializer \
  --property value.serializer=org.apache.kafka.common.serialization.LongSerializer
  
$ echo "alice:13\nbob:4\nchao:25\nbob:19\ndave:56\neve:78\nalice:40\nfang:99" | kcat -b localhost:19092 -t user-clicks -P -K: -s value=I
$ echo "alice:13L" | kcat -b localhost:19092 -t user-clicks -P -K: 
  
alice:13
bob:4
chao:25
bob:19
dave:56               
eve:78
alice:40
fang:99
```

3. Consume from the clicks-per-region topic

```bash
$ kafka-console-consumer \
  --topic clicks-per-region \
  --bootstrap-server localhost:19092 \
  --from-beginning \
  --property print.key=true \
  --property key.separator=":" \
  --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
  --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```
