#!/bin/sh
set -eu

escape_yaml_single_quoted() {
    printf '%s' "$1" | sed -e "s/'/''/g" -e 's/[\\/&]/\\&/g'
}

username=$(escape_yaml_single_quoted "${ATLAS_PROMETHEUS_USERNAME:-atlas-prometheus}")
password=$(escape_yaml_single_quoted "${ATLAS_PROMETHEUS_PASSWORD:-atlas-prometheus-change-me}")

sed \
    -e "s/__ATLAS_PROMETHEUS_USERNAME__/$username/g" \
    -e "s/__ATLAS_PROMETHEUS_PASSWORD__/$password/g" \
    /etc/prometheus/prometheus.yml.template > /tmp/prometheus.yml

# validate
promtool check config /tmp/prometheus.yml

# run prometheus with conf
exec prometheus --config.file=/tmp/prometheus.yml \
    --storage.tsdb.path=/prometheus \
    --web.console.libraries=/usr/share/prometheus/console_libraries \
    --web.console.templates=/usr/share/prometheus/consoles