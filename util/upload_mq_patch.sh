#!/bin/sh
# Copyright (c) 2009 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ---------------------------------------------------------------
#
# Upload a new patch to rietveld from an mq (mercurial queue) patch.
#
# If there is already an issue for the patch, it updates the issue, otherwise
# it creates a new one.
#
# The patch has to be applied to the current repository, but not necessarily
# has to be on the top.
set -e
hgroot="$(hg root)"

# File to store the "patchname:issue_id" pairs.
codereviewfile="$hgroot/.hg/codereview_patches.cfg"
touch $codereviewfile

# Ussage:
if [ -z "$1" ]; then
  echo "*** Usage: $0 patch_name"
  echo "*** Patches: "
  cat $hgroot/.hg/patches/series
  echo "*** Patches under code review: "
  cat $codereviewfile
  exit 1
fi

patch="$1"
branch=$(hg branch)
subject=$(cat $hgroot/.hg/patches/$patch | head -n 1)
description=$(cat $hgroot/.hg/patches/$patch | perl -pe 'exit if /^$/')
rev=$(hg id -q -r $patch)
parent_rev=$(hg parent -q -r $rev | cut -d ':' -f 2)
issue_id=$(cat $codereviewfile | grep "$patch:" | cut -d ":" -f 2)
if [ ! -z "$issue_id" ]; then
  issue_arg="-i $issue_id"
fi
echo "*** Branch: $branch"
echo "*** Current patch: $patch"
echo "*** - Subject:     $subject"
echo "*** - Revision:    $rev"
echo "*** - Parent rev:  $parent_rev"
echo "*** - Description: $description"
echo "*** Issue id: $issue_id"
echo "*** Unsubmitted changes:"
hg status
/bin/echo -n "*** Press ENTER if the above are OK... "
read x
echo "*** Uploading patch. If nothing happens for a long time, then type your username and password.."
description="$description
Patch name: $patch"
tmpfile="/tmp/codereview.$$"
rietveld_upload.py \
  -s emaily-codereview.appspot.com \
  -d "$description" \
  -m "$subject" \
  --rev=$parent_rev:$rev $issue_arg | tee -a $tmpfile
if [ -z "$issue_id" ]; then
  new_issue_id=$(cat $tmpfile | perl -ne 'print "$1\n" if m{URL.*/(\d+)}')
  cat $codereviewfile | grep -v "$patch:" >$codereviewfile.tmp
  echo "$patch:$new_issue_id" >>$codereviewfile.tmp
  mv -f $codereviewfile.tmp $codereviewfile
fi
