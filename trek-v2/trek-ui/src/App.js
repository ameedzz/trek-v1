import { useState, useEffect, useRef, useCallback } from 'react';

// ── Constants ────────────────────────────────────────────────────────────────
const ALGOS       = ['bm25', 'tfidf', 'vector'];
const API         = process.env.REACT_APP_API_URL || '';  // ← Render backend URL
const ALGO_LABELS = { bm25: 'BM25', tfidf: 'TF-IDF', vector: 'Vector' };
const ALGO_COLORS = { bm25: '#ff4500', tfidf: '#0dd3bb', vector: '#ffd635' };
const ALGO_DESC   = {
  bm25:   'Okapi BM25 — length-normalized, saturation-aware ranking',
  tfidf:  'TF-IDF — classic inverted index scoring',
  vector: 'Cosine similarity — vector space model',
};

const C = {
  bg:         '#060608',
  surface:    '#0e0e11',
  surface2:   '#141417',
  surface3:   '#1a1a1f',
  border:     '#1e1e24',
  border2:    '#28282f',
  orange:     '#ff4500',
  orangeBg:   'rgba(255,69,0,0.07)',
  orangeGlow: 'rgba(255,69,0,0.15)',
  teal:       '#0dd3bb',
  tealBg:     'rgba(13,211,187,0.07)',
  yellow:     '#ffd635',
  text:       '#e0e0e6',
  muted:      '#6e6e7a',
  dim:        '#2e2e36',
  white:      '#ffffff',
  nsfw:       '#ff585b',
  green:      '#46d160',
};

const TRENDING = [
  'artificial intelligence', 'climate change', 'quantum computing',
  'electric vehicle', 'gene editing', 'space exploration',
  'machine learning', 'renewable energy', 'cryptocurrency', 'neural networks',
];

// ── Global CSS ────────────────────────────────────────────────────────────────
const GLOBAL_CSS = `
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  html { scroll-behavior: smooth; }
  body {
    background: ${C.bg};
    font-family: 'DM Sans', sans-serif;
    color: ${C.text};
    -webkit-font-smoothing: antialiased;
  }
  input, select { caret-color: ${C.orange}; }
  input::placeholder { color: ${C.dim}; }
  a { color: inherit; text-decoration: none; }

  ::-webkit-scrollbar { width: 5px; height: 5px; }
  ::-webkit-scrollbar-track { background: ${C.bg}; }
  ::-webkit-scrollbar-thumb { background: ${C.border2}; border-radius: 99px; }

  @keyframes fadeUp {
    from { opacity:0; transform:translateY(10px); }
    to   { opacity:1; transform:translateY(0); }
  }
  @keyframes fadeIn {
    from { opacity:0; } to { opacity:1; }
  }
  @keyframes shimmer {
    0%   { background-position:-400px 0; }
    100% { background-position: 400px 0; }
  }
  @keyframes pulse {
    0%,100% { opacity:1; } 50% { opacity:0.35; }
  }
  @keyframes spin {
    from { transform:rotate(0deg); } to { transform:rotate(360deg); }
  }
  @keyframes slideDown {
    from { opacity:0; transform:translateY(-6px); }
    to   { opacity:1; transform:translateY(0); }
  }
  @keyframes scoreGrow { from { width:0; } }

  .card:hover { background:${C.surface2} !important; border-color:${C.border2} !important; transform:translateX(2px); }
  .card { transition: background .12s, border-color .15s, transform .1s; }
  .suggest-item:hover { background:${C.surface3} !important; color:${C.orange} !important; }
  .tab-btn:hover { color:${C.text} !important; }
  .pill:hover { opacity:.8; transform:translateY(-1px); }
  .pill { transition: all .15s; }
  .trend-card:hover { border-color:${C.border2} !important; transform:translateY(-2px); box-shadow:0 4px 20px rgba(0,0,0,0.4); }
  .trend-card { transition: all .15s; }
`;

// ── Hooks ─────────────────────────────────────────────────────────────────────
function useDebounce(value, delay) {
  const [d, setD] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setD(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return d;
}

function useHealth() {
  const [h, setH] = useState(null);
  useEffect(() => {
    fetch(`${API}/api/health`).then(r => r.json()).then(setH)
      .catch(() => setH({ engine: 'error' }));
  }, []);
  return h;
}

function useStats() {
  const [s, setS] = useState(null);
  useEffect(() => {
    fetch(`${API}/api/stats`).then(r => r.json()).then(setS).catch(() => {});
  }, []);
  return s;
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function fmt(n) {
  if (n == null) return '0';
  if (n >= 1000000) return (n/1000000).toFixed(1)+'m';
  if (n >= 1000)    return (n/1000).toFixed(1)+'k';
  return String(n);
}

function timeAgo(utc) {
  if (!utc) return '';
  const d = Math.floor(Date.now()/1000) - utc;
  if (d < 60)      return 'just now';
  if (d < 3600)    return Math.floor(d/60)+'m ago';
  if (d < 86400)   return Math.floor(d/3600)+'h ago';
  if (d < 2592000) return Math.floor(d/86400)+'d ago';
  if (d < 31536000)return Math.floor(d/2592000)+'mo ago';
  return Math.floor(d/31536000)+'y ago';
}

// ── Spinner ───────────────────────────────────────────────────────────────────
function Spinner({ size = 14 }) {
  return (
    <div style={{
      width: size, height: size, flexShrink: 0,
      border: `2px solid ${C.border2}`,
      borderTop: `2px solid ${C.orange}`,
      borderRadius: '50%',
      animation: 'spin 0.7s linear infinite',
    }} />
  );
}

// ── Skeleton ──────────────────────────────────────────────────────────────────
const SH_BG = `linear-gradient(90deg, #0e0e11 25%, #141417 50%, #0e0e11 75%)`;

function SkeletonCard() {
  const bar = (w, h=10, delay='0s') => (
    <div style={{
      height: h, width: w, borderRadius: 99,
      background: SH_BG, backgroundSize: '400px 100%',
      animation: `shimmer 1.4s ease infinite ${delay}`,
    }}/>
  );
  return (
    <div style={{
      display:'flex', background:C.surface, border:`1px solid ${C.border}`,
      borderRadius:8, marginBottom:8, overflow:'hidden', animation:'fadeIn .3s ease',
    }}>
      <div style={{ width:44, background:C.bg, borderRight:`1px solid ${C.border}`, flexShrink:0 }}/>
      <div style={{ padding:'14px 16px', flex:1, display:'flex', flexDirection:'column', gap:10 }}>
        <div style={{ display:'flex', gap:8 }}>
          {[['28%','0s'],['15%','.1s'],['12%','.2s']].map(([w,d],i)=>(
            <div key={i} style={{ height:9, width:w, borderRadius:99, background:SH_BG, backgroundSize:'400px 100%', animation:`shimmer 1.4s ease infinite ${d}` }}/>
          ))}
        </div>
        {bar('82%', 13, '.1s')}
        {bar('58%', 13, '.2s')}
        <div style={{ display:'flex', gap:12 }}>
          {[['18%','0s'],['20%','.1s'],['14%','.2s']].map(([w,d],i)=>(
            <div key={i} style={{ height:9, width:w, borderRadius:99, background:SH_BG, backgroundSize:'400px 100%', animation:`shimmer 1.4s ease infinite ${d}` }}/>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── Result Card ───────────────────────────────────────────────────────────────
function ResultCard({ result, rank, maxScore, algo, delay=0 }) {
  const color    = ALGO_COLORS[algo] || C.orange;
  const scorePct = Math.min((result.score / (maxScore||1)) * 100, 100);

  return (
    <a href={result.url} target="_blank" rel="noreferrer" className="card"
      style={{
        display:'flex', background:C.surface, border:`1px solid ${C.border}`,
        borderRadius:8, marginBottom:8, overflow:'hidden', color:'inherit',
        animation:'fadeUp .3s ease both', animationDelay:`${delay}ms`,
      }}
    >
      {/* Upvote col */}
      <div style={{
        width:44, background:C.bg, display:'flex', flexDirection:'column',
        alignItems:'center', justifyContent:'center', padding:'10px 4px',
        gap:3, flexShrink:0, borderRight:`1px solid ${C.border}`,
      }}>
        <span style={{ color: result.upvotes>0 ? C.orange : C.dim, fontSize:13 }}>▲</span>
        <span style={{
          fontFamily:"'IBM Plex Mono',monospace", fontSize:10,
          color: result.upvotes>0 ? C.orange : C.muted,
          fontWeight:700, textAlign:'center', lineHeight:1.2,
        }}>{result.upvotes!=null ? fmt(result.upvotes) : '—'}</span>
        <span style={{ color:C.dim, fontSize:11 }}>▼</span>
      </div>

      {/* Content */}
      <div style={{ padding:'12px 16px', flex:1, minWidth:0 }}>

        {/* Meta */}
        <div style={{ display:'flex', alignItems:'center', gap:6, marginBottom:7, flexWrap:'wrap' }}>
          <span style={{
            fontSize:11, fontWeight:700, color:C.text,
            fontFamily:"'IBM Plex Mono',monospace",
            background:C.surface3, padding:'2px 8px', borderRadius:99,
            border:`1px solid ${C.border2}`,
          }}>r/{result.subreddit||'general'}</span>

          <span style={{ color:C.dim, fontSize:9 }}>•</span>

          {result.author && (
            <span style={{ fontSize:11, color:C.teal }}>
              {result.author.startsWith('u/') ? result.author : `u/${result.author}`}
            </span>
          )}

          {result.createdUtc && (
            <><span style={{ color:C.dim, fontSize:9 }}>•</span>
            <span style={{ fontSize:11, color:C.muted }}>{timeAgo(result.createdUtc)}</span></>
          )}

          {result.isNsfw && (
            <span style={{
              fontSize:9, fontFamily:"'IBM Plex Mono',monospace", fontWeight:700,
              color:C.nsfw, background:'rgba(255,88,91,.1)',
              border:'1px solid rgba(255,88,91,.25)', padding:'1px 6px', borderRadius:3,
            }}>NSFW</span>
          )}
          {result.flair?.trim() && (
            <span style={{
              fontSize:9, fontFamily:"'IBM Plex Mono',monospace",
              color:C.teal, background:C.tealBg,
              border:'1px solid rgba(13,211,187,.2)', padding:'1px 6px', borderRadius:3,
            }}>{result.flair}</span>
          )}
        </div>

        {/* Title */}
        <div style={{ fontSize:14, fontWeight:600, color:C.white, lineHeight:1.5, marginBottom:10 }}>
          {result.title}
        </div>

        {/* Bottom */}
        <div style={{ display:'flex', alignItems:'center', gap:12, flexWrap:'wrap' }}>
          {result.comments!=null && (
            <span style={{ fontSize:11, color:C.muted }}>
              💬 <span style={{ color:C.text }}>{fmt(result.comments)}</span>
            </span>
          )}

          {/* Score bar */}
          <div style={{ display:'flex', alignItems:'center', gap:6 }}>
            <div style={{ width:60, height:3, background:C.border2, borderRadius:99, overflow:'hidden' }}>
              <div style={{
                height:'100%', width:`${scorePct}%`, background:color,
                borderRadius:99, animation:'scoreGrow .6s ease both',
                animationDelay:`${delay+200}ms`,
              }}/>
            </div>
            <span style={{ fontSize:10, fontFamily:"'IBM Plex Mono',monospace", color }}>
              {result.score?.toFixed(3)}
            </span>
          </div>

          <span style={{
            fontSize:9, fontFamily:"'IBM Plex Mono',monospace",
            color, background:`${color}15`, border:`1px solid ${color}30`,
            padding:'2px 7px', borderRadius:3,
          }}>#{rank} · {algo?.toUpperCase()}</span>

          <span style={{ fontSize:10, color:C.muted, marginLeft:'auto' }}>reddit.com ↗</span>
        </div>
      </div>
    </a>
  );
}

// ── Autocomplete ──────────────────────────────────────────────────────────────
function Autocomplete({ suggestions, onSelect, visible }) {
  if (!visible || !suggestions.length) return null;
  return (
    <div style={{
      position:'absolute', top:'calc(100% + 6px)', left:0, right:0,
      background:C.surface2, border:`1px solid ${C.border2}`,
      borderRadius:10, overflow:'hidden', zIndex:300,
      boxShadow:`0 8px 32px rgba(0,0,0,.5)`,
      animation:'slideDown .15s ease',
    }}>
      {suggestions.map((s, i) => (
        <div key={i} className="suggest-item"
          onMouseDown={() => onSelect(s)}
          style={{
            padding:'10px 16px', fontSize:13, cursor:'pointer', color:C.text,
            borderBottom: i<suggestions.length-1 ? `1px solid ${C.border}` : 'none',
            display:'flex', alignItems:'center', gap:10, transition:'background .1s',
          }}
        >
          <span style={{ color:C.muted, fontSize:12, flexShrink:0 }}>🔍</span>
          <span style={{ flex:1 }}>{s}</span>
          <span style={{ fontSize:10, color:C.dim, fontFamily:"'IBM Plex Mono',monospace" }}>↵</span>
        </div>
      ))}
    </div>
  );
}

// ── Search Tab ────────────────────────────────────────────────────────────────
function SearchTab({ engineReady }) {
  const [query,      setQuery]      = useState('');
  const [algo,       setAlgo]       = useState('bm25');
  const [results,    setResults]    = useState(null);
  const [loading,    setLoading]    = useState(false);
  const [meta,       setMeta]       = useState(null);
  const [topN,       setTopN]       = useState(10);
  const [subreddit,  setSubreddit]  = useState('');
  const [suggests,   setSuggests]   = useState([]);
  const [showSug,    setShowSug]    = useState(false);
  const inputRef = useRef(null);
  const dq = useDebounce(query, 280);

  // fetch backend suggestions
  const fetchSuggests = useCallback(async (q) => {
    if (!q.trim() || q.length < 2) { setSuggests([]); return; }
    try {
      const res  = await fetch(`${API}/api/suggest?q=${encodeURIComponent(q)}`);
      const data = await res.json();
      setSuggests(data.suggestions || []);
    } catch { setSuggests([]); }
  }, []);

  const doSearch = useCallback(async (q, a, sub, top) => {
    if (!q.trim()) { setResults(null); setMeta(null); return; }
    setLoading(true);
    try {
      const t0  = performance.now();
      const url = sub.trim()
        ? `${API}/api/search?q=${encodeURIComponent(q)}&algo=${a}&top=${top}&subreddit=${encodeURIComponent(sub)}`
        : `${API}/api/search?q=${encodeURIComponent(q)}&algo=${a}&top=${top}`;
      const res  = await fetch(url);
      const data = await res.json();
      setResults(data.results || []);
      setMeta({
        hits:     data.totalHits,
        fetched:  data.fetchedPosts,
        serverMs: data.timeTakenMs,
        clientMs: Math.round(performance.now() - t0),
      });
    } catch { setResults([]); }
    finally   { setLoading(false); }
  }, []);

  useEffect(() => { fetchSuggests(dq); }, [dq, fetchSuggests]);
  useEffect(() => { doSearch(dq, algo, subreddit, topN); }, [dq, algo, subreddit, topN, doSearch]);

  const pickSuggestion = (s) => {
    setQuery(s); setShowSug(false);
    inputRef.current?.focus();
  };

  const maxScore = results?.[0]?.score || 1;

  return (
    <div>

      {/* ── Search box ── */}
      <div style={{ position:'relative', marginBottom:12 }}>
        <div style={{
          display:'flex', alignItems:'center', gap:12,
          background:C.surface,
          border:`1.5px solid ${query ? C.orange : C.border2}`,
          borderRadius:12, padding:'0 16px',
          boxShadow: query ? `0 0 0 3px ${C.orangeGlow}` : 'none',
          transition:'border-color .2s, box-shadow .2s',
        }}>
          {loading ? <Spinner/> : <span style={{ color:C.muted, fontSize:16, flexShrink:0 }}>🔍</span>}
          <input
            ref={inputRef}
            autoFocus
            style={{
              flex:1, background:'transparent', border:'none',
              padding:'14px 0', fontSize:15, color:C.text, outline:'none',
              fontFamily:"'DM Sans',sans-serif",
            }}
            placeholder="Search Reddit in real-time..."
            value={query}
            onChange={e => { setQuery(e.target.value); setShowSug(true); }}
            onFocus={() => setShowSug(true)}
            onBlur={() => setTimeout(() => setShowSug(false), 150)}
            onKeyDown={e => {
              if (e.key === 'Escape') { setShowSug(false); }
              if (e.key === 'Enter')  { setShowSug(false); }
            }}
          />
          {query && (
            <button onClick={() => { setQuery(''); setResults(null); setMeta(null); setSuggests([]); inputRef.current?.focus(); }}
              style={{
                background:C.border2, border:'none', color:C.muted,
                cursor:'pointer', width:22, height:22, borderRadius:'50%',
                display:'flex', alignItems:'center', justifyContent:'center',
                fontSize:13, flexShrink:0,
              }}>×</button>
          )}
        </div>
        <Autocomplete suggestions={suggests} onSelect={pickSuggestion} visible={showSug && suggests.length > 0}/>
      </div>

      {/* ── Filters ── */}
      <div style={{ display:'flex', gap:8, marginBottom:12, alignItems:'center', flexWrap:'wrap' }}>
        {ALGOS.map(a => (
          <button key={a} className="pill" onClick={() => setAlgo(a)} style={{
            padding:'5px 14px', borderRadius:99, fontSize:11, cursor:'pointer',
            fontFamily:"'IBM Plex Mono',monospace", fontWeight: algo===a ? 700 : 400,
            border:`1.5px solid ${algo===a ? ALGO_COLORS[a] : C.border2}`,
            background: algo===a ? `${ALGO_COLORS[a]}15` : 'transparent',
            color: algo===a ? ALGO_COLORS[a] : C.muted,
          }}>{ALGO_LABELS[a]}</button>
        ))}

        <div style={{ width:1, height:18, background:C.border2, margin:'0 2px' }}/>

        {/* Subreddit filter */}
        <div style={{ display:'flex', alignItems:'center', gap:5 }}>
          <span style={{ fontSize:11, color:C.muted, fontFamily:"'IBM Plex Mono',monospace" }}>r/</span>
          <input
            style={{
              background:C.surface2, border:`1px solid ${C.border2}`,
              borderRadius:6, padding:'5px 10px', fontSize:11,
              color:C.text, outline:'none', width:110,
              fontFamily:"'IBM Plex Mono',monospace",
            }}
            placeholder="subreddit"
            value={subreddit}
            onChange={e => setSubreddit(e.target.value)}
          />
        </div>

        {/* Top N */}
        <select value={topN} onChange={e => setTopN(Number(e.target.value))} style={{
          background:C.surface2, border:`1px solid ${C.border2}`,
          borderRadius:6, padding:'5px 8px', fontSize:11, color:C.muted,
          outline:'none', cursor:'pointer', fontFamily:"'IBM Plex Mono',monospace",
        }}>
          {[5,10,20,50].map(n=>(
            <option key={n} value={n}>{n} results</option>
          ))}
        </select>
      </div>

      {/* ── Algo strip ── */}
      <div style={{
        fontSize:11, color:C.muted, fontFamily:"'IBM Plex Mono',monospace",
        marginBottom:14, padding:'7px 12px',
        background:C.surface, borderRadius:6, border:`1px solid ${C.border}`,
        borderLeft:`3px solid ${ALGO_COLORS[algo]}`,
      }}>{ALGO_DESC[algo]}</div>

      {/* ── Meta ── */}
      {meta && !loading && (
        <div style={{
          display:'flex', justifyContent:'space-between', alignItems:'center',
          marginBottom:12, fontSize:11,
          color:C.muted, fontFamily:"'IBM Plex Mono',monospace",
          animation:'fadeIn .2s ease',
        }}>
          <span>
            <span style={{ color:C.orange, fontWeight:700 }}>{meta.hits}</span>
            {' '}results for <span style={{ color:C.text }}>"{query}"</span>
            {subreddit.trim() && <span style={{ color:C.teal }}> in r/{subreddit}</span>}
          </span>
          <span style={{ display:'flex', alignItems:'center', gap:8 }}>
            <span style={{ color:C.green }}>⚡ {meta.serverMs}ms</span>
            <span style={{ color:C.dim }}>·</span>
            <span>{meta.clientMs}ms total · {meta.fetched} fetched</span>
          </span>
        </div>
      )}

      {/* ── States ── */}
      {loading && [1,2,3,4,5].map(i=><SkeletonCard key={i}/>)}

      {!loading && !query && (
        <div style={{ textAlign:'center', padding:'60px 0', animation:'fadeIn .3s ease' }}>
          <div style={{ fontSize:44, marginBottom:14 }}>🔍</div>
          <div style={{ fontSize:15, color:C.text, fontWeight:600, marginBottom:8 }}>
            Real-time Reddit Search
          </div>
          <div style={{ fontSize:13, color:C.muted, marginBottom:24 }}>
            {engineReady ? 'Results fetched live from Reddit via PullPush' : 'Connecting to backend...'}
          </div>
          <div style={{ display:'flex', gap:8, flexWrap:'wrap', justifyContent:'center' }}>
            {TRENDING.slice(0,6).map((q,i) => (
              <button key={i} className="pill" onClick={() => setQuery(q)} style={{
                padding:'6px 14px', borderRadius:99, fontSize:12, cursor:'pointer',
                background:C.surface, border:`1px solid ${C.border2}`, color:C.muted,
              }}>{q}</button>
            ))}
          </div>
        </div>
      )}

      {!loading && results?.length===0 && query && (
        <div style={{ textAlign:'center', padding:'60px 0', animation:'fadeIn .3s ease' }}>
          <div style={{ fontSize:44, marginBottom:12 }}>🤔</div>
          <div style={{ fontSize:15, color:C.text, marginBottom:8 }}>No results for "{query}"</div>
          <div style={{ fontSize:13, color:C.muted }}>Try a different query or algorithm</div>
        </div>
      )}

      {!loading && results?.map((r,i) => (
        <ResultCard key={i} result={r} rank={i+1} maxScore={maxScore} algo={algo} delay={i*40}/>
      ))}
    </div>
  );
}

// ── Compare Tab ───────────────────────────────────────────────────────────────
function CompareTab() {
  const [query,   setQuery]   = useState('');
  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(false);
  const dq = useDebounce(query, 400);

  const doCompare = useCallback(async (q) => {
    if (!q.trim()) { setData(null); return; }
    setLoading(true);
    try {
      const res  = await fetch(`${API}/api/compare?q=${encodeURIComponent(q)}&top=5`);
      setData(await res.json());
    } catch { setData(null); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { doCompare(dq); }, [dq, doCompare]);

  return (
    <div>
      <div style={{ marginBottom:16 }}>
        <div style={{
          display:'flex', alignItems:'center', gap:12,
          background:C.surface,
          border:`1.5px solid ${query ? C.teal : C.border2}`,
          borderRadius:12, padding:'0 16px',
          boxShadow: query ? '0 0 0 3px rgba(13,211,187,.1)' : 'none',
          transition:'border-color .2s, box-shadow .2s',
        }}>
          <span style={{ color:C.muted, fontSize:15 }}>⊞</span>
          <input autoFocus
            style={{ flex:1, background:'transparent', border:'none', padding:'14px 0', fontSize:15, color:C.text, outline:'none' }}
            placeholder="Compare all 3 algorithms side-by-side..."
            value={query}
            onChange={e => setQuery(e.target.value)}
          />
          {loading && <Spinner/>}
        </div>
      </div>

      {!query && (
        <div style={{ textAlign:'center', padding:'60px 0', color:C.muted }}>
          <div style={{ fontSize:44, marginBottom:12 }}>⊞</div>
          <div style={{ fontSize:14 }}>See how BM25, TF-IDF and Vector rank the same query differently</div>
        </div>
      )}

      {loading && (
        <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
          {ALGOS.map(a => (
            <div key={a} style={{ background:C.surface, border:`1px solid ${C.border}`, borderRadius:8, overflow:'hidden' }}>
              <div style={{ padding:'10px 14px', borderBottom:`1px solid ${C.border}` }}>
                <div style={{ height:10, width:'40%', background:C.border2, borderRadius:99 }}/>
              </div>
              {[1,2,3].map(i=><SkeletonCard key={i}/>)}
            </div>
          ))}
        </div>
      )}

      {!loading && data && (
        <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12, animation:'fadeUp .3s ease' }}>
          {ALGOS.map(a => {
            const res   = data[a] || [];
            const color = ALGO_COLORS[a];
            return (
              <div key={a} style={{
                background:C.surface, border:`1px solid ${C.border}`,
                borderRadius:8, overflow:'hidden',
              }}>
                <div style={{
                  padding:'10px 14px', borderBottom:`1px solid ${C.border}`,
                  background:`${color}08`, borderTop:`2px solid ${color}`,
                  display:'flex', alignItems:'center', gap:8,
                }}>
                  <span style={{ fontFamily:"'IBM Plex Mono',monospace", fontSize:12, fontWeight:700, color }}>{ALGO_LABELS[a]}</span>
                  <span style={{ fontSize:10, color:C.muted }}>{res.length} results</span>
                </div>
                {res.length === 0 ? (
                  <div style={{ padding:20, textAlign:'center', color:C.muted, fontSize:12 }}>No results</div>
                ) : res.map((r,i) => (
                  <a key={i} href={r.url} target="_blank" rel="noreferrer" className="card"
                    style={{ display:'block', padding:'10px 12px', borderBottom:`1px solid ${C.border}`, color:'inherit' }}
                  >
                    <div style={{ display:'flex', alignItems:'flex-start', gap:7, marginBottom:5 }}>
                      <span style={{
                        fontSize:9, fontFamily:"'IBM Plex Mono',monospace", fontWeight:700,
                        color, background:`${color}15`, padding:'2px 5px', borderRadius:3,
                        flexShrink:0, marginTop:2,
                      }}>#{i+1}</span>
                      <div style={{ fontSize:12, color:C.text, lineHeight:1.4, fontWeight:500 }}>
                        {r.title?.slice(0,70)}{r.title?.length>70?'…':''}
                      </div>
                    </div>
                    <div style={{ display:'flex', gap:8, alignItems:'center', paddingLeft:20 }}>
                      {r.subreddit && <span style={{ fontSize:9, color:C.muted, fontFamily:"'IBM Plex Mono',monospace" }}>r/{r.subreddit}</span>}
                      {r.upvotes!=null && <span style={{ fontSize:9, color:C.orange }}>▲ {fmt(r.upvotes)}</span>}
                      <span style={{ fontSize:9, color, fontFamily:"'IBM Plex Mono',monospace" }}>{r.score?.toFixed(4)}</span>
                      {r.isNsfw && <span style={{ fontSize:9, color:C.nsfw, fontWeight:700 }}>NSFW</span>}
                    </div>
                  </a>
                ))}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

// ── Trending Tab ──────────────────────────────────────────────────────────────
function TrendingTab() {
  const [results,     setResults]     = useState({});
  const [fetchLoading,setFetchLoading]= useState(true);
  const [active,      setActive]      = useState(null);
  const [detail,      setDetail]      = useState(null);
  const [detailLoad,  setDetailLoad]  = useState(false);

  useEffect(() => {
    Promise.all(
      TRENDING.slice(0,8).map(q =>
        fetch(`${API}/api/search?q=${encodeURIComponent(q)}&algo=bm25&top=1`)
          .then(r=>r.json())
          .then(d=>({ query:q, top:d.results?.[0]||null, hits:d.totalHits }))
          .catch(()=>({ query:q, top:null, hits:0 }))
      )
    ).then(data => {
      const map={};
      data.forEach(d=>{ map[d.query]=d; });
      setResults(map);
      setFetchLoading(false);
    });
  }, []);

  const handleClick = async (q) => {
    if (active===q) { setActive(null); setDetail(null); return; }
    setActive(q); setDetailLoad(true);
    try {
      const res  = await fetch(`${API}/api/search?q=${encodeURIComponent(q)}&algo=bm25&top=5`);
      const data = await res.json();
      setDetail({ query:q, results:data.results||[] });
    } catch { setDetail({ query:q, results:[] }); }
    finally { setDetailLoad(false); }
  };

  return (
    <div>
      <p style={{ fontSize:13, color:C.muted, marginBottom:20, lineHeight:1.6 }}>
        Hot topics in the corpus. Click any card to explore the top posts.
      </p>

      <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(190px,1fr))', gap:10, marginBottom:24 }}>
        {TRENDING.slice(0,8).map((q,i) => {
          const item=results[q], isActive=active===q;
          return (
            <div key={q} onClick={()=>handleClick(q)} className="trend-card"
              style={{
                background: isActive ? C.orangeBg : C.surface,
                border:`1px solid ${isActive ? C.orange : C.border}`,
                borderRadius:10, padding:'14px 16px', cursor:'pointer',
                animation:`fadeUp .3s ease both`, animationDelay:`${i*50}ms`,
              }}
            >
              <div style={{ display:'flex', justifyContent:'space-between', marginBottom:8 }}>
                <span style={{ fontFamily:"'IBM Plex Mono',monospace", fontSize:10, color:C.muted }}>#{i+1}</span>
                <span style={{
                  fontSize:10, fontFamily:"'IBM Plex Mono',monospace",
                  color: isActive ? C.orange : C.muted,
                  background: isActive ? C.orangeBg : C.border,
                  padding:'1px 6px', borderRadius:3,
                }}>{fetchLoading ? '···' : `${item?.hits??0} posts`}</span>
              </div>
              <div style={{ fontSize:13, fontWeight:600, color: isActive ? C.orange : C.text, marginBottom:5, textTransform:'capitalize' }}>
                {q}
              </div>
              {item?.top && (
                <div style={{ fontSize:11, color:C.muted, lineHeight:1.4, marginBottom:5 }}>
                  {item.top.title?.slice(0,50)}{item.top.title?.length>50?'…':''}
                </div>
              )}
              <div style={{ fontSize:10, color: isActive ? C.orange : C.teal, marginTop:4 }}>
                {isActive ? '▲ collapse' : '→ explore'}
              </div>
            </div>
          );
        })}
      </div>

      {active && (
        <div style={{
          background:C.surface, border:`1px solid ${C.border}`,
          borderTop:`2px solid ${C.orange}`, borderRadius:8, overflow:'hidden',
          animation:'slideDown .2s ease',
        }}>
          <div style={{
            padding:'12px 16px', borderBottom:`1px solid ${C.border}`,
            display:'flex', alignItems:'center', gap:10, background:C.orangeBg,
          }}>
            <span style={{ fontSize:14 }}>📈</span>
            <span style={{ fontFamily:"'IBM Plex Mono',monospace", fontSize:13, fontWeight:700, color:C.orange }}>{active}</span>
            <span style={{ fontSize:11, color:C.muted }}>— top posts</span>
          </div>
          {detailLoad
            ? [1,2,3].map(i=><SkeletonCard key={i}/>)
            : detail?.results.map((r,i)=>(
                <ResultCard key={i} result={r} rank={i+1} maxScore={detail.results[0]?.score} algo="bm25" delay={i*50}/>
              ))
          }
        </div>
      )}
    </div>
  );
}

// ── Stats Tab ─────────────────────────────────────────────────────────────────
function StatsTab() {
  const stats = useStats();
  if (!stats) return (
    <div style={{ textAlign:'center', padding:'60px 0', color:C.muted }}>
      <div style={{ fontSize:40, marginBottom:12, animation:'pulse 1.5s ease infinite' }}>⏳</div>
      <div style={{ fontSize:14 }}>Loading stats...</div>
    </div>
  );

  return (
    <div style={{ animation:'fadeUp .3s ease' }}>
      <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(140px,1fr))', gap:10, marginBottom:20 }}>
        {[
          { val:stats.corpus,             label:'Articles',     color:C.orange  },
          { val:stats.vocabulary,         label:'Unique Terms', color:'#6366f1' },
          { val:stats.algorithms?.length, label:'Algorithms',   color:C.teal    },
          { val:'8080',                   label:'API Port',     color:C.yellow  },
        ].map((s,i) => (
          <div key={i} style={{
            background:C.surface, border:`1px solid ${C.border}`,
            borderRadius:8, padding:'18px 16px', borderTop:`2px solid ${s.color}`,
            animation:`fadeUp .3s ease both`, animationDelay:`${i*60}ms`,
          }}>
            <div style={{ fontFamily:"'IBM Plex Mono',monospace", fontSize:26, fontWeight:700, color:s.color, lineHeight:1, marginBottom:6 }}>
              {s.val??'—'}
            </div>
            <div style={{ fontSize:11, color:C.muted, textTransform:'uppercase', letterSpacing:'1px' }}>{s.label}</div>
          </div>
        ))}
      </div>

      {/* Algorithms */}
      <div style={{ background:C.surface, border:`1px solid ${C.border}`, borderRadius:8, padding:18, marginBottom:12 }}>
        <div style={{ fontSize:11, fontFamily:"'IBM Plex Mono',monospace", color:C.muted, letterSpacing:'1px', textTransform:'uppercase', marginBottom:12 }}>
          Ranking Algorithms
        </div>
        {ALGOS.map((a,i) => (
          <div key={a} style={{
            display:'flex', alignItems:'center', gap:12, padding:'10px 0',
            borderBottom: i<ALGOS.length-1 ? `1px solid ${C.border}` : 'none',
          }}>
            <span style={{
              fontFamily:"'IBM Plex Mono',monospace", fontSize:12, fontWeight:700,
              color:ALGO_COLORS[a], background:`${ALGO_COLORS[a]}15`,
              padding:'3px 10px', borderRadius:99, minWidth:60, textAlign:'center',
            }}>{ALGO_LABELS[a]}</span>
            <span style={{ fontSize:12, color:C.muted }}>{ALGO_DESC[a]}</span>
          </div>
        ))}
      </div>

      {/* Endpoints */}
      <div style={{ background:C.surface, border:`1px solid ${C.border}`, borderRadius:8, padding:18 }}>
        <div style={{ fontSize:11, fontFamily:"'IBM Plex Mono',monospace", color:C.muted, letterSpacing:'1px', textTransform:'uppercase', marginBottom:12 }}>
          API Endpoints
        </div>
        {stats.endpoints?.map((ep,i) => (
          <div key={i} style={{
            fontFamily:"'IBM Plex Mono',monospace", fontSize:11, color:C.text,
            padding:'7px 0', borderBottom: i<stats.endpoints.length-1 ? `1px solid ${C.border}` : 'none',
          }}>
            <span style={{ color:C.green }}>GET </span>{ep.replace('GET ','')}
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Root ──────────────────────────────────────────────────────────────────────
export default function App() {
  const [tab, setTab] = useState('search');
  const health = useHealth();
  const ready  = health?.engine === 'ready';

  useEffect(() => {
    const s = document.createElement('style');
    s.textContent = GLOBAL_CSS;
    document.head.appendChild(s);
    return () => document.head.removeChild(s);
  }, []);

  const TABS = [
    { id:'search',   label:'🔍 Search'   },
    { id:'compare',  label:'⊞ Compare'   },
    { id:'trending', label:'📈 Trending'  },
    { id:'stats',    label:'📊 Stats'     },
  ];

  return (
    <div style={{ minHeight:'100vh', background:C.bg, color:C.text }}>

      {/* Topbar */}
      <div style={{
        background:'rgba(14,14,17,0.95)', backdropFilter:'blur(12px)',
        borderBottom:`1px solid ${C.border}`,
        position:'sticky', top:0, zIndex:100,
      }}>
        <div style={{
          maxWidth:1000, margin:'0 auto', padding:'0 20px',
          height:50, display:'flex', alignItems:'center', justifyContent:'space-between',
        }}>
          {/* Logo */}
          <div style={{ display:'flex', alignItems:'center', gap:10 }}>
            <div style={{
              width:30, height:30, borderRadius:8,
              background:`linear-gradient(135deg,${C.orange},#cc3700)`,
              display:'flex', alignItems:'center', justifyContent:'center',
              fontWeight:900, fontSize:13, color:'#fff',
              fontFamily:"'IBM Plex Mono',monospace",
              boxShadow:`0 2px 8px ${C.orangeGlow}`,
            }}>T</div>
            <div>
              <div style={{ fontFamily:"'IBM Plex Mono',monospace", fontWeight:700, fontSize:14, color:C.white }}>Trek v1</div>
              <div style={{ fontSize:9, color:C.muted, letterSpacing:'0.5px' }}>REDDIT SEARCH ENGINE</div>
            </div>
          </div>

          {/* Status pill */}
          <div style={{
            display:'flex', alignItems:'center', gap:6,
            fontSize:11, color: ready ? C.green : C.muted,
            fontFamily:"'IBM Plex Mono',monospace",
            background:C.surface, border:`1px solid ${C.border}`,
            padding:'4px 10px', borderRadius:99,
          }}>
            <div style={{
              width:6, height:6, borderRadius:'50%',
              background: ready ? C.green : C.muted,
              boxShadow: ready ? `0 0 6px ${C.green}` : 'none',
              animation: ready ? 'pulse 2s ease infinite' : 'none',
            }}/>
            {ready ? 'live · PullPush' : health?.engine==='error' ? 'offline — start backend' : 'connecting...'}
          </div>
        </div>

        {/* Tabs */}
        <div style={{ borderTop:`1px solid ${C.border}` }}>
          <div style={{ maxWidth:1000, margin:'0 auto', padding:'0 20px', display:'flex' }}>
            {TABS.map(t => (
              <button key={t.id} onClick={() => setTab(t.id)} className="tab-btn" style={{
                padding:'9px 16px', background:'none', border:'none',
                borderBottom: tab===t.id ? `2px solid ${C.orange}` : '2px solid transparent',
                color: tab===t.id ? C.white : C.muted,
                fontSize:12, fontWeight: tab===t.id ? 700 : 400,
                cursor:'pointer', fontFamily:"'DM Sans',sans-serif",
                transition:'color .15s', whiteSpace:'nowrap',
              }}>{t.label}</button>
            ))}
          </div>
        </div>
      </div>

      {/* Main grid */}
      <div style={{
        maxWidth:1000, margin:'0 auto', padding:'24px 20px 80px',
        display:'grid',
        gridTemplateColumns: tab==='compare' ? '1fr' : '1fr 260px',
        gap:20, alignItems:'start',
      }}>

        {/* Left — main content */}
        <div>
          {tab==='search'   && <SearchTab   engineReady={ready}/>}
          {tab==='compare'  && <CompareTab />}
          {tab==='trending' && <TrendingTab/>}
          {tab==='stats'    && <StatsTab   />}
        </div>

        {/* Right — sidebar (hidden on compare) */}
        {tab !== 'compare' && (
          <div style={{ position:'sticky', top:90 }}>

            {/* About box */}
            <div style={{ background:C.surface, border:`1px solid ${C.border}`, borderRadius:10, overflow:'hidden', marginBottom:12 }}>
              <div style={{ background:`linear-gradient(135deg,${C.orange},#cc3700)`, padding:'10px 14px', fontWeight:700, fontSize:12, color:'#fff', letterSpacing:'0.3px' }}>
                About Trek v1
              </div>
              <div style={{ padding:14 }}>
                <p style={{ fontSize:12, color:C.muted, lineHeight:1.7, marginBottom:12 }}>
                  Full-text Reddit search engine. Fetches posts live via PullPush and ranks them using 3 IR algorithms built from scratch in Java.
                </p>
                {[
                  { label:'Mode',       val:'Real-time live',      color:C.green  },
                  { label:'Algorithms', val:'BM25 · TF-IDF · Vec', color:C.orange },
                  { label:'Source',     val:'PullPush API',         color:C.teal   },
                  { label:'NSFW',       val:'included',             color:C.nsfw   },
                ].map((row,i)=>(
                  <div key={i} style={{
                    display:'flex', justifyContent:'space-between',
                    padding:'5px 0', borderBottom: i<3 ? `1px solid ${C.border}` : 'none',
                  }}>
                    <span style={{ fontSize:11, color:C.muted }}>{row.label}</span>
                    <span style={{ fontSize:11, color:row.color, fontWeight:600 }}>{row.val}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Algo guide */}
            <div style={{ background:C.surface, border:`1px solid ${C.border}`, borderRadius:10, padding:14, marginBottom:12 }}>
              <div style={{ fontSize:10, fontWeight:700, color:C.muted, textTransform:'uppercase', letterSpacing:'1.5px', marginBottom:10 }}>Algorithms</div>
              {ALGOS.map((a,i) => (
                <div key={a} style={{ display:'flex', alignItems:'center', gap:8, padding:'6px 0', borderBottom: i<2 ? `1px solid ${C.border}` : 'none' }}>
                  <span style={{
                    fontSize:9, fontFamily:"'IBM Plex Mono',monospace", fontWeight:700,
                    color:ALGO_COLORS[a], background:`${ALGO_COLORS[a]}15`,
                    padding:'2px 7px', borderRadius:99, minWidth:48, textAlign:'center',
                  }}>{ALGO_LABELS[a]}</span>
                  <span style={{ fontSize:10, color:C.muted }}>
                    {a==='bm25' ? 'Best overall, length-aware' : a==='tfidf' ? 'Classic, fast, reliable' : 'Angle-based similarity'}
                  </span>
                </div>
              ))}
            </div>

            {/* Try these */}
            <div style={{ background:C.surface, border:`1px solid ${C.border}`, borderRadius:10, padding:14 }}>
              <div style={{ fontSize:10, fontWeight:700, color:C.muted, textTransform:'uppercase', letterSpacing:'1.5px', marginBottom:10 }}>Try These</div>
              {TRENDING.slice(0,7).map((q,i)=>(
                <div key={i} style={{
                  fontSize:12, color:C.teal, padding:'5px 0', cursor:'pointer',
                  borderBottom: i<6 ? `1px solid ${C.border}` : 'none',
                  display:'flex', alignItems:'center', gap:6, transition:'color .15s',
                }}
                  onMouseEnter={e=>e.currentTarget.style.color=C.orange}
                  onMouseLeave={e=>e.currentTarget.style.color=C.teal}
                >
                  <span style={{ fontSize:10, color:C.dim }}>→</span> {q}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
