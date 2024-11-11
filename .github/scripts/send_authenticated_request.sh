#!/bin/bash -e

ACCESS_TOKEN=$(curl -s -d 'grant_type=client_credentials' -u 123:123 http://localhost:4011/connect/token | jq -r .access_token)

if [ -z "$ACCESS_TOKEN" ]; then
  echo "Missing access token"
  exit 1;
fi

BASE="http://localhost:8081/api/v3/requests"

if [ "201" = "$(curl -s --oauth2-bearer "$ACCESS_TOKEN" -o /dev/null -w '%{response_code}' -H "Content-Type: application/json" -d @.github/scripts/request.json "$BASE")" ]; then
  echo "OK 👍: successful authenticated system search request"
else
  echo "Fail 😞: failed authenticated system search request"
  exit 1
fi