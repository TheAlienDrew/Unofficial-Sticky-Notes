#!/bin/bash

DOWNLOAD_FILE="linkifyjs.zip"
DOWNLOAD_URL="https://github.com/Soapbox/linkifyjs/releases/latest/download/$DOWNLOAD_FILE"
JS_FILE="linkify.min.js"
JS_FOLDER="../UnofficialStickyNotes/app/src/main/assets/js"

wget -O "$DOWNLOAD_FILE" "$DOWNLOAD_URL"
if [ $? -eq 0 ]; then
  unzip -oj "$DOWNLOAD_FILE" "$JS_FILE" -d "$JS_FOLDER"
fi
rm "$DOWNLOAD_FILE"
