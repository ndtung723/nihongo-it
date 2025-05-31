#!/bin/bash

# Nihongo-IT - Python Service Startup Script
# This script starts only the Python FastAPI component

# Define colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get absolute path to project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}=== Nihongo-IT - Python Service Starter ===${NC}"
echo -e "${YELLOW}Project root: $PROJECT_ROOT${NC}"

# Check if python directory exists
if [ ! -d "$PROJECT_ROOT/python" ]; then
    echo -e "${RED}ERROR: python directory not found${NC}"
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
check_port 8000 || exit 1  # Python FastAPI

# Create log directory
mkdir -p "$PROJECT_ROOT/logs"

echo -e "${GREEN}Starting Python FastAPI service...${NC}"
cd "$PROJECT_ROOT/python"

# Check for Python virtual environment
if [ ! -d "venv" ]; then
    echo -e "${YELLOW}Setting up Python virtual environment...${NC}"
    python3 -m venv venv
    source venv/bin/activate
    pip install -r requirements.txt
else
    source venv/bin/activate
fi

# Check for OpenAI API key
if [ ! -f ".env" ]; then
    echo -e "${YELLOW}OpenAI API key not found. Please enter your OpenAI API key:${NC}"
    read -p "API Key: " api_key
    echo "OPENAI_API_KEY=$api_key" > .env
    echo "DEBUG_MODE=True" >> .env
    echo -e "${GREEN}OpenAI API key saved to .env file${NC}"
fi

# Start Python service
echo -e "${GREEN}Starting FastAPI service on port 8000...${NC}"
uvicorn main:app --host 0.0.0.0 --port 8000 --reload > "$PROJECT_ROOT/logs/python.log" 2>&1 &
PYTHON_PID=$!
echo -e "${GREEN}Python service started with PID: $PYTHON_PID${NC}"

# Show status information
echo -e "\n${BLUE}=== Python Service Information ===${NC}"
echo -e "${GREEN}FastAPI Service:${NC} http://localhost:8000"
echo -e "${GREEN}API Documentation:${NC} http://localhost:8000/docs"
echo -e "${GREEN}Health Check:${NC} http://localhost:8000/health"
echo -e "\n${YELLOW}Log file: $PROJECT_ROOT/logs/python.log${NC}"
echo -e "${YELLOW}PID: $PYTHON_PID${NC}"
echo -e "\n${RED}To stop the service, press Ctrl+C or run: kill $PYTHON_PID${NC}"

# Save PID to file for later termination
echo "$PYTHON_PID" > "$PROJECT_ROOT/logs/python_pid.txt"

# Keep script running to allow easy termination
echo -e "${BLUE}Python service is running. Press Ctrl+C to stop.${NC}"

# Handle termination
trap 'echo -e "${RED}Stopping Python service...${NC}"; kill $(cat "$PROJECT_ROOT/logs/python_pid.txt") 2>/dev/null; rm "$PROJECT_ROOT/logs/python_pid.txt"; echo -e "${GREEN}Python service stopped${NC}"; exit 0' INT TERM

# Wait indefinitely
while true; do
    sleep 1
done 