## fake\_data

Before running the script you should install protobuf tools (we need `protoc` to generate Python protobuf code) and python dependencies (drop `--user` and add `sudo` if you prefer to install globally):

```
pip3 install --user coincurve faker
```

Then you can run:

```
./generate_fake_data.sh
```

You might get the font missing, in such case edit `fake_data.py` and replace the path in `font_path` file (it doesn't have to be the same font, you can choose anything with wide range Unicode coverage).

Following files will be generated:

* `fake_data.sql` - SQL file inserting the data,
* `fake_data.txt` - info file, containing UUIDs to use for testing.

In order to insert the data, run (replacing `localhost` with target host and `geud_connector_db` with the relevant db):

```
psql -h localhost -U postgres geud_connector_db -f /fake_data.sql
```

If you don't have `psql`, but you have docker, you can run (you might need to disable SEL for that):

```
docker run -it -v `pwd`/fake_data.sql:/fake_data.sql --rm --network host postgres psql -h localhost -U postgres geud_connector_db -f /fake_data.sql
```
