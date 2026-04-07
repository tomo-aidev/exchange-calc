# QuickRate API - Cloudflare Workers

## Setup

1. Install wrangler: `npm install`
2. Login: `npx wrangler login`
3. Create KV namespace: `npx wrangler kv namespace create RATES`
4. Update `wrangler.toml` with the KV namespace ID
5. Deploy: `npm run deploy`

## Optional: Cron Trigger

Add to `wrangler.toml` to auto-fetch daily:

```toml
[triggers]
crons = ["0 8 * * *"]
```

## Endpoints

- `GET /latest` - Today's exchange rates
- `GET /history?days=90` - Rate history (max 90 days)

## Response Format

### /latest
```json
{
  "date": "2026-04-07",
  "rates": { "USD": 1, "JPY": 159.88, "EUR": 0.867, ... }
}
```

### /history
```json
{
  "count": 90,
  "data": [
    { "date": "2026-01-08", "rates": { ... } },
    { "date": "2026-01-09", "rates": { ... } },
    ...
  ]
}
```
