FROM node:lts-krypton AS  build-stage
WORKDIR /app
COPY . .
RUN yarn install
RUN yarn build

FROM nginxinc/nginx-unprivileged:1.31.2-perl@sha256:1580f7ab8afdc0ebfdd2a6f0e6223354c12f0bddb5b66215c2c88f071adabb89 AS production-stage
ARG NGINX_UID=101
WORKDIR /app
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --chown=${NGINX_UID}:${NGINX_UID} src/assets/scss /app/assets
COPY --chown=${NGINX_UID}:${NGINX_UID} start.sh .
COPY --chown=${NGINX_UID}:${NGINX_UID} --from=build-stage /app/dist .
USER ${NGINX_UID}
CMD ["./start.sh"]
