---
name: sms
category: communication
description: Send SMS (send_sms preferred) or open messaging app (intent). Lookup contact first. Confirm before sending.
permissions:
 - android.permission.SEND_SMS
 - android.permission.READ_SMS
 - android.permission.READ_CONTACTS
---
# SMS Skill

## Tool: send_sms (PREFERRED — background, no user tap)
```json
{ "phone_number": "+1234567890", "message": "TEXT" }
```
**ALWAYS confirm** before calling (msg + number). Cannot be undone.

## Tool: intent (Alternative — opens app for review)
```json
{ "action": "android.intent.action.SENDTO", "uri": "smsto:{NUM}", "extras": [{ "key": "sms_body", "value": "TEXT" }] }
```

## Workflow
1. `query` contacts → get number
2. Confirm: "Send to NAME (+NUM): 'TEXT'?"
3. After confirm → `send_sms`

## WhatsApp alt
```json
{ "action": "android.intent.action.VIEW", "uri": "https://wa.me/{NUM}?text={TEXT}", "package": "com.whatsapp" }
```
