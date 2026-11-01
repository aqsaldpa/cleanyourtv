#!/usr/bin/env bash
# Find Android TVs on the local network by scanning for an open ADB port (5555), then print
# each one's brand, model and device name. No IP typing needed.
#
# Usage: tools/find-tv.sh            # scans the /24 network of this computer
#        tools/find-tv.sh 10.0.0     # scans 10.0.0.1-254
set -uo pipefail
PREFIX="${1:-}"
if [ -z "$PREFIX" ]; then
  IP="$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || hostname -I 2>/dev/null | awk '{print $1}')"
  [ -n "$IP" ] || { echo "could not find this computer's IP; pass the prefix, e.g. tools/find-tv.sh 192.168.1" >&2; exit 1; }
  PREFIX="${IP%.*}"
fi
echo "scanning $PREFIX.1-254 for ADB (port 5555)..." >&2
TMP="$(mktemp)"
for i in $(seq 1 254); do
  ( if command -v nc >/dev/null; then
      nc -z -G 1 "$PREFIX.$i" 5555 2>/dev/null || nc -z -w 1 "$PREFIX.$i" 5555 2>/dev/null
    fi && echo "$PREFIX.$i" >> "$TMP" ) &
done
wait
if [ ! -s "$TMP" ]; then
  echo "no TV found. On the TV: Developer options > USB debugging on, TV awake, same Wi-Fi (not a guest network)." >&2
  rm -f "$TMP"; exit 1
fi
sort -t. -k4 -n "$TMP" | while read -r host; do
  adb connect "$host:5555" >/dev/null 2>&1
  sleep 1
  state="$(adb devices | awk -v h="$host:5555" '$1==h{print $2}')"
  if [ "$state" = device ]; then
    info="$(adb -s "$host:5555" shell 'echo "$(getprop ro.product.brand) $(getprop ro.product.model) ($(getprop ro.product.device), Android $(getprop ro.build.version.release))"' | tr -d '\r')"
    echo "$host  $info"
  else
    echo "$host  ${state:-no answer}: accept \"Allow USB debugging?\" on the TV (tick Always allow), then run again"
  fi
done
rm -f "$TMP"
