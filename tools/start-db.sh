#!/bin/bash

# Nihongo-IT - Database Services Starter
# This script starts only database services (PostgreSQL + pgAdmin)

# Define colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get absolute path to project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}=== Nihongo-IT - Database Services Starter ===${NC}"
echo -e "${YELLOW}Project root: $PROJECT_ROOT${NC}"

# Check if docker-compose.yaml exists
if [ ! -f "$PROJECT_ROOT/docker/docker-compose.yaml" ]; then
    echo -e "${RED}ERROR: docker/docker-compose.yaml not found${NC}"
    exit 1
fi

# Check if database services are already running
if docker ps | grep -qE 'nihongo-it-postgres|nihongo-it-pgadmin'; then
    echo -e "${YELLOW}Database services are already running:${NC}"
    docker ps --format "table {{.Names}}\t{{.Ports}}\t{{.Status}}" | grep -E 'nihongo-it-postgres|nihongo-it-pgadmin'
    echo -e "${GREEN}No action needed.${NC}"
    exit 0
fi

# Start database services
echo -e "${GREEN}Starting database services (PostgreSQL + pgAdmin)...${NC}"
cd "$PROJECT_ROOT/docker"
docker compose up -d postgres pgadmin

# Check if services started successfully
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Database services started successfully.${NC}"
    echo -e "${YELLOW}Services running:${NC}"
    docker ps --format "table {{.Names}}\t{{.Ports}}\t{{.Status}}" | grep -E 'nihongo-it-postgres|nihongo-it-pgadmin'
    
    echo -e "${BLUE}PostgreSQL connection information:${NC}"
    echo -e "  Host: localhost"
    echo -e "  Port: 5432"
    echo -e "  Database: NihongoIT"
    echo -e "  Username: postgres"
    echo -e "  Password: root (or value of DB_PASSWORD environment variable)"
    
    echo -e "${BLUE}pgAdmin connection information:${NC}"
    echo -e "  URL: http://localhost:5050"
    echo -e "  Email: admin@admin.com (or value of PGADMIN_DEFAULT_EMAIL environment variable)"
    echo -e "  Password: root (or value of PGADMIN_DEFAULT_PASSWORD environment variable)"
else
    echo -e "${RED}Failed to start database services.${NC}"
    exit 1
fi 