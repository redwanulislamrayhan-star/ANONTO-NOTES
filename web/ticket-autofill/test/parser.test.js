/* Run with:  node --test web/ticket-autofill/test  */
const test = require('node:test');
const assert = require('node:assert');
const { parseTicket } = require('../ticket-parser.js');
const { SAUDIA, BIMAN_GDS, MESSY_OCR } = require('./samples.js');

test('Saudia ticket: every value lands in its own field', () => {
  const r = parseTicket(SAUDIA);

  assert.strictEqual(r.passengerName, 'MR. Mohammad Faysal');
  assert.strictEqual(r.passportNo, 'A12345678');
  assert.strictEqual(r.nationality, 'Bangladeshi');
  assert.strictEqual(r.bookingReference, 'XK7Q2M');
  assert.strictEqual(r.ticketNumber, '065-1234567890');
  assert.strictEqual(r.issueDate, '02 Oct 2026');
  assert.strictEqual(r.airline, 'Saudia');
  assert.strictEqual(r.destination, 'Dhaka (DAC)');
  assert.strictEqual(r.travelDate, '15 Oct 2026');
  assert.strictEqual(r.returnDate, '05 Dec 2026');
  assert.strictEqual(r.tripType, 'Round Trip');
  assert.strictEqual(r.baggage, '2 x 23 Kg');
  assert.strictEqual(r.cabin, 'Economy');
});

test('Saudia ticket: two separate flight segments', () => {
  const r = parseTicket(SAUDIA);
  assert.strictEqual(r.segments.length, 2);

  const [s1, s2] = r.segments;
  assert.strictEqual(s1.flightNo, 'SV804');
  assert.strictEqual(s1.date, '15 Oct 2026');
  assert.strictEqual(s1.departureTime, '23:05');
  assert.strictEqual(s1.arrivalTime, '09:30');
  assert.strictEqual(s1.status, 'Confirmed');
  assert.strictEqual(s1.route, 'Buraydah (ELQ) → Dhaka (DAC)');

  assert.strictEqual(s2.flightNo, 'SV807');
  assert.strictEqual(s2.date, '05 Dec 2026');
  assert.strictEqual(s2.departureTime, '16:05');
  assert.strictEqual(s2.route, 'Dhaka (DAC) → Buraydah (ELQ)');
});

test('money is money: SAR 2390 never leaks into date/flight/time fields', () => {
  const r = parseTicket(SAUDIA);

  assert.strictEqual(r.currency, 'SAR');
  assert.strictEqual(r.totalAmount, 2390);
  assert.strictEqual(r.totalAmountText, 'SAR 2,390');
  assert.strictEqual(r.baseFare, 'SAR 1,990');
  assert.strictEqual(r.taxes, 'SAR 400');

  const leakTargets = [r.travelDate, r.returnDate, r.issueDate, r.flightNo, r.route, r.destination]
    .concat(r.segments.flatMap(s => [s.flightNo, s.date, s.departureTime, s.arrivalTime, s.route, s.status]));
  for (const v of leakTargets) {
    assert.ok(!/2390|SAR|\b00\b/.test(String(v)), `fare leaked into a non-money field: "${v}"`);
  }
});

test('the "00 SAR 2390" OCR garbage never produces a date', () => {
  const r = parseTicket('Travel Date 00 SAR 2390\nFlight SV804 15 Oct 2026 ELQ DAC 23:05');
  assert.strictEqual(r.travelDate, '15 Oct 2026');
  assert.strictEqual(r.flightNo, 'SV804');
  assert.strictEqual(r.totalAmount, 2390);
});

test('Biman GDS layout: bare times, DDMMMYY dates, slashed name', () => {
  const r = parseTicket(BIMAN_GDS);

  assert.strictEqual(r.passengerName, 'MR. Abdur Rahman');
  assert.strictEqual(r.bookingReference, '4KL9ZT');
  assert.strictEqual(r.ticketNumber, '997-1234567890');
  assert.strictEqual(r.issueDate, '12 Sep 2026');
  assert.strictEqual(r.airline, 'Biman Bangladesh Airlines');
  assert.strictEqual(r.segments.length, 2);

  const [s1, s2] = r.segments;
  assert.strictEqual(s1.flightNo, 'BG348');
  assert.strictEqual(s1.date, '25 Sep 2026');
  assert.strictEqual(s1.departureTime, '03:10');
  assert.strictEqual(s1.arrivalTime, '07:30');
  assert.strictEqual(s1.route, 'Dhaka (DAC) → Jeddah (JED)');
  assert.strictEqual(s1.status, 'Confirmed');

  assert.strictEqual(s2.flightNo, 'BG349');
  assert.strictEqual(s2.route, 'Jeddah (JED) → Dhaka (DAC)');

  assert.strictEqual(r.currency, 'BDT');
  assert.strictEqual(r.totalAmount, 97650);
  assert.strictEqual(r.baggage, '2 PC');
  assert.strictEqual(r.destination, 'Jeddah (JED)');
});

test('messy OCR: next-line values, slashed dates, one-way trip', () => {
  const r = parseTicket(MESSY_OCR);

  assert.strictEqual(r.passengerName, 'MRS. Mst Shirin Khatun');
  assert.strictEqual(r.bookingReference, 'J8PL2D');
  assert.strictEqual(r.passportNo, 'BX0912345');
  assert.strictEqual(r.nationality, 'Bangladeshi');
  assert.strictEqual(r.segments.length, 1);
  assert.strictEqual(r.segments[0].flightNo, 'EK585');
  assert.strictEqual(r.segments[0].date, '10 Nov 2026');
  assert.strictEqual(r.segments[0].departureTime, '02:45');
  assert.strictEqual(r.segments[0].route, 'Dhaka (DAC) → Dubai (DXB)');
  assert.strictEqual(r.segments[0].baggage, '30 Kg');
  assert.strictEqual(r.segments[0].terminal, '3');
  assert.strictEqual(r.tripType, 'One Way');
  assert.strictEqual(r.currency, 'USD');
  assert.strictEqual(r.totalAmount, 612.5);
});

test('Bengali digits are understood', () => {
  const r = parseTicket('Flight: BS৩২৪  Date: ১৫ Oct ২০২৬  Dep: ১০:৩০  DAC CGP\nTotal BDT ৫,৫০০');
  assert.strictEqual(r.segments[0].flightNo, 'BS324');
  assert.strictEqual(r.segments[0].date, '15 Oct 2026');
  assert.strictEqual(r.segments[0].departureTime, '10:30');
  assert.strictEqual(r.totalAmount, 5500);
});

test('empty input is safe', () => {
  const r = parseTicket('');
  assert.deepStrictEqual(r.segments, []);
  assert.strictEqual(r.passengerName, '');
  assert.strictEqual(r.totalAmount, null);
});
