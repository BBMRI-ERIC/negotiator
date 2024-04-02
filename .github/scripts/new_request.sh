#!/bin/bash

output=$(curl --location 'http://localhost:8081/api/directory/create_query' \
--header 'Content-Type: application/json' \
--header 'Authorization: Basic ZGlyZWN0b3J5OmRpcmVjdG9yeQ==' \
--header 'Cookie: JSESSIONID=F2B9F4656BFB54136BF43D759456848F' \
--data '{
    "nToken": "46e7831fc32d4dc3983d1aecd0085373__search__",
    "URL": "https://bbmritestnn.gcc.rug.nl/",
    "humanReadable": "New query 1",
    "collections"   : [{
        "collectionId" : "bbmri-eric:ID:SE_890:collection:dummy_collection",
        "biobankId"    : "bbmri-eric:ID:SE_890"
    }]
}' -s -w '%{response_code}')
echo $output | grep -q 201 || echo "Failed to create query" $output && exit 1