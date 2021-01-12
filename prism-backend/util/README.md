## fake\_data

Before running the script you should install protobuf tools (we need `protoc` to generate Python protobuf code) and python dependencies (drop `--user` and add `sudo` if you prefer to install globally):

```
pip3 install --user coincurve faker image google-api-python-client protobuf
```

Then you can run:

```
./generate_fake_data.sh
```

You might get the font missing, in such case edit `fake_data.py` and replace the path in `font_path` file (it doesn't have to be the same font, you can choose anything with wide range Unicode coverage).

Following files will be generated:

* `fake_data.sql` - SQL file inserting the data,
* `fake_data.txt` - info file, containing UUIDs to use for testing.

In order to insert the data, run (replacing `localhost` with target host and `connector_db` with the relevant db):

```
psql -h localhost -U postgres connector_db -f /fake_data.sql
```

If you don't have `psql`, but you have docker, you can run (you might need to disable SEL for that):

```
docker run -it -v `pwd`/fake_data.sql:/fake_data.sql --rm --network host postgres psql -h localhost -U postgres connector_db -f /fake_data.sql
```

If you want to insert the data to one of the AWS environments, remember to connect as the connector user for your environment.
For example, in environment 'develop', this will be connector-develop and your connection arguments would be 

```
PGPASSWD=<pswd> psql -U connector-develop -h credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com -d postgres -f fake_data.sql
```
replacing <pswd> with the password that is written output to the console after terraform runs (i.e. after running either ./env.sh -A or ./env.sh -s, or looking in the circleci build output).

