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

if [ "${PROMETHEUS_VALIDATE_ONLY:-0}" = "1" ]; then
    exec promtool check config /tmp/prometheus.yml
fi

exec /bin/prometheus --config.file=/tmp/prometheus.yml