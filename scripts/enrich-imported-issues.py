"""
Enrich imported issues with full CSV data.
Reads the Jira DC CSV export properly (handling multi-line fields),
then updates each issue via the issue-service REST API.
"""

import csv
import json
import sys
import urllib.request
import urllib.error
from datetime import datetime

AUTH_URL = "http://localhost:8081/auth/login"
ISSUE_API = "http://localhost:8084/api/issues"
PROJECT_ID = "08d9bae7-c8e6-4179-92e4-1d2b224ba03e"
CSV_PATH = r"C:\Users\SSHABNSA\Desktop\test\JDC-main\issue.csv"

PRIORITY_MAP = {
    "Highest": "b0000000-0000-0000-0000-000000000001",
    "High":    "b0000000-0000-0000-0000-000000000002",
    "Medium":  "b0000000-0000-0000-0000-000000000003",
    "Low":     "b0000000-0000-0000-0000-000000000004",
    "Lowest":  "b0000000-0000-0000-0000-000000000005",
    "Yellow":  "b0000000-0000-0000-0000-000000000002",  # Map Jira "Yellow" to High
    "Red":     "b0000000-0000-0000-0000-000000000001",  # Map Jira "Red" to Highest
    "Green":   "b0000000-0000-0000-0000-000000000004",  # Map Jira "Green" to Low
}

STATUS_MAP = {
    "To Do":       "c0000000-0000-0000-0000-000000000001",
    "In Progress": "c0000000-0000-0000-0000-000000000002",
    "In Review":   "c0000000-0000-0000-0000-000000000003",
    "Done":        "c0000000-0000-0000-0000-000000000004",
}

ISSUE_TYPE_MAP = {
    "Bug":     "a0000000-0000-0000-0000-000000000001",
    "Story":   "a0000000-0000-0000-0000-000000000002",
    "Task":    "a0000000-0000-0000-0000-000000000003",
    "Epic":    "a0000000-0000-0000-0000-000000000004",
    "Sub-task":"00000000-0000-0001-0001-000000000005",
}


def http_json(url, method="GET", data=None, headers=None):
    headers = headers or {}
    headers["Content-Type"] = "application/json"
    body = json.dumps(data).encode("utf-8") if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="replace")[:200]


def get_token():
    status, data = http_json(AUTH_URL, "POST", {"username": "ms86100", "password": "admin123"})
    return data["accessToken"]


def parse_legacy_date(date_str):
    if not date_str or not date_str.strip():
        return None
    for fmt in ["%d/%b/%y %I:%M %p", "%d/%b/%y %H:%M %p", "%d/%b/%y"]:
        try:
            return datetime.strptime(date_str.strip(), fmt).strftime("%Y-%m-%d")
        except ValueError:
            continue
    return None


def read_csv():
    with open(CSV_PATH, "r", encoding="utf-8-sig", errors="replace") as f:
        reader = csv.DictReader(f)
        rows = []
        for row in reader:
            if row.get("Issue key") and row["Issue key"].strip():
                rows.append(row)
    return rows


def get_existing_issues(token):
    headers = {"Authorization": "Bearer " + token}
    status, data = http_json(ISSUE_API + "?projectId=" + PROJECT_ID + "&size=100", "GET", headers=headers)
    issues = data.get("content", data) if isinstance(data, dict) else data
    return {i["issueKey"]: i for i in issues}


def build_update(row):
    update = {}

    desc = (row.get("Description") or "").strip()
    if desc:
        update["description"] = desc

    priority_name = (row.get("Priority") or "").strip()
    pid = PRIORITY_MAP.get(priority_name)
    if pid:
        update["priorityId"] = pid

    status_name = (row.get("Status") or "").strip()
    sid = STATUS_MAP.get(status_name)
    if sid:
        update["statusId"] = sid

    labels_str = (row.get("Labels") or "").strip()
    if labels_str:
        labels = [l.strip() for l in labels_str.replace(",", " ").split() if l.strip()]
        if labels:
            update["labels"] = labels

    env = (row.get("Environment") or "").strip()
    if env:
        update["environment"] = env

    due = parse_legacy_date(row.get("Due Date"))
    if due:
        update["dueDate"] = due

    return update


def main():
    print("=== Enriching Imported Issues ===\n")

    token = get_token()
    print(f"Authenticated.\n")

    csv_rows = read_csv()
    print(f"CSV rows with issue keys: {len(csv_rows)}")

    existing = get_existing_issues(token)
    print(f"Existing issues in project: {len(existing)}\n")

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "X-User-Id": "03189c89-ad84-4734-b54e-eea0dede4852",
    }

    updated = 0
    skipped = 0
    errors = 0

    for row in csv_rows:
        csv_key = row["Issue key"].strip()
        issue = existing.get(csv_key)

        if not issue:
            # Try matching by title
            title = (row.get("Summary") or "").strip()
            for k, v in existing.items():
                if v.get("title") == title:
                    issue = v
                    break

        if not issue:
            print(f"  SKIP {csv_key}: not found in project")
            skipped += 1
            continue

        issue_id = issue["id"]
        update_data = build_update(row)

        if not update_data:
            print(f"  SKIP {csv_key}: no fields to update")
            skipped += 1
            continue

        fields_list = ", ".join(update_data.keys())
        try:
            status_code, resp = http_json(
                ISSUE_API + "/" + issue_id,
                "PUT",
                update_data,
                headers,
            )
            if status_code == 200:
                print("  OK   {}: updated [{}]".format(csv_key, fields_list))
                updated += 1
            else:
                print("  FAIL {}: HTTP {} - {}".format(csv_key, status_code, str(resp)[:150]))
                errors += 1
        except Exception as e:
            print("  ERR  {}: {}".format(csv_key, e))
            errors += 1

    print(f"\n=== Results ===")
    print(f"  Updated: {updated}")
    print(f"  Skipped: {skipped}")
    print(f"  Errors:  {errors}")


if __name__ == "__main__":
    main()
