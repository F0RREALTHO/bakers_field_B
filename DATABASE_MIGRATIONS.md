# Database Migration Guide

## Production Deployment

### Why `ddl-auto: validate` is Critical

The `spring.jpa.hibernate.ddl-auto` setting controls how Hibernate handles your database schema on startup:

| Setting | Behavior | Risk |
|---------|----------|------|
| `update` | Auto-modifies schema on startup | 🔴 HIGH - Can corrupt data, drop columns, cause race conditions |
| `create-drop` | Drops & recreates on startup | 🔴 CRITICAL - Wipes all data every restart |
| `create` | Creates schema from scratch | 🔴 CRITICAL - Fails if schema exists |
| `validate` | Only checks schema matches entities | ✅ SAFE - Fails startup if mismatch (correct behavior) |
| `none` | Does nothing | ✅ SAFE - Manual migrations via Flyway/Liquibase |

### Current Configuration

**Production (`application.yml`):**
```yaml
spring.jpa.hibernate.ddl-auto: validate  # ✅ Safe - requires manual migrations
```

**Local Development (`application-local.yml`):**
```yaml
spring.jpa.hibernate.ddl-auto: update  # ✓ OK for dev - auto-creates tables
```

### How to Deploy Database Changes to Production

Since `ddl-auto: validate` requires the schema to exist and match, you have two options:

#### Option 1: Flyway Migrations (Recommended)
1. Add Flyway dependency to `pom.xml`:
   ```xml
   <dependency>
     <groupId>org.flywaydb</groupId>
     <artifactId>flyway-core</artifactId>
   </dependency>
   ```

2. Create migration files in `src/main/resources/db/migration/`:
   ```
   V1__Initial_schema.sql
   V2__Add_reviews_table.sql
   V3__Add_column_to_products.sql
   ```

3. Flyway automatically runs on startup (before Hibernate validates)

#### Option 2: Manual Schema Updates
1. Deploy code changes
2. Manually run SQL scripts against production database
3. Restart application (Hibernate validates schema matches)

### Startup Validation Flow
```
Application Starts
    ↓
Flyway runs migrations (if using Flyway)
    ↓
Hibernate validates schema matches entities
    ↓
✅ Success: Schema matches code
✗ Error: Schema mismatch - Fix and restart
```

### Never Use `update` in Production
- Multiple servers can race to modify schema simultaneously
- Risk of data loss or corruption
- Silent failures with no way to rollback
- Breaks zero-downtime deployments

### Testing Schema Changes Locally
```bash
# Start with update mode (local development)
# Verify schema changes work correctly
# Generate migration script from changes
# Test migration with validate mode
# Deploy prepared migration to production
```
