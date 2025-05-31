#!/bin/bash

# Script to backup PostgreSQL database from Docker container

# Define colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get absolute path to project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Create backup directory if it doesn't exist
BACKUP_DIR="$PROJECT_ROOT/db-backups"
mkdir -p "$BACKUP_DIR"

# Generate timestamp for backup filename
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/nihongo_it_backup_$TIMESTAMP.sql"

echo -e "${BLUE}=== Nihongo-IT - PostgreSQL Backup ===${NC}"
echo -e "${YELLOW}Creating backup at: $BACKUP_FILE${NC}"

# Check if container is running
if ! docker ps | grep -q nihongo-it-postgres; then
    echo -e "${RED}ERROR: PostgreSQL container (nihongo-it-postgres) is not running${NC}"
    exit 1
fi

# Perform database backup
echo -e "${GREEN}Backing up database...${NC}"
docker exec nihongo-it-postgres pg_dump -U postgres NihongoIT > "$BACKUP_FILE"

# Check if backup was successful
if [ $? -eq 0 ] && [ -s "$BACKUP_FILE" ]; then
    echo -e "${GREEN}Backup completed successfully!${NC}"
    echo -e "${BLUE}Backup file:${NC} $BACKUP_FILE"
    
    # Remove backups older than 30 days
    find "$BACKUP_DIR" -name "nihongo_it_backup_*.sql" -type f -mtime +30 -exec rm {} \;
    echo -e "${YELLOW}Removed backups older than 30 days${NC}"
else
    echo -e "${RED}Backup failed or created empty file${NC}"
    rm -f "$BACKUP_FILE"  # Remove empty backup file
    exit 1
fi

echo -e "${GREEN}Backup process completed.${NC}" 