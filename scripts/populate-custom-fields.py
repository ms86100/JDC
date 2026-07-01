"""
Populate custom fields from Jira DC CSV export into migration DB.
Creates field definitions and stores values linked to actual issue UUIDs.
Fields become visible in the UI via the /api/fields endpoints.
"""

import csv
import json
import re
import uuid
import sys
from datetime import datetime, date

# DB connection
import subprocess

DB_HOST = "in0-eplmdb-v01"
DB_NAME = "systems"
DB_USER = "systems_admin"
DB_PASS = "Hcu4ieD8R13qaf7JVSsu"
CSV_PATH = r"C:\Users\SSHABNSA\Desktop\test\JDC-main\issue.csv"
PROJECT_ID = "08d9bae7-c8e6-4179-92e4-1d2b224ba03e"

JAVA_PATH = r"C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\jbr\bin\java.exe"
PG_JAR = r"C:\Users\SSHABNSA\Desktop\test\JDC-main\.m2-cache\org\postgresql\postgresql\42.7.5\postgresql-42.7.5.jar"


def run_sql(statements):
    """Run SQL via Java + JDBC (since psql is not available)."""
    java_code = '''
import java.sql.*;
public class RunSql {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://''' + DB_HOST + ''':5432/''' + DB_NAME + '''",
                "''' + DB_USER + '''", "''' + DB_PASS + '''")) {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            String[] sqls = args[0].split(";;SEP;;");
            int count = 0;
            for (String sql : sqls) {
                sql = sql.trim();
                if (sql.isEmpty()) continue;
                try {
                    stmt.execute(sql);
                    count++;
                } catch (Exception e) {
                    System.err.println("SQL error: " + e.getMessage());
                    System.err.println("  SQL: " + sql.substring(0, Math.min(200, sql.length())));
                }
            }
            conn.commit();
            System.out.println("Executed " + count + " statements");
        }
    }
}
'''
    import tempfile, os
    java_file = os.path.join(tempfile.gettempdir(), "RunSql.java")
    with open(java_file, "w") as f:
        f.write(java_code)

    sql_arg = ";;SEP;;".join(statements)
    result = subprocess.run(
        [JAVA_PATH, "-cp", PG_JAR, java_file, sql_arg],
        capture_output=True, text=True, timeout=60
    )
    if result.stdout.strip():
        print(result.stdout.strip())
    if result.stderr.strip():
        for line in result.stderr.strip().split("\n"):
            if "SQL error" in line or "SQL:" in line:
                print("  " + line)
    return result.returncode == 0


def query_sql(sql):
    """Run a SELECT query and return rows."""
    java_code = '''
import java.sql.*;
public class QuerySql {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://''' + DB_HOST + ''':5432/''' + DB_NAME + '''",
                "''' + DB_USER + '''", "''' + DB_PASS + '''")) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(args[0]);
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) sb.append("\\t");
                    String v = rs.getString(i);
                    sb.append(v != null ? v : "");
                }
                sb.append("\\n");
            }
            System.out.print(sb.toString());
        }
    }
}
'''
    import tempfile, os
    java_file = os.path.join(tempfile.gettempdir(), "QuerySql.java")
    with open(java_file, "w") as f:
        f.write(java_code)

    result = subprocess.run(
        [JAVA_PATH, "-cp", PG_JAR, java_file, sql],
        capture_output=True, text=True, timeout=30
    )
    rows = []
    for line in result.stdout.strip().split("\n"):
        if line.strip():
            rows.append(line.split("\t"))
    return rows


def normalize_field_key(name):
    key = name.strip().lower()
    key = re.sub(r'[^a-z0-9_]', '_', key)
    key = re.sub(r'_+', '_', key).strip('_')
    return "customfield_" + key


def guess_field_type(values):
    non_empty = [v for v in values if v and v.strip()]
    if not non_empty:
        return "TEXT"

    date_pattern = re.compile(r'^\d{2}/\w{3}/\d{2}')
    number_pattern = re.compile(r'^-?\d+(\.\d+)?$')
    bool_values = {'true', 'false', 'yes', 'no'}

    dates = sum(1 for v in non_empty if date_pattern.match(v.strip()))
    numbers = sum(1 for v in non_empty if number_pattern.match(v.strip()))
    bools = sum(1 for v in non_empty if v.strip().lower() in bool_values)

    if dates > len(non_empty) * 0.7:
        return "DATE"
    if numbers > len(non_empty) * 0.7:
        return "NUMBER"
    if bools == len(non_empty):
        return "CHECKBOX"

    avg_len = sum(len(v) for v in non_empty) / len(non_empty)
    if avg_len > 200:
        return "TEXTAREA"

    return "TEXT"


def renderer_for_type(field_type):
    return {
        "TEXT": "TEXT",
        "TEXTAREA": "TEXTAREA",
        "DATE": "DATETIME_PICKER",
        "NUMBER": "NUMBER",
        "CHECKBOX": "TEXT",
        "SELECT": "SELECT",
    }.get(field_type, "TEXT")


def main():
    print("=== Populating Custom Fields from CSV ===\n")

    # 1. Read CSV
    with open(CSV_PATH, "r", encoding="utf-8-sig", errors="replace") as f:
        reader = csv.DictReader(f)
        headers = reader.fieldnames
        rows = [r for r in reader if r.get("Issue key") and r["Issue key"].strip()]

    print("CSV: {} issues, {} columns".format(len(rows), len(headers)))

    # 2. Identify custom field columns with data
    custom_fields = {}
    for h in headers:
        if h.startswith("Custom field ("):
            match = re.match(r'Custom field \((.+)\)', h)
            if match:
                name = match.group(1).strip()
                values = [r.get(h, "").strip() for r in rows]
                non_empty = [v for v in values if v]
                if non_empty:
                    custom_fields[name] = {
                        "header": h,
                        "values": values,
                        "non_empty_count": len(non_empty),
                        "field_key": normalize_field_key(name),
                        "field_type": guess_field_type(values),
                    }

    print("Custom fields with data: {}".format(len(custom_fields)))
    print()

    # Show top fields
    sorted_fields = sorted(custom_fields.items(), key=lambda x: -x[1]["non_empty_count"])
    print("Top populated custom fields:")
    for name, info in sorted_fields[:15]:
        print("  {:40s} {:>3d}/25 values  type={}".format(
            name[:40], info["non_empty_count"], info["field_type"]))
    print()

    # 3. Get issue key -> UUID mapping
    issue_rows = query_sql(
        "SELECT issue_key, id FROM jira_issue.issues "
        "WHERE project_id = '" + PROJECT_ID + "'"
    )
    issue_map = {r[0]: r[1] for r in issue_rows}
    print("Issue UUIDs loaded: {}".format(len(issue_map)))

    # 4. Create field definitions in migration DB
    print("\nCreating field definitions...")
    field_ids = {}
    create_stmts = []
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    for name, info in sorted_fields:
        fid = str(uuid.uuid4())
        field_ids[name] = fid
        fkey = info["field_key"]
        ftype = info["field_type"]
        renderer = renderer_for_type(ftype)

        sql = (
            "INSERT INTO jira_migration.field_definitions "
            "(id, field_key, display_name, field_type, renderer, screen_region, "
            "searchable, sortable, filterable, required, read_only, hidden, "
            "custom, built_in, deprecated, version, created_at, updated_at, navigable) "
            "VALUES ('{}', '{}', '{}', '{}', '{}', 'SIDEBAR_DETAILS', "
            "true, true, true, false, false, false, "
            "true, false, false, 1, '{}', '{}', true) "
            "ON CONFLICT (field_key) DO UPDATE SET "
            "display_name = EXCLUDED.display_name, "
            "field_type = EXCLUDED.field_type, "
            "updated_at = EXCLUDED.updated_at"
        ).format(
            fid,
            fkey.replace("'", "''"),
            name.replace("'", "''"),
            ftype,
            renderer,
            now, now
        )
        create_stmts.append(sql)

    run_sql(create_stmts)
    print("Created {} field definitions".format(len(create_stmts)))

    # 5. Get actual field IDs (in case ON CONFLICT used existing IDs)
    field_rows = query_sql(
        "SELECT field_key, id FROM jira_migration.field_definitions WHERE custom = true"
    )
    field_key_to_id = {r[0]: r[1] for r in field_rows}

    # 6. Insert field values for each issue
    print("\nPopulating field values...")
    value_stmts = []
    total_values = 0

    for row in rows:
        issue_key = row["Issue key"].strip()
        issue_id = issue_map.get(issue_key)
        if not issue_id:
            continue

        for name, info in custom_fields.items():
            value = row.get(info["header"], "").strip()
            if not value:
                continue

            fkey = info["field_key"]
            field_def_id = field_key_to_id.get(fkey)
            if not field_def_id:
                continue

            vid = str(uuid.uuid4())
            escaped_value = value.replace("'", "''").replace("\\", "\\\\")
            if len(escaped_value) > 4000:
                escaped_value = escaped_value[:4000]

            ftype = info["field_type"]

            if ftype == "NUMBER":
                try:
                    num_val = float(value)
                    sql = (
                        "INSERT INTO jira_migration.issue_field_values "
                        "(id, issue_id, field_definition_id, string_value, double_value, "
                        "formatted_value, raw_value, validation_status, value_source, "
                        "searchable_text, version, created_at, updated_at) "
                        "VALUES ('{}', '{}', '{}', '{}', {}, '{}', '{}', 'VALID', 'CSV_IMPORT', "
                        "'{}', 1, '{}', '{}') "
                        "ON CONFLICT (issue_id, field_definition_id) DO UPDATE SET "
                        "string_value = EXCLUDED.string_value, double_value = EXCLUDED.double_value, updated_at = EXCLUDED.updated_at"
                    ).format(vid, issue_id, field_def_id, escaped_value, num_val,
                             escaped_value, escaped_value, escaped_value, now, now)
                except ValueError:
                    sql = None
            else:
                sql = (
                    "INSERT INTO jira_migration.issue_field_values "
                    "(id, issue_id, field_definition_id, string_value, "
                    "formatted_value, raw_value, validation_status, value_source, "
                    "searchable_text, version, created_at, updated_at) "
                    "VALUES ('{}', '{}', '{}', '{}', '{}', '{}', 'VALID', 'CSV_IMPORT', "
                    "'{}', 1, '{}', '{}') "
                    "ON CONFLICT (issue_id, field_definition_id) DO UPDATE SET "
                    "string_value = EXCLUDED.string_value, updated_at = EXCLUDED.updated_at"
                ).format(vid, issue_id, field_def_id, escaped_value,
                         escaped_value, escaped_value, escaped_value, now, now)

            if sql:
                value_stmts.append(sql)
                total_values += 1

    # Execute in batches
    batch_size = 50
    for i in range(0, len(value_stmts), batch_size):
        batch = value_stmts[i:i + batch_size]
        run_sql(batch)

    print("Inserted {} field values".format(total_values))

    # 7. Verify
    print("\n=== Verification ===")
    count_rows = query_sql(
        "SELECT COUNT(*) FROM jira_migration.field_definitions WHERE custom = true"
    )
    print("Custom field definitions: {}".format(count_rows[0][0] if count_rows else 0))

    count_rows = query_sql(
        "SELECT COUNT(*) FROM jira_migration.issue_field_values WHERE value_source = 'CSV_IMPORT'"
    )
    print("Custom field values: {}".format(count_rows[0][0] if count_rows else 0))

    # Show sample
    sample_rows = query_sql(
        "SELECT d.display_name, COUNT(*) as cnt "
        "FROM jira_migration.issue_field_values v "
        "JOIN jira_migration.field_definitions d ON v.field_definition_id = d.id "
        "WHERE v.value_source = 'CSV_IMPORT' "
        "GROUP BY d.display_name ORDER BY cnt DESC LIMIT 10"
    )
    print("\nTop populated fields:")
    for r in sample_rows:
        print("  {:40s} {} issues".format(r[0][:40], r[1]))

    print("\nDone! Custom fields should now be visible in the UI.")
    print("Refresh the issue detail page to see them.")


if __name__ == "__main__":
    main()
