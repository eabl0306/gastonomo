# syntax=docker/dockerfile:1

# Imagen de produccion de la API. El contexto de build es apps/api.
#
#   docker build -t gastonomo-api:local apps/api
#
# cargo-chef separa la compilacion de dependencias de la del codigo: mientras
# los manifiestos no cambien, la capa de dependencias se reutiliza.

ARG RUST_VERSION=1.97
ARG CARGO_CHEF_VERSION=0.1.72

FROM rust:${RUST_VERSION}-slim-trixie AS chef
ARG CARGO_CHEF_VERSION
RUN apt-get update \
 && apt-get install --no-install-recommends --yes build-essential pkg-config \
 && rm -rf /var/lib/apt/lists/*
RUN cargo install cargo-chef --locked --version ${CARGO_CHEF_VERSION}
WORKDIR /app

FROM chef AS planner
COPY . .
RUN cargo chef prepare --recipe-path recipe.json

FROM chef AS builder
COPY --from=planner /app/recipe.json recipe.json
RUN cargo chef cook --release --recipe-path recipe.json
COPY . .
RUN cargo build --release --bin gastonomo-api

FROM debian:trixie-slim AS runtime
RUN apt-get update \
 && apt-get install --no-install-recommends --yes ca-certificates curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 10001 gastonomo \
 && useradd --system --uid 10001 --gid gastonomo --no-create-home gastonomo

COPY --from=builder /app/target/release/gastonomo-api /usr/local/bin/gastonomo-api

USER gastonomo:gastonomo
WORKDIR /srv

ENV GASTONOMO__SERVER__HOST=0.0.0.0 \
    GASTONOMO__SERVER__PORT=8080 \
    GASTONOMO__TELEMETRY__LOG_LEVEL=info

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
  CMD curl --fail --silent --show-error http://127.0.0.1:8080/health/live || exit 1

ENTRYPOINT ["/usr/local/bin/gastonomo-api"]
