#!/bin/bash

# Nihongo-IT - Spring Boot Services Startup Script
# This script starts all the Spring Boot services in the project without Docker

# Define colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get absolute path to project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}=== Nihongo-IT - Spring Boot Services Starter ===${NC}"
echo -e "${YELLOW}Project root: $PROJECT_ROOT${NC}"

# Cleanup function to stop all services
cleanup() {
    echo -e "${RED}Stopping all services...${NC}"
    if [ -f "$PROJECT_ROOT/logs/spring_pids.txt" ]; then
        kill $(cat "$PROJECT_ROOT/logs/spring_pids.txt") 2>/dev/null
        rm "$PROJECT_ROOT/logs/spring_pids.txt"
    fi
    echo -e "${GREEN}All services stopped${NC}"
    exit 0
}

# Handle termination signals
trap cleanup INT TERM

# Check if services are already running and stop them if needed
if [ -f "$PROJECT_ROOT/logs/spring_pids.txt" ]; then
    echo -e "${YELLOW}Found existing services. Stopping them before starting new ones...${NC}"
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

# Check required ports
check_port 8761  # Eureka Server
check_port 8080  # API Gateway
check_port 8086  # User Service
check_port 8087  # AI Service
check_port 8088  # Learning Service
check_port 8089  # Notification Service

# Create log directory
mkdir -p "$PROJECT_ROOT/logs"

# Go to services directory
cd "$PROJECT_ROOT/services"

# Build and start Eureka Server
echo -e "${GREEN}Building and starting Eureka Server...${NC}"
cd "$PROJECT_ROOT/services/eureka-server"
../gradlew bootRun > "$PROJECT_ROOT/logs/eureka-server.log" 2>&1 &
EUREKA_PID=$!
echo -e "${GREEN}Eureka Server started with PID: $EUREKA_PID${NC}"

# Give Eureka Server time to start
echo -e "${YELLOW}Waiting for Eureka Server to initialize...${NC}"
sleep 10

# Build and start API Gateway
echo -e "${GREEN}Building and starting API Gateway...${NC}"
cd "$PROJECT_ROOT/services/api-gateway"
../gradlew bootRun > "$PROJECT_ROOT/logs/api-gateway.log" 2>&1 &
API_GATEWAY_PID=$!
echo -e "${GREEN}API Gateway started with PID: $API_GATEWAY_PID${NC}"

# Build and start User Service
echo -e "${GREEN}Building and starting User Service...${NC}"
cd "$PROJECT_ROOT/services/user-service"
../gradlew bootRun > "$PROJECT_ROOT/logs/user-service.log" 2>&1 &
USER_SERVICE_PID=$!
echo -e "${GREEN}User Service started with PID: $USER_SERVICE_PID${NC}"

# Build and start AI Service
echo -e "${GREEN}Building and starting AI Service...${NC}"
cd "$PROJECT_ROOT/services/ai-service"
../gradlew bootRun > "$PROJECT_ROOT/logs/ai-service.log" 2>&1 &
AI_SERVICE_PID=$!
echo -e "${GREEN}AI Service started with PID: $AI_SERVICE_PID${NC}"

# Build and start Learning Service
echo -e "${GREEN}Building and starting Learning Service...${NC}"
cd "$PROJECT_ROOT/services/learning-service"
../gradlew bootRun > "$PROJECT_ROOT/logs/learning-service.log" 2>&1 &
LEARNING_SERVICE_PID=$!
echo -e "${GREEN}Learning Service started with PID: $LEARNING_SERVICE_PID${NC}"

# Build and start Notification Service
echo -e "${GREEN}Building and starting Notification Service...${NC}"
cd "$PROJECT_ROOT/services/notification-service"
../gradlew bootRun > "$PROJECT_ROOT/logs/notification-service.log" 2>&1 &
NOTIFICATION_SERVICE_PID=$!
echo -e "${GREEN}Notification Service started with PID: $NOTIFICATION_SERVICE_PID${NC}"

# Save PIDs to file for later termination
echo "$EUREKA_PID $API_GATEWAY_PID $USER_SERVICE_PID $AI_SERVICE_PID $LEARNING_SERVICE_PID $NOTIFICATION_SERVICE_PID" > "$PROJECT_ROOT/logs/spring_pids.txt"

# Show status information
echo -e "\n${BLUE}=== Services Information ===${NC}"
echo -e "${GREEN}Eureka Server:${NC} http://localhost:8761"
echo -e "${GREEN}API Gateway:${NC} http://localhost:8080"
echo -e "${GREEN}User Service:${NC} http://localhost:8086"
echo -e "${GREEN}AI Service:${NC} http://localhost:8087"
echo -e "${GREEN}Learning Service:${NC} http://localhost:8088"
echo -e "${GREEN}Notification Service:${NC} http://localhost:8089"
echo -e "\n${YELLOW}Log files are available in the logs directory${NC}"
echo -e "${YELLOW}PIDs: Eureka=$EUREKA_PID, API Gateway=$API_GATEWAY_PID, User Service=$USER_SERVICE_PID, AI Service=$AI_SERVICE_PID, Learning Service=$LEARNING_SERVICE_PID, Notification Service=$NOTIFICATION_SERVICE_PID${NC}"
echo -e "\n${RED}To stop all services, press Ctrl+C or run: tools/stop-services.sh${NC}"

# Keep script running to allow easy termination
echo -e "${BLUE}Services are running. Press Ctrl+C to stop all services.${NC}"

# Wait indefinitely
while true; do
    # Check if services are still running every 5 seconds
    for pid in $EUREKA_PID $API_GATEWAY_PID $USER_SERVICE_PID $AI_SERVICE_PID $LEARNING_SERVICE_PID $NOTIFICATION_SERVICE_PID; do
        if ! ps -p $pid > /dev/null; then
            echo -e "${RED}Service with PID $pid has stopped unexpectedly.${NC}"
            echo -e "${YELLOW}Stopping all remaining services...${NC}"
            cleanup
        fi
    done
    sleep 5
done 