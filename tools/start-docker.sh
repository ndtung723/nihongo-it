#!/bin/bash

# Nihongo-IT - Docker Service Starter
# This script starts Docker services defined in docker-compose.yaml

# Define colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get absolute path to project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}=== Nihongo-IT - Docker Service Starter ===${NC}"
echo -e "${YELLOW}Project root: $PROJECT_ROOT${NC}"

# Check if docker-compose.yaml exists
if [ ! -f "$PROJECT_ROOT/docker/docker-compose.yaml" ]; then
    echo -e "${RED}ERROR: docker/docker-compose.yaml not found${NC}"
    exit 1
fi

# Function to show help
show_help() {
    echo -e "${BLUE}Usage:${NC}"
    echo -e "  $0 [option]"
    echo -e ""
    echo -e "${BLUE}Options:${NC}"
    echo -e "  -h, --help       Show this help message"
    echo -e "  -a, --all        Start all services (default)"
    echo -e "  -d, --db         Start only database services (postgres + pgadmin)"
    echo -e "  -s, --services   Start only microservices (eureka + api-gateway + user-service + ai-service)"
    echo -e "  -r, --rebuild    Force rebuild the Docker images"
    echo -e "  -b, --build-only Build all Docker images without starting services"
    echo -e "  --build-ms       Build only microservice images without starting them"
}

# Export PWD variable for docker compose context paths
export PWD="$PROJECT_ROOT/docker"

# Parse command line arguments
REBUILD=""

if [[ "$1" == "-r" || "$1" == "--rebuild" ]]; then
    REBUILD="--build"
    shift
fi

case "$1" in
    -h|--help)
        show_help
        exit 0
        ;;
    -a|--all|"")
        echo -e "${GREEN}Starting all Docker services...${NC}"
        cd "$PROJECT_ROOT/docker"
        docker compose up -d $REBUILD
        ;;
    -d|--db)
        echo -e "${GREEN}Starting only database services (postgres + pgadmin)...${NC}"
        cd "$PROJECT_ROOT/docker"
        docker compose up -d postgres pgadmin
        ;;
    -s|--services)
        echo -e "${GREEN}Starting only microservices...${NC}"
        cd "$PROJECT_ROOT/docker"
        docker compose up -d eureka-server api-gateway user-service ai-service notification-service learning-service $REBUILD
        ;;
    -b|--build-only)
        echo -e "${GREEN}Building all Docker images without starting services...${NC}"
        cd "$PROJECT_ROOT/docker"
        docker compose build
        echo -e "${GREEN}All Docker images have been built.${NC}"
        echo -e "${YELLOW}Use $0 to start services with newly built images.${NC}"
        exit 0
        ;;
    --build-ms)
        echo -e "${GREEN}Building only microservice images without starting them...${NC}"
        cd "$PROJECT_ROOT/docker"
        docker compose build eureka-server api-gateway user-service ai-service notification-service learning-service $REBUILD
        echo -e "${GREEN}Microservice images have been built.${NC}"
        echo -e "${YELLOW}Use $0 -s to start microservices with newly built images.${NC}"
        exit 0
        ;;
    *)
        echo -e "${RED}Unknown option: $1${NC}"
        show_help
        exit 1
        ;;
esac

echo -e "${GREEN}Docker services started successfully.${NC}"
echo -e "${YELLOW}To check service status, run: docker compose ps${NC}"
echo -e "${RED}To stop all services, run: tools/stop-services.sh${NC}" 