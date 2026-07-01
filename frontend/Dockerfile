FROM node:lts-krypton AS  build-stage
WORKDIR /app
COPY . .
RUN yarn install
RUN yarn build

FROM nginxinc/nginx-unprivileged:1.31.2-perl@sha256:5b91b038338c84f7a75fbd12bd7843bef68eb198585b3126379e3ea48e59a895 AS production-stage
ARG NGINX_UID=101
WORKDIR /app
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --chown=${NGINX_UID}:${NGINX_UID} src/assets/scss /app/assets
COPY --chown=${NGINX_UID}:${NGINX_UID} start.sh .
COPY --chown=${NGINX_UID}:${NGINX_UID} --from=build-stage /app/dist .
USER ${NGINX_UID}
CMD ["./start.sh"]
