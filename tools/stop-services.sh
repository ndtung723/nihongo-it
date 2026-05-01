#!/bin/bash

# Nihongo-IT - Service Termination Script
# This script stops all running services (Docker, Python, Vue.js, Spring Boot)

# Define colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get absolute path to project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Parse command line arguments
KEEP_DB=false

# Function to show help
show_help() {
    echo -e "${BLUE}Usage:${NC}"
    echo -e "  $0 [option]"
    echo -e ""
    echo -e "${BLUE}Options:${NC}"
    echo -e "  -h, --help       Show this help message"
    echo -e "  -k, --keep-db    Keep database services running (postgres + pgadmin)"
    echo -e ""
}

# Parse command line arguments
for arg in "$@"; do
    case $arg in
        -h|--help)
            show_help
            exit 0
            ;;
        -k|--keep-db)
            KEEP_DB=true
            ;;
        *)
            echo -e "${RED}Unknown option: $arg${NC}"
            show_help
            exit 1
            ;;
    esac
done

echo -e "${BLUE}=== Nihongo-IT - Service Termination ===${NC}"

# Stop Docker services if running
if [ -f "$PROJECT_ROOT/docker/docker-compose.yaml" ]; then
    echo -e "${YELLOW}Checking Docker services...${NC}"

    if docker ps | grep -q "nihongo-it"; then
        if [ "$KEEP_DB" = true ]; then
            echo -e "${YELLOW}Stopping all Docker services except database...${NC}"
            cd "$PROJECT_ROOT/docker"
            docker compose stop eureka-server api-gateway user-service ai-service
            
            echo -e "${GREEN}Docker services stopped (database services kept running).${NC}"
            echo -e "${YELLOW}Database services still running:${NC}"
            docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E 'nihongo-it-postgres|nihongo-it-pgadmin'
        else
            echo -e "${YELLOW}Stopping all Docker services...${NC}"
            cd "$PROJECT_ROOT/docker"
            docker compose down
            echo -e "${GREEN}All Docker services stopped.${NC}"
        fi
    else
        echo -e "${GREEN}No Docker services running.${NC}"
    fi
fi

# Stop Spring Boot services if pids file exists
if [ -f "$PROJECT_ROOT/logs/spring_pids.txt" ]; then
    echo -e "${YELLOW}Stopping Spring Boot services...${NC}"
    kill $(cat "$PROJECT_ROOT/logs/spring_pids.txt") 2>/dev/null
    rm "$PROJECT_ROOT/logs/spring_pids.txt"
    echo -e "${GREEN}Spring Boot services stopped.${NC}"
else
    echo -e "${GREEN}No Spring Boot services running with PID file.${NC}"
fi

# Stop Python and Vue.js services if pids file exists
if [ -f "$PROJECT_ROOT/logs/pids.txt" ]; then
    echo -e "${YELLOW}Stopping Python and Vue.js services...${NC}"
    kill $(cat "$PROJECT_ROOT/logs/pids.txt") 2>/dev/null
    rm "$PROJECT_ROOT/logs/pids.txt"
    echo -e "${GREEN}Python and Vue.js services stopped.${NC}"
else
    echo -e "${GREEN}No Python/Vue.js services running with PID file.${NC}"
fi

# Function to kill process on specific port
kill_port() {
    local port=$1
    local name=$2
    local pid=$(lsof -ti:$port 2>/dev/null)
    if [ -n "$pid" ]; then
        echo -e "${YELLOW}Found $name service running on port $port (PID: $pid). Stopping it...${NC}"
        kill -9 $pid 2>/dev/null
        echo -e "${GREEN}$name service on port $port stopped.${NC}"
        return 0
    fi
    return 1
}

# Kill services by port if they're still running
echo -e "${YELLOW}Checking for any remaining services by port...${NC}"
kill_port 8761 "Eureka Server" || echo -e "${GREEN}No service on port 8761.${NC}"
kill_port 8080 "API Gateway" || echo -e "${GREEN}No service on port 8080.${NC}"
kill_port 8086 "User Service" || echo -e "${GREEN}No service on port 8086.${NC}"
kill_port 8087 "AI Service" || echo -e "${GREEN}No service on port 8087.${NC}"
kill_port 8000 "Python API" || echo -e "${GREEN}No service on port 8000.${NC}"
kill_port 5173 "Vue.js" || echo -e "${GREEN}No service on port 5173.${NC}"

if [ "$KEEP_DB" = true ]; then
    echo -e "${GREEN}All services have been stopped except database services.${NC}"
    echo -e "${YELLOW}To stop database services later, run: $0${NC}"
else
    echo -e "${GREEN}All services have been stopped.${NC}"
fi 