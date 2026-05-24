# -*- coding: utf-8 -*-
"""Service health check script for DevLoop."""
import sys
import urllib.request
import urllib.error

ports = [
    (8080, "Gateway"),
    (8081, "Auth"),
    (8082, "User"),
    (8083, "Project"),
    (8084, "Issue"),
    (8085, "Workflow"),
    (8086, "Comment"),
    (8087, "Notification"),
    (8088, "Search"),
    (8089, "Audit"),
    (8090, "Attachment"),
    (8091, "Sprint"),
    (8092, "Plan"),
    (8093, "Admin"),
]

print("\nJira Platform Service Health Check")
print("=" * 50)

all_up = 0
all_down = 0

for port, name in ports:
    try:
        r = urllib.request.urlopen(f"http://localhost:{port}/actuator/health", timeout=3)
        status = "UP" if r.status == 200 else f"HTTP {r.status}"
        print(f"  {name:15} :{port}  [{status}]")
        all_up += 1
    except urllib.error.HTTPError as e:
        print(f"  {name:15} :{port}  [HTTP {e.code}]")
        all_up += 1
    except Exception as e:
        print(f"  {name:15} :{port}  [DOWN]")
        all_down += 1

print("=" * 50)
print(f"  Summary: {all_up} up, {all_down} down\n")
sys.exit(0 if all_down == 0 else 1)