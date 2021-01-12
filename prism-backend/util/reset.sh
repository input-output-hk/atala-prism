
environment="$1"
connectorPassword="$2"

PGPASSWORD=$2 psql -U connector-$1 -h credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com -d postgres -f reset-connector.sql

PGPASSWORD=$2 psql -U connector-$1 -h credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com -d postgres -f fake_data.sql
