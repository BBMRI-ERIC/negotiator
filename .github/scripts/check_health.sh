#!/bin/bash

CONTAINER_NAME_OR_ID=$1
MAX_WAIT_TIME=60


ELAPSED_TIME=0
HEALTH_STATUS=""

while [ "$ELAPSED_TIME" -lt "$MAX_WAIT_TIME" ]; do
  HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' "$CONTAINER_NAME_OR_ID")

  if [ "$HEALTH_STATUS" = "healthy" ]; then
    echo "Container is healthy"
    exit 0
  else
    echo "Container is not healthy. Waiting for it to become healthy..."
    sleep 10
    ELAPSED_TIME=$((ELAPSED_TIME + 10))
  fi
done

echo "Container did not become healthy within $MAX_WAIT_TIME seconds."
echo "Printing logs of the container"
docker compose logs --tail 1000 negotiator
exit 1
