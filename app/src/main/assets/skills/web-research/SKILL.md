---
name: web-research
category: information
description: Search the web for facts and current information. No API key required. Uses DuckDuckGo, Wikipedia, and direct page fetching. Tools: http.
permissions:
 - android.permission.INTERNET
---
# Web Research Skill

Search the web and look up facts using free, no-key services.

## Tools

| Tool | Purpose |
|------|---------|
| `http` | GET requests to search and Wikipedia APIs |

---

## Source 1: DuckDuckGo Instant Answer (free, no key)

Best for: quick facts, definitions, calculations, conversions.

```json
{
  "method": "GET",
  "url": "https://api.duckduckgo.com/?q={QUERY}&format=json&no_html=1&skip_disambig=1"
}
```

**Key response fields:**
- `AbstractText` — summary answer
- `Answer` — direct answer (math, conversions, etc.)
- `RelatedTopics[].Text` — related info

If both `AbstractText` and `Answer` are empty → fall back to Wikipedia.

---

## Source 2: Wikipedia (free, no key)

Best for: people, places, concepts, history, science.

### Direct lookup (when you know the topic name)
```json
{
  "method": "GET",
  "url": "https://en.wikipedia.org/api/rest_v1/page/summary/{TOPIC}",
  "headers": [{ "key": "User-Agent", "value": "DoeyBot/1.0" }]
}
```
Returns `extract` (summary text) and `description`.

**Language support:** Replace `en` with `es`, `de`, `fr`, `pt`, `it`, etc.

### Search (when topic name is uncertain)
```json
{
  "method": "GET",
  "url": "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch={QUERY}&format=json&srlimit=5",
  "headers": [{ "key": "User-Agent", "value": "DoeyBot/1.0" }]
}
```
Returns `query.search[].title` — use the best match with the summary endpoint.

---

## Source 3: Direct page fetch (any URL)

Use when the user shares a URL or when you need to read a specific page.

```json
{
  "method": "GET",
  "url": "{PAGE_URL}",
  "response_format": "text",
  "headers": [{ "key": "User-Agent", "value": "DoeyBot/1.0" }]
}
```

Note: content is truncated at 5000 chars. Summarize what you find.

---

## Decision matrix

| User intent | First try | Fallback |
|-------------|-----------|----------|
| "What is X?" / "Who is X?" | Wikipedia summary | DuckDuckGo |
| "Search for X" / "Google X" | DuckDuckGo | Wikipedia search |
| "Latest news about X" | headlines skill (RSS) | Wikipedia |
| Quick fact / number / date | DuckDuckGo Instant | Wikipedia |
| "Read this page: [URL]" | Direct http fetch | — |
| "Tell me about X" | Wikipedia summary | DuckDuckGo |

---

## Workflows

### "What is X?" / "Who is X?"
1. `http` → Wikipedia summary for `{X}`
2. If 404 → Wikipedia search → pick best title → summary
3. If still no result → DuckDuckGo Instant Answer
4. Summarize via `tts`

### "Search for X" / "Google X"
1. `http` → DuckDuckGo `?q={X}`
2. If `AbstractText` and `Answer` are empty → Wikipedia search for `{X}`
3. Synthesize and respond

### "Latest news about X"
→ Use the **headlines** skill with RSS feeds. Web research is not ideal for breaking news.

### "Read this page: [URL]"
1. `http` → GET `{URL}` with `response_format: "text"`
2. Extract and summarize the meaningful content

---

## URL encoding rules

- Spaces → `+` or `%20`: `artificial+intelligence`
- Special chars: `&` → `%26`, `#` → `%23`
- Umlauts work directly in Wikipedia URLs: `München`, `Zürich`

---

## Output rules

- Always summarize in natural language — never dump raw JSON
- Cite the source: "According to Wikipedia…" / "DuckDuckGo shows…"
- Voice (TTS): 2–4 sentences max
- If nothing is found: "I couldn't find information about {topic}."
- Respond in the user's configured language even if the source is in another language
