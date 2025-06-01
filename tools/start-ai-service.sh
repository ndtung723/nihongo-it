#!/bin/bash

# Nihongo-IT - AI Service Startup Script
# This script starts only the AI Service

# Define colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get absolute path to project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}=== Nihongo-IT - AI Service Starter ===${NC}"
echo -e "${YELLOW}Project root: $PROJECT_ROOT${NC}"

# Cleanup function to stop AI service
cleanup() {
    echo -e "${RED}Stopping AI Service...${NC}"
    if [ -f "$PROJECT_ROOT/logs/ai_service_pid.txt" ]; then
        kill $(cat "$PROJECT_ROOT/logs/ai_service_pid.txt") 2>/dev/null
        rm "$PROJECT_ROOT/logs/ai_service_pid.txt"
    fi
    echo -e "${GREEN}AI Service stopped${NC}"
    exit 0
}

# Handle termination signals
trap cleanup INT TERM

# Check if AI service is already running and stop it if needed
if [ -f "$PROJECT_ROOT/logs/ai_service_pid.txt" ]; then
    echo -e "${YELLOW}Found existing AI Service. Stopping it before starting new one...${NC}"
    cleanup
fi

# Function to check if port is available
check_port() {
    local port=$1
    local pid=$(lsof -ti:$port 2>/dev/null)
    if [ -n "$pid" ]; then
        echo -e "${RED}ERROR: Port $port is already in use by PID $pid.${NC}"
        read -p "Do you want to kill the process and continue? (y/n): " choice
        if [[ "$choice" =~ ^[Yy]$ ]]; then
            echo -e "${YELLOW}Killing process $pid on port $port...${NC}"
            kill -9 $pid
            sleep 1
        else
            echo -e "${RED}Exiting...${NC}"
            exit 1
        fi
    fi
    return 0
}

# Check required port
check_port 8087  # AI Service

# Create log directory
mkdir -p "$PROJECT_ROOT/logs"

# Build and start AI Service
echo -e "${GREEN}Building and starting AI Service...${NC}"
cd "$PROJECT_ROOT/services/ai-service"
../gradlew bootRun > "$PROJECT_ROOT/logs/ai-service.log" 2>&1 &
AI_SERVICE_PID=$!
echo -e "${GREEN}AI Service started with PID: $AI_SERVICE_PID${NC}"

# Save PID to file for later termination
echo "$AI_SERVICE_PID" > "$PROJECT_ROOT/logs/ai_service_pid.txt"

# Show status information
echo -e "\n${BLUE}=== AI Service Information ===${NC}"
echo -e "${GREEN}AI Service:${NC} http://localhost:8087"
echo -e "\n${YELLOW}Log file: logs/ai-service.log${NC}"
echo -e "${YELLOW}PID: $AI_SERVICE_PID${NC}"
echo -e "\n${RED}To stop AI Service, press Ctrl+C${NC}"

# Keep script running to allow easy termination
echo -e "${BLUE}AI Service is running. Press Ctrl+C to stop.${NC}"

# Wait indefinitely
while true; do
    # Check if AI service is still running every 5 seconds
    if ! ps -p $AI_SERVICE_PID > /dev/null; then
        echo -e "${RED}AI Service with PID $AI_SERVICE_PID has stopped unexpectedly.${NC}"
        rm -f "$PROJECT_ROOT/logs/ai_service_pid.txt"
        exit 1
    fi
    sleep 5
done 