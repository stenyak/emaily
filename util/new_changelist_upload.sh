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
/bin/echo -n "Reviewer: "
read revierwer
echo "Describe the change (ctrl+z at the end):"
change=`cat`
change="$change
Base rev: $base_rev"
rietveld_upload.py \
  -s emaily-codereview.appspot.com \
  -d "$change" \
  -m "Revision: $current_rev" \
  -r "$reviewer" \
  --rev=$base_rev \
  --send_mail

