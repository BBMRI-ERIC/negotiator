FROM node:lts-krypton AS  build-stage
WORKDIR /app
COPY package.json yarn.lock .yarnrc.yml ./
COPY .yarn ./.yarn
ENV CYPRESS_INSTALL_BINARY=0
RUN yarn install --immutable
COPY . .
RUN yarn build

FROM nginx/nginx-unprivileged:1.31.9-perl@sha256:36629f7660274d738f76fdcf0b73ebce65c58fef0552232734deb2b498bdc422 AS production-stage
ARG NGINX_UID=101
WORKDIR /app
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --chown=${NGINX_UID}:${NGINX_UID} src/assets/scss /app/assets
COPY --chown=${NGINX_UID}:${NGINX_UID} start.sh .
COPY --chown=${NGINX_UID}:${NGINX_UID} --from=build-stage /app/dist .
USER ${NGINX_UID}
CMD ["./start.sh"]
