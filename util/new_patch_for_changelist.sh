#!/bin/sh
hg qpop -a >/dev/null 2>&1
current_rev=`hg parent --debug | head -n 1 | cut -d ':' -f 3`
echo "Current rev: $current_rev"
echo "Branch:      `hg branch`"
echo "Unsubmitted changes:"
hg status
/bin/echo -n "Press ENTER if the above are OK... "
read x
/bin/echo -n "Issue number: "
read issue
/bin/echo -n "Base revision: "
read base_rev
rietveld_upload.py \
  -s emaily-codereview.appspot.com \
  -m "Revision: $current_rev" \
  -i $issue \
  --rev=$base_rev
