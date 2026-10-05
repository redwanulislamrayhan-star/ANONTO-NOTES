/* End-to-end test: real PDF files → PDF.js text items → Engine V3 parse.
 * Run: npm install pdfjs-dist@3.11.174 && node test-e2e.js */
'use strict';
const fs = require('fs');
const path = require('path');
const Parser = require('./parser.js');

async function extractPdfLines(file) {
  const pdfjs = require('pdfjs-dist/legacy/build/pdf.js');
  const data = new Uint8Array(fs.readFileSync(file));
  const doc = await pdfjs.getDocument({ data, useSystemFonts: true }).promise;
  let all = [];
  for (let p = 1; p <= doc.numPages; p++) {
    const page = await doc.getPage(p);
    const tc = await page.getTextContent();
    const lines = Parser.linesFromPdfItems(tc.items.map(it => ({
      str: it.str, transform: it.transform, width: it.width
    })));
    all = all.concat(lines);
  }
  return all.join('\n');
}

let passed = 0, failed = 0;
const failures = [];
function eq(actual, expected, msg) {
  if (JSON.stringify(actual) === JSON.stringify(expected)) passed++;
  else { failed++; failures.push({ msg, expected, actual }); }
}

(async () => {
  const cases = [
    ['oman-air.pdf', {
      airline: 'Oman Air', passengerName: 'Md Mainul Islam', passport: 'BR0987654',
      pnr: 'G8YQ4P', segs: 2, tripType: 'Journey with Transit',
      s1: { from: 'DAC', to: 'MCT', fl: 'WY684', dep: '08:20 AM', arr: '11:15 AM', dur: '5h 45m' },
      transit: '2h 30m', ticketNumber: '920-2412345678'
    }],
    ['saudia.pdf', {
      airline: 'Saudia', passengerName: 'Mr Mohammed Alam', passport: 'A01234567',
      pnr: 'MMMR2L', segs: 1, tripType: 'One Way',
      s1: { from: 'DAC', to: 'JED', fl: 'SV772', dep: '3:30 AM', arr: '8:10 AM', dur: '8h 40m' },
      ticketNumber: '065-2412345678'
    }],
    ['qatar-airways.pdf', {
      airline: 'Qatar Airways', passengerName: 'Mrs Sabina Akter', passport: 'EF0123456',
      pnr: 'Q8R5T2', segs: 2, tripType: 'Round Trip',
      s1: { from: 'DAC', to: 'DOH', fl: 'QR635', dep: '2:45 AM', arr: '5:35 AM', dur: '5h 50m' },
      transit: '2h 5m', ticketNumber: '157-2412345678'
    }]
  ];

  for (const [file, exp] of cases) {
    const text = await extractPdfLines(path.join(__dirname, 'samples', file));
    const r = Parser.parse(text);
    eq(r.ok, true, file + ': parse ok');
    eq(r.airline, exp.airline, file + ': airline');
    eq(r.passengerName, exp.passengerName, file + ': passenger');
    eq(r.passport, exp.passport, file + ': passport');
    eq(r.pnr, exp.pnr, file + ': PNR');
    eq(r.segments.length, exp.segs, file + ': segment count');
    eq(r.tripType, exp.tripType, file + ': trip type');
    const s1 = r.segments[0];
    eq(s1.from.code, exp.s1.from, file + ': s1 from');
    eq(s1.to.code, exp.s1.to, file + ': s1 to');
    eq(s1.flightNumber, exp.s1.fl, file + ': s1 flight');
    eq(s1.departure, exp.s1.dep, file + ': s1 departure');
    eq(s1.arrival, exp.s1.arr, file + ': s1 arrival');
    eq(s1.duration, exp.s1.dur, file + ': s1 duration');
    if (exp.transit) eq(r.transits[0].duration, exp.transit, file + ': transit');
    eq(r.ticket.ticketNumber, exp.ticketNumber, file + ': ticket number');
    console.log(`${failed ? '✗ issues in' : '✓ e2e ok'}: ${file} (confidence ${r.confidence}%)`);
  }

  console.log('\nE2E Passed: ' + passed + '   Failed: ' + failed);
  failures.forEach(f => console.log('✗ ' + f.msg + '\n    expected: ' + JSON.stringify(f.expected) + '\n    actual:   ' + JSON.stringify(f.actual)));
  process.exit(failed ? 1 : 0);
})().catch(e => { console.error(e); process.exit(1); });
