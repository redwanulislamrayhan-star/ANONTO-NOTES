/* Drop-in binder tests — they prove autofill-bind.js can fill a form whose
 * field names it has never seen (labels, placeholders, table headers).
 *
 * jsdom is optional:  npm i -D jsdom   (অথবা NODE_PATH দিয়ে)
 * Run:  node --test web/ticket-autofill/test/bind.test.js
 */
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');

let JSDOM = null;
try { ({ JSDOM } = require('jsdom')); } catch (_) { /* optional */ }

const DIR = path.join(__dirname, '..');
const PARSER_SRC = fs.readFileSync(path.join(DIR, 'ticket-parser.js'), 'utf8');
const BIND_SRC = fs.readFileSync(path.join(DIR, 'autofill-bind.js'), 'utf8');
const { SAUDIA } = require('./samples.js');

// A form that looks nothing like our demo page: Bengali labels, snake_case ids,
// a <table> itinerary and the old "Imported Ticket Information" textarea.
const FORM = `<!DOCTYPE html><html><body><form>
  <div class="field">
    <label for="pax_full_name">যাত্রীর নাম / Passenger Name</label>
    <input id="pax_full_name" />
  </div>
  <input id="pp_no" placeholder="Passport Number" />
  <label>Nationality <input name="nation" /></label>
  <div class="form-group"><span class="label">Booking Ref (PNR)</span><input name="pnr_code" /></div>
  <div class="form-group"><span class="label">E-Ticket Number</span><input name="tkt" /></div>
  <div class="form-group"><label>Issue Date</label><input type="date" name="issued_on" /></div>
  <div class="form-group"><label>Airline</label><input name="carrier_name" /></div>
  <div class="form-group"><label>Final Destination</label><input name="dest_city" /></div>

  <table id="itinerary">
    <thead><tr><th>Flight No.</th><th>Date</th><th>Dep Time</th><th>Status</th><th>Route</th></tr></thead>
    <tbody>
      <tr><td><input /></td><td><input /></td><td><input /></td><td><input /></td><td><input /></td></tr>
      <tr><td><input /></td><td><input /></td><td><input /></td><td><input /></td><td><input /></td></tr>
    </tbody>
  </table>

  <label>Total Amount <input id="total_fare" /></label>
  <label>Baggage Allowance <input id="bag_allow" /></label>
  <label>Imported Ticket Information
    <textarea id="imported_info"></textarea>
  </label>
</form></body></html>`;

function boot(html) {
  const dom = new JSDOM(html, { runScripts: 'outside-only' });
  dom.window.eval(PARSER_SRC);
  dom.window.eval(BIND_SRC);
  return dom.window;
}

test('binder fills an unknown form, field by field', { skip: !JSDOM && 'jsdom not installed' }, () => {
  const w = boot(FORM);
  const out = w.TicketAutoFill.fill(SAUDIA);
  const d = w.document;
  const v = sel => d.querySelector(sel).value;

  assert.strictEqual(v('#pax_full_name'), 'MR. Mohammad Faysal');
  assert.strictEqual(v('#pp_no'), 'A12345678');
  assert.strictEqual(v('[name=nation]'), 'Bangladeshi');
  assert.strictEqual(v('[name=pnr_code]'), 'XK7Q2M');
  assert.strictEqual(v('[name=tkt]'), '065-1234567890');
  assert.strictEqual(v('[name=issued_on]'), '2026-10-02');   // type=date -> ISO
  assert.strictEqual(v('[name=carrier_name]'), 'Saudia');
  assert.strictEqual(v('[name=dest_city]'), 'Dhaka (DAC)');
  assert.strictEqual(v('#total_fare'), 'SAR 2,390');
  assert.strictEqual(v('#bag_allow'), '2 x 23 Kg');
  assert.ok(out.inputsFilled >= 15, 'should fill most inputs');
});

test('table itinerary rows become segment 1 and segment 2', { skip: !JSDOM && 'jsdom not installed' }, () => {
  const w = boot(FORM);
  w.TicketAutoFill.fill(SAUDIA);
  const rows = w.document.querySelectorAll('#itinerary tbody tr');
  const cells = r => Array.from(rows[r].querySelectorAll('input')).map(i => i.value);

  assert.deepStrictEqual(cells(0), ['SV804', '15 Oct 2026', '23:05', 'Confirmed', 'Buraydah (ELQ) → Dhaka (DAC)']);
  assert.deepStrictEqual(cells(1), ['SV807', '05 Dec 2026', '16:05', 'Confirmed', 'Dhaka (DAC) → Buraydah (ELQ)']);
});

test('the old "Imported Ticket Information" textarea is left empty', { skip: !JSDOM && 'jsdom not installed' }, () => {
  const w = boot(FORM);
  w.TicketAutoFill.fill(SAUDIA);
  assert.strictEqual(w.document.querySelector('#imported_info').value, '');
});

test('numbered labels (Flight No. 1 / Date 2) are respected', { skip: !JSDOM && 'jsdom not installed' }, () => {
  const w = boot(`<!DOCTYPE html><html><body>
    <label>Flight No. 2 <input id="f2" /></label>
    <label>Date 2 <input id="d2" /></label>
    <label>Flight No. 1 <input id="f1" /></label>
    <label>Date 1 <input id="d1" /></label>
    <label>Travel Date <input id="td" /></label>
  </body></html>`);
  w.TicketAutoFill.fill(SAUDIA);
  const v = id => w.document.getElementById(id).value;
  assert.strictEqual(v('f1'), 'SV804');
  assert.strictEqual(v('d1'), '15 Oct 2026');
  assert.strictEqual(v('f2'), 'SV807');
  assert.strictEqual(v('d2'), '05 Dec 2026');
  assert.strictEqual(v('td'), '15 Oct 2026');
});

test('fare never lands in a date field', { skip: !JSDOM && 'jsdom not installed' }, () => {
  const w = boot(FORM);
  w.TicketAutoFill.fill(SAUDIA);
  const vals = Array.from(w.document.querySelectorAll('input')).map(i => i.value);
  const dateish = vals.filter(v => /\d{4}-\d{2}-\d{2}|\d{1,2} [A-Z][a-z]{2} \d{4}/.test(v));
  dateish.forEach(v => assert.ok(!/SAR|2390/.test(v), `fare leaked: ${v}`));
});
