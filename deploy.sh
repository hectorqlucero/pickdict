#!/bin/bash

# Manual deployment script for PickDict
# Run this locally if GitHub Actions fails

set -e

echo "🚀 Manual PickDict Deployment to Clojars"
echo "========================================"

# Check if credentials are set
if [ -z "$CLOJARS_USERNAME" ]; then
    echo "❌ CLOJARS_USERNAME environment variable not set"
    echo "Set it with: export CLOJARS_USERNAME=your_username"
    exit 1
fi

if [ -z "$CLOJARS_PASSWORD" ]; then
    echo "❌ CLOJARS_PASSWORD environment variable not set"
    echo "Set it with: export CLOJARS_PASSWORD=your_token"
    exit 1
fi

echo "✅ Credentials found"
echo "Username: $CLOJARS_USERNAME"
echo "Token length: ${#CLOJARS_PASSWORD}"

# Run tests first
echo ""
echo "🧪 Running tests..."
lein test

# Deploy
echo ""
echo "📦 Deploying to Clojars..."
lein deploy clojars

echo ""
echo "🎉 Deployment successful!"
echo "Check: https://clojars.org/hector/pickdict"
