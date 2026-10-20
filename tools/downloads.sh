#!/bin/sh
# How many times each release file has been downloaded from GitHub, per release, newest first.
# Needs the gh CLI, signed in. F-Droid keeps no such count (docs/FDROID.md, Counting installs).
#     sh tools/downloads.sh
set -e
gh api repos/mininotesorg/mininotes/releases --paginate \
  --jq '.[] | "\(.tag_name)\t\(.published_at[:10])\tandroid \([.assets[] | select(.name | endswith(".apk")) | .download_count] | add // 0)\twindows \([.assets[] | select(.name | endswith(".zip")) | .download_count] | add // 0)"'
