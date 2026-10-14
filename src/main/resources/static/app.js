const $ = s => document.querySelector(s), $$ = s => [...document.querySelectorAll(s)];
const clientId = (() => { let id = localStorage.getItem('cid'); if (!id) { id = crypto.randomUUID(); localStorage.setItem('cid', id); } return id; })();
const state = { cars: [], favIds: new Set(), cmp: JSON.parse(localStorage.getItem('cmp') || '[]'), profile: 'balanced', segment: '' };
const api = async (url, opt = {}) => {
  const r = await fetch(url, { ...opt, headers: { 'Content-Type': 'application/json', 'X-Client-Id': clientId, ...(opt.headers || {}) } });
  if (!r.ok) { const msg = (await r.json().catch(() => ({}))).message || r.statusText; toast(msg); throw new Error(msg); }
  return r.status === 204 || r.headers.get('content-length') === '0' ? null : r.json().catch(() => null);
};
const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const toast = m => { const t = $('#toast'); t.textContent = m; t.classList.add('show'); setTimeout(() => t.classList.remove('show'), 1800); };
const price = p => '₹' + p.toFixed(1) + ' L';
const stars = n => '★'.repeat(n) + '☆'.repeat(5 - n);
const FUEL_RATE = { Petrol: 105, Diesel: 92, CNG: 90, Hybrid: 105, Electric: 9 }; // approx ₹ per litre / kg / kWh
const yearlyFuel = (c, km = 15000) => Math.round(km / c.mileage * (FUEL_RATE[c.fuelType] || 100));
const inr = n => '₹' + n.toLocaleString('en-IN');
const emi = (lakh, down, rate, yrs) => { const P = lakh * 1e5 * (1 - down / 100), r = rate / 1200, n = yrs * 12; return r ? Math.round(P * r * (1 + r) ** n / ((1 + r) ** n - 1)) : Math.round(P / n); };
const trans = c => c.transmission === 'Manual' ? 'Manual' : c.transmission + (c.transmissionType ? ' (' + c.transmissionType + ')' : '');
const engine = c => c.engineCc ? c.engineCc + ' cc' : 'Electric motor';
const mileageUnit = c => c.fuelType === 'Electric' ? 'km/kWh-eq' : c.fuelType === 'CNG' ? 'km/kg' : 'km/l';

/* ---------- tabs ---------- */
function showTab(t) {
  $$('nav button').forEach(b => b.classList.toggle('active', b.dataset.tab === t));
  $$('.tab').forEach(s => s.classList.toggle('active', s.id === t));
  if (t === 'compare') renderCompare();
  if (t === 'favorites') renderFavs();
  updateTray();
}
$$('nav button').forEach(b => b.onclick = () => showTab(b.dataset.tab));

const SHAPES = { Sedan: 'M10 60 L18 46 Q22 40 34 38 L62 34 Q76 18 98 17 L128 17 Q146 19 156 34 L178 38 Q192 42 192 60 Z', Hatchback: 'M10 60 L16 44 Q20 38 32 36 L58 33 Q70 16 94 16 L146 16 Q158 20 160 36 L182 40 Q192 44 192 60 Z', SUV: 'M10 60 L14 40 Q18 34 30 32 L52 30 L62 14 L152 12 Q166 14 170 30 L186 34 Q194 40 194 60 Z', MPV: 'M8 60 L12 38 Q16 32 28 30 L50 28 L58 12 L162 10 Q178 12 182 28 L192 34 Q198 40 198 60 Z' };
const carArt = c => `<svg viewBox="0 0 205 80" aria-hidden="true"><path d="${SHAPES[c.bodyType] || SHAPES.Sedan}" fill="${c.imageColor}"/><path d="M68 32 L78 20 L126 20 L142 32 Z" fill="#fff" opacity=".4"/><circle cx="52" cy="62" r="12" fill="#14181f"/><circle cx="150" cy="62" r="12" fill="#14181f"/><circle cx="52" cy="62" r="5" fill="#9aa3b2"/><circle cx="150" cy="62" r="5" fill="#9aa3b2"/></svg>`;
/* ---------- card ---------- */
function card(c) {
  const fav = state.favIds.has(c.id), inCmp = state.cmp.includes(c.id);
  return `<div class="card">
    <div class="thumb">${carArt(c)}
      <span class="body">${esc(c.segment || c.bodyType)}</span>
      <button class="heart" onclick="toggleFav(${c.id})" title="Save">${fav ? '❤️' : '🤍'}</button>
    </div>
    <div class="cbody">
      <div class="brand">${esc(c.brand)}</div><div class="name">${esc(c.model)}</div>
      <div class="price">${price(c.priceLakh)}</div>${badges(c)}
      <div class="specs"><span>${esc(c.fuelType)}</span><span>${esc(trans(c))}</span><span>${c.mileage} ${mileageUnit(c)}</span><span>${c.powerBhp} bhp</span><span>${c.torqueNm} Nm</span><span>${engine(c)}</span><span>${c.seats} seats</span><span title="Safety">${stars(c.safetyRating)}</span></div>
      <div class="actions">
        <button class="sec" onclick="showDetail(${c.id})">Details</button>
        <button class="sec ${inCmp ? 'on' : ''}" onclick="toggleCmp(${c.id})">${inCmp ? '✓ Added' : '+ Compare'}</button>
      </div>
    </div></div>`;
}

/* ---------- browse ---------- */
async function loadCars() {
  const p = new URLSearchParams();
  const [sort, dir] = $('#fSort').value.split('|');
  const map = { q: '#q', brand: '#fBrand', minSeats: '#fSeats', transType: '#fType', bodyType: '#fBody', fuelType: '#fFuel', transmission: '#fTrans', maxPrice: '#fMax' };
  for (const [k, sel] of Object.entries(map)) if ($(sel).value) p.set(k, $(sel).value);
  p.set('sort', sort); p.set('dir', dir); if (state.segment) p.set('segment', state.segment);
  state.cars = await api('/api/cars?' + p);
  $('#count').textContent = state.cars.length + ' cars found';
  $('#grid').innerHTML = state.cars.length ? state.cars.map(card).join('') : '<div class="empty">No cars match these filters. Clear a filter to see more.</div>';
  updateTray();
}
let debounce; ['#q', '#fMax'].forEach(s => $(s).oninput = () => { clearTimeout(debounce); debounce = setTimeout(loadCars, 250); });
['#fBrand', '#fType', '#fSeats', '#fBody', '#fFuel', '#fTrans', '#fSort'].forEach(s => $(s).onchange = () => { syncFilters(); loadCars(); });

async function detailCar(id) { return state.cars.find(c => c.id === id) || await api('/api/cars/' + id); }
async function showDetail(id) {
  const c = await detailCar(id);
  const rows = [['Price', price(c.priceLakh)], ['Fuel', c.fuelType], ['Transmission', trans(c)], ['Mileage', c.mileage + ' ' + mileageUnit(c)],
    ['Power', c.powerBhp + ' bhp'], ['Torque', c.torqueNm + ' Nm'], ['Engine', engine(c)], ['Segment', c.segment || '-'], ['Safety', stars(c.safetyRating)], ['Seats', c.seats], ['Boot space', c.bootLitres + ' L'], ['Body type', c.bodyType], ['Fuel cost / year*', inr(yearlyFuel(c))]];
  $('#sheet').innerHTML = `<div class="brand">${esc(c.brand)}</div><h2 style="margin:0 0 10px">${esc(c.model)}</h2>
    <table>${rows.map(r => `<tr><td>${r[0]}</td><td>${r[1]}</td></tr>`).join('')}</table>
    <h4>Key features</h4><div class="reasons">${(c.features || '').split(',').map(f => `<span>${esc(f)}</span>`).join('')}</div>
    <div id="similar"></div><h4>EMI calculator</h4><div class="emi"><label>Down payment %<input id="eDown" type="number" value="20" min="0" max="90"></label><label>Interest %<input id="eRate" type="number" value="9" step="0.1" min="0"></label><label>Years<input id="eYrs" type="number" value="5" min="1" max="8"></label></div><div id="emiOut"></div>
    <p class="muted" style="font-size:12px">*15,000 km/year at approx. pump prices. EMI is on ex-showroom price.</p>
    <div class="actions" style="margin-top:16px"><button class="sec" onclick="closeModal()">Close</button><button class="primary" onclick="toggleCmp(${c.id});closeModal()">+ Compare</button></div>`;
  $('#modal').classList.remove('hidden');
  const upd = () => $('#emiOut').textContent = inr(emi(c.priceLakh, +$('#eDown').value || 0, +$('#eRate').value || 0, +$('#eYrs').value || 1)) + ' / month';
  ['#eDown', '#eRate', '#eYrs'].forEach(s => $(s).oninput = upd); upd();
  api('/api/cars/' + c.id + '/similar').then(list => { if (list.length) $('#similar').innerHTML = '<h4>Similar cars</h4><div class="reasons">' + list.map(s => `<span style="cursor:pointer" onclick="showDetail(${s.id})">${esc(s.model)} ${price(s.priceLakh)}</span>`).join('') + '</div>'; }).catch(() => {});
}
const closeModal = () => $('#modal').classList.add('hidden');
document.onkeydown = e => { if (e.key === 'Escape') closeModal(); };
$('#modal').onclick = e => { if (e.target.id === 'modal') closeModal(); };

/* ---------- favorites ---------- */
async function loadFavs() {
  const list = await api('/api/favorites');
  state.favIds = new Set(list.map(c => c.id));
  $('#favCount').textContent = state.favIds.size;
  return list;
}
async function toggleFav(id) {
  await api('/api/favorites/' + id, { method: state.favIds.has(id) ? 'DELETE' : 'POST' });
  await loadFavs(); refresh();
}
async function renderFavs() {
  const list = await loadFavs();
  $('#favGrid').innerHTML = list.length ? list.map(card).join('') : '<div class="empty" style="grid-column:1/-1">No saved cars yet — tap 🤍 on any car.</div>';
}

/* ---------- compare ---------- */
function toggleCmp(id) {
  if (state.cmp.includes(id)) state.cmp = state.cmp.filter(x => x !== id);
  else if (state.cmp.length >= 4) return toast('You can compare up to 4 cars');
  else state.cmp.push(id);
  localStorage.setItem('cmp', JSON.stringify(state.cmp));
  $('#cmpCount').textContent = state.cmp.length; refresh();
  toast(state.cmp.includes(id) ? 'Added to compare' : 'Removed from compare');
}
function shareCmp() { navigator.clipboard?.writeText(location.origin + location.pathname + '#compare=' + state.cmp.join(',')); toast('Link copied'); }
function clearCmp() { state.cmp = []; localStorage.setItem('cmp', '[]'); $('#cmpCount').textContent = 0; refresh(); }
function updateTray() {
  const t = $('#tray'), n = state.cmp.length, names = state.cmp.map(id => state.cars.find(c => c.id === id)).filter(Boolean).map(c => esc(c.model));
  t.classList.toggle('show', n > 0 && !$('#compare').classList.contains('active'));
  t.innerHTML = `<span>${n}/4 selected${names.length ? ': ' + names.join(', ') : ''}</span><button class="primary" onclick="showTab('compare')" ${n < 2 ? 'disabled' : ''}>Compare now</button><button class="sec" onclick="clearCmp()">Clear</button>`;
}
async function renderCompare() {
  const out = $('#cmpOut');
  if (state.cmp.length < 2) { out.innerHTML = `<div class="empty">Pick at least 2 cars (up to 4) using “+ Compare” on any car. Selected: ${state.cmp.length}</div>`; return; }
  const cars = (await api('/api/compare?ids=' + state.cmp.join(','))).sort((a, b) => state.cmp.indexOf(a.id) - state.cmp.indexOf(b.id));
  // [label, getter, better: 'low' | 'high' | null]
  const rows = [['Price', c => c.priceLakh, 'low', v => price(v)], ['Fuel', c => c.fuelType], ['Segment', c => c.segment || '-'], ['Transmission', c => trans(c)], ['Body type', c => c.bodyType],
    ['Mileage', c => c.mileage, 'high', (v, c) => v + ' ' + mileageUnit(c)], ['Power', c => c.powerBhp, 'high', v => v + ' bhp'], ['Torque', c => c.torqueNm, 'high', v => v + ' Nm'], ['Engine', c => c.engineCc, null, v => v ? v + ' cc' : 'Electric motor'],
    ['Safety', c => c.safetyRating, 'high', v => stars(v)], ['Seats', c => c.seats, 'high'], ['Boot space', c => c.bootLitres, 'high', v => v + ' L'], ['Fuel cost / year*', c => yearlyFuel(c), 'low', v => inr(v)]];
  const wins = cars.map(() => 0); let judged = 0;
  rows.forEach(([, get, better]) => { if (!better) return; const v = cars.map(get); if (v.every(x => x === v[0])) return; judged++; const b = better === 'low' ? Math.min(...v) : Math.max(...v); v.forEach((x, i) => { if (x === b) wins[i]++; }); });
  const top = Math.max(...wins), leaders = cars.filter((_, i) => wins[i] === top);
  const banner = `<div class="verdict"><span>🏆 ${leaders.map(c => esc(c.model)).join(' & ')} ${leaders.length > 1 ? 'tie' : 'leads'} — best in ${top} of ${judged} categories</span><button class="sec" onclick="shareCmp()">Copy share link</button></div>`;
  out.innerHTML = banner + insight(cars) + `<div class="cmpwrap"><table class="cmp"><thead><tr><th class="row"></th>${cars.map(c =>
          `<td><div class="brand">${esc(c.brand)}</div><b>${esc(c.model)}</b><br><button class="sec" style="margin-top:6px" onclick="toggleCmp(${c.id});renderCompare()">Remove</button></td>`).join('')}</tr></thead><tbody>` +
      rows.map(([label, get, better, fmt]) => {
        const vals = cars.map(get); const best = better ? (better === 'low' ? Math.min(...vals) : Math.max(...vals)) : null;
        const allSame = vals.every(v => v === vals[0]);
        return `<tr><th class="row">${label}</th>${cars.map((c, i) => `<td class="${best !== null && !allSame && vals[i] === best ? 'best' : ''}">${fmt ? fmt(vals[i], c) : vals[i]}</td>`).join('')}</tr>`;
      }).join('') + `</tbody></table></div><p class="muted">Green = best value in that row. *Fuel cost assumes 15,000 km/year at approx. pump prices.</p>`;
}

/* ---------- recommend ---------- */
const WEIGHTS = { price: 'Low price', mileage: 'Mileage', performance: 'Performance', safety: 'Safety', space: 'Space' };
const PRESETS = { student: [5, 5, 1, 2, 1], family: [2, 3, 1, 5, 5], enthusiast: [1, 1, 5, 3, 1], balanced: [3, 3, 3, 3, 3] };
function setProfile(p) {
  state.profile = p;
  $$('#profiles button').forEach(b => b.classList.toggle('on', b.dataset.p === p));
  Object.keys(WEIGHTS).forEach((k, i) => $('#w_' + k).value = PRESETS[p][i]);
}
$('#sliders').innerHTML = Object.entries(WEIGHTS).map(([k, l]) => `<label>${l}<input type="range" min="0" max="5" id="w_${k}" value="3"></label>`).join('');
$$('#profiles button').forEach(b => b.onclick = () => setProfile(b.dataset.p));
$('#goReco').onclick = async () => {
  const weights = {}; Object.keys(WEIGHTS).forEach(k => weights[k] = +$('#w_' + k).value);
  const body = { budgetLakh: +$('#rBudget').value || null, fuelType: $('#rFuel').value || null, minSeats: +$('#rSeats').value || null, bodyType: $('#rBody').value || null, weights, profile: state.profile };
  const res = await api('/api/recommendations', { method: 'POST', body: JSON.stringify(body) });
  $('#recoOut').innerHTML = res.length ? res.map((r, i) => {
    const col = r.score >= 75 ? '#16a34a' : r.score >= 55 ? '#d97706' : '#dc2626';
    return `<div class="reco-item"><div class="rank">#${i + 1}</div>
      <div><div class="brand">${esc(r.car.brand)}</div><b style="font-size:18px">${esc(r.car.model)}</b> <span class="price" style="font-size:16px">${price(r.car.priceLakh)}</span><div class="muted" style="font-size:13px">Runs about ₹${r.costPerKm.toFixed(1)}/km. EMI about ${inr(r.emi)}/month</div>
        <div class="reasons">${r.reasons.map(x => `<span class="${x.startsWith('⚠') ? 'bad' : ''}">${esc(x)}</span>`).join('')}</div>
        <div class="actions" style="max-width:300px"><button class="sec" onclick="showDetail(${r.car.id})">Details</button><button class="sec" onclick="toggleCmp(${r.car.id})">+ Compare</button><button class="sec" onclick="toggleFav(${r.car.id})">${state.favIds.has(r.car.id) ? '❤️' : '🤍'}</button></div></div>
      <div class="score" style="background:${col}">${r.score}</div></div>`;
  }).join('') : '<div class="empty">No cars match those requirements. Try relaxing fuel/seats/body type.</div>';
};

$$('#budgetChips button').forEach(b => b.onclick = () => { $('#fMax').value = b.dataset.max; $$('#budgetChips button').forEach(x => x.classList.toggle('on', x === b)); loadCars(); });
/* ---------- insights: badges, radar, story ---------- */
function computeStats() {
  const A = state.all, cost = c => FUEL_RATE[c.fuelType] / c.mileage, mx = f => Math.max(...A.map(f)), mn = f => Math.min(...A.map(f));
  const S = state.stats = { minP: mn(c => c.priceLakh), maxP: mx(c => c.priceLakh), minC: mn(cost), maxC: mx(cost), maxPw: mx(c => c.powerBhp), maxB: mx(c => c.bootLitres), cost };
  const quality = c => c.safetyRating / 5 * .4 + c.powerBhp / S.maxPw * .2 + (1 - (cost(c) - S.minC) / (S.maxC - S.minC)) * .2 + Math.min(1, (c.features || '').split(',').length / 5) * .2;
  state.badges = {};
  const add = (c, t) => (state.badges[c.id] ||= []).push(t);
  [...new Set(A.map(c => c.segment).filter(Boolean))].forEach(seg => {
    const g = A.filter(c => c.segment === seg); if (g.length < 3) return;
    const top = (f, hi) => { const m = (hi ? Math.max : Math.min)(...g.map(f)); return g.filter(c => f(c) === m); };
    top(c => quality(c) / c.priceLakh, true).forEach(c => add(c, 'Best value'));
    top(c => yearlyFuel(c), false).forEach(c => add(c, 'Lowest running cost'));
    top(c => c.powerBhp, true).forEach(c => add(c, 'Most powerful'));
  });
}
const badges = c => (state.badges?.[c.id] || []).length ? `<div class="badges">${state.badges[c.id].slice(0, 2).map(b => `<span title="Best in its segment">${b}</span>`).join('')}</div>` : '';

const AXES = ['Price', 'Running cost', 'Power', 'Safety', 'Space'], COLORS = ['#2457ff', '#f08a00', '#12a150', '#d6336c'];
const radarVals = c => { const s = state.stats; return [1 - (c.priceLakh - s.minP) / (s.maxP - s.minP), 1 - (s.cost(c) - s.minC) / (s.maxC - s.minC), c.powerBhp / s.maxPw, c.safetyRating / 5, (c.seats / 7 + c.bootLitres / s.maxB) / 2]; };
function radar(cars) {
  const R = 100, C = 150, pt = (i, v) => { const a = (-90 + i * 72) * Math.PI / 180; return [(C + Math.cos(a) * R * v).toFixed(1), (C + Math.sin(a) * R * v).toFixed(1)]; };
  const ring = v => AXES.map((_, i) => pt(i, v).join(',')).join(' ');
  return `<svg viewBox="0 0 300 300" class="radarsvg" role="img" aria-label="Radar chart comparing the selected cars">` +
      [.25, .5, .75, 1].map(v => `<polygon points="${ring(v)}" class="ring"/>`).join('') +
      AXES.map((a, i) => { const [x, y] = pt(i, 1), [lx, ly] = pt(i, 1.2); return `<line x1="${C}" y1="${C}" x2="${x}" y2="${y}" class="ring"/><text x="${lx}" y="${ly}" text-anchor="middle" dominant-baseline="middle">${a}</text>`; }).join('') +
      cars.map((c, k) => `<polygon points="${radarVals(c).map((v, i) => pt(i, Math.max(v, .03)).join(',')).join(' ')}" fill="${COLORS[k]}" fill-opacity=".16" stroke="${COLORS[k]}" stroke-width="2.5"/>`).join('') + '</svg>' +
      `<div class="legend">${cars.map((c, k) => `<span><i style="background:${COLORS[k]}"></i>${esc(c.model)}</span>`).join('')}</div>`;
}
function story(cars) {
  const rank = (get, hi) => [...cars].sort((a, b) => hi ? get(b) - get(a) : get(a) - get(b)), m = c => `<b>${esc(c.model)}</b>`, out = [];
  let [a, b] = rank(c => c.priceLakh, false);
  if (a.priceLakh !== b.priceLakh) out.push(`${m(a)} is the cheapest to buy at ${price(a.priceLakh)}, ₹${(b.priceLakh - a.priceLakh).toFixed(1)} L less than ${m(b)}.`);
  [a, b] = rank(c => yearlyFuel(c), false);
  if (yearlyFuel(b) - yearlyFuel(a) > 500) out.push(`${m(a)} is the cheapest to run, saving about ${inr(yearlyFuel(b) - yearlyFuel(a))} a year in fuel compared with ${m(b)}.`);
  [a, b] = rank(c => c.powerBhp, true);
  if (a.powerBhp !== b.powerBhp) out.push(`${m(a)} is the strongest with ${a.powerBhp} bhp and ${a.torqueNm} Nm, ${a.powerBhp - b.powerBhp} bhp more than ${m(b)}.`);
  [a, b] = rank(c => c.safetyRating, true);
  out.push(a.safetyRating === b.safetyRating && cars.every(c => c.safetyRating === a.safetyRating) ? `All of them share the same ${a.safetyRating}★ safety rating.` : `${m(a)} is the safest with a ${a.safetyRating}★ rating.`);
  [a, b] = rank(c => c.bootLitres, true);
  if (a.bootLitres !== b.bootLitres) out.push(`${m(a)} has the biggest boot at ${a.bootLitres} L, ${a.bootLitres - b.bootLitres} L more than ${m(b)}.`);
  return out.map(x => `<li>${x}</li>`).join('');
}
const insight = cars => `<div class="insight"><div class="radar">${radar(cars)}</div><div class="story"><h3>How they stack up</h3><ul>${story(cars)}</ul></div></div>`;

/* Show only the options that make sense for what is already selected */
function syncFilters() {
  const val = id => $(id).value, seg = state.segment;
  const pool = (o = {}) => state.all.filter(c => (!(o.fuel ?? val('#fFuel')) || c.fuelType === (o.fuel ?? val('#fFuel'))) && (!(o.tr ?? val('#fTrans')) || c.transmission === (o.tr ?? val('#fTrans'))) && (!seg || c.segment === seg));
  [...$('#fFuel').options].filter(o => o.value).forEach(o => { o.disabled = !pool({ fuel: o.value }).length; });
  [...$('#fTrans').options].filter(o => o.value).forEach(o => { o.disabled = !pool({ tr: o.value }).length; });
  if ($('#fFuel').selectedOptions[0]?.disabled) $('#fFuel').value = '';
  if ($('#fTrans').selectedOptions[0]?.disabled) $('#fTrans').value = '';
  const kinds = new Set(pool({ tr: '' }).map(c => c.transmission));
  $('#fTrans').hidden = kinds.size < 2;
  if (kinds.size < 2) $('#fTrans').value = '';
  const auto = pool({ tr: 'Automatic' }), types = [...new Set(auto.map(c => c.transmissionType).filter(Boolean))].sort();
  const wantType = (val('#fTrans') === 'Automatic' || (kinds.size === 1 && kinds.has('Automatic'))) && types.length > 1;
  const keep = wantType ? val('#fType') : '';
  $('#fType').innerHTML = '<option value="">Any automatic type</option>' + types.map(t => `<option${t === keep ? ' selected' : ''}>${esc(t)}</option>`).join('');
  $('#fType').hidden = !wantType;
}
const resetFilters = () => {
  ['#q', '#fBrand', '#fSeats', '#fBody', '#fFuel', '#fTrans', '#fType', '#fMax'].forEach(s => $(s).value = '');
  $('#fSort').selectedIndex = 0; state.segment = '';
  $$('#segChips button, #budgetChips button').forEach(b => b.classList.toggle('on', !b.dataset.s && !b.dataset.max));
  syncFilters(); loadCars();
};
$('#logo').onclick = e => { e.preventDefault(); history.replaceState(null, '', location.pathname); showTab('browse'); resetFilters(); scrollTo({ top: 0, behavior: 'smooth' }); };
/* ---------- init ---------- */
const refresh = () => { loadCars(); if ($('#favorites').classList.contains('active')) renderFavs(); if ($('#compare').classList.contains('active')) renderCompare(); };
(async function init() {
  const f = await api('/api/filters');
  const fill = (sel, arr) => arr.forEach(v => $(sel).insertAdjacentHTML('beforeend', `<option>${esc(v)}</option>`));
  fill('#fBrand', f.brands); fill('#fBody', f.bodyTypes); fill('#fFuel', f.fuelTypes); fill('#fTrans', f.transmissions);
  fill('#fType', f.transmissionTypes.filter(t => t !== 'Manual'));
  const ORDER = ['Entry Level', 'Mid Range', 'Family', 'Touring', 'Off-Road', 'Electric', 'Luxury'];
  $('#segChips').innerHTML = '<button data-s="" class="on">All segments</button>' + ORDER.filter(s => f.segments.includes(s)).map(s => `<button data-s="${esc(s)}">${esc(s)}</button>`).join('');
  $$('#segChips button').forEach(b => b.onclick = () => { state.segment = b.dataset.s; $$('#segChips button').forEach(x => x.classList.toggle('on', x === b)); syncFilters(); loadCars(); });
  fill('#rFuel', f.fuelTypes); fill('#rBody', f.bodyTypes);
  $('#cmpCount').textContent = state.cmp.length;
  state.all = await api('/api/cars'); computeStats(); syncFilters();
  await loadFavs(); await loadCars();
  const m = location.hash.match(/compare=([\d,]+)/);
  if (m) { state.cmp = m[1].split(',').map(Number).slice(0, 4); localStorage.setItem('cmp', JSON.stringify(state.cmp)); $('#cmpCount').textContent = state.cmp.length; showTab('compare'); }
})();