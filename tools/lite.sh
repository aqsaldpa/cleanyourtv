#!/usr/bin/env bash
# Lite mode for low-RAM TVs (1-2 GB): faster animations, fewer cached background apps, and a
# memory report before and after. Every change is recorded and undone with `tools/lite.sh undo`.
#
# Usage: TV_HOST=192.168.1.50 tools/lite.sh          # apply
#        TV_HOST=192.168.1.50 tools/lite.sh undo     # put the old values back
#        TV_HOST=192.168.1.50 tools/lite.sh report   # memory only
set -euo pipefail
: "${TV_HOST:?set TV_HOST to the TV IP address (tools/find-tv.sh finds it)}"
cd "$(dirname "$0")/.."
DEV="$TV_HOST:5555"
adb connect "$DEV" >/dev/null 2>&1 || true
sh_tv() { adb -s "$DEV" shell "$@" | tr -d '\r'; }
REC="backups/lite-$TV_HOST.txt"
SCALES="window_animation_scale transition_animation_scale animator_duration_scale"

report() {
  sh_tv 'awk "/MemTotal|MemAvailable|SwapTotal|SwapFree/{printf \"%s %d MB\n\", \$1, \$2/1024}" /proc/meminfo'
}

case "${1:-apply}" in
  report) report ;;
  undo)
    [ -r "$REC" ] || { echo "no record at $REC: lite mode was not applied from this computer" >&2; exit 1; }
    while read -r key value; do
      if [ "$key" = max_cached_processes ]; then
        if [ "$value" = null ]; then sh_tv device_config delete activity_manager max_cached_processes >/dev/null
        else sh_tv device_config put activity_manager max_cached_processes "$value"; fi
      elif [ "$value" = null ]; then sh_tv settings delete global "$key" >/dev/null
      else sh_tv settings put global "$key" "$value"; fi
      echo "restored $key=$value"
    done < "$REC"
    rm -f "$REC"
    ;;
  apply)
    echo "before:"; report
    mkdir -p backups
    if [ ! -e "$REC" ]; then
      for k in $SCALES; do echo "$k $(sh_tv settings get global "$k")"; done > "$REC"
      echo "max_cached_processes $(sh_tv device_config get activity_manager max_cached_processes)" >> "$REC"
    fi
    for k in $SCALES; do sh_tv settings put global "$k" 0.5; done
    sh_tv device_config put activity_manager max_cached_processes 16
    sh_tv am kill-all
    sleep 3
    echo "after (animations 0.5x, at most 16 cached apps, background apps closed):"; report
    echo "undo: TV_HOST=$TV_HOST tools/lite.sh undo"
    ;;
  *) echo "usage: tools/lite.sh [apply|undo|report]" >&2; exit 1 ;;
esac
