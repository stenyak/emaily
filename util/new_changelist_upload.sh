#!/bin/sh
current_rev=`hg parent --debug | head -n 1 | cut -d ':' -f 3`
echo "Current rev: $current_rev"
echo "Branch:      `hg branch`"
echo "Unsubmitted changes:"
hg status
/bin/echo -n "Press ENTER if the above are OK... "
read x
/bin/echo -n "Base revision: "
read base_rev
/bin/echo -n "Describe the change in 1 line: "
read change
description="$change
Base rev: $base_rev
Patch 1 rev: $current_rev"
rietveld_upload.py \
  -s emaily-codereview.appspot.com \
  -d "$description" \
  -m "$change" \
  --rev=$base_rev
