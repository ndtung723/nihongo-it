#!/bin/bash

# Nihongo-IT - Database Information Tool
# This script shows detailed information about the PostgreSQL database in Docker

# Define colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get absolute path to project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}=== Nihongo-IT - Database Information ===${NC}"

# Check if PostgreSQL container is running
if ! docker ps | grep -q "nihongo-it-postgres"; then
    echo -e "${RED}ERROR: PostgreSQL container (nihongo-it-postgres) is not running!${NC}"
    echo -e "${YELLOW}Use './tools/start-db.sh' to start the database services first.${NC}"
    exit 1
fi

# Function to show help
show_help() {
    echo -e "${BLUE}Usage:${NC}"
    echo -e "  $0 [option]"
    echo -e ""
    echo -e "${BLUE}Options:${NC}"
    echo -e "  -h, --help       Show this help message"
    echo -e "  -v, --verbose    Show detailed information (environment variables, volumes, etc.)"
    echo -e ""
}

# Parse command line arguments
VERBOSE=false

for arg in "$@"; do
    case $arg in
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--verbose)
            VERBOSE=true
            ;;
        *)
            echo -e "${RED}Unknown option: $arg${NC}"
            show_help
            exit 1
            ;;
    esac
done

# Get PostgreSQL container IP
DB_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' nihongo-it-postgres)
DB_NETWORK=$(docker inspect -f '{{range $key, $value := .NetworkSettings.Networks}}{{$key}}{{end}}' nihongo-it-postgres)

echo -e "${YELLOW}Database Container Information:${NC}"
echo -e "${GREEN}Name:${NC} nihongo-it-postgres"
echo -e "${GREEN}Status:${NC} $(docker inspect -f '{{.State.Status}}' nihongo-it-postgres)"
echo -e "${GREEN}IP Address:${NC} ${DB_IP}"
echo -e "${GREEN}Network:${NC} ${DB_NETWORK}"
echo -e "${GREEN}Port Mapping:${NC} $(docker port nihongo-it-postgres 5432)"

# Get database size information
echo -e "\n${YELLOW}Database Size Information:${NC}"
docker exec nihongo-it-postgres psql -U postgres -c "SELECT pg_size_pretty(pg_database_size('NihongoIT')) AS db_size;"

# List available databases
echo -e "\n${YELLOW}Available Databases:${NC}"
docker exec nihongo-it-postgres psql -U postgres -c "SELECT datname FROM pg_database WHERE datistemplate = false;"

# If verbose, show more details
if [ "$VERBOSE" = true ]; then
    echo -e "\n${YELLOW}Environment Variables:${NC}"
    docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' nihongo-it-postgres | sort
    
    echo -e "\n${YELLOW}Volume Mounts:${NC}"
    docker inspect -f '{{range .Mounts}}{{.Source}} -> {{.Destination}}{{println}}{{end}}' nihongo-it-postgres
    
    echo -e "\n${YELLOW}Network Information:${NC}"
    docker inspect -f '{{json .NetworkSettings.Networks}}' nihongo-it-postgres | python3 -m json.tool
    
    echo -e "\n${YELLOW}Listening Ports Inside Container:${NC}"
    docker exec nihongo-it-postgres netstat -tln | grep -E '(Proto|LISTEN)'
fi

echo -e "\n${BLUE}Connection String Examples:${NC}"
echo -e "${GREEN}JDBC:${NC} jdbc:postgresql://${DB_IP}:5432/NihongoIT"
echo -e "${GREEN}psql command:${NC} psql -h ${DB_IP} -p 5432 -U postgres -d NihongoIT"
echo -e "${GREEN}Host connection:${NC} psql -h localhost -p 5432 -U postgres -d NihongoIT"

echo -e "\n${BLUE}pgAdmin Connection Information:${NC}"
PGADMIN_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' nihongo-it-pgadmin)
echo -e "${GREEN}Web Interface:${NC} http://localhost:5050"
echo -e "${GREEN}Container IP:${NC} ${PGADMIN_IP}"

echo -e "\n${YELLOW}Note:${NC} When connecting from microservices within Docker network, use the container IP (${DB_IP})"
echo -e "      When connecting from host machine, use 'localhost'" 