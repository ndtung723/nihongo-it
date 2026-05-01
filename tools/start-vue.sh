#!/bin/bash

# Nihongo-IT - Vue.js Frontend Startup Script
# This script starts only the Vue.js frontend component

# Define colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get absolute path to project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}=== Nihongo-IT - Vue.js Frontend Starter ===${NC}"
echo -e "${YELLOW}Project root: $PROJECT_ROOT${NC}"

# Check if frontend directory exists
if [ ! -d "$PROJECT_ROOT/frontend" ]; then
    echo -e "${RED}ERROR: frontend directory not found${NC}"
    exit 1
fi

# Function to check if port is available
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        echo -e "${RED}ERROR: Port $port is already in use. Please close the application using it.${NC}"
        return 1
    fi
    return 0
}

# Check required port
check_port 5173 || exit 1  # Vue.js

# Create log directory
mkdir -p "$PROJECT_ROOT/logs"

echo -e "${GREEN}Starting Vue.js frontend...${NC}"
cd "$PROJECT_ROOT/frontend"

# Check if dependencies are installed
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing Node.js dependencies...${NC}"
    npm install
fi

# Start Vue.js service
echo -e "${GREEN}Starting Vue.js service on port 5173...${NC}"
npm run dev > "$PROJECT_ROOT/logs/vue.log" 2>&1 &
VUE_PID=$!
echo -e "${GREEN}Vue.js service started with PID: $VUE_PID${NC}"

# Show status information
echo -e "\n${BLUE}=== Vue.js Frontend Information ===${NC}"
echo -e "${GREEN}Vue.js Frontend:${NC} http://localhost:5173"
echo -e "${GREEN}Network Access:${NC} http://0.0.0.0:5173"
echo -e "\n${YELLOW}Log file: $PROJECT_ROOT/logs/vue.log${NC}"
echo -e "${YELLOW}PID: $VUE_PID${NC}"
echo -e "\n${RED}To stop the service, press Ctrl+C or run: kill $VUE_PID${NC}"

# Save PID to file for later termination
echo "$VUE_PID" > "$PROJECT_ROOT/logs/vue_pid.txt"

# Keep script running to allow easy termination
echo -e "${BLUE}Vue.js frontend is running. Press Ctrl+C to stop.${NC}"

# Handle termination
trap 'echo -e "${RED}Stopping Vue.js service...${NC}"; kill $(cat "$PROJECT_ROOT/logs/vue_pid.txt") 2>/dev/null; rm "$PROJECT_ROOT/logs/vue_pid.txt"; echo -e "${GREEN}Vue.js service stopped${NC}"; exit 0' INT TERM

# Wait indefinitely
while true; do
    sleep 1
done 