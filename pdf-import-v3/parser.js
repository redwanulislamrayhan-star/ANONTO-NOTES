/* =====================================================================
 *  PDF IMPORT ENGINE  V3
 *  Section-based airline ticket parser.
 *
 *  Pipeline:
 *      Raw text
 *        → Line model (normalized)
 *        → Section map  (Header → Passenger → Journey → Fare/Ticket → Extra)
 *        → Field extractors (context-aware, blacklisted)
 *        → Structured ticket (passenger / journey segments / ticket info)
 *
 *  Design goals
 *    - Passenger name comes ONLY from the ticket's main passenger section;
 *      Agent / Agency / Travel-Office / Issued-By names are never used.
 *    - Airline = brand (Oman Air, Saudia, Biman ...), not a flight number.
 *    - Journey = unlimited segments grouped per flight, transit computed.
 *    - AM/PM shown as on the ticket; 24-hour times converted.
 *    - Airline Reference (PNR) is its own field.
 *    - Handles Saudia, Oman Air, SalamAir, Emirates, Qatar Airways,
 *      Biman Bangladesh, US-Bangla, Flydubai, Air Arabia and more.
 *
 *  Pure functions only — no DOM. Runs in the browser and in Node (tests).
 * ===================================================================== */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory();
  else root.TicketParserV3 = factory();
})(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  /* ===================================================================
   * 1. KNOWLEDGE BASES
   * =================================================================== */

  var AIRLINES = [
    { name: 'Oman Air',                  codes: ['WY'], tokens: ['oman air', 'omanair'] },
    { name: 'Saudia',                    codes: ['SV'], tokens: ['saudia', 'saudi arabian airlines', 'saudi airlines'] },
    { name: 'SalamAir',                  codes: ['OV'], tokens: ['salamair', 'salam air'] },
    { name: 'Emirates',                  codes: ['EK'], tokens: ['emirates'] },
    { name: 'Qatar Airways',             codes: ['QR'], tokens: ['qatar airways', 'qatar airline'] },
    { name: 'Biman Bangladesh Airlines', codes: ['BG'], tokens: ['biman bangladesh', 'biman'] },
    { name: 'US-Bangla Airlines',        codes: ['BS'], tokens: ['us-bangla', 'us bangla', 'usbangla'] },
    { name: 'Flydubai',                  codes: ['FZ'], tokens: ['flydubai', 'fly dubai'] },
    { name: 'Air Arabia',                codes: ['G9', '3L'], tokens: ['air arabia'] },
    { name: 'Etihad Airways',            codes: ['EY'], tokens: ['etihad'] },
    { name: 'Turkish Airlines',          codes: ['TK'], tokens: ['turkish airlines', 'turkish airline'] },
    { name: 'SriLankan Airlines',        codes: ['UL'], tokens: ['srilankan airlines', 'sri lankan airlines', 'srilankan'] },
    { name: 'Vistara',                   codes: ['UK'], tokens: ['vistara'] },
    { name: 'IndiGo',                    codes: ['6E'], tokens: ['indigo'] },
    { name: 'Air India',                 codes: ['AI'], tokens: ['air india'] },
    { name: 'Thai Airways',              codes: ['TG'], tokens: ['thai airways', 'thai airline'] },
    { name: 'Malaysia Airlines',         codes: ['MH'], tokens: ['malaysia airlines'] },
    { name: 'Singapore Airlines',        codes: ['SQ'], tokens: ['singapore airlines'] },
    { name: 'Kuwait Airways',            codes: ['KU'], tokens: ['kuwait airways'] },
    { name: 'Gulf Air',                  codes: ['GF'], tokens: ['gulf air'] },
    { name: 'EgyptAir',                  codes: ['MS'], tokens: ['egyptair', 'egypt air'] },
    { name: 'Royal Jordanian',           codes: ['RJ'], tokens: ['royal jordanian'] },
    { name: 'Iraqi Airways',             codes: ['IA'], tokens: ['iraqi airways'] },
    { name: 'Jazeera Airways',           codes: ['J9'], tokens: ['jazeera airways'] },
    { name: 'flynas',                    codes: ['XY'], tokens: ['flynas'] },
    { name: 'flyadeal',                  codes: ['F3'], tokens: ['flyadeal'] },
    { name: 'Air Astana',                codes: ['KC'], tokens: ['air astana'] },
    { name: 'Malindo Air',               codes: ['OD'], tokens: ['malindo air'] },
    { name: 'Regent Airways',            codes: ['RX'], tokens: ['regent airways'] },
    { name: 'Novoair',                   codes: ['VQ'], tokens: ['novoair'] },
    { name: 'Air Astra',                 codes: ['2A'], tokens: ['air astra'] }
  ];

  var AIRLINE_BY_CODE = {};
  var AIRLINE_TOKENS = [];
  AIRLINES.forEach(function (a) {
    a.codes.forEach(function (c) { AIRLINE_BY_CODE[c] = a.name; });
    a.tokens.forEach(function (t) { AIRLINE_TOKENS.push({ token: t, name: a.name }); });
  });

  /* Airport knowledge — code → city (common BD / Gulf / S-Asia / EU / NA routes).
   * Unknown airports still work via generic "(DAC)" and bare-code detection. */
  var AIRPORTS = {
    DAC: 'Dhaka', CGP: 'Chattogram', ZYL: 'Sylhet', CXB: "Cox's Bazar", SPD: 'Saidpur',
    JSR: 'Jashore', BZL: 'Barishal', RJH: 'Rajshahi',
    MCT: 'Muscat', SLL: 'Salalah', DMM: 'Dammam', RUH: 'Riyadh', JED: 'Jeddah',
    MED: 'Madinah', TUU: 'Tabuk', AHB: 'Abha', ELQ: 'Buraidah', GIZ: 'Jizan',
    TIF: 'Taif', YNB: 'Yanbu',
    DXB: 'Dubai', DWC: 'Dubai Al Maktoum', AUH: 'Abu Dhabi', SHJ: 'Sharjah', RKT: 'Ras Al Khaimah',
    DOH: 'Doha', KWI: 'Kuwait City', BAH: 'Bahrain',
    KHI: 'Karachi', LHE: 'Lahore', ISB: 'Islamabad', PEW: 'Peshawar',
    DEL: 'Delhi', BOM: 'Mumbai', MAA: 'Chennai', BLR: 'Bengaluru', HYD: 'Hyderabad',
    CCU: 'Kolkata', COK: 'Kochi', TRV: 'Thiruvananthapuram', IXB: 'Bagdogra', ATQ: 'Amritsar',
    GAU: 'Guwahati', IXA: 'Agartala',
    CMB: 'Colombo', KTM: 'Kathmandu', PBH: 'Paro',
    BKK: 'Bangkok', DMK: 'Bangkok Don Mueang', HKT: 'Phuket', CNX: 'Chiang Mai',
    KUL: 'Kuala Lumpur', PEN: 'Penang', SIN: 'Singapore', CGK: 'Jakarta', DPS: 'Bali',
    HKG: 'Hong Kong', CAN: 'Guangzhou', PEK: 'Beijing', PVG: 'Shanghai',
    IST: 'Istanbul', SAW: 'Istanbul Sabiha', ATH: 'Athens', FCO: 'Rome', MXP: 'Milan',
    CDG: 'Paris', AMS: 'Amsterdam', FRA: 'Frankfurt', MUC: 'Munich', ZRH: 'Zurich',
    VIE: 'Vienna', MAD: 'Madrid', BCN: 'Barcelona', LIS: 'Lisbon', CPH: 'Copenhagen',
    ARN: 'Stockholm', OSL: 'Oslo', HEL: 'Helsinki', DUB: 'Dublin', BRU: 'Brussels',
    LHR: 'London Heathrow', LGW: 'London Gatwick', STN: 'London Stansted',
    LTN: 'London Luton', LCY: 'London City', MAN: 'Manchester', BHX: 'Birmingham',
    LPL: 'Liverpool', NCL: 'Newcastle', EDI: 'Edinburgh', GLA: 'Glasgow', BRS: 'Bristol',
    JFK: 'New York JFK', EWR: 'Newark', YYZ: 'Toronto', YVR: 'Vancouver', YUL: 'Montreal',
    ORD: 'Chicago', IAD: 'Washington', BOS: 'Boston', LAX: 'Los Angeles', SFO: 'San Francisco',
    MIA: 'Miami', IAH: 'Houston', ATL: 'Atlanta', SEA: 'Seattle',
    CAI: 'Cairo', AMM: 'Amman', BEY: 'Beirut', TBS: 'Tbilisi',
    ALA: 'Almaty', TAS: 'Tashkent', FRU: 'Bishkek', DYU: 'Dushanbe',
    KBL: 'Kabul', MLE: 'Malé'
  };

  /* City name → code (when the code is missing but the city is spelled out) */
  var CITY_TO_CODE = {};
  Object.keys(AIRPORTS).forEach(function (code) {
    var city = AIRPORTS[code];
    if (city && !CITY_TO_CODE[city.toLowerCase()]) CITY_TO_CODE[city.toLowerCase()] = code;
  });
  CITY_TO_CODE['chittagong'] = 'CGP';
  CITY_TO_CODE['calcutta'] = 'CCU';
  CITY_TO_CODE['new york'] = 'JFK';
  CITY_TO_CODE['london'] = 'LHR';

  var MONTHS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
  var DISPLAY_MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  var MONTH_SET = new Set(MONTHS);

  /* 3-letter tokens that are NOT airport codes */
  var CODE_STOPWORDS = new Set([
    'PNR', 'ETK', 'ETKT', 'USD', 'BDT', 'SAR', 'AED', 'QAR', 'INR', 'GBP', 'EUR', 'OMR',
    'KWD', 'BHD', 'LKR', 'NPR', 'PKR', 'TRY', 'USA', 'UAE', 'KSA', 'GCC', 'YES',
    'NOT', 'AND', 'THE', 'FOR', 'PER', 'ONE', 'TWO', 'SIX', 'TEN', 'TAX', 'VAT',
    'REF', 'NOS', 'APT', 'GDS', 'WEB', 'APP', 'PDF', 'XML', 'SMS', 'OTP', 'RMK',
    'GST', 'IDD', 'ADT', 'CHD', 'INF', 'CNN', 'STF', 'PKG', 'PCS', 'LBS', 'BAG',
    'ALL', 'END', 'NEW', 'OLD', 'OUT', 'OFF', 'BUS', 'ECO', 'PRE', 'FIR', 'SEC',
    'ARR', 'DEP', 'STA', 'STD', 'ETD', 'ETA', 'FLT', 'AIR', 'WAY', 'INC',
    'LTD', 'LLC', 'PVT', 'PRO', 'NAM', 'ADD', 'MOB', 'TEL', 'FAX', 'EMA', 'WWW',
    'DATE', 'TIME', 'FROM', 'VIA', 'SEE', 'NON', 'SELF', 'PAGE', 'TOUR', 'TRIP',
    'OW', 'RT', 'TIC', 'TKT', 'ITN', 'PAY', 'DUE', 'SUB', 'CHG', 'OPN', 'CLS',
    'MR', 'MRS', 'MSTR', 'MISS', 'MS', 'DR', 'GUEST', 'CHECK'
  ]);
  MONTHS.forEach(function (m) { CODE_STOPWORDS.add(m); });

  /* Two-letter prefixes that must never be treated as airline designators
   * (only applied to prefixes that are NOT known airline codes). */
  var FLIGHT_PREFIX_STOP = new Set([
    'NO', 'TO', 'OF', 'IN', 'ON', 'AT', 'BY', 'IF', 'IS', 'IT', 'AS', 'AN', 'OR',
    'SO', 'UP', 'WE', 'HE', 'ID', 'AM', 'PM', 'DO', 'GO', 'MY', 'ME', 'US', 'DT',
    'ST', 'RD', 'TH', 'ND', 'SEQ', 'PG', 'PAX', 'FLT', 'TKT', 'REF', 'HR', 'MIN',
    'DAY', 'PCS', 'KG', 'AD', 'CD', 'IN', 'EX'
  ]);

  var TITLES = ['MR', 'MSTR', 'MS', 'MRS', 'MISS', 'DR', 'MASTER', 'MDM', 'MX', 'PROF', 'MD'];

  var AGENT_PREFIX_RE = /^(agent|agency|agencies|office|issued|booked|prepared|sold|seller|contact|phone|mobile|email|address|operator|branch|counter|handled|billed)\b/i;
  var AGENT_WORD_RE = /\b(agency|travels|tours|ltd|llc|limited|pvt|consultant|office|operator|branch)\b/i;
  var AGENT_CONTAINS = [
    'agent', 'agency', 'travel office', 'issued by', 'booked by', 'prepared by',
    'sold by', 'printed by', 'generated by', 'designed by', 'tour code',
    'remarks', 'endorsement', 'handling', 'billing', 'invoice'
  ];

  var NAME_BAD_WORDS = new Set([
    'NAME', 'PASSENGER', 'PASSENGERS', 'PAX', 'AGENT', 'AGENCY', 'OFFICE', 'TRAVELS',
    'TOURS', 'LTD', 'LLC', 'LIMITED', 'PVT', 'CONTACT', 'EMAIL', 'PHONE', 'MOBILE',
    'AIRLINE', 'AIRWAYS', 'AIRLINES', 'AIR', 'FLIGHT', 'TICKET', 'NATIONALITY',
    'PASSPORT', 'VISA', 'BANGLADESH', 'DHAKA', 'INFORMATION', 'DETAILS', 'RECEIPT',
    'ITINERARY', 'JOURNEY', 'BOOKING', 'REFERENCE', 'TRAVEL', 'CHECK', 'WEB',
    'ISSUED', 'CABIN', 'CLASS', 'DATE', 'TIME', 'BAGGAGE', 'TOTAL', 'FARE'
  ]);

  /* ===================================================================
   * 2. SMALL HELPERS
   * =================================================================== */

  function collapse(s) { return String(s || '').replace(/\s+/g, ' ').trim(); }

  var TITLE_CASE = { mr: 'Mr', ms: 'Ms', dr: 'Dr', mx: 'Mx', md: 'Md', mstr: 'Mstr', miss: 'Miss', master: 'Master', prof: 'Prof', mdm: 'Mdm' };

  function titleCaseWord(w) {
    if (!w) return w;
    var lower = w.toLowerCase();
    if (TITLE_CASE[lower]) return TITLE_CASE[lower];
    if (w === w.toUpperCase() && w.length >= 2) {
      return w.toLowerCase().replace(/(^|[\s'\-])([a-z])/g, function (m, p, c) { return p + c.toUpperCase(); });
    }
    return w.charAt(0).toUpperCase() + w.slice(1);
  }
  function prettifyName(s) { return collapse(s).split(' ').map(titleCaseWord).join(' '); }

  /* ---- time ---- */
  /* Parse a time string → { h, m, ampm|null } or null */
  function parseTime(s) {
    if (s == null) return null;
    s = collapse(s).toUpperCase();
    var m = s.match(/^(\d{1,2})[:.](\d{2})\s*(AM|PM|A\.M\.|P\.M\.|A|P)?$/) ||
            s.match(/^(\d{1,2})\s*(AM|PM)$/) ||
            s.match(/^([01]\d|2[0-3])([0-5]\d)$/);
    if (!m) return null;
    var h, min, ap = null;
    if (m[2] && /^\d+$/.test(m[2])) { h = +m[1]; min = +m[2]; ap = m[3] || null; }
    else if (m[3] && /^(AM|PM)/.test(m[3])) { h = +m[1]; min = 0; ap = m[3].replace(/\./g, '').toUpperCase(); }
    else { h = +m[1]; min = +m[2]; }
    if (ap === 'A') ap = 'AM';
    if (ap === 'P') ap = 'PM';
    if (h > 23 || min > 59) return null;
    if (ap && h > 12) return null;
    return { h: h, m: min, ampm: ap };
  }

  /* V3 display rule: when the ticket itself shows AM/PM keep the digits as
   * printed; otherwise convert 24-hour ("15:20") to "3:20 PM". */
  function displayTime(raw) {
    var t = parseTime(raw);
    if (!t) return collapse(raw);
    if (t.ampm) {
      return collapse(raw).toUpperCase().replace(/^(\d{1,2})[:.](\d{2})\s*(A|P)\.?M?\.?$/, function (_, h, mm, ap) {
        return h + ':' + mm + ' ' + ap + 'M';
      });
    }
    var ap = t.h >= 12 ? 'PM' : 'AM';
    var hh = t.h % 12 === 0 ? 12 : t.h % 12;
    return hh + ':' + String(t.m).padStart(2, '0') + ' ' + ap;
  }

  function timeToMinutes(t) {
    if (!t) return null;
    var h = t.h, m = t.m;
    if (t.ampm === 'PM' && h < 12) h += 12;
    if (t.ampm === 'AM' && h === 12) h = 0;
    return h * 60 + m;
  }

  /* ---- date ---- */
  var MONTH_RE = MONTHS.join('|');

  function expandYear(y) {
    y = parseInt(y, 10);
    if (y < 100) y += 2000;
    return y;
  }
  function buildDate(d, mon, y) {
    if (mon == null || mon < 0 || mon > 11 || d < 1 || d > 31) return null;
    var base = new Date();
    var year = y != null ? y : base.getFullYear();
    if (year < 1990 || year > 2100) year = base.getFullYear();
    return { day: d, month: mon, year: year, text: d + ' ' + DISPLAY_MONTHS[mon] + ' ' + year };
  }

  function parseDateStr(s) {
    if (!s) return null;
    s = collapse(s).toUpperCase();
    var m;
    m = s.match(new RegExp('\\b(\\d{1,2})\\s*[-\\/ ]?\\s*(' + MONTH_RE + ')[A-Z]*\\.?\\s*[-\\/, ]?\\s*(\\d{2,4})?\\b'));
    if (m) return buildDate(+m[1], MONTHS.indexOf(m[2].slice(0, 3)), m[3] != null ? expandYear(m[3]) : null);
    m = s.match(new RegExp('\\b(' + MONTH_RE + ')[A-Z]*\\.?\\s+(\\d{1,2})\\s*,?\\s*(\\d{2,4})?\\b'));
    if (m) return buildDate(+m[2], MONTHS.indexOf(m[1].slice(0, 3)), m[3] != null ? expandYear(m[3]) : null);
    m = s.match(/\b(\d{4})-(\d{1,2})-(\d{1,2})\b/);
    if (m) return buildDate(+m[3], +m[2] - 1, +m[1]);
    m = s.match(/\b(\d{1,2})[-\/.](\d{1,2})[-\/.](\d{2,4})\b/);
    if (m) {
      var a = +m[1], b = +m[2], y = expandYear(m[3]);
      if (a > 12 && b <= 12) return buildDate(a, b - 1, y);
      if (b > 12 && a <= 12) return buildDate(b, a - 1, y);
      return buildDate(a, b - 1, y); // day-first default (BD / Gulf context)
    }
    return null;
  }

  function dateToUtc(d) { return d ? Date.UTC(d.year, d.month, d.day) : null; }

  /* First date on a line → {day,month,year,text} */
  function findDateOnLine(line) {
    var re = new RegExp(
      '(\\d{1,2}\\s*[-\\/ ]?\\s*(?:' + MONTH_RE + ')[A-Z]*\\.?\\s*[-\\/, ]?\\s*\\d{0,4})' +
      '|((?:' + MONTH_RE + ')\\s+\\d{1,2}\\s*,?\\s*\\d{0,4})' +
      '|(\\d{4}-\\d{1,2}-\\d{1,2})' +
      '|(\\d{1,2}[-\\/.]\\d{1,2}[-\\/.]\\d{2,4})', 'i');
    var m = collapse(line).match(re);
    if (!m) return null;
    return parseDateStr(m[0]);
  }

  /* ---- duration ---- */
  function parseDurationStr(s) {
    if (!s) return null;
    s = collapse(s);
    var m = s.match(/(\d{1,2})\s*(?:H|HR|HRS|HOUR|HOURS)\s*(\d{1,2})?\s*(?:M|MIN|MINS|MINUTE|MINUTES)?\b/i);
    if (m) return { h: +m[1], m: m[2] ? +m[2] : 0 };
    m = s.match(/duration[^0-9]*(\d{1,2})[:.](\d{2})/i);
    if (m) return { h: +m[1], m: +m[2] };
    return null;
  }
  function formatDuration(d) {
    if (!d) return '';
    if (!d.h) return d.m + 'm';
    if (d.m) return d.h + 'h ' + d.m + 'm';
    return d.h + 'h';
  }

  function minutesBetween(fromTime, toTime) {
    var a = timeToMinutes(fromTime), b = timeToMinutes(toTime);
    if (a == null || b == null) return null;
    var diff = b - a;
    if (diff < 0) diff += 24 * 60; // overnight
    return diff;
  }

  /* ===================================================================
   * 3. SECTION MAP
   * =================================================================== */

  var PASSENGER_SECTION_RES = [
    /web\s*check[\s\-]?in/i, /travel\s*information/i, /journey\s*details?/i,
    /flight\s*details?/i, /flight\s*information/i, /electronic\s*ticket/i,
    /passenger\s*(information|details?|name)\b/i, /^passengers?\s*:?$/i,
    /booking\s*details?/i, /itinerary\b/i, /trip\s*details?/i, /e-?ticket/i
  ];
  var JOURNEY_SECTION_RES = [
    /flight\s*(details?|information|itinerary|segment)/i, /itinerary\b/i,
    /journey\b/i, /sector/i, /coupon/i, /travel\s*(information|details?)/i,
    /from\b.*\bto\b/i
  ];
  var FARE_SECTION_RES = [
    /\bfare\b/i, /\btax(es)?\b/i, /\btotal\b/i, /amount/i, /payment/i, /price/i, /charge/i
  ];

  function firstMatchIndex(lines, resList, from, to) {
    from = from || 0; to = to == null ? lines.length : to;
    for (var i = from; i < to; i++) {
      var l = collapse(lines[i]);
      for (var j = 0; j < resList.length; j++) if (resList[j].test(l)) return i;
    }
    return -1;
  }

  function mapSections(lines) {
    var passengerStart = firstMatchIndex(lines, PASSENGER_SECTION_RES, 0);
    var journeyStart = firstMatchIndex(lines, JOURNEY_SECTION_RES, passengerStart >= 0 ? passengerStart : 0);
    if (journeyStart < 0) journeyStart = passengerStart;
    var fareStart = firstMatchIndex(lines, FARE_SECTION_RES, journeyStart >= 0 ? journeyStart + 1 : 0);
    return {
      passengerStart: passengerStart,
      journeyStart: journeyStart,
      journeyEnd: fareStart >= 0 ? fareStart : lines.length,
      fareStart: fareStart
    };
  }

  /* ===================================================================
   * 4. HEADER — AIRLINE BRAND
   * =================================================================== */

  function detectAirline(lines) {
    var joined = lines.slice(0, Math.min(lines.length, 45)).join(' \n ').toLowerCase();
    var best = null;
    AIRLINE_TOKENS.forEach(function (t) {
      if (!best && joined.indexOf(t.token) >= 0) best = t.name;
    });
    return best;
  }

  /* ===================================================================
   * 5. PASSENGER SECTION (SMART NAME DETECTION)
   * =================================================================== */

  function hasAgentContext(text) {
    var t = collapse(text).toLowerCase();
    if (!t) return false;
    if (AGENT_PREFIX_RE.test(t)) return true;
    if (/\bpassenger\b/.test(t) || /\bpax\b/.test(t)) return false; // passenger lines are fine
    if (AGENT_WORD_RE.test(t)) return true;
    for (var i = 0; i < AGENT_CONTAINS.length; i++) {
      if (t.indexOf(AGENT_CONTAINS[i]) >= 0) return true;
    }
    return false;
  }

  function looksLikeName(s) {
    s = collapse(s).replace(/['"]/g, '');
    if (!s) return false;
    if (/\d/.test(s)) return false;
    if (/[\\/@#$%^&*()_+=\[\]{};:|<>?~]/.test(s)) return false;
    var tokens = s.split(' ').filter(Boolean);
    if (tokens.length < 2 || tokens.length > 5) return false;
    var letters = 0;
    for (var i = 0; i < tokens.length; i++) {
      var tk = tokens[i].replace(/[.\-']/g, '');
      if (tk.length < 2 || tk.length > 25) return false;
      if (!/^[A-Za-z]+$/.test(tk)) return false;
      letters += tk.length;
      if (NAME_BAD_WORDS.has(tk.toUpperCase())) return false;
    }
    return letters >= 5;
  }

  /* Ticket name formats:
   *   "MD MAINUL ISLAM"     → Md Mainul Islam
   *   "MR MOHAMMED ALAM"    → Mr Mohammed Alam
   *   "ALAM/MOHAMMED"       → Mohammed Alam   (SURNAME/GIVEN)
   *   "ISLAM/MOHAMMED MR"   → Mr Mohammed Islam
   */
  function normalizeName(raw) {
    var s = collapse(raw).replace(/['"]/g, "'").replace(/\s*\/\s*/g, '/');
    if (/\b(travels|tours|ltd|llc|limited|pvt|agency|agencies|airlines|airways|airline)\b/i.test(s)) return null;
    var title = '';
    var firstTok = s.split(' ')[0].toUpperCase().replace(/\.$/, '');
    if (TITLES.indexOf(firstTok) >= 0) { title = firstTok; s = s.split(' ').slice(1).join(' '); }

    if (s.indexOf('/') >= 0) {
      var parts = s.split('/').filter(Boolean);
      if (parts.length >= 2) {
        var surname = parts[0], given = parts[parts.length - 1];
        var gToks = given.split(' ').filter(Boolean);
        if (gToks.length > 1 && TITLES.indexOf(gToks[gToks.length - 1].toUpperCase()) >= 0) {
          if (!title) title = gToks[gToks.length - 1].toUpperCase();
          gToks.pop(); given = gToks.join(' ');
        }
        var sToks = surname.split(' ').filter(Boolean);
        if (sToks.length > 1 && TITLES.indexOf(sToks[0].toUpperCase()) >= 0) {
          if (!title) title = sToks[0].toUpperCase();
          sToks.shift(); surname = sToks.join(' ');
        }
        if (parts.length > 2) given = given + ' ' + parts.slice(1, -1).join(' ');
        s = collapse(given + ' ' + surname);
      }
    }
    var toks = s.split(' ').filter(function (t) {
      return t && !NAME_BAD_WORDS.has(t.toUpperCase()) && /^[A-Za-z'.\-]+$/.test(t);
    });
    s = toks.join(' ');
    if (!looksLikeName(s)) return null;
    return prettifyName((title ? title + ' ' : '') + s);
  }

  function extractPassengers(lines, sections) {
    var found = [];
    var seen = {};
    var start = sections.passengerStart >= 0 ? sections.passengerStart : 0;

    function pushCandidate(value, lineIdx, method) {
      value = collapse(value);
      if (!value) return;
      var key = value.toUpperCase();
      if (seen[key]) return;
      if (hasAgentContext(value)) return;
      var norm = normalizeName(value);
      if (!norm) return;
      seen[key] = true;
      found.push({ name: norm, line: lineIdx, method: method });
    }

    // 1) labelled "Passenger Name: X"
    var labelRe = /^(?:passenger(?:\s*name)?|pax(?:\s*name)?|travellers?(?:\s*name)?|travelers?(?:\s*name)?|guest(?:\s*name)?|name)\s*[:\-]\s*(.+)$/i;
    for (var i = start; i < lines.length; i++) {
      var m = collapse(lines[i]).match(labelRe);
      if (m) pushCandidate(m[1], i, 'label');
      if (found.length >= 4) break;
    }

    // 2) "PASSENGER(S)" header, then the next name-like line
    if (!found.length) {
      for (var i2 = start; i2 < lines.length; i2++) {
        if (/^passengers?\s*[:\-]?\s*(name)?\s*$/i.test(collapse(lines[i2]))) {
          for (var k = i2 + 1; k <= Math.min(i2 + 3, lines.length - 1); k++) {
            var cand = collapse(lines[k]);
            if (!cand || /^[\d\s.,#*()\-]+$/.test(cand)) continue;
            pushCandidate(cand, k, 'after-header');
            if (found.length) break;
          }
          if (found.length) break;
        }
      }
    }

    // 3) honorific scan inside the passenger section
    if (!found.length) {
      var honRe = /\b(MR|MSTR|MS|MRS|MISS|DR|MASTER|MDM|MD|PROF)\b\.?\s+((?:[A-Za-z][A-Za-z'\-]*\s*){1,4})/g;
      for (var i3 = start; i3 < Math.min(start + 80, lines.length); i3++) {
        var line = collapse(lines[i3]);
        var hm;
        while ((hm = honRe.exec(line))) {
          var before = line.slice(Math.max(0, hm.index - 30), hm.index);
          if (hasAgentContext(before) && !/passenger|name|pax/i.test(before)) continue;
          pushCandidate(hm[0], i3, 'honorific');
          if (found.length) break;
        }
      }
    }

    // 4) last resort — labelled "NAME" anywhere
    if (!found.length) {
      for (var i4 = 0; i4 < lines.length; i4++) {
        var m4 = collapse(lines[i4]).match(/^(?:passenger\s*)?name\s*[:\-]\s*(.+)$/i);
        if (m4 && !hasAgentContext(lines[i4])) { pushCandidate(m4[1], i4, 'label-any'); break; }
      }
    }
    return found;
  }

  /* ===================================================================
   * 6. LABELLED FIELDS
   * =================================================================== */

  function fieldByLabel(lines, regexes, opts) {
    opts = opts || {};
    for (var i = 0; i < lines.length; i++) {
      var l = collapse(lines[i]);
      for (var r = 0; r < regexes.length; r++) {
        var m = l.match(regexes[r]);
        if (m) {
          var val = collapse(m[1]);
          if (opts.validate && !opts.validate(val, l)) continue;
          if (opts.noAgent && hasAgentContext(l)) continue;
          return { value: val, line: i };
        }
      }
    }
    return null;
  }

  function extractPassport(lines) {
    var res = fieldByLabel(lines, [
      /(?:passport|ppt)\s*(?:no\.?|number|#)?\s*[:\-]\s*\**\s*([A-Z0-9]{6,9})\b/i,
      /\bpassport\s*(?:no\.?|number)?\s*[:\-]?\s*\**([A-Z0-9]{6,9})\b/i,
      /\bpp\s*(?:no\.?|number)?\s*[:\-]\s*([A-Z0-9]{6,9})\b/i
    ], { validate: function (v) { return /\d/.test(v); } });
    return res ? res.value.toUpperCase() : '';
  }

  function extractNationality(lines) {
    var res = fieldByLabel(lines, [
      /nationality\s*[:\-]\s*([A-Za-z ()\/]{3,25})/i
    ], { validate: function (v) { return /^[A-Za-z ()]+$/.test(v) && v.length >= 3; } });
    if (!res) return '';
    var val = res.value.replace(/\(.*?\)/g, '').replace(/\/.*$/, '').trim();
    if (!val) return '';
    var lower = val.toLowerCase();
    if (lower === 'bangladesh' || lower === 'bangladeshi') return 'Bangladeshi';
    return val.split(' ').map(titleCaseWord).join(' ');
  }

  var PNR_VALUE_BLACKLIST = new Set([
    'ETKT', 'ETICKET', 'ECONOMY', 'BUSINESS', 'FIRST', 'TICKET', 'NUMBER',
    'SAUDIA', 'OMANAIR', 'EMIRATE', 'QATAR', 'BIMAN', 'TOTAL', 'FARE', 'TAXES',
    'BAGGAGE', 'CABIN', 'ONLINE', 'MOBILE', 'SEATS'
  ]);

  function validPNR(v) {
    if (!/^[A-Z0-9]{5,6}$/.test(v)) return false;
    if (!/[A-Z]/.test(v)) return false;
    if (PNR_VALUE_BLACKLIST.has(v)) return false;
    if (/^(.)\1+$/.test(v)) return false;
    return true;
  }

  function extractPNR(lines) {
    var labelled = [
      /(?:airline|booking|reservation|itinerary|flight)\s*(?:reference|ref|locator|confirmation)\s*(?:no\.?|number|#)?\s*(?:\(pnr\))?\s*[:\-]?\s*\**\s*([A-Z0-9]{5,6})\b/i,
      /(?:booking|reservation)\s*(?:no\.?|number|#)\s*(?:\(pnr\))?\s*[:\-]\s*\**\s*([A-Z0-9]{5,6})\b/i,
      /\bp\.?\s?n\.?\s?r\.?\s*(?:no\.?|number|#)?\s*[:\-]\s*\**\s*([A-Z0-9]{5,6})\b/i,
      /(?:reference|locator|confirmation)\s*(?:no\.?|number|#|code)?\s*[:\-]\s*\**\s*([A-Z0-9]{5,6})\b/i
    ];
    var res = fieldByLabel(lines, labelled, { validate: validPNR, noAgent: true });
    if (res) return res.value.toUpperCase();

    // standalone code near the top (header style)
    var limit = Math.min(lines.length, 25);
    for (var i = 0; i < limit; i++) {
      var l = collapse(lines[i]);
      var m = l.match(/^\**([A-Z0-9]{6})\**$/);
      if (m && validPNR(m[1]) && /\d/.test(m[1])) return m[1];
      var m2 = l.match(/(?:ref|pnr|booking|record)[^A-Z0-9]*([A-Z0-9]{5,6})\s*$/i);
      if (m2 && validPNR(m2[1]) && /\d/.test(m2[1]) && !hasAgentContext(l)) return m2[1];
    }
    return '';
  }

  /* Booking class: single letter — "Class: Y", "Booking Class: T",
   * "Economy (S)", "Class: Economy (O)" */
  function extractBookingClass(text) {
    var t = collapse(text);
    var m = t.match(/\bclass\s*[:\-][^()]{0,20}\(\s*([A-Z])\s*\)/i) ||
            t.match(/\b(?:booking\s*)?class\s*[:\-]\s*\(?\s*([A-Z])\s*\)?\s*$/i) ||
            t.match(/\b(?:economy|business|first|premium\s+economy)[^()]{0,15}\(\s*([A-Z])\s*\)/i);
    return m ? m[1].toUpperCase() : '';
  }

  /* ===================================================================
   * 7. AIRPORT / CITY MENTIONS (ordered)
   * =================================================================== */

  var PAREN_CITY_RE = /((?:[A-Za-z][A-Za-z'\-]*\s+){0,2}[A-Za-z][A-Za-z'\-]*)\s*[（(]\s*([A-Z]{3})\s*[)）]/g;
  var CODE_FIRST_RE = /\b([A-Z]{3})\b\s*[-–—:]\s*([A-Za-z][A-Za-z'\- ]{1,24})/g;
  var BARE_CODE_RE = /\b([A-Z]{3})\b/g;
  var LABEL_STRIP_RE = /^(from|to|via|depart|departure|arrive|arrival|destination|origin)\s*[:\-]?\s*/i;

  function isAirportCode(code) {
    return !!AIRPORTS[code] || !CODE_STOPWORDS.has(code);
  }

  /* "Arrival: 11:15 AM Muscat" → "Muscat" */
  function cleanCity(s) {
    s = collapse(s)
      .replace(LABEL_STRIP_RE, '')
      .replace(/[^A-Za-z'\- ]+/g, ' ')
      .replace(/\b(AM|PM)\b/gi, ' ');
    return collapse(s.split(' ').filter(function (w) { return w && w.toLowerCase() !== 'via' && w.toLowerCase() !== 'flight'; }).map(titleCaseWord).join(' '));
  }

  function findAirportMentions(lines, fromIdx, toIdx) {
    var out = [];
    for (var i = fromIdx; i <= toIdx && i < lines.length; i++) {
      var l = collapse(lines[i]);
      var lineCodes = {}; // codes already found on this line (dedupe sources)
      var m;
      PAREN_CITY_RE.lastIndex = 0;
      while ((m = PAREN_CITY_RE.exec(l))) {
        var code = m[2];
        if (!isAirportCode(code)) continue;
        var city = cleanCity(m[1]);
        out.push({ city: city ? prettifyName(city) : (AIRPORTS[code] || code), code: code, line: i, pos: m.index, labeled: true });
        lineCodes[code] = true;
      }
      CODE_FIRST_RE.lastIndex = 0;
      while ((m = CODE_FIRST_RE.exec(l))) {
        if (!AIRPORTS[m[1]] || lineCodes[m[1]]) continue;
        out.push({ city: prettifyName(m[2]), code: m[1], line: i, pos: m.index, labeled: true });
        lineCodes[m[1]] = true;
      }
      BARE_CODE_RE.lastIndex = 0;
      while ((m = BARE_CODE_RE.exec(l))) {
        var c2 = m[1];
        if (!isAirportCode(c2) || lineCodes[c2]) continue;
        if (!AIRPORTS[c2]) continue;                 // bare unknown codes are too risky
        out.push({ city: AIRPORTS[c2], code: c2, line: i, pos: m.index, labeled: false });
        lineCodes[c2] = true;
      }
      // bare known city names ("Dhaka - Muscat")
      Object.keys(CITY_TO_CODE).forEach(function (cname) {
        var re = new RegExp('\\b' + cname.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\b', 'i');
        var bm = l.match(re);
        if (bm) {
          var code2 = CITY_TO_CODE[cname];
          if (lineCodes[code2]) return;
          out.push({ city: titleCaseWord(cname), code: code2, line: i, pos: bm.index, labeled: false, cityOnly: true });
          lineCodes[code2] = true;
        }
      });
    }
    out.sort(function (a, b) { return a.line - b.line || a.pos - b.pos; });
    return out;
  }

  /* ===================================================================
   * 8. FLIGHT REFERENCES
   * =================================================================== */

  var PNR_LINE_RE = /(?:\bp\.?\s?n\.?\s?r\b|booking\s*(?:reference|ref)|reservation|confirmation|record\s*locator)/i;

  function findFlightRefs(lines, fromIdx, toIdx) {
    var refs = [];
    var re = /\b([0-9A-Z]{2})\s?-?\s?(\d{2,4})\b/g;
    for (var i = fromIdx; i <= toIdx && i < lines.length; i++) {
      var l = collapse(lines[i]);
      // PNR / reference lines must not produce flights (e.g. "PNR: EK585")
      if (PNR_LINE_RE.test(l) && !/\bflight\b/i.test(l)) continue;
      var known = AIRLINE_BY_CODE.hasOwnProperty(l.slice(0, 2)) ? false : false; // placeholder
      var m;
      while ((m = re.exec(l))) {
        var prefix = m[1], num = m[2];
        if (!/[A-Z]/.test(prefix)) continue;
        var isKnown = !!AIRLINE_BY_CODE[prefix];
        if (!isKnown) {
          if (FLIGHT_PREFIX_STOP.has(prefix)) continue;
          if (num.length === 4 && /^(19|20)/.test(num)) continue;      // looks like a year
          // unknown designator: only trust it next to the word "flight"
          if (!/\bflight\b/i.test(l)) continue;
        } else if (num.length === 4 && /^(19|20)/.test(num) && !/\bflight\b/i.test(l) && !AIRLINE_BY_CODE[prefix]) {
          continue;
        }
        refs.push({ prefix: prefix, num: num, flight: prefix + num, line: i, pos: m.index, airline: AIRLINE_BY_CODE[prefix] || null });
      }
    }
    return refs;
  }

  /* ===================================================================
   * 9. TIMES
   * =================================================================== */

  /* find all times in text; bare 4-digit runs 2020-2099 are treated as years */
  function findTimes(text) {
    var out = [];
    var re = /\b(\d{1,2})[:.](\d{2})\s*(AM|PM)?\b|\b([01]\d|2[0-3])([0-5]\d)\b|\b(\d{1,2})\s?(AM|PM)\b/g;
    var s = collapse(text);
    var m;
    while ((m = re.exec(s))) {
      var t = null;
      if (m[1] != null) t = { h: +m[1], m: +m[2], ampm: m[3] ? m[3].toUpperCase() : null };
      else if (m[4] != null) {
        var four = parseInt(m[4] + m[5], 10);
        if (four >= 2020 && four <= 2099) continue; // year, not a time
        t = { h: +m[4], m: +m[5], ampm: null };
      }
      else if (m[6] != null) t = { h: +m[6], m: 0, ampm: m[7].toUpperCase() };
      if (!t) continue;
      if (t.m > 59) continue;
      if (t.ampm && t.h > 12) continue;
      out.push({ time: t, pos: m.index, raw: m[0] });
    }
    return out;
  }

  /* replace flight tokens ("WY 684", "WY684") so they are not read as times */
  function maskFlights(text, flights) {
    var s = text;
    flights.forEach(function (f) {
      s = s.replace(new RegExp(f.prefix + '\\s?-?\\s?' + f.num, 'g'), ' ');
    });
    return s;
  }

  /* ===================================================================
   * 10. JOURNEY — SEGMENTS
   * =================================================================== */

  function emptySegment() {
    return {
      from: { city: '', code: '' },
      to: { city: '', code: '' },
      airline: '', flightNumber: '',
      date: '', departure: '', arrival: '',
      duration: '', durationMin: null,
      cabin: '', bookingClass: '', seat: '', terminal: '', gate: ''
    };
  }

  function extractSegments(lines, sections, brand) {
    var jStart = sections.journeyStart >= 0 ? sections.journeyStart
      : (sections.passengerStart >= 0 ? sections.passengerStart : 0);
    var jEnd = sections.journeyEnd - 1;
    if (jEnd < jStart) jEnd = lines.length - 1;

    var warnings = [];
    var flights = findFlightRefs(lines, jStart, jEnd);
    var seenF = {};
    flights = flights.filter(function (f) {
      var k = f.line + ':' + f.flight;
      if (seenF[k]) return false;
      seenF[k] = true;
      return true;
    });

    var segments = [];
    if (flights.length) {
      var sameLineSeen = false;
      for (var fi = 0; fi < flights.length; fi++) {
        var f = flights[fi];
        var blockStart = f.line;
        var blockEnd = fi + 1 < flights.length ? flights[fi + 1].line - 1 : jEnd;
        if (blockEnd < blockStart) { blockEnd = blockStart; sameLineSeen = true; }

        // widen: include the previous line when it mentions routes and has no flight
        if (blockStart - 1 >= jStart && !findFlightRefs(lines, blockStart - 1, blockStart - 1).length) {
          var prev = lines[blockStart - 1];
          if (/[(]/.test(prev) || /\bto\b|\bfrom\b|\bvia\b|\bdepart|\barriv/i.test(prev)) blockStart = blockStart - 1;
        }
        segments.push(parseSegmentBlock(lines, blockStart, blockEnd, f, flights));
      }
      if (sameLineSeen) warnings.push('Two flight numbers were found on the same line — please double-check the segments.');
    } else {
      segments = chainCityPairs(lines, jStart, jEnd);
      if (!segments.length) warnings.push('No flight segments detected — please add them manually.');
    }

    segments.forEach(function (s) { if (!s.airline) s.airline = brand || ''; });
    return { segments: segments, warnings: warnings };
  }

  function parseSegmentBlock(lines, start, end, flightRef, allFlights) {
    var seg = emptySegment();
    seg.flightNumber = flightRef.flight;
    seg.airline = flightRef.airline || '';

    var flightsInBlock = (allFlights || []).filter(function (f) { return f.line >= start && f.line <= end; });

    /* ---------- airports ---------- */
    var depCity = null, arrCity = null, fromCity = null, toCity = null;
    for (var i = start; i <= end && i < lines.length; i++) {
      var l = collapse(lines[i]);
      if (/^(departure|depart|dep)\b/i.test(l) && !depCity) {
        var dm = findAirportMentions(lines, i, i);
        if (dm.length) depCity = dm[0];
      }
      if (/^(arrival|arrive|arr)\b/i.test(l) && !arrCity) {
        var am = findAirportMentions(lines, i, i);
        if (am.length) arrCity = am[0];
      }
      if (/^(from|origin)\b/i.test(l) && !fromCity) {
        var fm = findAirportMentions(lines, i, i);
        if (fm.length) fromCity = fm[0];
      }
      if (/^(to|destination)\b/i.test(l) && !toCity) {
        var tm2 = findAirportMentions(lines, i, i);
        if (tm2.length) toCity = tm2[0];
      }
    }
    var origin = depCity || fromCity;
    var dest = arrCity || toCity;
    if (origin && dest && origin.code === dest.code) dest = null;

    var mentions = findAirportMentions(lines, start, end);
    if (!origin || !dest) {
      if (mentions.length >= 2) {
        if (!origin) origin = mentions[0];
        if (!dest) dest = mentions[mentions.length - 1];
      } else if (mentions.length === 1) {
        if (!origin) origin = mentions[0];
        else if (!dest) dest = mentions[0];
      }
    }
    if (origin && dest && origin.code === dest.code) dest = null;
    if (origin) seg.from = { city: origin.city || AIRPORTS[origin.code] || origin.code, code: origin.code };
    if (dest) seg.to = { city: dest.city || AIRPORTS[dest.code] || dest.code, code: dest.code };

    /* ---------- times (flight numbers masked out) ---------- */
    var depLabel = null, arrLabel = null, loose = [];
    for (var t = start; t <= end && t < lines.length; t++) {
      var raw = collapse(lines[t]);
      var masked = maskFlights(raw, flightsInBlock);
      if (/^(departure|depart|dep)\b/i.test(raw) || /\bdeparture\b\s*[:\-]/i.test(raw)) {
        var dts = findTimes(masked);
        if (dts.length && !depLabel) depLabel = dts[0];
      }
      if (/^(arrival|arrive|arr)\b/i.test(raw) || /\barrival\b\s*[:\-]/i.test(raw)) {
        var ats = findTimes(masked);
        if (ats.length && !arrLabel) arrLabel = ats[0];
      }
      findTimes(masked).forEach(function (x) { loose.push(x); });
    }
    if (depLabel) seg.departure = displayTime(depLabel.raw);
    if (arrLabel) seg.arrival = displayTime(arrLabel.raw);
    if (!seg.departure && loose.length) seg.departure = displayTime(loose[0].raw);
    if (!seg.arrival && loose.length > 1) seg.arrival = displayTime(loose[1].raw);

    /* ---------- date ---------- */
    for (var d = start; d <= end && d < lines.length; d++) {
      var dd = findDateOnLine(lines[d]);
      if (dd) { seg.date = dd.text; break; }
    }

    /* ---------- duration ---------- */
    for (var u = start; u <= end && u < lines.length; u++) {
      if (/duration|flight\s*time|journey\s*time|elapsed/i.test(lines[u])) {
        var du = parseDurationStr(lines[u]);
        if (du) { seg.duration = formatDuration(du); seg.durationMin = du.h * 60 + du.m; break; }
      }
    }
    if (!seg.duration && seg.departure && seg.arrival) {
      var mins = minutesBetween(parseTime(seg.departure), parseTime(seg.arrival));
      if (mins != null) {
        seg.duration = formatDuration({ h: Math.floor(mins / 60), m: mins % 60 });
        seg.durationMin = mins;
      }
    }

    /* ---------- cabin / class / seat / terminal / gate ---------- */
    var blockText = lines.slice(start, end + 1).join(' ');
    var cb = blockText.match(/\b(economy|business|first|premium\s+economy)\b/i);
    if (cb) seg.cabin = cb[1].replace(/\s+/g, ' ').replace(/^./, function (c) { return c.toUpperCase(); });
    var bc = extractBookingClass(blockText);
    if (bc) seg.bookingClass = bc;
    var st = blockText.match(/\bseat\s*[:\-]?\s*(\d{1,2}\s*[A-K])\b/i);
    if (st) seg.seat = st[1].toUpperCase().replace(/\s+/g, '');
    var tm = blockText.match(/\bterminal\s*[:\-]?\s*(\d{1,2})\b/i);
    if (tm) seg.terminal = tm[1];
    var gt = blockText.match(/\bgate\s*[:\-]?\s*([A-Z]{0,2}\d{1,2})\b/i);
    if (gt) seg.gate = gt[1].toUpperCase();

    return seg;
  }

  /* Fallback (no flight numbers): pair consecutive city mentions */
  function chainCityPairs(lines, start, end) {
    var mentions = findAirportMentions(lines, start, end);
    var seq = [];
    for (var i = 0; i < mentions.length; i++) {
      var prev = seq[seq.length - 1];
      if (prev && prev.code === mentions[i].code) continue;
      seq.push(mentions[i]);
    }
    var times = [];
    for (var l = start; l <= end && l < lines.length; l++) {
      findTimes(lines[l]).forEach(function (x) { times.push(x); });
    }
    var segs = [];
    var nPairs = Math.max(0, seq.length - 1);
    if (nPairs >= 1 && times.length >= nPairs * 2) {
      for (var p = 0; p < nPairs; p++) {
        var s = emptySegment();
        s.from = { city: seq[p].city || AIRPORTS[seq[p].code], code: seq[p].code };
        s.to = { city: seq[p + 1].city || AIRPORTS[seq[p + 1].code], code: seq[p + 1].code };
        s.departure = displayTime(times[p * 2].raw);
        s.arrival = displayTime(times[p * 2 + 1].raw);
        var mins = minutesBetween(parseTime(s.departure), parseTime(s.arrival));
        if (mins != null) { s.duration = formatDuration({ h: Math.floor(mins / 60), m: mins % 60 }); s.durationMin = mins; }
        var dd = findDateOnLine(lines[Math.min(seq[p].line, lines.length - 1)]);
        if (dd) s.date = dd.text;
        segs.push(s);
      }
    }
    return segs;
  }

  /* ===================================================================
   * 11. TRANSIT + TRIP TYPE
   * =================================================================== */

  function computeTransitAndType(segments, lines) {
    var transits = [];
    for (var i = 0; i + 1 < segments.length; i++) {
      var a = segments[i], b = segments[i + 1];
      var connected = !!(a.to.code && b.from.code && a.to.code === b.from.code);
      var mins = null;

      if (connected && a.arrival && b.departure) {
        // same-day / next-day transit only; a later return flight is not a transit
        var da = dateToUtc(parseDateStr(a.date));
        var db = dateToUtc(parseDateStr(b.date));
        var daysApart = (da != null && db != null) ? Math.round((db - da) / 86400000) : 0;
        if (daysApart >= 0 && daysApart <= 1) {
          mins = minutesBetween(parseTime(a.arrival), parseTime(b.departure));
        } else {
          connected = false; // separate journey legs (e.g. return flight)
        }
      }
      if (connected && mins == null && lines) {
        for (var l = 0; l < lines.length; l++) {
          if (/transit|layover|stopover|connection/i.test(lines[l])) {
            var du = parseDurationStr(lines[l]);
            if (du && a.to.code && lines[l].toUpperCase().indexOf(a.to.code) >= 0) {
              mins = du.h * 60 + du.m; break;
            }
          }
        }
      }
      transits.push({
        at: a.to.city || a.to.code || '',
        code: a.to.code || '',
        connected: connected,
        duration: mins != null ? formatDuration({ h: Math.floor(mins / 60), m: mins % 60 }) : '',
        durationMin: mins
      });
    }

    var tripType;
    if (segments.length <= 1) tripType = 'One Way';
    else {
      var first = segments[0].from.code;
      var lastDest = segments[segments.length - 1].to.code;
      if (first && lastDest && first === lastDest) tripType = 'Round Trip';
      else if (transits.length && transits.every(function (t) { return t.connected; })) tripType = 'Journey with Transit';
      else tripType = 'Multi-city';
    }
    return { transits: transits, tripType: tripType };
  }

  /* ===================================================================
   * 12. TICKET INFORMATION
   * =================================================================== */

  function extractTicketInfo(lines) {
    var info = {
      ticketNumber: '', fareBasis: '', baggage: '', cabin: '', bookingClass: '',
      seat: '', terminal: '', gate: '', operatingCarrier: '', aircraft: ''
    };

    var tn = fieldByLabel(lines, [
      /(?:e-?ticket|ticket)\s*(?:no\.?|number|#|id)?\s*[:\-]\s*\**\s*(\d{3}[-\s]?\d{7,11})\b/i,
      /\b(?:etkt|e-?ticket)\s*(?:no\.?)?\s*[:\-]?\s*(\d{3}[-\s]?\d{7,11})\b/i,
      /\b(\d{3}-\d{10})\b/
    ], { validate: function (v) { var n = v.replace(/\D/g, '').length; return n >= 10 && n <= 13; } });
    if (tn) info.ticketNumber = tn.value;

    var fb = fieldByLabel(lines, [/fare\s*basis\s*[:\-]?\s*([A-Z0-9]{1,15})\b/i]);
    if (fb) info.fareBasis = fb.value;

    var bg = fieldByLabel(lines, [
      /baggage(?:\s*(?:allowance|info(?:rmation)?))?\s*[:\-]\s*\**\s*(\d{1,2}\s*(?:pc|pcs|kg|kgs|pieces?|lbs?)?)\b/i,
      /checked\s*(?:baggage|bags?)\s*[:\-]\s*\**\s*(\d{1,2}\s*(?:pc|pcs|kg|kgs)?)\b/i,
      /\bbagg(?:age)?\s+(\d{1,2}\s*(?:pc|pcs|kg|kgs))\b/i,
      /\b(\d{1,2}\s*(?:pc|pcs|kg|kgs))\s*(?:checked)?\s*(?:baggage|bags?)\b/i
    ]);
    if (bg) info.baggage = bg.value.toUpperCase().replace('KGS', 'KG').replace('PCS', 'PC').replace('PIECES', 'PC').replace('PIECE', 'PC');

    var cb = fieldByLabel(lines, [
      /(?:cabin|class\s*of\s*service)\s*[:\-]\s*(economy|business|first|premium\s+economy)/i,
      /\bclass\s*[:\-]\s*(economy|business|first|premium\s+economy)\b/i
    ]);
    if (cb) info.cabin = cb.value.replace(/\b\w/g, function (c) { return c.toUpperCase(); });

    var cls = '';
    for (var ci = 0; ci < lines.length && !cls; ci++) cls = extractBookingClass(lines[ci]);
    if (cls) info.bookingClass = cls;

    var st = fieldByLabel(lines, [/seat(?:s)?\s*(?:no\.?|number|#)?\s*[:\-]\s*(\d{1,3}\s*[A-K])\b/i]);
    if (st) info.seat = st.value.toUpperCase().replace(/\s+/g, '');

    var tm = fieldByLabel(lines, [/terminal\s*[:\-]\s*(T?\s*\d{1,2})\b/i]);
    if (tm) info.terminal = tm.value.toUpperCase().replace(/\s+/g, '');

    var gt = fieldByLabel(lines, [/gate\s*[:\-]\s*([A-Z]{0,2}\d{1,2})\b/i]);
    if (gt) info.gate = gt.value.toUpperCase();

    var oc = fieldByLabel(lines, [/operating\s*(?:carrier|airline)\s*[:\-]\s*([A-Za-z .\-&]{2,30})/i]);
    if (oc) info.operatingCarrier = prettifyName(oc.value);

    var ac = fieldByLabel(lines, [/(?:aircraft|equipment|plane)\s*(?:type)?\s*[:\-]\s*([A-Za-z0-9\-\/ ]{2,25})/i]);
    if (ac) info.aircraft = collapse(ac.value).toUpperCase();

    return info;
  }

  /* ===================================================================
   * 13. PUBLIC API
   * =================================================================== */

  function parse(rawText) {
    var text = String(rawText || '');
    var lines = text.split(/\r?\n/).map(collapse).filter(function (l) { return l.length > 0; });
    var warnings = [];

    if (!lines.length || text.replace(/\s/g, '').length < 20) {
      return {
        ok: false, error: 'empty',
        message: 'No readable text found. The PDF may be scanned (image-only) — please paste the ticket text manually.',
        lines: lines, segments: [], transits: [], warnings: []
      };
    }

    var sections = mapSections(lines);
    var airline = detectAirline(lines);

    var pass = extractPassengers(lines, sections);
    var passengerName = pass.length ? pass[0].name : '';
    if (!passengerName) warnings.push('Passenger name could not be detected automatically — please type it in.');
    if (pass.length > 1) {
      warnings.push(pass.length + ' passenger names found; the first was selected. Others: ' +
        pass.slice(1).map(function (p) { return p.name; }).join(', '));
    }

    var passport = extractPassport(lines);
    var nationality = extractNationality(lines);
    var pnr = extractPNR(lines);
    if (!passport) warnings.push('Passport number not found — the field is left empty.');
    if (!pnr) warnings.push('Airline Reference (PNR) not found — please check manually.');

    var journey = extractSegments(lines, sections, airline);
    var segments = journey.segments;
    warnings = warnings.concat(journey.warnings);

    if (!airline && segments.length) {
      var withAirline = segments.filter(function (s) { return s.airline; });
      if (withAirline.length) airline = withAirline[0].airline;
    }
    segments.forEach(function (s) { if (!s.airline) s.airline = airline || ''; });

    var tt = computeTransitAndType(segments, lines);
    var ticketInfo = extractTicketInfo(lines);

    segments.forEach(function (s) {
      if (!s.cabin && ticketInfo.cabin) s.cabin = ticketInfo.cabin;
      if (!s.bookingClass && ticketInfo.bookingClass) s.bookingClass = ticketInfo.bookingClass;
      if (!s.seat && ticketInfo.seat) s.seat = ticketInfo.seat;
      if (!s.terminal && ticketInfo.terminal) s.terminal = ticketInfo.terminal;
    });

    var found = {
      passengerName: !!passengerName,
      passport: !!passport,
      nationality: !!nationality,
      airline: !!airline,
      pnr: !!pnr,
      segments: segments.length > 0,
      dates: segments.length > 0 && segments.every(function (s) { return !!s.date; }),
      times: segments.length > 0 && segments.every(function (s) { return !!s.departure; })
    };
    var foundCount = Object.keys(found).filter(function (k) { return found[k]; }).length;
    var confidence = Math.round((foundCount / 8) * 100);

    return {
      ok: true,
      airline: airline || '',
      passengerName: passengerName,
      additionalPassengers: pass.slice(1).map(function (p) { return p.name; }),
      passport: passport,
      nationality: nationality,
      pnr: pnr,
      ticket: ticketInfo,
      segments: segments,
      transits: tt.transits,
      tripType: tt.tripType,
      found: found,
      confidence: confidence,
      warnings: warnings,
      sections: sections,
      lines: lines
    };
  }

  /* ---- PDF.js item → text-lines helper (used by the app) ----
   * items: [{str, transform:[a,b,c,d,e,f], width}, ...]
   * Groups glyphs sharing the same baseline (y within tolerance). */
  function linesFromPdfItems(items, tolerance) {
    tolerance = tolerance || 2.5;
    var rows = [];
    items.forEach(function (it) {
      if (!it.str || !it.str.trim()) return;
      var y = it.transform[5];
      var x = it.transform[4];
      var placed = false;
      for (var i = 0; i < rows.length; i++) {
        if (Math.abs(rows[i].y - y) <= tolerance) {
          rows[i].parts.push({ x: x, s: it.str, w: it.width || 0 });
          rows[i].y = (rows[i].y + y) / 2;
          placed = true; break;
        }
      }
      if (!placed) rows.push({ y: y, parts: [{ x: x, s: it.str, w: it.width || 0 }] });
    });
    rows.sort(function (a, b) { return b.y - a.y; });
    return rows.map(function (r) {
      r.parts.sort(function (a, b) { return a.x - b.x; });
      var out = '';
      r.parts.forEach(function (p, i) {
        if (i > 0) {
          var prev = r.parts[i - 1];
          var gap = p.x - (prev.x + (prev.w || 0));
          if (gap > 6) out += gap > 40 ? '   ' : ' ';
        }
        out += p.s;
      });
      return collapse(out);
    }).filter(function (l) { return l.length > 0; });
  }

  return {
    parse: parse,
    linesFromPdfItems: linesFromPdfItems,
    computeTransitAndType: computeTransitAndType,
    _internals: {
      parseTime: parseTime, displayTime: displayTime, parseDateStr: parseDateStr,
      parseDurationStr: parseDurationStr, formatDuration: formatDuration,
      normalizeName: normalizeName, looksLikeName: looksLikeName, hasAgentContext: hasAgentContext,
      mapSections: mapSections, findAirportMentions: findAirportMentions,
      findFlightRefs: findFlightRefs, findTimes: findTimes, extractBookingClass: extractBookingClass
    }
  };
});
