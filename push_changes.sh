#!/bin/bash
# Script to push changes to GitHub

# Check current branch
current_branch=$(git rev-parse --abbrev-ref HEAD)
echo "Current branch: $current_branch"

# Checkout main branch
echo "Checking out main branch..."
git checkout main

# Merge the fix branch into main
echo "Merging $current_branch into main..."
git merge $current_branch

# Push to GitHub
echo "Pushing to GitHub..."
git push origin main

echo "Done!"
