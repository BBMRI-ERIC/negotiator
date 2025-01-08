FROM node:lts-bookworm AS  build-stage
WORKDIR /app
COPY . .
RUN yarn install
RUN yarn build

FROM bitnami/nginx:1.24 AS production-stage
WORKDIR /app
COPY nginx.conf /opt/bitnami/nginx/conf/server_blocks/my_server_block.conf
COPY src/assets/scss /app/assets
COPY start.sh .
COPY --from=build-stage /app/dist .
USER 0
RUN chmod -R g+rwx /app
USER 1001
CMD ["./start.sh"]
