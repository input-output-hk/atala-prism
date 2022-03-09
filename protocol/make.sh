#!/usr/bin/env bash

set -eux
set -o pipefail

SCRIPT_PATH=${BASH_SOURCE[0]}
SCRIPT_FOLDER=$(readlink -m "$(dirname "$SCRIPT_PATH")")

function make() {
  echo Making "$1"

  pandoc -N -f markdown+smart \
  --self-contained \
  --standalone \
  --toc \
  --highlight-style pygments \
  --filter pandoc-citeproc \
  --bibliography biblio.bib \
  --pdf-engine=xelatex \
  -V papersize:a4 \
  -V geometry:margin=1.2in \
  -V fontsize=10pt \
  -V date="Revision: $(git show -s --format='%h')\\\\Revision Date: $(git show -s --format='%cD')" \
  -V author="The Atala Team, IOHK" \
  -s \
  intro.md \
  protocol-v0.1.md \
  protocol-v0.2.md \
  protocol-v0.3.md \
  canonicalization.md \
  key-derivation.md \
  unpublished-dids.md \
  late-publish.md \
  protocol-other-ideas.md \
  -o "$1"
}

cd "$SCRIPT_FOLDER"

make protocol.pdf
make protocol.docx
