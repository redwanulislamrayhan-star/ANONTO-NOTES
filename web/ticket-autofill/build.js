#!/usr/bin/env node
/* build.js — makes copy-paste artifacts in bundle/
 *   1) bundle/autofill-patch-snippet.html  → নিজের HTML-এ </body> এর আগে পেস্ট করার ব্লক
 *   2) bundle/anonto-ticket-autofill.html  → সম্পূর্ণ অ্যাপ, এক ফাইলে (অফলাইনে ডেমো চলে)
 * Run: node build.js
 */
const fs = require('fs');
const path = require('path');

const DIR = __dirname;
const OUT = path.join(DIR, 'bundle');
// ইনলাইন করার সময় সোর্সের ভেতরের "</script>" (কমেন্টেও) HTML ভেঙে দেয় — এস্কেপ করা হয়
const escapeForInline = (src) => src.replace(/<\/(script)/gi, '<\\/$1');
const readRaw = (f) => fs.readFileSync(path.join(DIR, f), 'utf8');
const read = (f) => escapeForInline(readRaw(f));

const parser = read('ticket-parser.js');
const binder = read('autofill-bind.js');

fs.mkdirSync(OUT, { recursive: true });

/* ----------------------------- 1. patch snippet ----------------------------- */
const snippet = `<!-- ══════════════════════════════════════════════════════════════════════
     e-Ticket AUTO FILL PATCH  ·  Anonto Notes
     ─────────────────────────────────────────────────────────────────────
     ব্যবহার: নিচের পুরো ব্লকটা কপি করে আপনার HTML ফাইলের </body> ট্যাগের
     ঠিক আগে পেস্ট করুন। আর কিছু বদলাতে হবে না।

     এটা যা করে:
       • পেজের প্রতিটি PDF/ছবি file input-এ নিজে থেকে যুক্ত হয়
       • পুরোনো "Imported Ticket Information" textarea-তে লেখা এলেই সেটা পার্স
         করে প্রতিটি তথ্য নিজের ফিল্ডে বসায় (Apply চাপার দরকার নেই)
       • Passenger / Passport / Nationality / PNR / Ticket No / Issue Date /
         Airline / Destination / Travel Date / Flight 1-2-3 / Date / Time /
         Status / Route / Baggage / Currency / Total — সব আলাদা ফিল্ডে
       • "SAR 2390" সবসময় টাকা হিসেবেই ধরা হয় — কখনো Date/Flight-এ যায় না

     ফিল্ড ভুল জায়গায় গেলে নিচের CONFIG-এ map দিয়ে ঠিক করে নিন।
     ══════════════════════════════════════════════════════════════════════ -->

<!-- PDF/OCR লাইব্রেরি (আপনার পেজে আগে থেকেই থাকলে এই দুটো লাইন বাদ দিন) -->
<script src="https://cdn.jsdelivr.net/npm/pdfjs-dist@3.11.174/build/pdf.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/tesseract.js@5.1.0/dist/tesseract.min.js"></script>

<script>
/* ---------- ticket-parser.js (inlined) ---------- */
${parser}
</script>
<script>
/* ---------- autofill-bind.js (inlined) ---------- */
${binder}
</script>
<script>
(function () {
  if (window.pdfjsLib && !pdfjsLib.GlobalWorkerOptions.workerSrc) {
    pdfjsLib.GlobalWorkerOptions.workerSrc =
      'https://cdn.jsdelivr.net/npm/pdfjs-dist@3.11.174/build/pdf.worker.min.js';
  }

  /* ======================= CONFIG — দরকার হলে বদলান ======================= */
  var CONFIG = {
    // hideRawBox: true দিলে পুরোনো বড় "Imported Ticket Information" বক্সটা লুকিয়ে যাবে
    hideRawBox: false,

    // overwrite: false দিলে হাতে লেখা মান মুছে যাবে না
    overwrite: true,

    // কোনো তথ্য ভুল ঘরে গেলে এখানে নিজে ম্যাপ করুন (selector → key)
    map: {
      // '#travelDate':  'travelDate',
      // '#flightNo1':   'segments.0.flightNo',
      // '#route1':      'segments.0.route',
      // '#flightNo2':   'segments.1.flightNo',
      // '#totalAmount': 'totalAmountText'
    },

    onFill: function (r) {
      console.log('[AutoFill] ' + r.inputsFilled + ' টি ফিল্ড ভরা হয়েছে', r.result);
      if (r.unmatched.length) console.warn('[AutoFill] ফর্মে ঘর পাওয়া যায়নি:', r.unmatched);
    },
    onError: function (e) { console.error('[AutoFill]', e); alert('টিকিট পড়া যায়নি: ' + e.message); }
  };
  /* ====================================================================== */

  function go() {
    var r = TicketAutoFill.autowire(CONFIG);
    console.log('[AutoFill] ready — ' + r.fileInputs + ' file input, ' + r.textAreas + ' raw box');
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', go);
  else go();

  // নিজের কোড থেকে ডাকতে চাইলে:  TicketAutoFill.fill(ocrText, CONFIG);
  window.__autoFillConfig = CONFIG;
})();
</script>
<!-- ═══════════════════ AUTO FILL PATCH শেষ ═══════════════════ -->
`;
fs.writeFileSync(path.join(OUT, 'autofill-patch-snippet.html'), snippet);

/* --------------------------- 2. standalone app --------------------------- */
let app = readRaw('index.html');
// NOTE: function replacements — the sources contain "$&", which would otherwise
// be expanded by String.replace().
const inline = (src) => () => '<script>\n' + src + '\n</script>';
app = app
  .replace('<script src="ticket-parser.js"></script>', inline(parser))
  .replace('<script src="test/samples.js"></script>', inline(read('test/samples.js')))
  .replace('<script src="app.js"></script>', inline(read('app.js')));
fs.writeFileSync(path.join(OUT, 'anonto-ticket-autofill.html'), app);

const kb = (f) => (fs.statSync(path.join(OUT, f)).size / 1024).toFixed(1) + ' KB';
console.log('bundle/autofill-patch-snippet.html   ', kb('autofill-patch-snippet.html'));
console.log('bundle/anonto-ticket-autofill.html   ', kb('anonto-ticket-autofill.html'));
