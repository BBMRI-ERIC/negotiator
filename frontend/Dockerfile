FROM node:lts-krypton AS  build-stage
WORKDIR /app
COPY package.json yarn.lock .yarnrc.yml ./
COPY .yarn ./.yarn
ENV CYPRESS_INSTALL_BINARY=0
RUN yarn install
COPY . .
RUN yarn build

FROM nginxinc/nginx-unprivileged:1.31.3-perl@sha256:0a773d2d70e9e9123bddddc50275eee083eb59467dbc15e05fda5de9d6b89ef9 AS production-stage
ARG NGINX_UID=101
WORKDIR /app
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --chown=${NGINX_UID}:${NGINX_UID} src/assets/scss /app/assets
COPY --chown=${NGINX_UID}:${NGINX_UID} start.sh .
COPY --chown=${NGINX_UID}:${NGINX_UID} --from=build-stage /app/dist .
USER ${NGINX_UID}
CMD ["./start.sh"]
