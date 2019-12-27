```bash
$ brew install avro-tools
```

### Convert from json to avro

```bash
$ avro-tools fromjson --schema-file src/main/resources/avro/schema.avsc customer.json > customer.avro
```

### Get the schema from avro

```bash
$ avro-tools getschema customer.avro
```

### From avro to json

```bash
$ avro-tools tojson --pretty customer.avro
```


Avro Specification:
http://avro.apache.org/docs/current/spec.html