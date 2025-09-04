#!/bin/bash

# PickDict Release Script
# Usage: ./release.sh <new-version>

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 <new-version>"
    echo "Example: $0 0.1.1"
    exit 1
fi

NEW_VERSION=$1
TAG="v$NEW_VERSION"

echo "🚀 Preparing release $NEW_VERSION"

# Update version in project.clj
echo "📝 Updating project.clj version..."
sed -i "s/\"[0-9]\+\.[0-9]\+\.[0-9]\+\"/\"$NEW_VERSION\"/" project.clj

# Update CHANGELOG.md
echo "📝 Updating CHANGELOG.md..."
DATE=$(date +%Y-%m-%d)
sed -i "s/## \[Unreleased\]/## [$NEW_VERSION] - $DATE\n### Added\n- \n\n## [Unreleased]/" CHANGELOG.md

# Commit changes
echo "💾 Committing changes..."
git add project.clj CHANGELOG.md
git commit -m "Bump version to $NEW_VERSION"

# Create and push tag
echo "🏷️  Creating tag $TAG..."
git tag "$TAG"
git push origin main
git push origin "$TAG"

echo "✅ Release $NEW_VERSION prepared!"
echo "🔄 GitHub Actions will now:"
echo "   - Run tests"
echo "   - Publish to Clojars"
echo "   - Create GitHub release"
echo ""
echo "📦 Users can then add to their project.clj:"
echo "   [org.clojars.hector/pickdict \"$NEW_VERSION\"]"
