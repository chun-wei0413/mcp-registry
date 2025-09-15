#!/bin/bash

# PostgreSQL MCP Server - Docker Hub Push Script
# Version: 0.2.0

set -e

DOCKER_USERNAME="${DOCKER_USERNAME:-russellli}"
IMAGE_NAME="postgresql-mcp-server"
VERSION="0.2.0"

echo "🚀 PostgreSQL MCP Server Docker Hub Push Script"
echo "================================================="
echo "Username: $DOCKER_USERNAME"
echo "Image: $IMAGE_NAME"
echo "Version: $VERSION"
echo ""

# Check if user is logged in to Docker Hub
echo "🔐 Checking Docker Hub login status..."
if ! docker info | grep -q "Username"; then
    echo "❌ Not logged in to Docker Hub. Please run: docker login"
    exit 1
fi

echo "✅ Docker Hub login verified"

# Build the image
echo "🔨 Building Docker image..."
docker build -f deployment/docker/Dockerfile -t $IMAGE_NAME:$VERSION -t $IMAGE_NAME:latest .

# Tag for Docker Hub
echo "🏷️  Tagging image for Docker Hub..."
docker tag $IMAGE_NAME:$VERSION $DOCKER_USERNAME/$IMAGE_NAME:$VERSION
docker tag $IMAGE_NAME:latest $DOCKER_USERNAME/$IMAGE_NAME:latest

# Push to Docker Hub
echo "📤 Pushing to Docker Hub..."
docker push $DOCKER_USERNAME/$IMAGE_NAME:$VERSION
docker push $DOCKER_USERNAME/$IMAGE_NAME:latest

echo ""
echo "🎉 Successfully pushed to Docker Hub!"
echo "📦 Available images:"
echo "   - $DOCKER_USERNAME/$IMAGE_NAME:$VERSION"
echo "   - $DOCKER_USERNAME/$IMAGE_NAME:latest"
echo ""
echo "📋 Usage:"
echo "   docker pull $DOCKER_USERNAME/$IMAGE_NAME:$VERSION"
echo "   docker run -d -p 3000:3000 $DOCKER_USERNAME/$IMAGE_NAME:$VERSION"