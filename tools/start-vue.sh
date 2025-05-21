#!/bin/bash

# Nihongo-IT - Service Startup Script
# This script starts the Python and Frontend components of the Nihongo-IT application

# Define colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get absolute path to project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}=== Nihongo-IT - Service Starter ===${NC}"
echo -e "${YELLOW}Project root: $PROJECT_ROOT${NC}"

# Check if all required directories exist
if [ ! -d "$PROJECT_ROOT/python" ]; then
    echo -e "${RED}ERROR: python directory not found${NC}"
    exit 1
fi

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

# Check required ports
check_port 8000 || exit 1  # Python FastAPI
check_port 5173 || exit 1  # Vue.js

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
    read api_key
    echo "OPENAI_API_KEY=$api_key" > .env
    echo "OpenAI API key saved to .env file"
fi

# Start Python service in background
echo -e "${GREEN}Starting FastAPI service on port 8000...${NC}"
uvicorn main:app --host 0.0.0.0 --port 8000 > "$PROJECT_ROOT/logs/python.log" 2>&1 &
PYTHON_PID=$!
echo -e "${GREEN}Python service started with PID: $PYTHON_PID${NC}"

# Give Python service time to start
echo -e "${YELLOW}Waiting for Python service to initialize...${NC}"
sleep 3

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
echo -e "\n${BLUE}=== Services Information ===${NC}"
echo -e "${GREEN}Python FastAPI:${NC} http://localhost:8000"
echo -e "${GREEN}Vue.js Frontend:${NC} http://localhost:5173"
echo -e "\n${YELLOW}Log files are available in the logs directory${NC}"
echo -e "${YELLOW}PIDs: Python=$PYTHON_PID, Vue=$VUE_PID${NC}"
echo -e "\n${RED}To stop all services, press Ctrl+C or run: kill $PYTHON_PID $VUE_PID${NC}"

# Save PIDs to file for later termination
echo "$PYTHON_PID $VUE_PID" > "$PROJECT_ROOT/logs/pids.txt"

# Keep script running to allow easy termination
echo -e "${BLUE}Services are running. Press Ctrl+C to stop all services.${NC}"

# Handle termination
trap 'echo -e "${RED}Stopping all services...${NC}"; kill $(cat "$PROJECT_ROOT/logs/pids.txt") 2>/dev/null; rm "$PROJECT_ROOT/logs/pids.txt"; echo -e "${GREEN}All services stopped${NC}"; exit 0' INT TERM

# Wait indefinitely
while true; do
    sleep 1
done 