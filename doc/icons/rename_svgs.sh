#!/bin/bash

for CURRENT_NAME in icon\ *.svg; do
  NEW_NAME=$(echo $CURRENT_NAME | sed 's/icon //g');
  mv "${CURRENT_NAME}" ${NEW_NAME}
done

