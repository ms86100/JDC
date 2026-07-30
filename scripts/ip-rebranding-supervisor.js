export const meta = {
  name: 'ip-rebranding-supervisor',
  description: 'Verify and fix all remaining jira references after rebranding, ensure build succeeds',
  phases: [
    { title: 'Audit', detail: 'Scan every service for remaining jira references' },
    { title: 'Fix', detail: 'Fix all remaining references found by audit' },
    { title: 'Build', detail: 'Compile backend + build frontend until clean' },
  ],
}

const PROJECT_ROOT = 'c:/Users/SSHABNSA/Desktop/test/JDC-main'

// =============================================
// PHASE 1: AUDIT — Find ALL remaining "jira" references
// =============================================
phase('Audit')
log('Auditing entire codebase for remaining jira references...')

const auditResult = await agent(`You are a QA auditor. Scan the ENTIRE codebase for any remaining "jira" references that were missed during rebranding.

PROJECT ROOT: ${PROJECT_ROOT}

## What to Check

### 1. Java Files — most critical
Run: grep -ri "com\\.jira" --include="*.java" . | grep -v node_modules | grep -v ".git/" | grep -v "db/migration/V" | head -200
Run: grep -ri "package com\\.jira" --include="*.java" . | grep -v node_modules | grep -v ".git/" | head -100
Run: grep -ri "import com\\.jira" --include="*.java" . | grep -v node_modules | grep -v ".git/" | head -100

### 2. Configuration Files
Run: grep -ri "jira" --include="*.yml" --include="*.yaml" --include="*.properties" . | grep -v node_modules | grep -v ".git/" | grep -v "db/migration/V" | head -100

### 3. POM Files
Run: grep -ri "jira" --include="pom.xml" . | grep -v node_modules | grep -v ".git/" | head -50

### 4. Docker Files
Run: grep -ri "jira" --include="Dockerfile" --include="docker-compose*" --include="*.conf" . | grep -v node_modules | grep -v ".git/" | head -50

### 5. META-INF / Spring Config
Run: grep -ri "com\\.jira" --include="*.imports" --include="*.factories" . | grep -v node_modules | grep -v ".git/" | head -50

### 6. Frontend Files
Run: grep -ri "jira" --include="*.tsx" --include="*.ts" --include="*.css" --include="*.json" jira-frontend/src/ 2>/dev/null | head -100
Run: grep -ri "jira" --include="*.tsx" --include="*.ts" --include="*.css" --include="*.json" avionics-systems-frontend/src/ 2>/dev/null | head -100

### 7. Check folder names
Run: ls -d */ | grep jira
Run: find . -maxdepth 3 -type d -name "*jira*" | grep -v node_modules | grep -v ".git/" | head -50

### 8. Check Java package directories
Run: find . -type d -path "*/com/jira" | grep -v node_modules | grep -v ".git/" | head -50

## Output Format
Report a structured summary:
1. Total remaining jira references found (excluding existing SQL migrations which we never modify)
2. Grouped by type: Java packages, imports, configs, Docker, frontend, folder names
3. For each finding: exact file path and line number
4. Assessment: which ones are critical (will cause build failure) vs cosmetic (comments, descriptions)

Be thorough. Check EVERYTHING.`, {
  label: 'audit:remaining-refs',
  phase: 'Audit',
  effort: 'high'
})

log('Audit complete. Launching fixers...')

// =============================================
// PHASE 2: FIX — Fix everything the audit found
// =============================================
phase('Fix')

const fixResults = await parallel([
  // Fixer 1: Java packages, imports, class names
  () => agent(`You are a senior Java developer. Fix ALL remaining "jira" references in Java source files.

PROJECT ROOT: ${PROJECT_ROOT}

## Context
The codebase was rebranded from "jira" to "avionics-systems" / "avionics_systems" but some references were missed.

## Your job
1. Find ALL Java files that still have "com.avionics_systems" in package declarations:
   grep -rn "package com\\.jira" --include="*.java" . | grep -v node_modules | grep -v ".git/" | grep -v "db/migration/"

2. Find ALL Java files that still have "com.avionics_systems" in imports:
   grep -rn "import com\\.jira" --include="*.java" . | grep -v node_modules | grep -v ".git/"

3. Find ALL Java files that still have "Jira" in class names (as class/interface declarations):
   grep -rn "class Jira\\|interface Jira" --include="*.java" . | grep -v node_modules | grep -v ".git/"

4. Find ALL Java files with jira in @Table annotations:
   grep -rn 'schema.*=.*"jira_' --include="*.java" . | grep -v node_modules | grep -v ".git/"

5. Find ALL Java files with jira in @Value annotations:
   grep -rn '@Value.*jira\\.' --include="*.java" . | grep -v node_modules | grep -v ".git/"

6. Find ALL Java files with jira in string literals:
   grep -rn '".*jira.*"' --include="*.java" . | grep -v node_modules | grep -v ".git/" | grep -v "db/migration/" | grep -v test

For EACH finding:
- Read the file
- Replace com.avionics_systems → com.avionics_systems
- Replace Jira prefix in class names → AvionicsSystem
- Replace jira_ schema → avionics_systems_
- Replace jira. config → avionics-systems.
- Replace "avionics-systems-xxx" strings → "avionics-systems-xxx"

7. Check for Java files that are still in com/jira/ directories:
   find . -type f -name "*.java" -path "*/com/jira/*" | grep -v node_modules | grep -v ".git/"
   If found, move them to com/avionics_systems/ and remove the old directory.

8. Check for Java class files named Jira*.java that need renaming:
   find . -name "Jira*.java" | grep -v node_modules | grep -v ".git/"
   Rename them and update the class declaration inside.

Fix EVERY single one. Do not leave any behind.`, {
    label: 'fix:java',
    phase: 'Fix',
    effort: 'high'
  }),

  // Fixer 2: Config files (YAML, properties, XML)
  () => agent(`You are a senior DevOps engineer. Fix ALL remaining "jira" references in configuration files.

PROJECT ROOT: ${PROJECT_ROOT}

## Your job
1. Find ALL YAML/properties with jira references:
   grep -rn "jira" --include="*.yml" --include="*.yaml" --include="*.properties" . | grep -v node_modules | grep -v ".git/" | grep -v "db/migration/"

2. Find ALL pom.xml with jira references:
   grep -rn "jira" --include="pom.xml" . | grep -v node_modules | grep -v ".git/"

3. Find ALL Docker files with jira references:
   grep -rn "jira" --include="Dockerfile" . | grep -v node_modules | grep -v ".git/"
   grep -rn "jira" --include="docker-compose*" . | grep -v node_modules | grep -v ".git/"

4. Find ALL nginx/config files:
   grep -rn "jira" --include="*.conf" --include="*.json" . | grep -v node_modules | grep -v ".git/" | grep -v "package-lock" | head -100

5. Find ALL Spring auto-configuration files:
   grep -rn "jira" --include="*.imports" --include="*.factories" . | grep -v node_modules | grep -v ".git/"

6. Find ALL logback files:
   grep -rn "jira" --include="logback*" . | grep -v node_modules | grep -v ".git/"

For EACH finding, apply the correct replacement:
- com.avionics_systems → com.avionics_systems
- jira-xxx-service → avionics-systems-xxx-service
- avionics_systems_platform → avionics_systems_platform
- jira_xxx (schema) → avionics_systems_xxx
- avionicsadmin → avionicsadmin
- jira: (config key) → avionics-systems:
- avionics-systems-network → avionics-systems-network
- AVIONICS_SYSTEMS_ (env var) → AVIONICS_SYSTEMS_

Also check root pom.xml module names — they should all be avionics-systems-* not jira-*.

Fix EVERY single one.`, {
    label: 'fix:config',
    phase: 'Fix',
    effort: 'high'
  }),

  // Fixer 3: Folder renames
  () => agent(`You are performing structural renames. Fix any remaining jira-named folders.

PROJECT ROOT: ${PROJECT_ROOT}

## Your job
1. Check if top-level service folders still have "jira" in the name:
   ls -d */ | grep jira

2. If ANY jira-* folders remain, rename them:
   For each: mv jira-xxx avionics-systems-xxx

3. Check that the root pom.xml <module> entries match the actual folder names:
   Read pom.xml, list the <module> entries
   ls the actual directories
   If mismatches exist, update pom.xml to match reality

4. Check for any sub-directories with "jira" in the name:
   find . -maxdepth 4 -type d -name "*jira*" | grep -v node_modules | grep -v ".git/" | grep -v "db/migration"

5. Check for Java package directories still under com/jira/:
   find . -type d -path "*/com/jira" | grep -v node_modules | grep -v ".git/"
   If found, check if there are any files inside. If files exist, move them to com/avionics_systems/.
   Remove empty com/jira/ directories.

6. Check docker-compose files reference the correct folder names in build.context:
   grep -n "context:" docker-compose*.yml | grep -v node_modules

7. Verify .gitignore patterns match the new names.

Fix everything. The project structure should have ZERO folders with "jira" in the name (except maybe docs/ referencing historical info).`, {
    label: 'fix:folders',
    phase: 'Fix',
    effort: 'high'
  }),

  // Fixer 4: Frontend
  () => agent(`You are a frontend developer. Fix ALL remaining "jira" references in the frontend.

PROJECT ROOT: ${PROJECT_ROOT}

## Your job
Find the frontend directory (could be jira-frontend/ or avionics-systems-frontend/).

1. Scan for all remaining jira references:
   grep -ri "jira" --include="*.tsx" --include="*.ts" --include="*.css" --include="*.html" --include="*.json" <frontend-dir>/src/ | grep -v node_modules | head -200

2. For each finding, apply the correct replacement:
   - "Jira" (display text) → "Avionics Systems"
   - "jira" (technical) → "avisys" or "avionics-systems" depending on context
   - CSS class .jira- → .avisys-
   - Component Jira* → AviSys*
   - import paths with jira → updated paths
   - API paths with jira → updated paths

3. Check package.json:
   grep "jira" <frontend-dir>/package.json

4. Check tailwind config:
   grep "jira" <frontend-dir>/tailwind.config.*

5. Check nginx.conf:
   grep "jira" <frontend-dir>/nginx.conf

6. Check index.html:
   grep -i "jira" <frontend-dir>/index.html <frontend-dir>/dist/index.html 2>/dev/null

7. Check for files with "jira" in filename:
   find <frontend-dir>/src -name "*jira*" -o -name "*Jira*" | head -50

Fix EVERY single reference. After fixes, verify:
   grep -ri "jira" <frontend-dir>/src/ | grep -v node_modules | wc -l
This should be ZERO.`, {
    label: 'fix:frontend',
    phase: 'Fix',
    effort: 'high'
  }),
])

log('All fixers complete: ' + fixResults.filter(Boolean).length + '/4')

// =============================================
// PHASE 3: BUILD — Compile until clean
// =============================================
phase('Build')
log('Running build verification...')

const buildResult = await agent(`You are a build engineer. Make the ENTIRE project compile and build successfully.

PROJECT ROOT: ${PROJECT_ROOT}

## Step 1: Backend Compilation
Run: mvn compile -f pom.xml 2>&1 | tail -300

If it fails:
- Read each error message
- Identify the file and issue (missing import, wrong package, class not found, etc.)
- Fix the file
- Re-run: mvn compile -pl <failing-module> 2>&1 | tail -50
- Keep iterating until that module compiles
- Then re-run the full build

Common issues:
a. Package declaration still says com.avionics_systems → change to com.avionics_systems
b. Import still references com.avionics_systems → change to com.avionics_systems
c. Class name mismatch (file named AvionicsSystem* but class still says Jira*) → fix class declaration
d. @Table schema annotation still says jira_ → change to avionics_systems_
e. Maven dependency coordinates wrong → fix groupId/artifactId in pom.xml
f. Spring auto-config file references old class names → update META-INF files
g. mainClass in pom.xml points to old class → update
h. Module name in root pom.xml doesn't match folder name → fix either
i. Missing dependency because cluster-commons coordinates changed → update consumer pom.xml

## Step 2: Frontend Build
Find the frontend directory and run:
cd <frontend-dir> && npm run build 2>&1 | tail -100

If it fails:
- Fix TypeScript/import errors
- Fix missing component references
- Re-run until clean

## Step 3: Final Verification
Run:
grep -rn "com\\.jira\\." --include="*.java" . | grep -v node_modules | grep -v ".git/" | grep -v "db/migration/" | wc -l
This MUST be ZERO (except in existing Flyway migrations which we never touch).

Run:
grep -rn "package com\\.jira" --include="*.java" . | grep -v node_modules | grep -v ".git/" | wc -l
This MUST be ZERO.

## Step 4: Docker Compose Validation
Run: docker compose config 2>&1 | head -20
If docker compose is available, verify the config parses without errors.

Keep fixing until EVERYTHING builds clean. Do NOT stop until:
1. mvn compile succeeds for ALL modules
2. Frontend build succeeds (if npm is available)
3. Zero remaining com.avionics_systems references in Java source files`, {
  label: 'build:final',
  phase: 'Build',
  effort: 'high'
})

return { fixResults: fixResults.filter(Boolean).length, buildResult }
