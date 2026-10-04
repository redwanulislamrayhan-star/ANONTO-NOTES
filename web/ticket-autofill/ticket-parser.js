/*!
 * ticket-parser.js — e-Ticket / Boarding-pass text -> structured fields
 * ---------------------------------------------------------------------
 * Problem this solves:
 *   OCR/PDF text used to be dumped into ONE big "Imported Ticket Information"
 *   textarea, so values leaked into the wrong inputs (e.g. "SAR 2390" landing
 *   in Travel Date, flight numbers never split into SV804 / SV807, routes
 *   never separated).
 *
 * This module parses the raw text with labels + patterns and returns one value
 * per field, with a hard rule: money NEVER becomes a date/flight/time, and a
 * date NEVER becomes money.
 *
 * Works in the browser (window.TicketParser) and in Node (module.exports).
 */
(function (root, factory) {
  var api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.TicketParser = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  /* ------------------------------------------------------------------ *
   * Dictionaries
   * ------------------------------------------------------------------ */

  var MONTHS = {
    JAN: 1, JANUARY: 1, FEB: 2, FEBRUARY: 2, MAR: 3, MARCH: 3, APR: 4, APRIL: 4,
    MAY: 5, JUN: 6, JUNE: 6, JUL: 7, JULY: 7, AUG: 8, AUGUST: 8,
    SEP: 9, SEPT: 9, SEPTEMBER: 9, OCT: 10, OCTOBER: 10, NOV: 11, NOVEMBER: 11,
    DEC: 12, DECEMBER: 12
  };
  var MONTH_NAMES = ['', 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  // Currency codes / symbols. Anything tied to these is money — never a flight.
  var CURRENCY_CODES = {
    SAR: 'SAR', SR: 'SAR', SRL: 'SAR', BDT: 'BDT', TK: 'BDT', TAKA: 'BDT',
    USD: 'USD', EUR: 'EUR', GBP: 'GBP', AED: 'AED', QAR: 'QAR', KWD: 'KWD',
    OMR: 'OMR', BHD: 'BHD', INR: 'INR', PKR: 'PKR', NPR: 'NPR', LKR: 'LKR',
    MYR: 'MYR', SGD: 'SGD', THB: 'THB', JPY: 'JPY', CNY: 'CNY', TRY: 'TRY',
    CAD: 'CAD', AUD: 'AUD', EGP: 'EGP', JOD: 'JOD'
  };
  var CURRENCY_SYMBOLS = { '৳': 'BDT', '₹': 'INR', '$': 'USD', '€': 'EUR', '£': 'GBP', '﷼': 'SAR', '¥': 'JPY' };

  var AIRLINES = {
    SV: 'Saudia (Saudi Arabian Airlines)', XY: 'flynas', F3: 'flyadeal',
    BG: 'Biman Bangladesh Airlines', BS: 'US-Bangla Airlines', VQ: 'Novoair',
    EK: 'Emirates', FZ: 'flydubai', EY: 'Etihad Airways', QR: 'Qatar Airways',
    GF: 'Gulf Air', WY: 'Oman Air', KU: 'Kuwait Airways', G9: 'Air Arabia',
    J9: 'Jazeera Airways', MS: 'EgyptAir', RJ: 'Royal Jordanian',
    TK: 'Turkish Airlines', PK: 'Pakistan International Airlines',
    AI: 'Air India', '6E': 'IndiGo', UK: 'Vistara', UL: 'SriLankan Airlines',
    TG: 'Thai Airways', MH: 'Malaysia Airlines', SQ: 'Singapore Airlines',
    CX: 'Cathay Pacific', BA: 'British Airways', LH: 'Lufthansa',
    KL: 'KLM', AF: 'Air France', QF: 'Qantas', ET: 'Ethiopian Airlines',
    WB: 'RwandAir', KQ: 'Kenya Airways', OV: 'Salam Air', NE: 'Nesma Airlines'
  };

  // Airport code -> city (used to pretty-print routes when OCR only gives codes)
  var AIRPORTS = {
    DAC: 'Dhaka', CGP: 'Chattogram', ZYL: 'Sylhet', JSR: 'Jashore', CXB: "Cox's Bazar",
    ELQ: 'Buraydah', RUH: 'Riyadh', JED: 'Jeddah', DMM: 'Dammam', MED: 'Madinah',
    TIF: 'Taif', AHB: 'Abha', TUU: 'Tabuk', HAS: "Ha'il", GIZ: 'Jazan', YNB: 'Yanbu',
    DXB: 'Dubai', SHJ: 'Sharjah', AUH: 'Abu Dhabi', DOH: 'Doha', KWI: 'Kuwait',
    BAH: 'Bahrain', MCT: 'Muscat', SLL: 'Salalah', AMM: 'Amman', CAI: 'Cairo',
    IST: 'Istanbul', SAW: 'Istanbul Sabiha', KUL: 'Kuala Lumpur', SIN: 'Singapore',
    BKK: 'Bangkok', DMK: 'Bangkok Don Mueang', HKG: 'Hong Kong', ICN: 'Seoul',
    NRT: 'Tokyo Narita', PEK: 'Beijing', PVG: 'Shanghai', CAN: 'Guangzhou',
    KTM: 'Kathmandu', CMB: 'Colombo', MLE: 'Maldives', DEL: 'Delhi', BOM: 'Mumbai',
    CCU: 'Kolkata', MAA: 'Chennai', BLR: 'Bengaluru', HYD: 'Hyderabad',
    KHI: 'Karachi', LHE: 'Lahore', ISB: 'Islamabad', LHR: 'London Heathrow',
    LGW: 'London Gatwick', MAN: 'Manchester', CDG: 'Paris', FRA: 'Frankfurt',
    AMS: 'Amsterdam', FCO: 'Rome', MXP: 'Milan', MAD: 'Madrid', BCN: 'Barcelona',
    JFK: 'New York', EWR: 'Newark', IAD: 'Washington', ORD: 'Chicago',
    LAX: 'Los Angeles', YYZ: 'Toronto', YUL: 'Montreal', SYD: 'Sydney',
    MEL: 'Melbourne', RGN: 'Yangon', ADD: 'Addis Ababa', NBO: 'Nairobi'
  };

  var NATIONALITY = {
    BANGLADESH: 'Bangladeshi', BANGLADESHI: 'Bangladeshi', BGD: 'Bangladeshi', BD: 'Bangladeshi',
    'SAUDI ARABIA': 'Saudi', SAUDI: 'Saudi', KSA: 'Saudi', SAU: 'Saudi',
    INDIA: 'Indian', INDIAN: 'Indian', IND: 'Indian',
    PAKISTAN: 'Pakistani', PAKISTANI: 'Pakistani', PAK: 'Pakistani',
    NEPAL: 'Nepali', NEPALI: 'Nepali', NEPALESE: 'Nepali',
    'SRI LANKA': 'Sri Lankan', 'SRI LANKAN': 'Sri Lankan',
    PHILIPPINES: 'Filipino', FILIPINO: 'Filipino',
    EGYPT: 'Egyptian', EGYPTIAN: 'Egyptian',
    INDONESIA: 'Indonesian', MALAYSIA: 'Malaysian', MYANMAR: 'Myanmar',
    'UNITED KINGDOM': 'British', BRITISH: 'British',
    'UNITED STATES': 'American', AMERICAN: 'American'
  };

  var TITLES = ['MR', 'MRS', 'MS', 'MISS', 'MSTR', 'MASTER', 'DR', 'PROF', 'INF', 'CHD'];

  var STATUS_MAP = {
    OK: 'Confirmed', HK: 'Confirmed', CONFIRMED: 'Confirmed', CONFIRM: 'Confirmed',
    RR: 'Reconfirmed', TK: 'Confirmed', HL: 'Waitlisted', WL: 'Waitlisted',
    WAITLIST: 'Waitlisted', WAITLISTED: 'Waitlisted', RQ: 'On Request',
    PENDING: 'Pending', XX: 'Cancelled', CANCELLED: 'Cancelled', CANCELED: 'Cancelled',
    FLOWN: 'Flown', USED: 'Used', OPEN: 'Open'
  };

  var BENGALI_DIGITS = '০১২৩৪৫৬৭৮৯';

  /* ------------------------------------------------------------------ *
   * Small helpers
   * ------------------------------------------------------------------ */

  function toAsciiDigits(s) {
    return String(s).replace(/[০-৯]/g, function (d) { return String(BENGALI_DIGITS.indexOf(d)); });
  }

  function normalizeText(raw) {
    return toAsciiDigits(String(raw || ''))
      .replace(/\r\n?/g, '\n')
      .replace(/[\u00a0\u2007\u202f]/g, ' ')          // nbsp family
      .replace(/[\u2010-\u2015\u2212]/g, '-')          // dashes
      .replace(/[\u2018\u2019]/g, "'")
      .replace(/[\u201c\u201d]/g, '"')
      .replace(/[\u2192\u21d2\u27a1\u2794]/g, '->')    // arrows
      .replace(/[ \t]+/g, ' ')
      .replace(/[ ]*\n[ ]*/g, '\n')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
  }

  function toLines(text) {
    return text.split('\n').map(function (l) { return l.trim(); }).filter(function (l) { return l.length > 0; });
  }

  function titleCase(s) {
    return String(s).toLowerCase().replace(/\b([a-z])/g, function (m, c) { return c.toUpperCase(); });
  }

  function pad2(n) { return (n < 10 ? '0' : '') + n; }

  function isLabelLine(line) {
    return /^[A-Za-z][A-Za-z .\/#&()'-]{1,38}\s*:/.test(line);
  }

  var INLINE_LABELS = '(?:TKT|E-?TICKET|TICKET(?:\\s*(?:NO|NUMBER))?|PNR|PASSPORT(?:\\s*(?:NO|NUMBER))?|SEAT|CLASS|CABIN|STATUS|DATE|TIME|DEP|ARR|FROM|TO|FLIGHT(?:\\s*NO)?|NATIONALITY|BAGGAGE|FARE|TOTAL|GATE|TERMINAL|BOOKING\\s*[A-Z]*)';
  var INLINE_LABEL_RE = new RegExp('\\s+' + INLINE_LABELS + '\\s*[:#]\\s*.*$', 'i');

  /**
   * Reads "Label : value". The value may sit on the next line, and a second
   * label on the same line is cut off. `opts.test` rejects a bogus hit (e.g.
   * the word "TICKET" inside the banner "ELECTRONIC TICKET RECEIPT") and makes
   * the scan continue to the next candidate line.
   */
  function grabLabel(lines, labelRe, opts) {
    opts = opts || {};
    var accept = opts.test || function (v) { return /[A-Za-z0-9]/.test(v); };
    for (var i = 0; i < lines.length; i++) {
      var m = lines[i].match(labelRe);
      if (!m) continue;
      var rest = lines[i].slice(m.index + m[0].length).replace(/^\s*[:：#\-–—]?\s*/, '').trim();
      if (opts.stopAtNextLabel !== false) rest = rest.replace(INLINE_LABEL_RE, '').trim();
      if (rest && accept(rest)) return { value: rest, line: i, raw: lines[i] };
      if (!rest && opts.allowNextLine !== false && lines[i + 1] && !isLabelLine(lines[i + 1])) {
        var next = lines[i + 1].trim();
        if (accept(next)) return { value: next, line: i + 1, raw: next };
      }
    }
    return null;
  }

  /* ------------------------------------------------------------------ *
   * Money (detected FIRST so its text can be masked out everywhere else)
   * ------------------------------------------------------------------ */

  var AMOUNT = '(\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?|\\d+(?:\\.\\d{1,2})?)';
  var CODE_LIST = Object.keys(CURRENCY_CODES).join('|');
  var MONEY_RE_CODE_FIRST = new RegExp('\\b(' + CODE_LIST + ')\\s*\\.?\\s*' + AMOUNT, 'g');
  var MONEY_RE_CODE_LAST = new RegExp('\\b' + AMOUNT + '\\s*(' + CODE_LIST + ')\\b', 'g');
  var MONEY_RE_SYMBOL = new RegExp('([৳₹$€£﷼¥])\\s*' + AMOUNT, 'g');

  function parseAmount(s) { return parseFloat(String(s).replace(/,/g, '')); }

  function formatAmount(n) {
    if (n == null || isNaN(n)) return '';
    var fixed = Math.round(n * 100) / 100;
    var s = (fixed % 1 === 0) ? String(fixed) : fixed.toFixed(2);
    var parts = s.split('.');
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    return parts.join('.');
  }

  /** Every money mention in the text, with its character span. */
  function findMoney(text) {
    var out = [], m;
    MONEY_RE_CODE_FIRST.lastIndex = 0;
    while ((m = MONEY_RE_CODE_FIRST.exec(text))) {
      out.push({ currency: CURRENCY_CODES[m[1].toUpperCase()], amount: parseAmount(m[2]), start: m.index, end: m.index + m[0].length, text: m[0], priority: 2 });
    }
    MONEY_RE_CODE_LAST.lastIndex = 0;
    while ((m = MONEY_RE_CODE_LAST.exec(text))) {
      out.push({ currency: CURRENCY_CODES[m[2].toUpperCase()], amount: parseAmount(m[1]), start: m.index, end: m.index + m[0].length, text: m[0], priority: 1 });
    }
    MONEY_RE_SYMBOL.lastIndex = 0;
    while ((m = MONEY_RE_SYMBOL.exec(text))) {
      out.push({ currency: CURRENCY_SYMBOLS[m[1]], amount: parseAmount(m[2]), start: m.index, end: m.index + m[0].length, text: m[0], priority: 2 });
    }
    // Resolve overlaps by priority: "SAR 2390" wins over the bogus "00 SAR".
    out.sort(function (a, b) { return (b.priority - a.priority) || (a.start - b.start); });
    var kept = [];
    out.forEach(function (x) {
      var overlap = kept.some(function (k) { return x.start < k.end && k.start < x.end; });
      if (!overlap) kept.push(x);
    });
    kept.sort(function (a, b) { return a.start - b.start; });
    return kept;
  }

  /**
   * Replaces every money span with spaces, so later date/flight/time passes can
   * never see "SAR 2390". This is the core fix for the reported bug.
   */
  function maskMoney(text, money) {
    var chars = text.split('');
    money.forEach(function (mm) {
      for (var i = mm.start; i < mm.end; i++) if (chars[i] !== '\n') chars[i] = ' ';
    });
    return chars.join('');
  }

  function pickFare(moneyList, text) {
    if (!moneyList.length) return {};
    var lineStarts = [];
    text.split('\n').reduce(function (pos, l) { lineStarts.push(pos); return pos + l.length + 1; }, 0);
    function lineOf(idx) {
      var li = 0;
      for (var i = 0; i < lineStarts.length; i++) if (lineStarts[i] <= idx) li = i;
      return text.split('\n')[li] || '';
    }
    var scored = moneyList.map(function (m) {
      var ctx = lineOf(m.start).toUpperCase();
      var score = 0;
      if (/GRAND\s*TOTAL|TOTAL\s*(AMOUNT|FARE|PRICE|PAID)|AMOUNT\s*PAID|TOTAL\b/.test(ctx)) score += 60;
      if (/\bFARE\b/.test(ctx)) score += 20;
      if (/\bTAX|SURCHARGE|YQ|YR\b/.test(ctx)) score -= 25;
      if (/BASE|FARE BASIS/.test(ctx)) score -= 5;
      score += Math.min(m.amount, 100000) / 100000;    // bigger total wins ties
      return { m: m, score: score, ctx: ctx };
    });
    scored.sort(function (a, b) { return b.score - a.score; });
    var best = scored[0].m;

    var base = null, tax = null;
    scored.forEach(function (s) {
      if (base == null && /BASE|\bFARE\b/.test(s.ctx) && !/TOTAL/.test(s.ctx)) base = s.m;
      if (tax == null && /TAX|SURCHARGE|\bYQ\b|\bYR\b/.test(s.ctx)) tax = s.m;
    });

    return {
      currency: best.currency,
      totalAmount: best.amount,
      totalAmountText: best.currency + ' ' + formatAmount(best.amount),
      baseFare: base ? base.currency + ' ' + formatAmount(base.amount) : '',
      taxes: tax ? tax.currency + ' ' + formatAmount(tax.amount) : ''
    };
  }

  /* ------------------------------------------------------------------ *
   * Dates
   * ------------------------------------------------------------------ */

  var MONTH_WORD = Object.keys(MONTHS).sort(function (a, b) { return b.length - a.length; }).join('|');
  var DATE_PATTERNS = [
    // 15 Oct 2026 | 15-OCT-2026 | 15OCT26 | 15 October 2026
    { re: new RegExp('\\b(\\d{1,2})\\s*[-/ ]?\\s*(' + MONTH_WORD + ')\\s*[-/, ]?\\s*(\\d{4}|\\d{2})\\b', 'gi'), map: function (m) { return { d: +m[1], mo: MONTHS[m[2].toUpperCase()], y: +m[3] }; } },
    // Oct 15, 2026 | OCTOBER 15 2026
    { re: new RegExp('\\b(' + MONTH_WORD + ')\\s*[-/ ]\\s*(\\d{1,2})\\s*[-/, ]+\\s*(\\d{4}|\\d{2})\\b', 'gi'), map: function (m) { return { d: +m[2], mo: MONTHS[m[1].toUpperCase()], y: +m[3] }; } },
    // 2026-10-15
    { re: /\b(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\b/g, map: function (m) { return { d: +m[3], mo: +m[2], y: +m[1] }; } },
    // 15/10/2026 | 15.10.2026 (day-first, auto-swapped when impossible)
    { re: /\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})\b/g, map: function (m) {
        var a = +m[1], b = +m[2];
        if (a > 12 && b <= 12) return { d: a, mo: b, y: +m[3] };
        if (b > 12 && a <= 12) return { d: b, mo: a, y: +m[3] };
        return { d: a, mo: b, y: +m[3] };
      } },
    // 15OCT (no year) — year inferred later
    { re: new RegExp('\\b(\\d{1,2})\\s?(' + MONTH_WORD + ')\\b(?!\\s*\\d)', 'gi'), map: function (m) { return { d: +m[1], mo: MONTHS[m[2].toUpperCase()], y: null }; } }
  ];

  function buildDate(parts, fallbackYear) {
    if (!parts || !parts.mo || !parts.d) return null;
    if (parts.mo < 1 || parts.mo > 12 || parts.d < 1 || parts.d > 31) return null;
    var y = parts.y;
    if (y == null) y = fallbackYear || new Date().getFullYear();
    else if (y < 100) y += (y >= 70 ? 1900 : 2000);
    if (y < 1900 || y > 2100) return null;
    return {
      day: parts.d, month: parts.mo, year: y,
      text: pad2(parts.d) + ' ' + MONTH_NAMES[parts.mo] + ' ' + y,
      iso: y + '-' + pad2(parts.mo) + '-' + pad2(parts.d)
    };
  }

  /** All dates inside a (money-masked) string, in order of appearance. */
  function findDates(str, fallbackYear) {
    var hits = [];
    DATE_PATTERNS.forEach(function (p, pi) {
      p.re.lastIndex = 0;
      var m;
      while ((m = p.re.exec(str))) {
        var d = buildDate(p.map(m), fallbackYear);
        if (d) hits.push({ date: d, start: m.index, end: m.index + m[0].length, rank: pi });
      }
    });
    hits.sort(function (a, b) { return a.start - b.start || a.rank - b.rank; });
    var kept = [];
    hits.forEach(function (h) {
      if (!kept.some(function (k) { return h.start < k.end && k.start < h.end; })) kept.push(h);
    });
    return kept;
  }

  function firstDate(str, fallbackYear) {
    var d = findDates(str, fallbackYear);
    return d.length ? d[0].date : null;
  }

  /* ------------------------------------------------------------------ *
   * Times
   * ------------------------------------------------------------------ */

  var TIME_RE = /\b([01]?\d|2[0-3])\s*[:.]\s*([0-5]\d)\s*(AM|PM|A\.M\.|P\.M\.)?\b/gi;
  var TIME_HHMM_RE = /\b([01]\d|2[0-3])([0-5]\d)\s*(?:HRS|HOURS|H)\b/gi;
  var TIME_BARE_RE = /\b([01]\d|2[0-3])([0-5]\d)\b/g;

  function overlaps(list, m) {
    var s = m.index, e = m.index + m[0].length;
    return list.some(function (o) { return s < o.end && o.start < e; });
  }

  function findTimes(str, allowBare) {
    var out = [], m;
    TIME_RE.lastIndex = 0;
    while ((m = TIME_RE.exec(str))) {
      var h = +m[1], mi = +m[2], ap = (m[3] || '').toUpperCase().replace(/\./g, '');
      if (ap === 'PM' && h < 12) h += 12;
      if (ap === 'AM' && h === 12) h = 0;
      out.push({ text: pad2(h) + ':' + pad2(mi), start: m.index, end: m.index + m[0].length });
    }
    TIME_HHMM_RE.lastIndex = 0;
    while ((m = TIME_HHMM_RE.exec(str))) {
      if (!overlaps(out, m)) out.push({ text: m[1] + ':' + m[2], start: m.index, end: m.index + m[0].length });
    }
    if (allowBare) {
      // Bare GDS times ("0310 0730"). Only safe once dates/flights/money are masked.
      TIME_BARE_RE.lastIndex = 0;
      while ((m = TIME_BARE_RE.exec(str))) {
        if (!overlaps(out, m)) out.push({ text: m[1] + ':' + m[2], start: m.index, end: m.index + m[0].length });
      }
    }
    out.sort(function (a, b) { return a.start - b.start; });
    return out;
  }

  /* ------------------------------------------------------------------ *
   * Flight numbers
   * ------------------------------------------------------------------ */

  // 2-char IATA designator (AA | A1 | 1A) + 1..4 digits, bounded so that
  // "A12345678" (passport) and "SAR 2390" (money) can never match.
  var FLIGHT_RE = /\b([A-Z]{2}|[A-Z]\d|\d[A-Z])\s?-?\s?(\d{1,4})([A-Z]?)\b/g;
  var FLIGHT_BLACKLIST = { NO: 1, ID: 1, PC: 1, KG: 1, PK: 0, ON: 1, AT: 1, TO: 1, OF: 1, BY: 1, IN: 1, PM: 1, AM: 1, PP: 1, VA: 1 };

  function findFlights(str) {
    var out = [], m;
    FLIGHT_RE.lastIndex = 0;
    while ((m = FLIGHT_RE.exec(str))) {
      var code = m[1].toUpperCase(), num = m[2];
      if (FLIGHT_BLACKLIST[code]) continue;
      if (CURRENCY_CODES[code]) continue;                   // TK 2390 on a fare line etc.
      if (num.length < 2 && !AIRLINES[code]) continue;      // too weak
      var before = str.slice(Math.max(0, m.index - 14), m.index).toUpperCase();
      var after = str.slice(m.index + m[0].length, m.index + m[0].length + 6).toUpperCase();
      if (/[৳₹$€£﷼]\s*$/.test(before)) continue;             // money symbol in front
      if (/(TOTAL|FARE|AMOUNT|PRICE|TAX|PAID|CHARGE)\s*:?\s*$/.test(before)) continue;
      if (/(PNR|BOOKING|RESERVATION|LOCATOR|CONFIRMATION|REF(ERENCE)?|SEAT|GATE)[^A-Z0-9]{0,10}$/.test(before)) continue;
      if (/^\s*(KG|KGS|PC|PCS|SAR|BDT|USD|RIYAL|TAKA)\b/.test(after)) continue;
      if (/\d$/.test(before.replace(/\s$/, ''))) continue;   // middle of a long number
      var score = 0;
      if (AIRLINES[code]) score += 50;
      if (/(FLIGHT|FLT|FL\s*NO|CARRIER)[^A-Z0-9]{0,12}$/.test(before)) score += 40;
      if (num.length >= 3) score += 10;
      out.push({ code: code, number: num, text: code + num + (m[3] || ''), start: m.index, end: m.index + m[0].length, score: score });
    }
    return out.filter(function (f) { return f.score > 0; });
  }

  /* ------------------------------------------------------------------ *
   * Airports & routes
   * ------------------------------------------------------------------ */

  var CITY_CODE_RE = /([A-Za-z][A-Za-z .'-]{1,24}?)\s*\(\s*([A-Z]{3})\s*\)/g;
  var BARE_CODE_RE = /\b([A-Z]{3})\b/g;
  var NOT_AIRPORT = { PNR: 1, MRS: 1, MSS: 1, ETK: 1, TKT: 1, REF: 1, SAR: 1, BDT: 1, USD: 1, AED: 1, QAR: 1, INR: 1, PKR: 1, NPR: 1, LKR: 1, SUM: 1, VAT: 1, TAX: 1, NET: 1, PAX: 1, SEQ: 1, GMT: 1, UTC: 1, AIR: 1, FEE: 1, ADT: 1, CHD: 1, INF: 1, ECO: 1, BUS: 1, OWN: 1, NON: 1, THE: 1, AND: 1, FOR: 1, NEW: 1, ONE: 1, TWO: 1, DAY: 1, MON: 1, TUE: 1, WED: 1, THU: 1, FRI: 1, SAT: 1, SUN: 1, JAN: 1, FEB: 1, MAR: 1, APR: 1, JUN: 1, JUL: 1, AUG: 1, SEP: 1, OCT: 1, NOV: 1, DEC: 1 };

  function findAirports(str) {
    var out = [], m, seenSpan = [];
    CITY_CODE_RE.lastIndex = 0;
    while ((m = CITY_CODE_RE.exec(str))) {
      var code = m[2];
      if (NOT_AIRPORT[code]) continue;
      out.push({ code: code, city: titleCase(m[1].trim()), start: m.index, end: m.index + m[0].length });
      seenSpan.push([m.index, m.index + m[0].length]);
    }
    BARE_CODE_RE.lastIndex = 0;
    while ((m = BARE_CODE_RE.exec(str))) {
      var c = m[1];
      if (NOT_AIRPORT[c] || !AIRPORTS[c]) continue;
      var inside = seenSpan.some(function (s) { return m.index >= s[0] && m.index < s[1]; });
      if (inside) continue;
      out.push({ code: c, city: AIRPORTS[c], start: m.index, end: m.index + m[0].length });
    }
    out.sort(function (a, b) { return a.start - b.start; });
    return out;
  }

  function airportLabel(a) {
    if (!a) return '';
    var city = a.city || AIRPORTS[a.code] || '';
    return city ? city + ' (' + a.code + ')' : a.code;
  }

  /* ------------------------------------------------------------------ *
   * Segment extraction
   * ------------------------------------------------------------------ */

  function blankSpans(str, spans) {
    var chars = str.split('');
    spans.forEach(function (sp) {
      for (var i = sp.start; i < sp.end && i < chars.length; i++) if (chars[i] !== '\n') chars[i] = ' ';
    });
    return chars.join('');
  }

  function normalizeStatus(str) {
    var up = (str || '').toUpperCase();
    var m = up.match(/\b(CONFIRMED|CONFIRM|WAITLISTED|WAITLIST|CANCELLED|CANCELED|PENDING|FLOWN|USED|OPEN|OK|HK\d*|RR|HL|WL|RQ|XX)\b/);
    if (!m) return '';
    var key = m[1].replace(/\d+$/, '');
    return STATUS_MAP[key] || titleCase(m[1]);
  }

  function findCabin(str) {
    var up = (str || '').toUpperCase();
    if (/\bFIRST\s*CLASS\b/.test(up)) return 'First';
    if (/\bBUSINESS\b/.test(up)) return 'Business';
    if (/\bPREMIUM\s*ECONOMY\b/.test(up)) return 'Premium Economy';
    if (/\bECONOMY\b|\bECO\b|\bCOACH\b/.test(up)) return 'Economy';
    return '';
  }

  function findBaggage(str) {
    var m = str.match(/\b(\d)\s*[xX*]\s*(\d{2})\s*(KGS?|KILOS?)\b/i);
    if (m) return m[1] + ' x ' + m[2] + ' Kg';
    m = str.match(/\b(\d{2,3})\s*(KGS?|KILOS?)\b/i);
    if (m) return m[1] + ' Kg';
    m = str.match(/\b(\d)\s*(PCS?|PIECES?)\b/i);
    if (m) return m[1] + ' PC';
    return '';
  }

  function findTerminal(str) {
    var m = str.match(/\bTERMINAL\s*[:#]?\s*([A-Z0-9]{1,3})\b/i);
    return m ? m[1].toUpperCase() : '';
  }

  /**
   * Builds one segment per flight number found. The text window for a segment
   * spans from its flight line to just before the next flight line, so values
   * never bleed between SV804 and SV807.
   */
  function extractSegments(maskedText, fallbackYear) {
    var lines = maskedText.split('\n');
    var lineInfo = [], pos = 0;
    lines.forEach(function (l, i) { lineInfo.push({ i: i, text: l, start: pos }); pos += l.length + 1; });

    var flights = findFlights(maskedText);
    if (!flights.length) return [];

    // keep the strongest flight hit per line
    var byLine = {};
    flights.forEach(function (f) {
      var li = 0;
      for (var i = 0; i < lineInfo.length; i++) if (lineInfo[i].start <= f.start) li = i;
      if (!byLine[li] || byLine[li].score < f.score) byLine[li] = f;
    });
    var flightLines = Object.keys(byLine).map(Number).sort(function (a, b) { return a - b; });

    var segments = [];
    flightLines.forEach(function (li, idx) {
      var nextLi = idx + 1 < flightLines.length ? flightLines[idx + 1] : lines.length;
      var from = li;
      // include up to 2 lines above when they clearly belong to this leg
      for (var up = 1; up <= 2; up++) {
        var prev = lines[li - up];
        if (!prev) break;
        if (flightLines.indexOf(li - up) >= 0) break;
        if (/^(DEPART|ARRIV|FROM|TO|ROUTE|SECTOR|OUTBOUND|RETURN|DATE|TIME)\b/i.test(prev)) from = li - up; else break;
      }
      var window = lines.slice(from, Math.min(nextLi, from + 10)).join('\n');

      var f = byLine[li];
      var dates = findDates(window, fallbackYear);
      var airports = findAirports(window);
      // Blank out date + flight spans so bare GDS times ("0310") can be read
      // without mistaking a year ("2026") or a flight number for a clock time.
      var timeSrc = blankSpans(window, dates.concat(findFlights(window)));
      var times = findTimes(timeSrc, true);

      var dep = airports[0] || null;
      var arr = airports.length > 1 ? airports[1] : null;
      // explicit "From:"/"To:" labels win over positional guesses
      var wl = window.split('\n');
      wl.forEach(function (l) {
        var mf = l.match(/\b(FROM|DEPARTURE|DEPART(?:URE)?\s*(?:CITY|AIRPORT)?|ORIGIN)\b\s*[:\-]?\s*(.+)$/i);
        if (mf) { var a = findAirports(mf[2]); if (a.length) dep = a[0]; }
        var mt = l.match(/\b(TO|ARRIVAL|ARRIVE|DESTINATION)\b\s*[:\-]?\s*(.+)$/i);
        if (mt) { var b = findAirports(mt[2]); if (b.length) arr = b[0]; }
      });

      segments.push({
        index: segments.length + 1,
        flightNo: f.text,
        airline: AIRLINES[f.code] || '',
        airlineCode: f.code,
        date: dates[0] ? dates[0].date.text : '',
        dateIso: dates[0] ? dates[0].date.iso : '',
        departureTime: times[0] ? times[0].text : '',
        arrivalTime: times[1] ? times[1].text : '',
        arrivalDate: dates[1] ? dates[1].date.text : '',
        status: normalizeStatus(window) || 'Confirmed',
        cabin: findCabin(window),
        from: dep ? airportLabel(dep) : '',
        fromCode: dep ? dep.code : '',
        to: arr ? airportLabel(arr) : '',
        toCode: arr ? arr.code : '',
        route: dep && arr ? airportLabel(dep) + ' → ' + airportLabel(arr) : '',
        baggage: findBaggage(window),
        terminal: findTerminal(window),
        rawWindow: window
      });
    });

    // Fill missing years: assume chronological order across legs
    for (var i = 1; i < segments.length; i++) {
      if (!segments[i].date && segments[i - 1].date) segments[i].date = '';
    }
    return segments;
  }

  /* ------------------------------------------------------------------ *
   * Identity fields
   * ------------------------------------------------------------------ */

  function cleanTitle(t) {
    var up = (t || '').toUpperCase().replace(/\./g, '');
    if (up === 'MASTER') up = 'MSTR';
    return up ? up + '.' : '';
  }

  function normalizeName(raw) {
    if (!raw) return '';
    var s = raw.replace(/\s+/g, ' ').trim().replace(/[.,;]+$/, '');
    s = s.replace(/\b(ADT|CHD|INF|ADULT|CHILD|INFANT)\b/gi, '').trim();
    var title = '';
    var titleRe = new RegExp('\\b(' + TITLES.join('|') + ')\\b\\.?', 'i');
    var tm = s.match(titleRe);
    if (tm) { title = cleanTitle(tm[1]); s = s.replace(titleRe, ' ').trim(); }
    s = s.replace(/\s+/g, ' ').replace(/^[\/\-,\s]+|[\/\-,\s]+$/g, '');
    var name;
    if (s.indexOf('/') >= 0) {
      var parts = s.split('/').map(function (p) { return p.trim(); }).filter(Boolean);
      // GDS order is SURNAME/GIVEN NAMES -> display "Given Surname"
      name = titleCase((parts[1] || '') + ' ' + (parts[0] || '')).trim();
    } else {
      name = titleCase(s);
    }
    name = name.replace(/\s+/g, ' ').trim();
    if (!name) return '';
    return (title ? title + ' ' : '') + name;
  }

  function findPassengerName(lines, maskedText) {
    var hit = grabLabel(lines, /\b(PASSENGER\s*NAME|NAME\s*OF\s*PASSENGER|PASSENGER|TRAVELLER|TRAVELER|PAX\s*NAME|NAME)\b/i);
    if (hit && /[A-Za-z]{2}/.test(hit.value) && !/^\s*(AIRLINE|FLIGHT)/i.test(hit.value)) {
      var v = hit.value.replace(/\b(PASSPORT|TICKET|SEAT|PNR)\b.*$/i, '').trim();
      var n = normalizeName(v);
      if (n && n.replace(/[^A-Za-z]/g, '').length >= 3) return n;
    }
    // GDS style: "FAYSAL/MOHAMMAD MR"
    var m = maskedText.match(/\b([A-Z][A-Z' -]{1,30})\/([A-Z][A-Z' -]{1,30})\s*(MR|MRS|MS|MSTR|MISS|DR)?\b/);
    if (m) return normalizeName(m[0]);
    // "MR. MOHAMMAD FAYSAL"
    m = maskedText.match(/\b(MR|MRS|MS|MSTR|MISS|DR)\.?\s+([A-Z][A-Za-z'-]+(?:\s+[A-Z][A-Za-z'-]+){0,3})/);
    if (m) return normalizeName(m[0]);
    return '';
  }

  function findPassport(lines, maskedText) {
    var hit = grabLabel(lines, /\b(PASSPORT\s*(NO|NUMBER|#)?|DOC(?:UMENT)?\s*(NO|NUMBER)?|PP\s*NO)\b/i, {
      test: function (v) { return /\b([A-Z]{1,2}\s?\d{6,9}|\d{8,9})\b/i.test(v); }
    });
    if (hit) {
      var m = hit.value.match(/\b([A-Z]{1,2}\s?\d{6,9}|\d{8,9})\b/i);
      if (m) return m[1].replace(/\s/g, '').toUpperCase();
    }
    var g = maskedText.match(/\b([A-Z]{1,2}\d{7,8})\b/);
    return g ? g[1] : '';
  }

  function findNationality(lines, maskedText) {
    var hit = grabLabel(lines, /\b(NATIONALITY|CITIZENSHIP|COUNTRY\s*OF\s*(ISSUE|RESIDENCE)?)\b/i);
    var src = hit ? hit.value : lines.filter(function (l) {
      return !/AIRLIN|AIRWAYS|AIRPORT|TERMINAL|AVIATION|\bAIR\b/i.test(l);
    }).join('\n');
    var up = src.toUpperCase();
    var keys = Object.keys(NATIONALITY).sort(function (a, b) { return b.length - a.length; });
    for (var i = 0; i < keys.length; i++) {
      if (new RegExp('\\b' + keys[i] + '\\b').test(up)) return NATIONALITY[keys[i]];
    }
    return hit ? titleCase(hit.value) : '';
  }

  function findBookingRef(lines, maskedText) {
    var hit = grabLabel(lines, /\b(BOOKING\s*(REF(?:ERENCE)?|CODE|ID)|PNR(?:\s*(NO|CODE|NUMBER))?|RESERVATION\s*(CODE|NUMBER|REF)|RECORD\s*LOCATOR|AIRLINE\s*REF(?:ERENCE)?|CONFIRMATION\s*(NO|NUMBER|CODE))\b/i, {
      test: function (v) { return /\b[A-Z0-9]{5,8}\b/.test(v) && /[A-Z]/.test(v) && /\d/.test(v); }
    });
    if (hit) {
      var m = hit.value.match(/\b([A-Z0-9]{5,8})\b/);
      if (m && /[A-Z]/.test(m[1])) return m[1];
    }
    var g = maskedText.match(/\bPNR[^A-Z0-9]{0,8}([A-Z0-9]{5,8})\b/i);
    return g ? g[1].toUpperCase() : '';
  }

  function findTicketNumber(lines, maskedText) {
    var hit = grabLabel(lines, /\b(E?-?\s*TICKET\s*(NO|NUMBER|#)?|TKT\s*(NO|NUMBER)?|DOCUMENT\s*NUMBER)\b/i, {
      test: function (v) { return /\d{6,}/.test(v.replace(/[^\d]/g, '')) || /\d{3}\s*-\s*\d{10}/.test(v); }
    });
    var src = hit ? hit.value : maskedText;
    var m = src.match(/\b(\d{3})\s*-?\s*(\d{10})\b/);
    if (m) return m[1] + '-' + m[2];
    m = src.match(/\b(\d{13})\b/);
    if (m) return m[1].slice(0, 3) + '-' + m[1].slice(3);
    if (hit) {
      var any = hit.value.match(/\b([0-9][0-9\- ]{8,18}[0-9])\b/);
      if (any) return any[1].trim();
    }
    return '';
  }

  function findIssueDate(lines, fallbackYear) {
    var hit = grabLabel(lines, /\b(ISSUE\s*DATE|DATE\s*OF\s*ISSUE|ISSUED\s*(ON|DATE)|BOOKING\s*DATE)\b/i, {
      test: function (v) { return !!firstDate(v, fallbackYear); }
    });
    if (!hit) return '';
    var d = firstDate(hit.value, fallbackYear);
    return d ? d.text : '';
  }

  function findAirline(lines, segments, maskedText) {
    var hit = grabLabel(lines, /\b(AIRLINE|CARRIER|OPERATED\s*BY|OPERATING\s*CARRIER)\b/i);
    if (hit && /[A-Za-z]{3}/.test(hit.value)) return hit.value.replace(/\s*\([A-Z0-9]{2}\)\s*$/, '').trim();
    if (segments.length && segments[0].airline) return segments[0].airline;
    var names = Object.keys(AIRLINES).map(function (k) { return AIRLINES[k]; });
    for (var i = 0; i < names.length; i++) {
      var short = names[i].split(' (')[0];
      if (new RegExp('\\b' + short.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\b', 'i').test(maskedText)) return names[i];
    }
    return '';
  }

  function findBaggageOverall(lines, segments, maskedText) {
    var hit = grabLabel(lines, /\b(BAGGAGE(\s*(ALLOWANCE|INFO))?|FREE\s*BAGGAGE|CHECKED\s*BAGGAGE|LUGGAGE)\b/i);
    if (hit) { var b = findBaggage(hit.value); if (b) return b; }
    for (var i = 0; i < segments.length; i++) if (segments[i].baggage) return segments[i].baggage;
    return findBaggage(maskedText);
  }

  /* ------------------------------------------------------------------ *
   * Public API
   * ------------------------------------------------------------------ */

  function parseTicket(rawText, options) {
    options = options || {};
    var text = normalizeText(rawText);
    var result = emptyResult();
    result.rawText = text;
    if (!text) return result;

    // 1) money first, then blank it out everywhere else -> "SAR 2390" can never
    //    reach Travel Date / Flight No. / Time.
    var money = findMoney(text);
    var fare = pickFare(money, text);
    var masked = maskMoney(text, money);
    var lines = toLines(masked);

    var fallbackYear = options.fallbackYear || new Date().getFullYear();

    // 2) flights / dates / routes from the masked text
    var segments = extractSegments(masked, fallbackYear);

    // 3) identity + reference fields
    result.passengerName = findPassengerName(lines, masked);
    result.passportNo = findPassport(lines, masked);
    result.nationality = findNationality(lines, masked);
    result.bookingReference = findBookingRef(lines, masked);
    result.ticketNumber = findTicketNumber(lines, masked);
    result.issueDate = findIssueDate(lines, fallbackYear);
    result.airline = findAirline(lines, segments, masked);
    result.baggage = findBaggageOverall(lines, segments, masked);
    result.cabin = segments.length && segments[0].cabin ? segments[0].cabin : findCabin(masked);

    // 4) money fields
    result.currency = fare.currency || '';
    result.totalAmount = fare.totalAmount != null ? fare.totalAmount : null;
    result.totalAmountText = fare.totalAmountText || '';
    result.baseFare = fare.baseFare || '';
    result.taxes = fare.taxes || '';
    result.allAmounts = money.map(function (m) { return m.currency + ' ' + formatAmount(m.amount); });

    // 5) itinerary summary
    result.segments = segments.map(function (s) { delete s.rawWindow; return s; });
    if (segments.length) {
      var first = segments[0], last = segments[segments.length - 1];
      result.origin = first.from;
      result.destination = (last.toCode && last.toCode === first.fromCode) ? first.to : (last.to || first.to);
      result.travelDate = first.date;
      result.returnDate = segments.length > 1 ? last.date : '';
      result.tripType = (last.toCode && last.toCode === first.fromCode) ? 'Round Trip' : (segments.length > 1 ? 'Multi City' : 'One Way');
      result.flightNo = first.flightNo;
      result.route = first.route;
    }

    // Destination fallback from an explicit label
    if (!result.destination) {
      var dh = grabLabel(lines, /\b(DESTINATION|GOING\s*TO|ARRIVAL\s*CITY)\b/i);
      if (dh) { var ap = findAirports(dh.value); result.destination = ap.length ? airportLabel(ap[0]) : titleCase(dh.value); }
    }
    if (!result.travelDate) {
      var th = grabLabel(lines, /\b(TRAVEL\s*DATE|DEPARTURE\s*DATE|DATE\s*OF\s*(TRAVEL|JOURNEY|DEPARTURE)|FLIGHT\s*DATE)\b/i);
      if (th) { var td = firstDate(th.value, fallbackYear); if (td) result.travelDate = td.text; }
    }

    result.warnings = buildWarnings(result);
    result.missingFields = REQUIRED_FIELDS.filter(function (f) { return !result[f.key]; }).map(function (f) { return f.label; });
    return result;
  }

  var REQUIRED_FIELDS = [
    { key: 'passengerName', label: 'Passenger Name' },
    { key: 'passportNo', label: 'Passport No.' },
    { key: 'travelDate', label: 'Travel Date' },
    { key: 'destination', label: 'Destination' }
  ];

  function buildWarnings(r) {
    var w = [];
    if (!r.segments.length) w.push('No flight segment detected — check that the OCR text contains flight numbers.');
    r.segments.forEach(function (s) {
      if (!s.date) w.push('Flight ' + s.flightNo + ': date not found.');
      if (!s.route) w.push('Flight ' + s.flightNo + ': route not found.');
    });
    if (r.totalAmount == null) w.push('No fare/total amount detected.');
    return w;
  }

  function emptyResult() {
    return {
      passengerName: '', passportNo: '', nationality: '', airline: '',
      bookingReference: '', ticketNumber: '', issueDate: '',
      origin: '', destination: '', travelDate: '', returnDate: '',
      tripType: '', flightNo: '', route: '', cabin: '', baggage: '',
      currency: '', totalAmount: null, totalAmountText: '', baseFare: '', taxes: '',
      allAmounts: [], segments: [], warnings: [], missingFields: [], rawText: ''
    };
  }

  return {
    parseTicket: parseTicket,
    // exposed for tests / reuse
    normalizeText: normalizeText,
    normalizeName: normalizeName,
    findMoney: findMoney,
    maskMoney: maskMoney,
    findDates: findDates,
    findTimes: findTimes,
    findFlights: findFlights,
    findAirports: findAirports,
    formatAmount: formatAmount,
    AIRLINES: AIRLINES,
    AIRPORTS: AIRPORTS
  };
});
