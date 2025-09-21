# Backup Data Directory

This directory is intended to hold the MySQL backup files for database operations and migration.

## How to Use

1. **Place your MySQL backup files here**:
   - SQL dump files (`.sql`)
   - Compressed dumps (`.sql.gz`, `.sql.bz2`)
   - Directory with individual table dumps

2. **Backup File Format Expected**:
   ```
   backup_data/
   ├── sample_database.sql          # Full database dump
   ├── tables/                      # Individual table dumps
   │   ├── users.sql
   │   ├── projects.sql
   │   ├── cards.sql
   │   └── ...
   └── compressed/                  # Compressed backups
       └── sample_database.sql.gz
   ```

3. **Loading Data**:

   After starting the MySQL container, you can load your backup data:

   ```bash
   # For SQL dump files
   docker exec -i mysql-source-db mysql -u migration_user -pmigration_pass sample_database < backup_data/sample_database.sql

   # For compressed files
   zcat backup_data/sample_database.sql.gz | docker exec -i mysql-source-db mysql -u migration_user -pmigration_pass sample_database
   ```

4. **Automatic Loading**:

   You can also create an initialization script to automatically load your data:

   ```bash
   # Create a script in docker/init/
   # Files in init/ are executed in alphabetical order during container startup
   ```

## Migration Process

Once your backup data is loaded:

1. **Start the MySQL MCP Server**:
   ```bash
   cd mysql-mcp-server
   docker-compose up -d
   ```

2. **Connect to the database**:
   - Use the MySQL MCP Server tools to connect to `mysql-source-db:3306`
   - Database: `sample_database`
   - User: `migration_user`
   - Password: `migration_pass`

3. **Analyze and migrate**:
   - Use LLM with both PostgreSQL and MySQL MCP Servers
   - Analyze source schema structure
   - Transform data to target PostgreSQL schema
   - Execute intelligent data migration and transformation

## Notes

- The directory is mounted as read-only in the container for security
- Ensure your backup files have proper permissions
- Large files may need streaming/chunked processing during migration
- Test with sample data first before running full migration