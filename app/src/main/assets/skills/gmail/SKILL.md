---
name: gmail
category: communication
description: Read and send emails via Gmail app UI automation. No API key required. Tools: intent, accessibility.
android_package: com.google.android.gm
permissions: []
---
# Gmail Skill

Read and send emails by automating the Gmail app UI. No OAuth or API key needed.

## Tool: intent — Open app

### Open Gmail
```json
{ "action": "android.intent.action.MAIN", "package": "com.google.android.gm" }
```

### Compose a new email (pre-filled)
```json
{
  "action": "android.intent.action.SENDTO",
  "uri": "mailto:recipient@example.com?subject=SUBJECT&body=BODY"
}
```
URL-encode the subject and body (spaces = `%20`).

---

## Tool: accessibility — Read and interact

### Workflow: Check recent emails

1. Open Gmail via intent
2. `wait_for_app` with `package_name: "com.google.android.gm"`
3. `get_tree` — read the inbox list: sender names, subjects, preview text visible on screen
4. Summarize via `tts`

### Workflow: Read a specific email

1. Open Gmail, `wait_for_app`
2. `get_tree` — find the email in the list by sender or subject
3. `click` it to open
4. `get_tree` — read the full body text
5. Summarize via `tts`

### Workflow: Send an email

**Option A — via intent (fastest, opens compose screen):**
1. Build mailto URI with recipient, subject, body (URL-encoded)
2. Fire intent `android.intent.action.SENDTO`
3. `wait_for_app` on Gmail
4. `get_tree` — find Send button
5. `click` Send
6. Confirm via `tts`

**Option B — fully via accessibility:**
1. Open Gmail, `wait_for_app`
2. `get_tree` — find Compose button (pencil/edit icon), `click`
3. `get_tree` — find "To" field, `type` recipient email
4. Find "Subject" field, `type` subject
5. Find body field, `type` message
6. Find Send button (paper plane icon), `click`
7. Confirm via `tts`

### Workflow: Search emails

1. Open Gmail, `wait_for_app`
2. `get_tree` — find Search icon, `click`
3. `type` search query (e.g. sender name, subject keyword)
4. `get_tree` — read results

---

## Examples

- "Check my inbox" → open Gmail → read inbox list → TTS summary
- "Read the email from Pedro" → open Gmail → find email → open → read body → TTS
- "Send email to itzy@example.com: I love you" → intent mailto → send via accessibility
- "Do I have unread emails?" → open Gmail → read inbox, look for bold/unread indicators

## Notes
- Always summarize email content — never read verbatim unless explicitly asked
- Always confirm before sending
- Never attempt HTTP calls to gmail.googleapis.com — no API key is available

