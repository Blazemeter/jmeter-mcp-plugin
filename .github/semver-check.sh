#!/bin/bash
#
# This script checks that the version number of the release is an expected one, and avoid erroneous releases which don't follow semver
set -eo pipefail

git fetch --tags --quiet
VERSION="$1"
# Other version tags, excluding the one being published. `git rev-list --tags --skip=1`
# walks every ancestor of a tag, so on the first release it returns the parent commit
# and `git describe` fails before the JAR is uploaded.
PREV_TAG=$(git tag --list 'v*' --sort=-v:refname | grep -vx "v${VERSION}" | head -n 1 || true)
if [ -z "$PREV_TAG" ]; then
  echo "No previous release tag; accepting first release $VERSION"
  exit 0
fi
PREV_VERSION=${PREV_TAG#v}
PREV_MAJOR="${PREV_VERSION%%.*}"
PREV_VERSION="${PREV_VERSION#*.}"
PREV_MINOR="${PREV_VERSION%%.*}"
PREV_PATCH="${PREV_VERSION#*.}"
if [[ "$PREV_VERSION" == "$PREV_PATCH" ]]; then
   PREV_PATCH="0"
fi

[[ "$VERSION" == "$PREV_MAJOR.$PREV_MINOR.$((PREV_PATCH + 1))" || "$VERSION" == "$PREV_MAJOR.$((PREV_MINOR + 1))" || "$VERSION" == "$((PREV_MAJOR + 1)).0" ]]
