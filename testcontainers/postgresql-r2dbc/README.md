```bash
$ psql -h localhost -p 5432 -d test -U admin
secret
test=# \d
            List of relations
 Schema |     Name     |   Type   | Owner
--------+--------------+----------+-------
 public | facts        | table    | admin
 public | facts_id_seq | sequence | admin
```