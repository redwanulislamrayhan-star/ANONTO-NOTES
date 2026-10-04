/*!
 * autofill-bind.js — drop-in Auto Fill for an EXISTING ticket/visa form.
 * ----------------------------------------------------------------------
 * আপনার নিজের HTML-এ ফিল্ডের id/name যা-ই হোক, এই স্ক্রিপ্ট label/placeholder/
 * id/name পড়ে বুঝে নেয় কোন ইনপুট কোন তথ্যের জন্য — তারপর TicketParser-এর
 * ফলাফল প্রতিটি ফিল্ডে আলাদা করে বসিয়ে দেয়। কোনো বড় textarea + "Apply" লাগে না।
 *
 * ব্যবহার:
 *   <script src="ticket-parser.js"></script>
 *   <script src="autofill-bind.js"></script>
 *   <script>
 *     // সবচেয়ে সহজ: কোনো কোড না বদলে পুরো পেজে প্যাচ
 *     TicketAutoFill.autowire();
 *
 *     // অথবা OCR/PDF থেকে পাওয়া লেখা নিজে দিন:
 *     TicketAutoFill.fill(extractedText);
 *
 *     // অথবা নিজের ফাইল-ইনপুট যুক্ত করে দিন (pdf.js / Tesseract থাকলে):
 *     TicketAutoFill.attach(document.querySelector('#pdfInput'));
 *   </script>
 *
 * Requires: ticket-parser.js (window.TicketParser)
 */
(function (root, factory) {
  var api = factory(root);
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.TicketAutoFill = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function (root) {
  'use strict';

  /* ------------------------------------------------------------------ *
   * 1. কোন ফিল্ড কী — keyword patterns (ইংরেজি + বাংলা)
   * ------------------------------------------------------------------ */

  // স্কোর: যত বেশি, ম্যাচ তত নিশ্চিত। negative = এই শব্দ থাকলে বাদ।
  var FIELD_RULES = {
    passengerName: {
      must: [/passenger\s*name/, /name\s*of\s*(the\s*)?passenger/, /\bpax\s*name/, /traveller?\s*name/, /যাত্রীর?\s*নাম/, /\bname\b/, /\bনাম\b/],
      boost: [/passenger/, /pax/, /যাত্রী/],
      avoid: [/airline/, /father/, /mother/, /husband/, /sponsor/, /agent/, /company/, /hotel/, /file\s*name/, /user/, /বাবা/, /মা(য়ের)?\s*নাম/, /স্পনসর/]
    },
    passportNo: {
      must: [/passport\s*(no|number|#)?/, /\bpp\s*(no|number)/, /পাসপোর্ট/],
      avoid: [/expiry|expire|issue|valid|date|মেয়াদ/]
    },
    nationality: { must: [/nationality/, /citizenship/, /জাতীয়তা/], avoid: [] },
    destination: { must: [/destination/, /going\s*to/, /arrival\s*city/, /\bto\s*(city|country)/, /গন্তব্য/], avoid: [/from/, /origin/] },
    origin: { must: [/origin/, /departure\s*(city|airport|from)/, /\bfrom\s*(city|airport)/, /যাত্রা\s*শুরু/], avoid: [/destination/] },
    bookingReference: { must: [/booking\s*(ref|reference|code|id)/, /\bpnr\b/, /reservation\s*(code|no|number)/, /record\s*locator/, /confirmation\s*(no|code|number)/, /বুকিং/], avoid: [] },
    ticketNumber: { must: [/e?-?\s*ticket\s*(no|number|#)/, /\btkt\s*(no|number)?/, /document\s*number/, /টিকিট\s*(নম্বর|নং)/], avoid: [/date/] },
    issueDate: { must: [/issue\s*date/, /date\s*of\s*issue/, /issued\s*(on|date)/, /booking\s*date/, /ইস্যু/], avoid: [] },
    airline: { must: [/airlines?\b/, /carrier/, /operated\s*by/, /এয়ারলাইন/, /বিমান\s*সংস্থা/], avoid: [/code/, /name\s*of\s*passenger/] },
    travelDate: { must: [/travel\s*date/, /date\s*of\s*(travel|journey|departure)/, /departure\s*date/, /flight\s*date/, /onward\s*date/, /ভ্রমণ(ের)?\s*তারিখ/, /যাত্রার\s*তারিখ/], avoid: [/return/, /issue/, /expiry/, /birth/, /ফেরার/] },
    returnDate: { must: [/return\s*date/, /date\s*of\s*return/, /inbound\s*date/, /ফেরার\s*তারিখ/], avoid: [] },
    currency: { must: [/currency/, /মুদ্রা/], avoid: [] },
    totalAmountText: { must: [/total\s*(amount|fare|price|paid)/, /grand\s*total/, /amount\s*(paid|total)/, /\btotal\b/, /\bfare\b/, /\bamount\b/, /মোট\s*(টাকা|মূল্য|ভাড়া)/, /ভাড়া/], avoid: [/base/, /tax/, /refund/] },
    baseFare: { must: [/base\s*fare/, /basic\s*fare/, /net\s*fare/, /মূল\s*ভাড়া/], avoid: [] },
    taxes: { must: [/tax(es)?\b/, /surcharge/, /vat\b/, /কর\b/], avoid: [] },
    baggage: { must: [/baggage/, /luggage/, /allowance/, /লাগেজ/, /ব্যাগেজ/], avoid: [] },
    cabin: { must: [/class\b/, /cabin/, /শ্রেণ(ী|ি)/], avoid: [/first\s*name/] }
  };

  // সেগমেন্ট-লেভেল ফিল্ড (Flight 1/2/3 …)
  var SEGMENT_RULES = {
    flightNo: { must: [/flight\s*(no|number|#)/, /\bflt\b/, /\bflight\b/, /ফ্লাইট/], avoid: [/date/, /time/, /status/, /route/] },
    date: { must: [/date/, /তারিখ/], avoid: [/issue/, /birth/, /expiry/, /travel/, /journey/, /return/, /ভ্রমণ/, /ফেরার/] },
    departureTime: { must: [/dep(arture)?\s*time/, /\btime\b/, /\bdep\b/, /সময়/], avoid: [/arr/, /landing/] },
    arrivalTime: { must: [/arr(ival)?\s*time/, /\barr\b/, /landing/, /পৌঁছ/], avoid: [/dep/] },
    status: { must: [/status/, /\bok\b/, /অবস্থা/], avoid: [] },
    route: { must: [/route/, /sector/, /itinerary/, /রুট/], avoid: [] },
    from: { must: [/\bfrom\b/, /origin/, /departure\s*(city|airport|station)/, /কোথা\s*থেকে/], avoid: [/to\b/] },
    to: { must: [/\bto\b/, /destination/, /arrival\s*(city|airport|station)/, /কোথায়/], avoid: [/from/] },
    cabin: { must: [/class/, /cabin/], avoid: [] },
    baggage: { must: [/baggage/, /luggage/, /ব্যাগেজ/], avoid: [] }
  };

  /* ------------------------------------------------------------------ *
   * 2. একটি ইনপুটের "পরিচয় লেখা" বের করা
   * ------------------------------------------------------------------ */

  function textOf(node) {
    if (!node) return '';
    return (node.textContent || '').replace(/\s+/g, ' ').trim();
  }

  function labelText(el) {
    var doc = el.ownerDocument;
    var bits = [];
    if (el.id) {
      var lab = doc.querySelector('label[for="' + cssEscape(el.id) + '"]');
      if (lab) bits.push(textOf(lab));
    }
    var wrapLabel = el.closest ? el.closest('label') : null;
    if (wrapLabel) bits.push(textOf(wrapLabel));

    // আশেপাশের label / small / span / div.label
    var holder = el.closest ? (el.closest('.field, .form-group, .input-group, .row, td, th, li, p, div')) : null;
    if (holder) {
      var inner = holder.querySelector('label, .label, small, b, strong, span');
      if (inner && !inner.contains(el)) bits.push(textOf(inner));
    }
    // টেবিল হলে কলাম হেডার
    var td = el.closest ? el.closest('td') : null;
    if (td && td.parentElement) {
      var row = td.parentElement, cells = Array.prototype.indexOf.call(row.children, td);
      var table = el.closest('table');
      if (table) {
        var headRow = table.querySelector('thead tr') || table.querySelector('tr');
        if (headRow && headRow !== row && headRow.children[cells]) bits.push(textOf(headRow.children[cells]));
      }
      if (row.children[0] && row.children[0] !== td) bits.push(textOf(row.children[0]));
    }
    // ঠিক আগের টেক্সট নোড/এলিমেন্ট
    var prev = el.previousElementSibling;
    if (prev && !/input|select|textarea|button/i.test(prev.tagName)) bits.push(textOf(prev));

    return bits.join(' ');
  }

  function cssEscape(s) { return String(s).replace(/["\\]/g, '\\$&'); }

  function haystack(el) {
    return [
      el.id, el.name, el.getAttribute('placeholder'), el.getAttribute('aria-label'),
      el.getAttribute('data-field'), el.getAttribute('data-key'), el.getAttribute('title'),
      labelText(el)
    ].filter(Boolean).join(' ')
      .replace(/[_\-]+/g, ' ')
      .replace(/([a-z])([A-Z])/g, '$1 $2')   // camelCase -> "camel Case"
      .replace(/\s+/g, ' ')
      .toLowerCase();
  }

  /** "Flight No. 2", "date2", "seg-3-time" ইত্যাদি থেকে সেগমেন্ট নম্বর */
  function segIndexOf(hay) {
    var m = hay.match(/(?:^|[^0-9])([1-9])(?:[^0-9]|$)/g);
    if (!m) return null;
    var nums = hay.match(/\b(?:no\.?|number|#)?\s*([1-9])\b/g);
    var last = (nums && nums.length) ? nums[nums.length - 1].match(/([1-9])/)[1] : null;
    return last ? parseInt(last, 10) : null;
  }

  function score(hay, rule) {
    if (!hay) return 0;
    var i, s = 0, hit = false;
    for (i = 0; i < rule.must.length; i++) {
      if (rule.must[i].test(hay)) { hit = true; s += (rule.must.length - i) * 10; break; }
    }
    if (!hit) return 0;
    (rule.boost || []).forEach(function (r) { if (r.test(hay)) s += 5; });
    for (i = 0; i < (rule.avoid || []).length; i++) if (rule.avoid[i].test(hay)) return 0;
    return s;
  }

  /* ------------------------------------------------------------------ *
   * 3. ফিল্ড খোঁজা ও ভরা
   * ------------------------------------------------------------------ */

  function candidates(scope) {
    var doc = scope || (root && root.document);
    if (!doc) return [];
    var nodes = doc.querySelectorAll('input, textarea, select');
    return Array.prototype.filter.call(nodes, function (el) {
      var t = (el.type || 'text').toLowerCase();
      if (['hidden', 'file', 'submit', 'button', 'reset', 'image', 'checkbox', 'radio', 'range', 'color'].indexOf(t) >= 0) return false;
      if (el.disabled || el.readOnly) return false;
      // বিশাল textarea (পুরোনো "Imported Ticket Information" বক্স) এড়ানো
      if (el.tagName === 'TEXTAREA' && /imported|raw|extract|paste|ocr|আমদানি/i.test(haystack(el))) return false;
      return true;
    });
  }

  function setValue(el, value, opts) {
    if (!el || value == null || value === '') return false;
    if (!opts.overwrite && String(el.value || '').trim() !== '') return false;
    if (el.tagName === 'SELECT') {
      var want = String(value).toLowerCase(), done = false;
      Array.prototype.forEach.call(el.options, function (o) {
        if (!done && (o.value.toLowerCase() === want || o.text.toLowerCase().indexOf(want) >= 0)) { el.value = o.value; done = true; }
      });
      if (!done) return false;
    } else {
      el.value = String(value);
    }
    el.dispatchEvent(new (root.Event || function () {})('input', { bubbles: true }));
    el.dispatchEvent(new (root.Event || function () {})('change', { bubbles: true }));
    if (opts.highlight !== false && el.style) {
      var old = el.style.backgroundColor;
      el.style.transition = 'background-color .8s';
      el.style.backgroundColor = 'rgba(34,197,94,.28)';
      setTimeout(function () { el.style.backgroundColor = old || ''; }, 900);
    }
    return true;
  }

  /** ডেট-ইনপুট (type=date) হলে ISO লাগে */
  function valueFor(el, key, result, segment) {
    var v;
    if (segment) v = segment[key];
    else v = result[key];
    if (el.type === 'date') {
      if (segment && key === 'date') return segment.dateIso || isoFromText(v);
      if (!segment && /date/i.test(key)) return isoFromText(v);
    }
    if (el.type === 'time' && /time/i.test(key)) return v || '';
    if (el.type === 'number' && key === 'totalAmountText') return result.totalAmount != null ? result.totalAmount : '';
    return v;
  }

  function isoFromText(text) {
    if (!text) return '';
    var p = root.TicketParser && root.TicketParser.findDates ? root.TicketParser.findDates(String(text)) : [];
    return p.length ? p[0].date.iso : '';
  }

  /**
   * মূল কাজ: পার্স করা ফলাফল পেজের ফিল্ডে বসানো।
   * @param {string|object} textOrResult  OCR টেক্সট বা parseTicket()-এর ফলাফল
   * @param {object} [options] { scope, overwrite=true, highlight=true, map }
   * @returns {{result:object, filled:object, unmatched:string[]}}
   */
  function fill(textOrResult, options) {
    options = options || {};
    var opts = {
      overwrite: options.overwrite !== false,
      highlight: options.highlight !== false
    };
    var Parser = root.TicketParser || (typeof require === 'function' ? require('./ticket-parser.js') : null);
    if (!Parser) throw new Error('ticket-parser.js আগে লোড করুন।');

    var result = (typeof textOrResult === 'string') ? Parser.parseTicket(textOrResult) : textOrResult;
    var els = candidates(options.scope);
    var info = els.map(function (el) { return { el: el, hay: haystack(el), seg: null }; });
    var filled = {}, used = [];

    // 3a. ব্যবহারকারীর নিজের ম্যাপ (id/selector -> key) সবার আগে
    if (options.map) {
      Object.keys(options.map).forEach(function (sel) {
        var el = (options.scope || root.document).querySelector(sel);
        if (!el) return;
        var key = options.map[sel];
        if (setValue(el, deepGet(result, key), opts)) { filled[key] = true; used.push(el); }
      });
    }

    // 3b. সেগমেন্ট ফিল্ড (একই লেবেল একাধিকবার থাকলে ক্রম অনুযায়ী 1,2,3…)
    var segs = result.segments || [];
    Object.keys(SEGMENT_RULES).forEach(function (key) {
      var rule = SEGMENT_RULES[key];
      var matches = info.filter(function (x) {
        return used.indexOf(x.el) < 0 && score(x.hay, rule) > 0;
      });
      if (!matches.length) return;

      // যেগুলোতে স্পষ্ট নম্বর আছে ("Flight No. 2") সেগুলো আগে
      var numbered = matches.filter(function (x) { return segIndexOf(x.hay) != null; });
      var plain = matches.filter(function (x) { return segIndexOf(x.hay) == null; });

      numbered.forEach(function (x) {
        var i = segIndexOf(x.hay) - 1;
        if (segs[i] && setValue(x.el, valueFor(x.el, key, result, segs[i]), opts)) {
          filled['segment' + (i + 1) + '.' + key] = true; used.push(x.el);
        }
      });
      // নম্বর ছাড়া ফিল্ডগুলো ডকুমেন্ট-ক্রমে 1,2,3…
      plain.forEach(function (x, i) {
        if (segs[i] && setValue(x.el, valueFor(x.el, key, result, segs[i]), opts)) {
          filled['segment' + (i + 1) + '.' + key] = true; used.push(x.el);
        }
      });
    });

    // 3c. সাধারণ ফিল্ড — প্রতিটির জন্য সবচেয়ে ভালো ম্যাচ একটি ইনপুট
    Object.keys(FIELD_RULES).forEach(function (key) {
      if (filled[key]) return;
      var rule = FIELD_RULES[key], best = null, bestScore = 0;
      info.forEach(function (x) {
        if (used.indexOf(x.el) >= 0) return;
        var s = score(x.hay, rule);
        if (s > bestScore) { bestScore = s; best = x; }
      });
      if (best && setValue(best.el, valueFor(best.el, key, result, null), opts)) {
        filled[key] = true; used.push(best.el);
      }
    });

    var unmatched = Object.keys(FIELD_RULES).filter(function (k) { return result[k] && !filled[k]; });
    return { result: result, filled: filled, unmatched: unmatched, inputsFilled: used.length };
  }

  function deepGet(obj, path) {
    return String(path).split('.').reduce(function (o, k) {
      if (o == null) return '';
      if (/^\d+$/.test(k)) return o[+k];
      return o[k];
    }, obj);
  }

  /* ------------------------------------------------------------------ *
   * 4. ফাইল ইনপুট যুক্ত করা (PDF text layer / OCR)
   * ------------------------------------------------------------------ */

  function extractText(file) {
    var isPdf = /\.pdf$/i.test(file.name) || file.type === 'application/pdf';
    if (isPdf && root.pdfjsLib) {
      return file.arrayBuffer()
        .then(function (buf) { return root.pdfjsLib.getDocument({ data: buf }).promise; })
        .then(function (pdf) {
          var chain = Promise.resolve(), pages = [];
          for (var i = 1; i <= pdf.numPages; i++) {
            (function (n) {
              chain = chain.then(function () {
                return pdf.getPage(n)
                  .then(function (p) { return p.getTextContent(); })
                  .then(function (tc) { pages.push(linesFromItems(tc.items)); });
              });
            })(i);
          }
          return chain.then(function () { return pages.join('\n'); });
        });
    }
    if (root.Tesseract) {
      return root.Tesseract.recognize(file, 'eng').then(function (r) { return r.data.text; });
    }
    return Promise.reject(new Error('PDF/OCR লাইব্রেরি নেই — pdf.js বা tesseract.js যোগ করুন।'));
  }

  function linesFromItems(items) {
    var rows = [];
    items.forEach(function (it) {
      if (!it.str || !it.str.trim()) return;
      var y = Math.round(it.transform[5]), row = null;
      for (var i = 0; i < rows.length; i++) if (Math.abs(rows[i].y - y) <= 3) { row = rows[i]; break; }
      if (!row) { row = { y: y, parts: [] }; rows.push(row); }
      row.parts.push({ x: it.transform[4], str: it.str });
    });
    rows.sort(function (a, b) { return b.y - a.y; });
    return rows.map(function (r) {
      r.parts.sort(function (a, b) { return a.x - b.x; });
      return r.parts.map(function (p) { return p.str; }).join(' ').replace(/\s+/g, ' ').trim();
    }).filter(Boolean).join('\n');
  }

  /** ফাইল ইনপুটে ফাইল দিলেই নিজে নিজে ফিল্ড ভরবে। */
  function attach(input, options) {
    options = options || {};
    if (!input) return;
    input.addEventListener('change', function (e) {
      var file = e.target.files && e.target.files[0];
      if (!file) return;
      if (options.onStart) options.onStart(file);
      extractText(file).then(function (text) {
        var out = fill(text, options);
        if (options.onFill) options.onFill(out);
      }).catch(function (err) {
        if (options.onError) options.onError(err); else console.error(err);
      });
    });
  }

  /**
   * কোনো কোড না বদলেই পুরো পেজে প্যাচ বসায়:
   *  1) প্রতিটি PDF/ছবির file input-এ নিজে থেকে যুক্ত হয়
   *  2) পুরোনো "Imported Ticket Information" জাতীয় বড় textarea-তে লেখা এলেই
   *     সেটা পার্স করে ফিল্ড ভরে দেয় (আলাদা Apply চাপতে হয় না)
   *  3) চাইলে ঐ raw বক্সটা আড়ালও করে দেয় (hideRawBox: true)
   */
  function autowire(options) {
    options = options || {};
    var doc = options.document || (root && root.document);
    if (!doc) return { fileInputs: 0, textAreas: 0 };

    var fileInputs = Array.prototype.filter.call(doc.querySelectorAll('input[type=file]'), function (el) {
      var a = (el.getAttribute('accept') || '').toLowerCase();
      return !a || /pdf|image|\.png|\.jpg|\.jpeg|\.webp/.test(a);
    });
    fileInputs.forEach(function (el) {
      if (el.__ticketAutoFill) return;
      el.__ticketAutoFill = true;
      attach(el, options);
    });

    var areas = Array.prototype.filter.call(doc.querySelectorAll('textarea'), function (t) {
      return /import|raw|ocr|extract|ticket|paste|scan|তথ্য|লেখা/i.test(haystack(t));
    });
    areas.forEach(function (t) {
      if (t.__ticketAutoFill) return;
      t.__ticketAutoFill = true;
      var last = '';
      var run = function () {
        var v = t.value || '';
        if (v.length < 60 || v === last) return;
        last = v;
        var out = fill(v, options);
        if (options.onFill) options.onFill(out);
      };
      t.addEventListener('input', run);
      t.addEventListener('change', run);
      t.addEventListener('blur', run);
      if (options.poll !== false && root.setInterval) root.setInterval(run, 900);
      run();
      if (options.hideRawBox) {
        var box = t.closest ? (t.closest('.field, .form-group, label, div') || t) : t;
        if (box.style) box.style.display = 'none';
      }
    });

    return { fileInputs: fileInputs.length, textAreas: areas.length };
  }

  return {
    fill: fill,
    attach: attach,
    autowire: autowire,
    extractText: extractText,
    // ডিবাগ/কাস্টমাইজের জন্য
    haystack: haystack,
    FIELD_RULES: FIELD_RULES,
    SEGMENT_RULES: SEGMENT_RULES
  };
});
