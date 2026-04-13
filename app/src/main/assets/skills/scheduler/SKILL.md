---
name: scheduler
category: productivity
description: Schedule one-time or recurring tasks. Sub-agent executes instruction at trigger time with full tool access. Tools: scheduler. Always use datetime tool for trigger_at_ms.
test_prompt: List all existing schedules
exclusive_tool: scheduler
permissions:
 - android.permission.SCHEDULE_EXACT_ALARM
---
# Scheduler Skill

Sub-agent runs stored instruction at scheduled time with all tools (SMS, HTTP, TTS, beep, intent…).
Use for: tasks needing execution. NOT for simple beep timers → use `timer`.

## Tool: scheduler

### Create
```json
{
  "action": "create",
  "label": "LABEL",
  "instruction": "SELF-CONTAINED INSTRUCTION",
  "trigger_at_ms": TIMESTAMP_MS,
  "recurrence_type": "once|interval|daily|weekly"
}
```
`instruction` must be fully self-contained (no conv context). Include phone numbers, not contact names.

### Recurrence variants
| type | extra fields |
|---|---|
| `interval` | `recurrence_interval_ms` |
| `daily` | `recurrence_time: "HH:MM"` |
| `weekly` | `recurrence_time`, `recurrence_days_of_week: [1-7]` (1=Mon) |

### Other actions
- `list` → all schedules
- `get {schedule_id}` → single
- `update {schedule_id, ...fields}` → partial update
- `delete {schedule_id}`
- `enable/disable {schedule_id}`

## Time calculation (ALWAYS use datetime tool — never manual)

| Request | datetime call |
|---|---|
| "in 5 min" | `add, base:"now", amount:5, unit:"minute"` |
| "tomorrow 9am" | `absolute, base:"tomorrow", time:"09:00"` |
| "next Tue 8pm" | `absolute, base:"next_tuesday", time:"20:00"` |
| "every 30 min interval" | `interval, amount:30, interval_unit:"minute"` → ms for `recurrence_interval_ms` |

All output `output_unit: "milliseconds"`. Minimum trigger: 1 min future.

## Good vs bad instructions
✅ `"TTS: Roast out of oven! + beep(alarm,5)"`
✅ `"SMS to +4366012345: On my way"`
❌ `"Remind me"` / `"SMS to Peter"` (missing context)

## Key workflows

**"Remind me in 5 min about roast"**: datetime add → scheduler create (instruction: beep+TTS message, once) → confirm

**"SMS Peter 2pm: on my way"**: query contacts → datetime absolute today 14:00 → scheduler create

**"Read calendar every 8am"**: datetime absolute tomorrow 08:00 → scheduler create (daily, recurrence_time:"08:00")

## Rules
- Min 1 min. Immediate sounds → `beep` direct.
- Never show schedule IDs to user.
- Schedules survive reboots.
- SMS instructions need phone number (not name).
