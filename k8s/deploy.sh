#!/usr/bin/env bash
# делает скрипт по принципу fail-fast (любая ошибка в пайплайне завершает его немедленно и отмечает неуспешным целиком)
set -euo pipefail

on_exit() {
  code=$?
  if [ "$code" -ne 0 ]; then
    echo "ОШИБКА: скрипт завершился с кодом $code."
  fi
  read -r -p "Нажмите Enter, чтобы закрыть окно"
}
trap on_exit EXIT

cd "$(dirname "$0")/.."
NS=link-tracker
ENV_FILE=".env"

[ -f "$ENV_FILE" ] || { echo "нет $ENV_FILE"; exit 1; }

# функция извлечения пар ключ=значение из .env
env_get() { grep -E "^$1=" "$ENV_FILE" | head -n 1 | cut -d= -f2- | tr -d '\r'; }

echo "==> сборка образов"
minikube image build -t link-tracker/scrapper:0.0.1   -f scrapper/Dockerfile .
minikube image build -t link-tracker/bot:0.0.1        -f bot/Dockerfile .
minikube image build -t link-tracker/migrations:0.0.1 -f migrations/Dockerfile .

# проверяет существующие деплои (тег образа тот же, поэтому после пересборки их надо перезапустить)
existing=$(kubectl -n "$NS" get deploy scrapper bot -o name 2>/dev/null || true)

echo "==> namespace, config, secret"
kubectl apply -f k8s/00-namespace.yml -f k8s/01-common-config.yml
kubectl -n "$NS" create secret generic link-tracker-secrets \
  --from-literal=APP_TELEGRAM_TOKEN="$(env_get APP_TELEGRAM_TOKEN)" \
  --from-literal=GITHUB_TOKEN="$(env_get GITHUB_TOKEN)" \
  --from-literal=STACKOVERFLOW_KEY="$(env_get STACKOVERFLOW_KEY)" \
  --from-literal=STACKOVERFLOW_ACCESS_KEY="$(env_get STACKOVERFLOW_ACCESS_KEY)" \
  --from-literal=POSTGRES_DB="$(env_get POSTGRES_DB)" \
  --from-literal=POSTGRES_USER="$(env_get POSTGRES_USER)" \
  --from-literal=POSTGRES_PASSWORD="$(env_get POSTGRES_PASSWORD)" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "==> postgres, kafka, valkey + sentinel"
kubectl apply -f k8s/10-postgres.yml -f k8s/20-kafka.yml -f k8s/30-valkey.yml
for s in postgres kafka valkey valkey-sentinel; do
  kubectl -n "$NS" rollout status "sts/$s" --timeout=300s
done

echo "==> миграции"
kubectl -n "$NS" delete job migrations --ignore-not-found
kubectl apply -f k8s/40-migrations-job.yml
kubectl -n "$NS" wait --for=condition=complete job/migrations --timeout=300s

echo "==> scrapper, bot"
kubectl apply -f k8s/50-scrapper.yml -f k8s/60-bot.yml
if [ -n "$existing" ]; then
  kubectl -n "$NS" rollout restart deploy/scrapper deploy/bot
fi
kubectl -n "$NS" rollout status deploy/scrapper --timeout=300s
kubectl -n "$NS" rollout status deploy/bot --timeout=300s

echo "==> кластер успешно запущен"
kubectl -n "$NS" get pods -o wide
