#!/bin/bash

# Script to restore PostgreSQL database from backup file

# Define colors for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get absolute path to project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Function to show help
show_help() {
    echo -e "${BLUE}Usage:${NC}"
    echo -e "  $0 <backup_file>"
    echo -e ""
    echo -e "${BLUE}Example:${NC}"
    echo -e "  $0 db-backups/nihongo_it_backup_20230520_120000.sql"
    echo -e ""
    echo -e "${YELLOW}Note: This will overwrite the current database!${NC}"
}

# Check if backup file is provided
if [ $# -eq 0 ]; then
    echo -e "${RED}ERROR: Backup file not specified${NC}"
    show_help
    exit 1
fi

BACKUP_FILE="$1"

# Check if the backup file exists
if [ ! -f "$BACKUP_FILE" ]; then
    echo -e "${RED}ERROR: Backup file not found: $BACKUP_FILE${NC}"
    exit 1
fi

echo -e "${BLUE}=== Nihongo-IT - PostgreSQL Restore ===${NC}"
echo -e "${YELLOW}Restoring from: $BACKUP_FILE${NC}"

# Check if container is running
if ! docker ps | grep -q nihongo-it-postgres; then
    echo -e "${RED}ERROR: PostgreSQL container (nihongo-it-postgres) is not running${NC}"
    exit 1
fi

# Ask for confirmation
echo -e "${RED}WARNING: This will overwrite the current database. All existing data will be lost!${NC}"
read -p "Are you sure you want to proceed? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Restore cancelled.${NC}"
    exit 0
fi

# Prepare the SQL file
TEMP_SQL="/tmp/nihongo_it_restore_temp.sql"
if [[ "$BACKUP_FILE" == *.gz ]]; then
    echo -e "${GREEN}Decompressing backup file...${NC}"
    gunzip -c "$BACKUP_FILE" > "$TEMP_SQL"
else
    cp "$BACKUP_FILE" "$TEMP_SQL"
fi

# Drop and recreate the database
echo -e "${YELLOW}Dropping existing database...${NC}"
docker exec nihongo-it-postgres psql -U postgres -c "DROP DATABASE IF EXISTS \"NihongoIT\";"
echo -e "${YELLOW}Creating new database...${NC}"
docker exec nihongo-it-postgres psql -U postgres -c "CREATE DATABASE \"NihongoIT\";"

# Restore the database
echo -e "${GREEN}Restoring database...${NC}"
cat "$TEMP_SQL" | docker exec -i nihongo-it-postgres psql -U postgres -d NihongoIT

# Check restore result
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Database restore completed successfully!${NC}"
else
    echo -e "${RED}Database restore failed!${NC}"
    exit 1
fi

# Clean up
rm -f "$TEMP_SQL"
echo -e "${GREEN}Restore process completed.${NC}" 