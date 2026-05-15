@echo off
setlocal EnableDelayedExpansion

:: Jira Platform - Safe Database Migration Script
:: Runs ALL migrations - skips if already applied

title Jira Platform - Running Migrations

echo.
echo  +============================================================+
echo  :                                                            :
echo  :          JIRA PLATFORM - DATABASE MIGRATIONS               :
echo  :                                                            :
echo  +============================================================+
echo.

set "DB_HOST=localhost"
set "DB_PORT=5432"
set "DB_USER=jiraadmin"
set "DB_PASS=UNIpay@123"
set "PROJECT_ROOT=%~dp0"

:: Some databases are owned by postgres, not jiraadmin
set "DB_USER_AUTH=postgres"
set "DB_USER_JIRA=jiraadmin"

set TOTAL=0
set SUCCESS=0
set SKIPPED=0
set FAILED=0

goto :main

:run_migration
set "DB=%~1"
set "MIGRATION=%~2"
set "DESC=%~3"
set "DBUSER=%~4"

if "!DBUSER!"=="" set "DBUSER=%DB_USER%"

set /a TOTAL+=1
echo     Running %DESC%...

:: Run psql with password
set PGPASSWORD=%DB_PASS%
psql -U !DBUSER! -d %DB% -h %DB_HOST% -p %DB_PORT% -f "%MIGRATION%" > "%TEMP%\psql_out.txt" 2>&1

:: Check result
findstr /C:"already exists" /C:"duplicate key" /C:"skipping" /C:"NOTICE" /C:"CREATE TABLE" /C:"CREATE INDEX" /C:"CREATE SCHEMA" /C:"CREATE EXTENSION" /C:"INSERT" /C:"CREATE TYPE" /C:"ERROR:" "%TEMP%\psql_out.txt" | findstr /C:"ERROR:" >nul 2>&1
if !errorlevel! equ 0 (
    :: Check if only NOTICE errors (which are ok)
    findstr /C:"NOTICE" "%TEMP%\psql_out.txt" >nul 2>&1
    if !errorlevel! equ 0 (
        echo     [OK] (with notices)
        set /a SUCCESS+=1
    ) else (
        type "%TEMP%\psql_out.txt"
        set /a FAILED+=1
    )
) else (
    echo     [OK]
    set /a SUCCESS+=1
)

del "%TEMP%\psql_out.txt" 2>nul
exit /b 0

:main

echo [INFO] Starting database migrations...
echo.

:: =================================================================
:: ADMIN SERVICE - admin_db (owned by postgres)
:: =================================================================
echo --- admin-service: admin_db ---
call :run_migration "admin_db" "%PROJECT_ROOT%jira-admin-service\src\main\resources\db\migration\V1__admin_service_initial.sql" "V1__admin_service_initial.sql" "%DB_USER_AUTH%"
echo.

:: =================================================================
:: ATTACHMENT SERVICE - attachment_db
:: =================================================================
echo --- attachment-service: attachment_db ---
call :run_migration "attachment_db" "%PROJECT_ROOT%jira-attachment-service\src\main\resources\db\migration\V1__init.sql" "V1__init.sql" "%DB_USER_JIRA%"
echo.

:: =================================================================
:: AUDIT SERVICE - audit_db
:: =================================================================
echo --- audit-service: audit_db ---
call :run_migration "audit_db" "%PROJECT_ROOT%jira-audit-service\src\main\resources\db\migration\V1__init.sql" "V1__init.sql" "%DB_USER_JIRA%"
echo.

:: =================================================================
:: AUTH SERVICE - auth_db (owned by postgres)
:: =================================================================
echo --- auth-service: auth_db ---
call :run_migration "auth_db" "%PROJECT_ROOT%jira-auth-service\src\main\resources\db\migration\V1__init.sql" "V1__init.sql" "%DB_USER_AUTH%"
echo.

:: =================================================================
:: COMMENT SERVICE - comment_db
:: =================================================================
echo --- comment-service: comment_db ---
call :run_migration "comment_db" "%PROJECT_ROOT%jira-comment-service\src\main\resources\db\migration\V1__init.sql" "V1__init.sql" "%DB_USER_JIRA%"
echo.

:: =================================================================
:: ISSUE SERVICE - issue_db
:: =================================================================
echo --- issue-service: issue_db ---
call :run_migration "issue_db" "%PROJECT_ROOT%jira-issue-service\src\main\resources\db\migration\V1__init.sql" "V1__init.sql" "%DB_USER_JIRA%"
call :run_migration "issue_db" "%PROJECT_ROOT%jira-issue-service\src\main\resources\db\migration\V2__add_worklogs_links_labels_history.sql" "V2__add_worklogs_links_labels_history.sql" "%DB_USER_JIRA%"
call :run_migration "issue_db" "%PROJECT_ROOT%jira-issue-service\src\main\resources\db\migration\V3__issue_types_and_schemes.sql" "V3__issue_types_and_schemes.sql" "%DB_USER_JIRA%"
call :run_migration "issue_db" "%PROJECT_ROOT%jira-issue-service\src\main\resources\db\migration\V4__enhanced_issues.sql" "V4__enhanced_issues.sql" "%DB_USER_JIRA%"
echo.

:: =================================================================
:: MIGRATION SERVICE - migration_db
:: =================================================================
echo --- migration-service: migration_db ---
call :run_migration "migration_db" "%PROJECT_ROOT%jira-migration-service\src\main\resources\db\migration\V1__init_migration_service.sql" "V1__init_migration_service.sql" "%DB_USER_JIRA%"
echo.

:: =================================================================
:: NOTIFICATION SERVICE - notification_db
:: =================================================================
echo --- notification-service: notification_db ---
call :run_migration "notification_db" "%PROJECT_ROOT%jira-notification-service\src\main\resources\db\migration\V1__init.sql" "V1__init.sql" "%DB_USER_JIRA%"
echo.

:: =================================================================
:: PLAN SERVICE - plan_db (13 migrations)
:: =================================================================
echo --- plan-service: plan_db ---
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V1__create_program_schema.sql" "V1__create_program_schema.sql" "%DB_USER_JIRA%"
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V2__create_plan_schema.sql" "V2__create_plan_schema.sql" "%DB_USER_JIRA%"
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V3__create_program_plan_table.sql" "V3__create_program_plan_table.sql" "%DB_USER_JIRA%"
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V4__create_plan_items_table.sql" "V4__create_plan_items_table.sql" "%DB_USER_JIRA%"
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V5__create_plan_teams_table.sql" "V5__create_plan_teams_table.sql" "%DB_USER_JIRA%"
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V6__create_plan_releases_table.sql" "V6__create_plan_releases_table.sql" "%DB_USER_JIRA%"
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V7__create_issue_dependencies_table.sql" "V7__create_issue_dependencies_table.sql" "%DB_USER_JIRA%"
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V8__create_plan_warnings_table.sql" "V8__create_plan_warnings_table.sql" "%DB_USER_JIRA%"
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V9__create_lexorank_schema.sql" "V9__create_lexorank_schema.sql" "%DB_USER_JIRA%"
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V10__create_working_days_schema.sql" "V10__create_working_days_schema.sql" "%DB_USER_JIRA%"
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V11__create_board_config_schema.sql" "V11__create_board_config_schema.sql" "%DB_USER_JIRA%"
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V12__create_sprint_schema.sql" "V12__create_sprint_schema.sql" "%DB_USER_JIRA%"
call :run_migration "plan_db" "%PROJECT_ROOT%jira-plan-service\src\main\resources\db\migration\V13__create_permissions_schema.sql" "V13__create_permissions_schema.sql" "%DB_USER_JIRA%"
echo.

:: =================================================================
:: PROJECT SERVICE - project_db
:: =================================================================
echo --- project-service: project_db ---
call :run_migration "project_db" "%PROJECT_ROOT%jira-project-service\src\main\resources\db\migration\V1__init.sql" "V1__init.sql" "%DB_USER_JIRA%"
call :run_migration "project_db" "%PROJECT_ROOT%jira-project-service\src\main\resources\db\migration\V2__project_types_schemes_templates.sql" "V2__project_types_schemes_templates.sql" "%DB_USER_JIRA%"
call :run_migration "project_db" "%PROJECT_ROOT%jira-project-service\src\main\resources\db\migration\V3__enhanced_permissions.sql" "V3__enhanced_permissions.sql" "%DB_USER_JIRA%"
echo.

:: =================================================================
:: SEARCH SERVICE - search_db
:: =================================================================
echo --- search-service: search_db ---
call :run_migration "search_db" "%PROJECT_ROOT%jira-search-service\src\main\resources\db\migration\V1__init.sql" "V1__init.sql" "%DB_USER_JIRA%"
echo.

:: =================================================================
:: SPRINT SERVICE - sprint_db
:: =================================================================
echo --- sprint-service: sprint_db ---
call :run_migration "sprint_db" "%PROJECT_ROOT%jira-sprint-service\src\main\resources\db\migration\V1__create_sprints.sql" "V1__create_sprints.sql" "%DB_USER_JIRA%"
call :run_migration "sprint_db" "%PROJECT_ROOT%jira-sprint-service\src\main\resources\db\migration\V5__agile_boards_service.sql" "V5__agile_boards_service.sql" "%DB_USER_JIRA%"
echo.

:: =================================================================
:: USER SERVICE - user_db (owned by postgres)
:: =================================================================
echo --- user-service: user_db ---
call :run_migration "user_db" "%PROJECT_ROOT%jira-user-service\src\main\resources\db\migration\V1__init.sql" "V1__init.sql" "%DB_USER_AUTH%"
echo.

:: =================================================================
:: WORKFLOW SERVICE - workflow_db
:: =================================================================
echo --- workflow-service: workflow_db ---
call :run_migration "workflow_db" "%PROJECT_ROOT%jira-workflow-service\src\main\resources\db\migration\V1__init.sql" "V1__init.sql" "%DB_USER_JIRA%"
call :run_migration "workflow_db" "%PROJECT_ROOT%jira-workflow-service\src\main\resources\db\migration\V2__default_workflows.sql" "V2__default_workflows.sql" "%DB_USER_JIRA%"
echo.

:: =================================================================
:: SUMMARY
:: =================================================================
echo  +============================================================+
echo  :                     MIGRATION SUMMARY                        :
echo  +============================================================+
echo.
echo   Total migrations attempted: !TOTAL!
echo   Successful: !SUCCESS!
echo   Skipped (already applied): !SKIPPED!
echo   Failed: !FAILED!
echo.
if !FAILED! equ 0 (
    echo   [SUCCESS] All migrations completed or already applied!
    echo.
    echo   Now run: start-platform.bat
) else (
    echo   [WARNING] Some migrations had issues
)
echo.

pause