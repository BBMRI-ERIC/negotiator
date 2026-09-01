FROM node:lts-krypton AS  build-stage
WORKDIR /app
COPY . .
RUN yarn install
RUN yarn build

FROM nginxinc/nginx-unprivileged:1.31.4-perl@sha256:cfccfa3ded2d1839340aa2da1a8f70f65cfefba9685fd1168494c81db04921aa AS production-stage
ARG NGINX_UID=101
WORKDIR /app
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --chown=${NGINX_UID}:${NGINX_UID} src/assets/scss /app/assets
COPY --chown=${NGINX_UID}:${NGINX_UID} start.sh .
COPY --chown=${NGINX_UID}:${NGINX_UID} --from=build-stage /app/dist .
USER ${NGINX_UID}
CMD ["./start.sh"]
