#!/bin/bash

# Profile Startup Test Script
# This script tests that both InMemory and Outbox profiles can start successfully
# Usage: bash .ai/scripts/test-profile-startup.sh

set -e

echo "========================================="
echo "Profile Startup Test"
echo "========================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Function to test profile startup
test_profile() {
    local profile=$1
    local port=$2
    
    echo -e "\n${YELLOW}Testing $profile Profile on port $port...${NC}"
    
    # Start the application in background
    SPRING_PROFILES_ACTIVE=$profile /opt/homebrew/bin/mvn spring-boot:run -q 2>&1 &
    local PID=$!
    
    # Wait for startup
    echo "Waiting for application to start (PID: $PID)..."
    sleep 15
    
    # Check if process is still running
    if ps -p $PID > /dev/null 2>&1; then
        # Test API endpoint
        if curl -s -f http://localhost:$port/v1/api/products > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $profile Profile started successfully and API is responsive${NC}"
            # Clean shutdown
            kill $PID 2>/dev/null || true
            sleep 2
            return 0
        else
            echo -e "${RED}❌ $profile Profile started but API is not responsive${NC}"
            kill $PID 2>/dev/null || true
            sleep 2
            return 1
        fi
    else
        echo -e "${RED}❌ $profile Profile failed to start${NC}"
        return 1
    fi
}

# Test both profiles
FAILED=0

# Test InMemory Profile
if ! test_profile "test-inmemory" "8080"; then
    FAILED=1
fi

# Test Outbox Profile  
if ! test_profile "test-outbox" "9090"; then
    FAILED=1
fi

echo -e "\n========================================="
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ All profiles started successfully!${NC}"
else
    echo -e "${RED}❌ Some profiles failed to start${NC}"
    exit 1
fi
echo "========================================="