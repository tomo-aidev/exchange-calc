/**
 * QuickRate API - Cloudflare Workers
 *
 * Endpoints:
 *   GET /latest       → today's rates (cached in KV)
 *   GET /history?base=usd&days=90  → 90-day rate history
 *
 * Upstream: fawazahmed0/currency-api via jsDelivr + Cloudflare Pages fallback
 */

const PRIMARY_URL = 'https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@{date}/v1/currencies/usd.min.json';
const FALLBACK_URL = 'https://{date}.currency-api.pages.dev/v1/currencies/usd.min.json';

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
};

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: CORS_HEADERS });
    }

    try {
      if (url.pathname === '/latest') {
        return await handleLatest(env);
      }
      if (url.pathname === '/history') {
        const days = parseInt(url.searchParams.get('days') || '90', 10);
        const from = (url.searchParams.get('from') || '').toUpperCase();
        const to = (url.searchParams.get('to') || '').toUpperCase();
        if (from && to) {
          return await handlePairHistory(env, Math.min(days, 90), from, to);
        }
        return await handleHistory(env, Math.min(days, 90));
      }
      return jsonResponse({ error: 'Not found' }, 404);
    } catch (e) {
      return jsonResponse({ error: e.message }, 500);
    }
  },

  // Cron trigger: daily fetch & store
  async scheduled(event, env) {
    await fetchAndStoreToday(env);
  },
};

// ── /latest ──────────────────────────────────────

async function handleLatest(env) {
  const today = dateString(0);
  const cacheKey = `rates:${today}`;

  // Try KV cache first
  let cached = await env.RATES.get(cacheKey, 'json');
  if (cached) {
    return jsonResponse(cached);
  }

  // Fetch from upstream
  const data = await fetchUpstream(today);
  if (data) {
    await env.RATES.put(cacheKey, JSON.stringify(data), { expirationTtl: 86400 * 7 });
    return jsonResponse(data);
  }

  // Fallback: try yesterday
  const yesterday = dateString(1);
  cached = await env.RATES.get(`rates:${yesterday}`, 'json');
  if (cached) {
    return jsonResponse(cached);
  }

  return jsonResponse({ error: 'No rates available' }, 503);
}

// ── /history ─────────────────────────────────────

async function handleHistory(env, days) {
  const results = [];
  const missing = [];
  const dates = [];
  for (let i = 0; i < days; i++) dates.push(dateString(i));

  // Parallel KV reads
  const kvResults = await Promise.all(dates.map(d => env.RATES.get(`rates:${d}`, 'json')));
  for (let i = 0; i < dates.length; i++) {
    if (kvResults[i]) {
      results.push(kvResults[i]);
    } else {
      missing.push(dates[i]);
    }
  }

  // Fetch missing (30 concurrent)
  if (missing.length > 0) {
    const batches = chunk(missing, 30);
    for (const batch of batches) {
      await Promise.all(batch.map(async (d) => {
        const data = await fetchUpstream(d);
        if (data) {
          await env.RATES.put(`rates:${d}`, JSON.stringify(data), { expirationTtl: 86400 * 100 });
          results.push(data);
        }
      }));
    }
  }

  // Sort by date ascending
  results.sort((a, b) => a.date.localeCompare(b.date));

  return jsonResponse({ count: results.length, data: results });
}

// ── /history?from=USD&to=JPY (lightweight pair history) ──

async function handlePairHistory(env, days, from, to) {
  const results = [];
  const missing = [];
  const dates = [];
  for (let i = 0; i < days; i++) dates.push(dateString(i));

  // Parallel KV reads (all at once)
  const kvResults = await Promise.all(dates.map(d => env.RATES.get(`rates:${d}`, 'json')));

  for (let i = 0; i < dates.length; i++) {
    const cached = kvResults[i];
    if (cached && cached.rates[from] && cached.rates[to]) {
      results.push({ date: cached.date, rate: cached.rates[to] / cached.rates[from] });
    } else {
      missing.push(dates[i]);
    }
  }

  // Fetch all missing in parallel (30 concurrent)
  if (missing.length > 0) {
    const batches = chunk(missing, 30);
    for (const batch of batches) {
      await Promise.all(batch.map(async (d) => {
        const data = await fetchUpstream(d);
        if (data) {
          await env.RATES.put(`rates:${d}`, JSON.stringify(data), { expirationTtl: 86400 * 100 });
          if (data.rates[from] && data.rates[to]) {
            results.push({ date: data.date, rate: data.rates[to] / data.rates[from] });
          }
        }
      }));
    }
  }

  results.sort((a, b) => a.date.localeCompare(b.date));
  return jsonResponse({ from, to, count: results.length, data: results });
}

// ── Upstream fetch ───────────────────────────────

async function fetchUpstream(dateStr) {
  const urls = [
    PRIMARY_URL.replace('{date}', dateStr),
    FALLBACK_URL.replace('{date}', dateStr),
  ];

  for (const u of urls) {
    try {
      const resp = await fetch(u, { cf: { cacheTtl: 3600 } });
      if (!resp.ok) continue;
      const json = await resp.json();
      const rates = json.usd;
      if (!rates) continue;

      // Uppercase keys and add metadata
      const uppercased = { USD: 1 };
      for (const [k, v] of Object.entries(rates)) {
        if (k.length === 3) {
          uppercased[k.toUpperCase()] = v;
        }
      }

      return { date: json.date || dateStr, rates: uppercased };
    } catch {
      continue;
    }
  }
  return null;
}

// ── Daily cron job ───────────────────────────────

async function fetchAndStoreToday(env) {
  const today = dateString(0);
  const data = await fetchUpstream(today);
  if (data) {
    await env.RATES.put(`rates:${today}`, JSON.stringify(data), { expirationTtl: 86400 * 100 });
  }
}

// ── Helpers ──────────────────────────────────────

function dateString(daysAgo) {
  const d = new Date();
  d.setUTCDate(d.getUTCDate() - daysAgo);
  return d.toISOString().slice(0, 10);
}

function chunk(arr, size) {
  const out = [];
  for (let i = 0; i < arr.length; i += size) {
    out.push(arr.slice(i, i + size));
  }
  return out;
}

function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json',
      ...CORS_HEADERS,
    },
  });
}
