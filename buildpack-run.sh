#!/bin/bash

echo "project_id=$PROJECT_ID" > gradle-local.properties
echo "$FIREBASE_CONFIG" > resources/static/js/firebase.js