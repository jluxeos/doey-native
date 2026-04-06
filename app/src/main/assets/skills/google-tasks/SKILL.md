---
name: google-tasks
category: productivity
description: Manage tasks via Google Tasks app UI automation. No API key required. Tools: intent, accessibility.
android_package: com.google.android.apps.tasks
permissions: []
---
# Google Tasks Skill

Manage to-do tasks by automating the Google Tasks app UI. No OAuth or API key needed.

## Tool: intent — Open app

### Open Google Tasks
```json
{ "action": "android.intent.action.MAIN", "package": "com.google.android.apps.tasks" }
```

---

## Tool: accessibility — Full task management

All operations use `get_tree` → interact pattern.

### Workflow: Read tasks aloud

1. Open Google Tasks via intent
2. `wait_for_app` with `package_name: "com.google.android.apps.tasks"`
3. `get_tree` — read all task items visible on screen
4. Scroll down if needed to see more
5. Read them aloud via `tts`

### Workflow: Create a new task

1. Open Google Tasks via intent
2. `wait_for_app`
3. `get_tree` — find the "Add a task" button or "+" FAB
4. `click` it
5. `get_tree` — find the task title input field
6. `type` the task title
7. If due date needed: find "Add date/time" option and interact
8. Find "Save" or confirm button, `click` it
9. Confirm via `tts`: "Task created: [title]"

### Workflow: Complete a task

1. Open Google Tasks via intent
2. `wait_for_app`
3. `get_tree` — find the task by its label
4. Find the checkbox next to it, `click` it
5. Confirm via `tts`: "Task completed: [title]"

### Workflow: Delete a task

1. Open Google Tasks via intent
2. `wait_for_app`
3. `get_tree` — find the task
4. `long_click` the task to open options, or swipe to find delete
5. Find "Delete" option, `click` it
6. Confirm via `tts`

---

## Examples

- "What do I still need to do?" → open app → read task list → TTS
- "Create task: Buy milk" → open app → click add → type "Buy milk" → save
- "Mark groceries as done" → open app → find task → click checkbox
- "Delete the dentist task" → open app → find task → long click → delete

## Notes
- Always `wait_for_app` before reading the tree
- If Google Tasks is not installed, inform the user
- Never attempt HTTP calls to tasks.googleapis.com — no API key is available

