#!/bin/sh

# Install required packages
echo "Installing required packages..."
apk add --no-cache curl python3 py3-pip

# Install Python packages
pip3 install --break-system-packages pyyaml requests 2>/dev/null || pip install --break-system-packages pyyaml requests

# Wait for Vault to be ready
echo "Waiting for Vault to be ready..."
while ! curl -s http://vault_srvc:8200/v1/sys/health | grep -q '"initialized":true'; do
    echo "Vault not ready yet, waiting 2 seconds..."
    sleep 2
done

echo "Vault is ready!"

# Enable KV secrets engine if not already enabled
if ! curl -s -H "X-Vault-Token: myroot" http://vault_srvc:8200/v1/sys/mounts | grep -q '"type":"kv"'; then
    echo "Enabling KV secrets engine..."
    curl -s -X POST -H "X-Vault-Token: myroot" \
        -H "Content-Type: application/merge-patch+json" \
        -d '{"type": "kv", "options": {"version": "2"}}' \
        http://vault_srvc:8200/v1/sys/mounts/secret
    echo "KV secrets engine enabled."
else
    echo "KV secrets engine already enabled."
fi

# Use Python to parse YAML and load secrets into Vault
python3 << 'EOF'
import yaml
import requests
import sys
from pathlib import Path

VAULT_ADDR = "http://vault_srvc:8200"
VAULT_TOKEN = "myroot"

headers = {
    "X-Vault-Token": VAULT_TOKEN,
    "Content-Type": "application/merge-patch+json"}

path = Path("secret_load_dir/")

for file in path.glob("*"):
    print(file.name)

    try:
        with open(file, 'r') as f:
            secrets = yaml.safe_load(f)

        if not secrets:
            print("Warning: No secrets found in YAML file")
            sys.exit(0)

        print(f"Found {len(secrets)} secrets to load")
        first_start = True

        for key, value in secrets.items():
            # Convert value to string if not already
            value_str = str(value) if value is not None else ""

            # Use KV v2 API - data must be nested under 'data' key
            url = f"{VAULT_ADDR}/v1/secret/data/{file.stem}"
            payload = {
                "data": {
                    key: value_str
                }
            }

            if first_start:
                response = requests.post(url, json=payload, headers=headers)
                first_start = False
            else:
                response = requests.patch(url, json=payload, headers=headers)

            if response.status_code in [200, 201, 204]:
                print(f"✓ Loaded secret: {key}")
            else:
                print(f"✗ Failed to load {key}: {response.status_code} - {response.text}")

        print("\nSecret loading completed!")

    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

EOF

echo "Script completed successfully!"