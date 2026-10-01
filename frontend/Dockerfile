FROM node:lts-krypton AS  build-stage
WORKDIR /app
COPY package.json yarn.lock .yarnrc.yml ./
COPY .yarn ./.yarn
ENV CYPRESS_INSTALL_BINARY=0
RUN yarn install --immutable
COPY . .
RUN yarn build

FROM nginxinc/nginx-unprivileged:1.31.6-perl@sha256:a893234c4f1ecce2a4e6206ec3ec920ea25ff660ad6807c741471fc447d5d365 AS production-stage
ARG NGINX_UID=101
WORKDIR /app
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --chown=${NGINX_UID}:${NGINX_UID} src/assets/scss /app/assets
COPY --chown=${NGINX_UID}:${NGINX_UID} start.sh .
COPY --chown=${NGINX_UID}:${NGINX_UID} --from=build-stage /app/dist .
USER ${NGINX_UID}
CMD ["./start.sh"]
