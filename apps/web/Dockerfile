# syntax=docker/dockerfile:1
# El contexto de build es la RAIZ del repo, porque el lockfile de pnpm vive alli:
#   docker build -f apps/web/Dockerfile -t gastonomo-web .
FROM node:26-alpine AS base
ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
RUN corepack enable
WORKDIR /repo

FROM base AS deps
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc ./
COPY packages/eslint-config/package.json packages/eslint-config/
COPY packages/tsconfig/package.json packages/tsconfig/
COPY tools/checks/package.json tools/checks/
COPY apps/web/package.json apps/web/
RUN --mount=type=cache,id=pnpm,target=/pnpm/store \
    pnpm install --frozen-lockfile --filter @gastonomo/web...

FROM deps AS build
COPY packages/ packages/
COPY apps/web/ apps/web/
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL
RUN pnpm --filter @gastonomo/web build

FROM nginx:alpine AS runtime
COPY apps/web/nginx.conf /etc/nginx/nginx.conf
COPY --from=build /repo/apps/web/dist /usr/share/nginx/html
RUN chown -R nginx:nginx /usr/share/nginx/html
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1:8080/healthz || exit 1
CMD ["nginx", "-g", "daemon off;"]
