---
name: timer
category: productivity
description: Countdown timers (beep on end) & stopwatches. Use scheduler for complex tasks. Tool: timer.
exclusive_tool: timer
permissions:
 - android.permission.SCHEDULE_EXACT_ALARM
---
# Timer Skill

Countdown timers (beep only) and stopwatches. For tasks needing execution (SMS, TTS, etc.) → use `scheduler`.

## Tool: timer — Actions

| action | params | desc |
|---|---|---|
| `start_timer` | `duration_ms`, `label?` | Start countdown; beep on end |
| `start_stopwatch` | `label?` | Count up |
| `list` | — | All active timers/stopwatches |
| `get_status` | `timer_id` | Remaining/elapsed ms |
| `stop` | `timer_id` | Stop stopwatch → returns elapsed |
| `cancel` | `timer_id` | Remove any timer |

`duration_ms`: ms integer (20s=20000, 3min=180000). Use `datetime` tool — never calculate manually.
Timer IDs: internal only, never show user. Describe by label+time.

## Workflows

**Set timer** ("egg timer 20s", "3 min timer"):
1. Calc ms → `timer start_timer {duration_ms, label}` → confirm "Timer: Xs"

**Stopwatch** ("start stopwatch for running"):
1. `timer start_stopwatch {label}` → confirm

**Check elapsed** / **Stop** / **Cancel** / **List**:
1. `timer list` → find by label → `get_status` / `stop` / `cancel` → report (no IDs)

## Rules
- `stop` = stopwatches only. `cancel` = any.
- Multiple timers/stopwatches run in parallel.
- Auto-removed on expiry (timers) or stop (stopwatches).
