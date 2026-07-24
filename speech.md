# SYSDOPS JIRA — Live Demonstration Speech

---

## Opening

Good morning, everyone.

Thank you for joining today's demonstration.

Today, I'd like to showcase our AI-powered Application Lifecycle Management platform and demonstrate how it helps organisations transition smoothly from Jira Data Center while preserving their existing data, workflows, and project structure.

Our objective was simple. We didn't want to build just another project management tool. We wanted to build a platform that feels familiar to Jira users, simplifies migration, and provides a modern user experience — without losing traceability or important project information.

---

## Part 1 — Project Creation

Let me begin by creating a new project.

Just like Jira Data Center, project creation is simple and intuitive.

I click on **Create Project**, where I can choose from multiple templates based on the team's needs. We currently support templates such as Scrum, Kanban, Task Management, Portfolio Management, Software Development, and Process Management.

Each template comes with its own predefined workflow and configuration, so teams can start working immediately without spending time on manual setup.

For today's demonstration, I'll create a **Scrum** project.

I simply select the Scrum template and click **Continue**.

Next, I enter the project details. I'll name this project **Flight Management System**.

The project key is generated automatically, and I can also add a meaningful description, configure project visibility, choose the default assignee, and define other project settings.

Once everything is configured, I click **Next**.

The platform now shows a summary of the project configuration.

Here I can review the issue types that will be available, including Epics, Stories, Tasks, Bugs, and Sub-tasks.

Along with that, all the Scrum modules are automatically enabled — Backlog, Sprint Planning, Story Points, Velocity Tracking, Burndown Charts, Scrum Reports, and Epic Management.

If I need to make any changes, I can simply go back. Otherwise, I click **Create Project**.

Within a few seconds, the project is created and ready for the team.

---

## Part 2 — Project Features

Now when I open the project, you can see that all the Scrum features are already available.

### The Backlog

The Backlog acts as your structured staging ground — catching engineering data and technical requirements before they are officially scheduled for development. This is where System Designers organise, prioritise, and refine work items before they move into a sprint.

### Sprint Planning

The Sprint Planning module helps us plan upcoming iterations. Issues are pulled from the backlog into a sprint, giving the team a focused scope of work for each development cycle.

### Issues

Issues are the core work items in our platform. Whether you are logging technical modifications with a Change Card, tracking model drops using a Deliverable issue type, or capturing a defect found during verification — every piece of work is an issue with full traceability.

### Releases

Every System Standard is directly linked to an official project Release through the **Fix Versions** field. This tells the entire programme exactly which software configuration or hardware component baseline is being packaged for the aircraft. Releases provide a clear milestone structure aligned with our delivery cadence.

### Components

To ensure your work is easily discoverable, the **Components** field categorises your issues by physical systems and subsystems — Navigation Logic, Sensor Interface, Display Rendering, and so on. This brings structure to large-scale aircraft system developments where multiple subsystems are developed in parallel.

### Workflows

Workflows are the fundamental building blocks that enable traceability, collaboration, and efficiency in your aircraft system development.

Workflows are not just visual status trackers — they strictly map to mandatory aviation maturity phases. A task moves from **Backlog** to **To Do**, then to **In Progress**, through **In Review**, and finally to **Done**. At each transition, the workflow enforces quality gates. For example, you cannot move directly from In Progress to Done without going through the Review step.

Every transition is recorded in the Activity tab with full audit trail — who made the change and when. This supports our EASA traceability requirements and compliance documentation for certifications.

Since all these capabilities are included with the selected template, the team can start working immediately without any additional configuration.

---

## Part 3 — Migration Centre

Now I'd like to move to one of the most important capabilities of our platform — the **Migration Centre**.

One of the biggest challenges organisations face when moving away from Jira Data Center is migrating years of valuable project information.

Many organisations have customised Jira over time with their own workflows, fields, and configurations. Recreating all of that manually can be time-consuming and risky.

Keeping this challenge in mind, we've designed our Migration Centre to make the transition as simple and seamless as possible.

From the navigation menu, I open **Migration & Audit**, and then select **Migration Centre**.

Here, we support multiple migration options, including CSV Import, XML Import, Workflow Import, Project Export, and Project Copy. We are also working on supporting complete backup imports for end-to-end project migration.

For today's demonstration, I'll use the **CSV Import** option.

### Step 1 — Export from Jira Data Center

First, I'll switch to the existing Jira Data Center instance.

I'll select an existing project — in this case, **SLM Test**.

Jira displays all the issues available in that project.

To keep the demonstration simple, I'll export the issues using **CSV (Current Fields)**.

Once the export is complete, I'll briefly open the CSV file to verify its contents.

Here we can see all the issue information exactly as it exists in Jira.

### Step 2 — Upload to Our Platform

Now I'll return to our platform.

Inside the Migration Centre, I upload the downloaded CSV file.

The platform reads the file and prepares it for import.

Next, I click **Continue** and select the destination project that I created earlier.

The platform now displays a preview of the imported data, allowing me to cross-check everything before proceeding.

### Step 3 — Field Mapping

The next step is one of the key highlights of our migration capability — **Field Mapping**.

On the left, we have the source fields coming from the CSV file.

On the right, we have the target fields available in our platform.

The platform automatically maps all the standard fields that already exist, reducing manual effort and making the migration process much faster.

However, many organisations also use custom fields that are unique to their Jira environment.

When the platform encounters such fields, they remain unmapped because they don't yet exist in our system.

Instead of asking users to modify their source data, our platform allows administrators to create those custom fields directly within the application.

Once the new field is created, it immediately becomes available for mapping, allowing organisation-specific information to be migrated without any data loss.

This approach provides both flexibility and accuracy, ensuring that existing project data can be migrated with confidence while preserving traceability.

### Step 4 — Import Execution

Once all the fields are mapped, I click **Execute Import**.

Within seconds, all issues are imported — with their original issue keys preserved, statuses correctly applied, and custom field values mapped.

What used to take days of manual data entry is now done in under a minute.

---

## Part 4 — Verification

Let me now verify the imported data.

I open the project backlog, and here are all the issues — with their original SST1 keys, correct statuses showing To Do, In Review, and Done, and full field data preserved.

When I click into an individual issue, I can see:

- The **Summary** and **Description** carried over from the source system
- The **Status** correctly reflecting the original workflow state
- The **Issue Type** mapped to our platform's type system
- **Custom Fields** showing the values from the source Jira instance
- The **Activity** tab ready to capture all future changes with full audit trail

Every piece of information that existed in Jira Data Center is now available in our platform — with full traceability maintained.

---

## Closing

To conclude, our goal is not simply to replace Jira Data Center.

Our goal is to provide a modern, AI-powered platform that makes migration easier, reduces manual effort, preserves existing project information, and provides a familiar experience for users from day one.

With intelligent project templates, automated field mapping, configurable workflows, and a seamless migration process, organisations can transition to a modern platform with confidence and minimal disruption.

The platform stores only user ID, name, and login timestamps — no additional personal data beyond what is required for traceability. All processing serves exclusively the distribution and resolution of work tasks, traceability of processing status, and project management. Performance or behaviour monitoring is explicitly excluded.

We are fully aligned with the Group Works Agreement principles, and the system is designed to remove manual, non-value-added tasks from our System Designers — letting them focus on what matters: building safe, reliable aircraft systems.

Thank you. I'd be happy to answer any questions.

---

*Presentation tip: Speak slowly and pause after each click. Let the audience look at the screen before you continue. This script naturally fits a 10–12 minute demo and will sound conversational rather than memorised.*
