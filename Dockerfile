FROM ubuntu:noble-slim
WORKDIR /app

RUN apt-get update && apt-get install -y libstdc++6 postgresql-client && rm -rf /var/lib/apt/lists/*

COPY target/atlas atlas

EXPOSE 8080

ENTRYPOINT ["./atlas"]
