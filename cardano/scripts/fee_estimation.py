#!/usr/bin/python3

import os
import random
import requests

BYTESTRING_LIMIT = 64
MAX_TX_SIZE = 16384
SAMPLE_RUNS = 10
PRISM_INDEX = 21325

host = os.environ['NODE_CARDANO_WALLET_API_HOST']
port = os.environ['NODE_CARDANO_WALLET_API_PORT']
wallet_id = os.environ['NODE_CARDANO_WALLET_ID']
address = os.environ['NODE_CARDANO_PAYMENT_ADDRESS']
url = f'http://{host}:{port}/v2/wallets/{wallet_id}/payment-fees'


def estimate_fee(metadata):
    json_request = f'''{{
        "payments": [{{
            "address": "{address}",
            "amount": {{
                "quantity": 1000000,
                "unit": "lovelace"
            }}
        }}],
        "metadata": {metadata}
    }}
    '''
    response = requests.post(url, data=json_request, headers={'Content-Type': 'application/json'})
    if not response.ok:
        print(response.text)
        response.raise_for_status()

    estimation = response.json()
    return int(estimation['estimated_min']['quantity'])


def random_bytes(size):
    return [random.randrange(-128, 128) for _ in range(size)]


def schema_v1(payload):
    payload_str = ', '.join(map(str, payload))
    return f'''{{
    "{PRISM_INDEX}": {{
        "version": 1,
        "content": [{payload_str}]
    }}
}}'''


def schema_v2(payload):
    payload_str = ', '.join(map(str, payload))
    return f'''{{
    "{PRISM_INDEX}": {{
        "v": 2,
        "c": [{payload_str}]
    }}
}}'''


def schema_v3(payload):
    payload_bytes = [b & 0xff for b in payload]
    payload_chunks = []
    for i in range(0, len(payload_bytes), BYTESTRING_LIMIT):
        payload_chunks.append(payload_bytes[i:i + BYTESTRING_LIMIT])

    payload_chunks_str = [f'"0x{bytes(chunk).hex()}"' for chunk in payload_chunks]
    payload_str = ', '.join(payload_chunks_str)
    return f'''{{
    "{PRISM_INDEX}": {{
        "v": 3,
        "c": [{payload_str}]
    }}
}}'''


def analyze_fee(base_fee, fee):
    diff = base_fee - fee
    diff_perc = round(diff / base_fee * 100)
    return f'diff: {diff}, diff perc: {diff_perc}%'


def average_estimate_fee(schema, payloads):
    fees = [estimate_fee(schema(payload)) for payload in payloads]
    return round(sum(fees) / len(fees))


def compare_schemas(payloads, comparison_title=None):
    #v1_fee = average_estimate_fee(schema_v1, payloads)
    #v2_fee = average_estimate_fee(schema_v2, payloads)
    v1_fee = 1
    v2_fee = 1
    v3_fee = average_estimate_fee(schema_v3, payloads)

    if comparison_title is None:
        comparison_title = f'Average fee of {len(payloads)} random payloads of {len(payloads[0])} bytes each'
    print(f'''{comparison_title}:
  Schema 1: {v1_fee} (base)
  Schema 2: {v2_fee} ({analyze_fee(v1_fee, v2_fee)})
  Schema 3: {v3_fee} ({analyze_fee(v1_fee, v3_fee)})
''')


# Analyze a real createDID operation
real_payload = [10, 32, 84, 10, 3, -104, -95, 115, -36, 71, 94, -84, 30, 111, -4, 89, -112, -70, -68, -42, -91, 40, 66,
                -113, -121, 97, 82, -42, -127, -54, -44, 105, -69, 65]
# compare_schemas([real_payload], comparison_title=f'Fee of a real payload of {len(real_payload)} bytes')

# Analyze several possible transaction sizes with random metadata
# `8000` was found empirically
max_metadata_size = MAX_TX_SIZE - 8000
# sizes = [100, 500, 1000, max_metadata_size]
# sizes = [199, 200, 201, 202, 262, 263, 264, 265, 266, 267]
sizes = [195, 260]
for size in sizes:
    payloads = [random_bytes(size) for _ in range(SAMPLE_RUNS)]
    compare_schemas(payloads)
