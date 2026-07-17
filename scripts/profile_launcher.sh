#!/usr/bin/env bash
set -euo pipefail

package_name=com.r19988088.tvlauncher
output_dir="build/perf-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$output_dir"

adb shell am force-stop "$package_name"
adb shell monkey -p "$package_name" 1 >/dev/null
adb shell dumpsys gfxinfo "$package_name" reset

# Alternate right and left so focus remains inside the populated first row.
adb shell 'for i in $(seq 1 250); do input keyevent 22; input keyevent 21; done'

adb shell dumpsys gfxinfo "$package_name" framestats > "$output_dir/gfxinfo-framestats.txt"
adb shell dumpsys meminfo "$package_name" > "$output_dir/meminfo.txt"
adb shell dumpsys display > "$output_dir/display.txt"
adb shell dumpsys SurfaceFlinger --latency > "$output_dir/surfaceflinger-latency.txt" || true

echo "Performance evidence saved to $output_dir"

