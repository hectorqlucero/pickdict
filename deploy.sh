#!/bin/bash

# Manual deployment script for PickDict to Clojars
# Run this locally: ./deploy.sh

set -e

echo "🚀 PickDict Manual Deployment to Clojars"
echo "========================================"

# Check if credentials are provided
if [ -z "$CLOJARS_USERNAME" ]; then
    echo "❌ CLOJARS_USERNAME not set"
    echo ""
    echo "Set your credentials:"
    echo "export CLOJARS_USERNAME=hector"
    echo "export CLOJARS_PASSWORD=your_deploy_token_here"
    echo ""
    echo "Get your deploy token from: https://clojars.org/tokens"
    exit 1
fi

if [ -z "$CLOJARS_PASSWORD" ]; then
    echo "❌ CLOJARS_PASSWORD not set"
    echo ""
    echo "Set your credentials:"
    echo "export CLOJARS_USERNAME=hector"
    echo "export CLOJARS_PASSWORD=your_deploy_token_here"
    echo ""
    echo "Get your deploy token from: https://clojars.org/tokens"
    exit 1
fi

echo "✅ Credentials configured"
echo "Username: $CLOJARS_USERNAME"
echo "Token: [HIDDEN]"

# Verify we're in the right directory
if [ ! -f "project.clj" ]; then
    echo "❌ project.clj not found. Run this from the pickdict directory."
    exit 1
fi

# Run tests
echo ""
echo "🧪 Running tests..."
if ! lein test; then
    echo "❌ Tests failed. Fix issues before deploying."
    exit 1
fi
echo "✅ All tests passed"

# Check Leiningen version
echo ""
echo "🔧 Leiningen version:"
lein version

# Deploy
echo ""
echo "📦 Deploying to Clojars..."
echo "This may take a minute or two..."
if lein deploy clojars; then
    echo ""
    echo "🎉 SUCCESS! PickDict deployed to Clojars"
    echo ""
    echo "📋 Library Details:"
    echo "   Group ID: org.clojars.hector"
    echo "   Artifact ID: pickdict"
    echo "   Version: 0.1.0"
    echo ""
    echo "📦 Installation for users:"
    echo "   [org.clojars.hector/pickdict \"0.1.0\"]"
    echo ""
    echo "🔗 Clojars URL: https://clojars.org/org.clojars.hector/pickdict"
    echo "📖 GitHub URL: https://github.com/hectorqlucero/pickdict"
else
    echo ""
    echo "❌ Deployment failed!"
    echo ""
    echo "🔍 Possible issues:"
    echo "   - Invalid credentials"
    echo "   - Network connectivity"
    echo "   - Clojars service issues"
    echo ""
    echo "🔧 Troubleshooting:"
    echo "   1. Verify credentials: https://clojars.org/tokens"
    echo "   2. Check network: ping clojars.org"
    echo "   3. Try again later if Clojars is down"
    exit 1
fi
