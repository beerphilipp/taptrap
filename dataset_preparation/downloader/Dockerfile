# Dockerfile for the Google Play APK Downloader (TapTrap)
# Builds apkeep and rs-google-play with Pixel 6a support in a multi-stage setup.
# Author: Philipp Beer

# === Build Stage ===
FROM rust:1.86-slim AS builder
WORKDIR /build

RUN apt-get update && \
    apt-get install -y pkg-config libssl-dev && \
    rm -rf /var/lib/apt/lists/*

COPY apkeep/Cargo.toml ./apkeep/
COPY rs-google-play/Cargo.toml ./rs-google-play/

COPY apkeep ./apkeep
COPY rs-google-play ./rs-google-play

RUN cargo build --release --manifest-path rs-google-play/Cargo.toml
RUN cargo build --release --manifest-path apkeep/Cargo.toml

# === Runtime Stage ===
FROM debian:12-slim
WORKDIR /app
RUN apt-get update && \
    apt-get install -y libssl3 ca-certificates parallel && \
    rm -rf /var/lib/apt/lists/*
COPY --from=builder /build/apkeep/target/release/apkeep .
COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh
ENV PATH="/app:${PATH}"
ENTRYPOINT ["./entrypoint.sh"]