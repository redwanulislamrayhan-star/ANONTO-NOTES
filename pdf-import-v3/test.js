/* Engine V3 test harness — run: node test.js */
'use strict';
const Parser = require('./parser.js');
const Samples = require('./samples.js');

let passed = 0, failed = 0;
const failures = [];

function eq(actual, expected, msg) {
  const a = JSON.stringify(actual), e = JSON.stringify(expected);
  if (a === e) { passed++; }
  else { failed++; failures.push({ msg, expected, actual }); }
}

function ok(v, msg) { eq(!!v, true, msg); }

/* ---------------- unit tests ---------------- */
const I = Parser._internals;

eq(I.displayTime('15:20'), '3:20 PM', 'displayTime 24h → PM');
eq(I.displayTime('08:20'), '8:20 AM', 'displayTime 24h → AM (no leading zero)');
eq(I.displayTime('08:20 AM'), '08:20 AM', 'displayTime keeps ticket AM/PM');
eq(I.displayTime('13:00'), '1:00 PM', 'displayTime 13:00');
eq(I.displayTime('00:25'), '12:25 AM', 'displayTime midnight');
eq(I.displayTime('21:10'), '9:10 PM', 'displayTime evening');

ok(I.parseDateStr('12 OCT 2025'), 'parse date 12 OCT 2025');
eq(I.parseDateStr('12 OCT 2025').text, '12 Oct 2025', 'date text');
eq(I.parseDateStr('05 NOV 25').text, '5 Nov 2025', '2-digit year');
eq(I.parseDateStr('2025-10-12').text, '12 Oct 2025', 'ISO date');

eq(I.normalizeName('MR ALAM/MOHAMMED'), 'Mr Mohammed Alam', 'SURNAME/GIVEN');
eq(I.normalizeName('ISLAM/MOHAMMED MR'), 'Mr Mohammed Islam', 'SURNAME/GIVEN MR');
eq(I.normalizeName('MD MAINUL ISLAM'), 'Md Mainul Islam', 'plain name + MD');
eq(I.normalizeName('MOHAMMAD AL AMIN'), 'Mohammad Al Amin', 'multi-word');
eq(I.normalizeName('AL MUSAFIR TRAVELS LTD'), null, 'agency name rejected');
eq(I.normalizeName('MOHAMMED 12345'), null, 'digits rejected');

eq(I.hasAgentContext('Agent: AL MUSAFIR TRAVELS'), true, 'agent line detected');
eq(I.hasAgentContext('Issued By: TRAVEL OFFICE'), true, 'issued by detected');
eq(I.hasAgentContext('Passenger Name: MD ISLAM'), false, 'passenger line is not agent');

/* ---------------- full ticket tests ---------------- */

function runSample(key, checks) {
  const r = Parser.parse(Samples[key].text);
  ok(r.ok, key + ': parse ok');
  checks(r);
}

/* ---- Oman Air: transit journey, AM/PM on ticket ---- */
runSample('omanAir', (r) => {
  eq(r.airline, 'Oman Air', 'OM airline');
  eq(r.passengerName, 'Md Mainul Islam', 'OM passenger (not the agent!)');
  eq(r.passport, 'BR0987654', 'OM passport');
  eq(r.nationality, 'Bangladeshi', 'OM nationality');
  eq(r.pnr, 'G8YQ4P', 'OM PNR');
  eq(r.segments.length, 2, 'OM two segments');
  const s1 = r.segments[0], s2 = r.segments[1];
  eq(s1.from.code, 'DAC', 'OM s1 from');
  eq(s1.to.code, 'MCT', 'OM s1 to');
  eq(s1.flightNumber, 'WY684', 'OM s1 flight');
  eq(s1.date, '12 Oct 2025', 'OM s1 date');
  eq(s1.departure, '08:20 AM', 'OM s1 departure (as on ticket)');
  eq(s1.arrival, '11:15 AM', 'OM s1 arrival');
  eq(s1.duration, '5h 45m', 'OM s1 duration');
  eq(s2.from.code, 'MCT', 'OM s2 from');
  eq(s2.to.code, 'SLL', 'OM s2 to');
  eq(s2.flightNumber, 'WY225', 'OM s2 flight');
  eq(s2.departure, '01:45 PM', 'OM s2 departure');
  eq(s2.arrival, '03:20 PM', 'OM s2 arrival');
  eq(r.tripType, 'Journey with Transit', 'OM trip type');
  eq(r.transits.length, 1, 'OM one transit');
  eq(r.transits[0].connected, true, 'OM transit connected');
  eq(r.transits[0].duration, '2h 30m', 'OM transit duration');
  eq(r.transits[0].at, 'Muscat', 'OM transit at');
  eq(r.ticket.ticketNumber, '920-2412345678', 'OM ticket number');
  eq(r.ticket.fareBasis, 'OLOWBD', 'OM fare basis');
  eq(r.ticket.baggage, '30KG', 'OM baggage');
  eq(r.ticket.cabin, 'Economy', 'OM cabin');
  eq(r.ticket.bookingClass, 'O', 'OM booking class');
  eq(r.ticket.operatingCarrier, 'Oman Air', 'OM operating carrier');
  eq(r.ticket.aircraft, 'BOEING 737-800', 'OM aircraft');
});

/* ---- Saudia: 24h times, SURNAME/GIVEN, agent trap ---- */
runSample('saudia', (r) => {
  eq(r.airline, 'Saudia', 'SV airline');
  eq(r.passengerName, 'Mr Mohammed Alam', 'SV passenger from ALAM/MOHAMMED');
  eq(r.passport, 'A01234567', 'SV passport');
  eq(r.nationality, 'Bangladeshi', 'SV nationality');
  eq(r.pnr, 'MMMR2L', 'SV PNR');
  eq(r.segments.length, 1, 'SV one segment');
  const s = r.segments[0];
  eq(s.from.code, 'DAC', 'SV from');
  eq(s.to.code, 'JED', 'SV to');
  eq(s.flightNumber, 'SV772', 'SV flight');
  eq(s.date, '5 Nov 2025', 'SV date');
  eq(s.departure, '3:30 AM', 'SV departure converted');
  eq(s.arrival, '8:10 AM', 'SV arrival converted');
  eq(s.duration, '8h 40m', 'SV duration');
  eq(s.terminal, '1', 'SV terminal');
  eq(s.cabin, 'Economy', 'SV cabin');
  eq(r.tripType, 'One Way', 'SV trip type');
  eq(r.ticket.ticketNumber, '065-2412345678', 'SV ticket number');
  eq(r.ticket.fareBasis, 'YRTBD', 'SV fare basis');
  eq(r.ticket.baggage, '2PC', 'SV baggage');
});

/* ---- Qatar: round trip, compact rows, bare airport codes ---- */
runSample('qatar', (r) => {
  eq(r.airline, 'Qatar Airways', 'QR airline');
  eq(r.passengerName, 'Mrs Sabina Akter', 'QR passenger');
  eq(r.passport, 'EF0123456', 'QR passport');
  eq(r.pnr, 'Q8R5T2', 'QR PNR');
  eq(r.segments.length, 2, 'QR two segments');
  const s1 = r.segments[0], s2 = r.segments[1];
  eq(s1.from.code, 'DAC', 'QR s1 from');
  eq(s1.to.code, 'DOH', 'QR s1 to');
  eq(s1.flightNumber, 'QR635', 'QR s1 flight');
  eq(s1.date, '20 Oct 2025', 'QR s1 date');
  eq(s1.departure, '2:45 AM', 'QR s1 departure');
  eq(s1.arrival, '5:35 AM', 'QR s1 arrival');
  eq(s1.duration, '5h 50m', 'QR s1 duration');
  eq(s1.bookingClass, 'S', 'QR s1 class from (S)');
  eq(s2.from.code, 'DOH', 'QR s2 from');
  eq(s2.to.code, 'DAC', 'QR s2 to');
  eq(s2.departure, '7:40 AM', 'QR s2 departure');
  eq(s2.arrival, '7:55 PM', 'QR s2 arrival');
  eq(r.tripType, 'Round Trip', 'QR trip type');
  eq(r.transits[0].connected, true, 'QR transit connected');
  eq(r.transits[0].duration, '2h 5m', 'QR transit duration');
  eq(r.ticket.ticketNumber, '157-2412345678', 'QR ticket number');
  eq(r.ticket.baggage, '2PC', 'QR baggage');
});

/* ---- Emirates: overnight transit, next-day date ---- */
runSample('emirates', (r) => {
  eq(r.airline, 'Emirates', 'EK airline');
  eq(r.passengerName, 'Ms Nusrat Rahman', 'EK passenger');
  eq(r.passport, 'A1B2C3456', 'EK passport');
  eq(r.pnr, 'KB4T7V', 'EK PNR from Confirmation Number');
  eq(r.segments.length, 2, 'EK two segments');
  const s1 = r.segments[0], s2 = r.segments[1];
  eq(s1.from.code, 'DAC', 'EK s1 from');
  eq(s1.to.code, 'DXB', 'EK s1 to');
  eq(s1.flightNumber, 'EK585', 'EK s1 flight');
  eq(s1.date, '15 Dec 2025', 'EK s1 date');
  eq(s1.departure, '9:45 AM', 'EK s1 departure');
  eq(s1.arrival, '1:00 PM', 'EK s1 arrival');
  eq(s1.terminal, '3', 'EK s1 terminal');
  eq(s2.from.code, 'DXB', 'EK s2 from');
  eq(s2.to.code, 'LHR', 'EK s2 to');
  eq(s2.flightNumber, 'EK029', 'EK s2 flight');
  eq(s2.date, '16 Dec 2025', 'EK s2 date');
  eq(s2.departure, '10:05 AM', 'EK s2 departure');
  eq(s2.arrival, '2:20 PM', 'EK s2 arrival');
  eq(r.tripType, 'Journey with Transit', 'EK trip type');
  eq(r.transits[0].connected, true, 'EK transit connected');
  eq(r.transits[0].duration, '21h 5m', 'EK overnight transit');
  eq(r.ticket.bookingClass, 'T', 'EK booking class');
  eq(r.ticket.baggage, '30KG', 'EK baggage');
  eq(r.ticket.aircraft, 'BOEING 777', 'EK aircraft');
  eq(r.ticket.operatingCarrier, 'Emirates', 'EK operating carrier');
  eq(r.ticket.fareBasis, 'TLBD2', 'EK fare basis');
});

/* ---- Biman: one way, 24h ---- */
runSample('biman', (r) => {
  eq(r.airline, 'Biman Bangladesh Airlines', 'BG airline');
  eq(r.passengerName, 'Mohammad Al Amin', 'BG passenger');
  eq(r.passport, 'DP0123456', 'BG passport');
  eq(r.pnr, 'XL9M2K', 'BG PNR');
  eq(r.segments.length, 1, 'BG one segment');
  const s = r.segments[0];
  eq(s.from.code, 'CGP', 'BG from');
  eq(s.to.code, 'DAC', 'BG to');
  eq(s.flightNumber, 'BG435', 'BG flight');
  eq(s.date, '12 Oct 2025', 'BG date');
  eq(s.departure, '4:20 PM', 'BG departure');
  eq(s.arrival, '5:15 PM', 'BG arrival');
  eq(s.duration, '55m', 'BG duration');
  eq(s.cabin, 'Economy', 'BG cabin');
  eq(s.bookingClass, 'Y', 'BG booking class');
  eq(r.tripType, 'One Way', 'BG trip type');
  eq(r.ticket.ticketNumber, '997-2401234567', 'BG ticket number');
  eq(r.ticket.baggage, '20KG', 'BG baggage');
});

/* ---- US-Bangla: round trip, return is NOT a transit ---- */
runSample('usBangla', (r) => {
  eq(r.airline, 'US-Bangla Airlines', 'BS airline');
  eq(r.passengerName, 'Mrs Farzana Chowdhury', 'BS passenger');
  eq(r.passport, 'B12345678', 'BS passport');
  eq(r.pnr, 'BS7G1H', 'BS PNR');
  eq(r.segments.length, 2, 'BS two segments');
  const s1 = r.segments[0], s2 = r.segments[1];
  eq(s1.from.code, 'DAC', 'BS s1 from');
  eq(s1.to.code, 'CGP', 'BS s1 to');
  eq(s1.flightNumber, 'BS321', 'BS s1 flight');
  eq(s1.departure, '10:30 AM', 'BS s1 departure');
  eq(s1.arrival, '11:35 AM', 'BS s1 arrival');
  eq(s2.from.code, 'CGP', 'BS s2 from');
  eq(s2.to.code, 'DAC', 'BS s2 to');
  eq(s2.departure, '5:40 PM', 'BS s2 departure');
  eq(s2.arrival, '6:45 PM', 'BS s2 arrival');
  eq(r.tripType, 'Round Trip', 'BS trip type');
  eq(r.transits[0].connected, false, 'BS return leg is not a transit');
  eq(r.transits[0].duration, '', 'BS no fake transit duration');
  eq(r.ticket.baggage, '20KG', 'BS baggage');
  eq(r.ticket.fareBasis, 'EL2BG', 'BS fare basis');
});

/* ---- SalamAir: overnight arrival ---- */
runSample('salamAir', (r) => {
  eq(r.airline, 'SalamAir', 'OV airline');
  eq(r.passengerName, 'Mr Rakib Shakib', 'OV passenger');
  eq(r.passport, 'A7654321', 'OV passport');
  eq(r.pnr, 'OV9K3L', 'OV PNR');
  eq(r.segments.length, 1, 'OV one segment');
  const s = r.segments[0];
  eq(s.from.code, 'DAC', 'OV from');
  eq(s.to.code, 'MCT', 'OV to');
  eq(s.flightNumber, 'OV537', 'OV flight');
  eq(s.departure, '9:10 PM', 'OV departure');
  eq(s.arrival, '12:25 AM', 'OV arrival next day');
  eq(s.duration, '3h 15m', 'OV duration');
  eq(r.tripType, 'One Way', 'OV trip type');
  eq(r.ticket.baggage, '15KG', 'OV baggage');
});

/* ---------------- report ---------------- */
console.log('');
console.log('==================================================');
console.log('  PDF IMPORT ENGINE V3 — TEST RESULTS');
console.log('==================================================');
if (failures.length) {
  failures.forEach((f) => {
    console.log('\n✗ ' + f.msg);
    console.log('    expected: ' + f.expected);
    console.log('    actual:   ' + f.actual);
  });
}
console.log('\nPassed: ' + passed + '   Failed: ' + failed);
console.log(failed ? '\nSOME TESTS FAILED' : '\nALL TESTS PASSED ✅');
process.exit(failed ? 1 : 0);
