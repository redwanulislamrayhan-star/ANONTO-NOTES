/* app.js — file handling (PDF text layer / OCR) + field binding.
 * All parsing intelligence lives in ticket-parser.js; this file only moves
 * values from the parse result into their own inputs. No "paste everything in
 * a big box then Apply" step. */
(function () {
  'use strict';

  var $ = function (id) { return document.getElementById(id); };
  var drop = $('drop'), fileInput = $('file'), statusEl = $('status'), bar = $('bar');
  var segmentsEl = $('segments'), rawText = $('rawText'), warnBox = $('warnBox');

  var SIMPLE_KEYS = ['passengerName', 'passportNo', 'nationality', 'destination', 'bookingReference',
    'ticketNumber', 'issueDate', 'airline', 'travelDate', 'returnDate',
    'currency', 'totalAmountText', 'baseFare', 'taxes', 'baggage', 'cabin'];

  if (window.pdfjsLib) {
    pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdn.jsdelivr.net/npm/pdfjs-dist@3.11.174/build/pdf.worker.min.js';
  }

  /* ----------------------------- status ----------------------------- */
  function setStatus(msg, kind) {
    statusEl.textContent = msg || '';
    statusEl.className = kind || '';
  }
  function setProgress(p) {
    if (p == null) { bar.classList.remove('show'); bar.firstElementChild.style.width = '0'; return; }
    bar.classList.add('show');
    bar.firstElementChild.style.width = Math.max(0, Math.min(100, p)) + '%';
  }

  /* --------------------------- PDF / OCR ---------------------------- */

  // Rebuild visual lines from the PDF text layer (items are unordered).
  function itemsToLines(items) {
    var rows = [];
    items.forEach(function (it) {
      var str = it.str;
      if (!str || !str.trim()) return;
      var y = Math.round(it.transform[5]);
      var x = it.transform[4];
      var row = null;
      for (var i = 0; i < rows.length; i++) {
        if (Math.abs(rows[i].y - y) <= 3) { row = rows[i]; break; }
      }
      if (!row) { row = { y: y, parts: [] }; rows.push(row); }
      row.parts.push({ x: x, str: str });
    });
    rows.sort(function (a, b) { return b.y - a.y; });
    return rows.map(function (r) {
      r.parts.sort(function (a, b) { return a.x - b.x; });
      var out = '', lastEnd = null;
      r.parts.forEach(function (p) {
        if (lastEnd !== null && p.x - lastEnd > 6) out += '   ';
        else if (out && !/\s$/.test(out) && !/^\s/.test(p.str)) out += ' ';
        out += p.str;
        lastEnd = p.x + p.str.length * 4.6;
      });
      return out.trim();
    }).filter(Boolean).join('\n');
  }

  function readPdf(file) {
    return file.arrayBuffer().then(function (buf) {
      return pdfjsLib.getDocument({ data: buf }).promise;
    }).then(function (pdf) {
      var pages = [];
      var chain = Promise.resolve();
      for (var i = 1; i <= pdf.numPages; i++) {
        (function (n) {
          chain = chain.then(function () {
            setProgress((n - 1) / pdf.numPages * 60);
            setStatus('PDF পড়ছি… page ' + n + '/' + pdf.numPages);
            return pdf.getPage(n).then(function (page) {
              return page.getTextContent().then(function (tc) {
                pages.push(itemsToLines(tc.items));
                return page;
              });
            });
          });
        })(i);
      }
      return chain.then(function () {
        var text = pages.join('\n');
        if (text.replace(/\s/g, '').length > 40) return text;
        setStatus('টেক্সট লেয়ার নেই — স্ক্যান করা PDF, OCR চালাচ্ছি…');
        return ocrPdfPages(pdf);
      });
    });
  }

  function ocrPdfPages(pdf) {
    var out = [];
    var chain = Promise.resolve();
    var max = Math.min(pdf.numPages, 4);
    for (var i = 1; i <= max; i++) {
      (function (n) {
        chain = chain.then(function () {
          return pdf.getPage(n).then(function (page) {
            var vp = page.getViewport({ scale: 2 });
            var canvas = document.createElement('canvas');
            canvas.width = vp.width; canvas.height = vp.height;
            return page.render({ canvasContext: canvas.getContext('2d'), viewport: vp }).promise
              .then(function () { return ocrImage(canvas, n, max); })
              .then(function (t) { out.push(t); });
          });
        });
      })(i);
    }
    return chain.then(function () { return out.join('\n'); });
  }

  function ocrImage(source, pageNo, pageTotal) {
    if (!window.Tesseract) return Promise.reject(new Error('OCR লাইব্রেরি লোড হয়নি (ইন্টারনেট সংযোগ দেখুন)।'));
    return Tesseract.recognize(source, 'eng', {
      logger: function (m) {
        if (m.status === 'recognizing text') {
          var base = pageTotal ? ((pageNo - 1) / pageTotal) * 100 : 0;
          var span = pageTotal ? 100 / pageTotal : 100;
          setProgress(base + m.progress * span);
          setStatus('OCR চলছে… ' + Math.round(m.progress * 100) + '%');
        }
      }
    }).then(function (res) { return res.data.text; });
  }

  function handleFile(file) {
    if (!file) return;
    setProgress(2);
    var isPdf = /\.pdf$/i.test(file.name) || file.type === 'application/pdf';
    var job;
    if (isPdf) {
      if (!window.pdfjsLib) { setStatus('pdf.js লোড হয়নি — লেখা পেস্ট করে চেষ্টা করুন।', 'err'); setProgress(null); return; }
      job = readPdf(file);
    } else {
      setStatus('ছবিতে OCR চলছে…');
      job = ocrImage(file);
    }
    job.then(function (text) {
      setProgress(100);
      applyParse(text, file.name);
      setTimeout(function () { setProgress(null); }, 500);
    }).catch(function (err) {
      console.error(err);
      setProgress(null);
      setStatus('পড়া যায়নি: ' + err.message, 'err');
    });
  }

  /* --------------------------- field binding --------------------------- */

  function setVal(el, value) {
    if (!el) return;
    el.value = value == null ? '' : String(value);
    if (el.value) {
      el.classList.remove('filled');
      void el.offsetWidth;
      el.classList.add('filled');
    }
  }

  function segmentCard(seg, i) {
    var wrap = document.createElement('div');
    wrap.className = 'seg';
    var n = i + 1;
    wrap.innerHTML =
      '<div class="head"><strong>Segment ' + n + (seg.airline ? ' · ' + seg.airline : '') + '</strong>' +
      '<button type="button" data-remove="' + i + '">মুছুন</button></div>' +
      '<div class="grid-3">' +
        field('Flight No. ' + n, 'seg-flight-' + i, seg.flightNo, 'SV804') +
        field('Date ' + n, 'seg-date-' + i, seg.date, '15 Oct 2026') +
        field('Time ' + n, 'seg-time-' + i, seg.departureTime, '23:05') +
      '</div><div class="grid-3">' +
        field('Arrival Time ' + n, 'seg-arr-' + i, seg.arrivalTime, '09:30') +
        field('Status ' + n, 'seg-status-' + i, seg.status, 'Confirmed') +
        field('Class ' + n, 'seg-cabin-' + i, seg.cabin, 'Economy') +
      '</div><div class="grid-2">' +
        field('From ' + n, 'seg-from-' + i, seg.from, 'Buraydah (ELQ)') +
        field('To ' + n, 'seg-to-' + i, seg.to, 'Dhaka (DAC)') +
      '</div><div class="grid-2">' +
        field('Route ' + n, 'seg-route-' + i, seg.route, 'Buraydah (ELQ) → Dhaka (DAC)') +
        field('Baggage ' + n, 'seg-bag-' + i, seg.baggage, '2 x 23 Kg') +
      '</div>';
    return wrap;
  }

  function field(label, id, value, placeholder) {
    return '<div class="field"><label for="' + id + '">' + label + '</label>' +
      '<input id="' + id + '" value="' + escapeHtml(value || '') + '" placeholder="' + escapeHtml(placeholder) + '" /></div>';
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  var currentSegments = [];

  function renderSegments(segments) {
    currentSegments = segments.slice();
    segmentsEl.innerHTML = '';
    currentSegments.forEach(function (s, i) { segmentsEl.appendChild(segmentCard(s, i)); });
    $('segCount').textContent = currentSegments.length;
    segmentsEl.querySelectorAll('input').forEach(function (inp) {
      if (inp.value) { inp.classList.add('filled'); }
    });
  }

  function collectSegments() {
    return currentSegments.map(function (_, i) {
      var g = function (k) { var el = $('seg-' + k + '-' + i); return el ? el.value.trim() : ''; };
      return {
        flightNo: g('flight'), date: g('date'), departureTime: g('time'), arrivalTime: g('arr'),
        status: g('status'), cabin: g('cabin'), from: g('from'), to: g('to'),
        route: g('route'), baggage: g('bag')
      };
    });
  }

  function showWarnings(result) {
    var list = (result.warnings || []).slice();
    if (result.missingFields && result.missingFields.length) {
      list.unshift('এই ফিল্ডগুলো পাওয়া যায়নি, হাতে লিখুন: ' + result.missingFields.join(', '));
    }
    if (!list.length) { warnBox.style.display = 'none'; warnBox.innerHTML = ''; return; }
    warnBox.style.display = 'block';
    warnBox.innerHTML = '<b>⚠️ যাচাই করুন</b><ul>' + list.map(function (w) { return '<li>' + escapeHtml(w) + '</li>'; }).join('') + '</ul>';
  }

  function applyParse(text, sourceName) {
    var result = TicketParser.parseTicket(text);
    rawText.value = result.rawText;
    SIMPLE_KEYS.forEach(function (k) { setVal($(k), result[k]); });
    renderSegments(result.segments);
    showWarnings(result);
    var found = SIMPLE_KEYS.filter(function (k) { return result[k]; }).length;
    setStatus('✅ ' + (sourceName ? sourceName + ' — ' : '') + found + ' টি ফিল্ড + ' + result.segments.length + ' টি ফ্লাইট সেগমেন্ট অটো-ফিল হয়েছে।', 'ok');
    window.__lastResult = result;
  }

  function currentData() {
    var data = {};
    SIMPLE_KEYS.forEach(function (k) { data[k] = $(k).value.trim(); });
    data.segments = collectSegments();
    return data;
  }

  /* ----------------------------- events ----------------------------- */

  drop.addEventListener('click', function () { fileInput.click(); });
  fileInput.addEventListener('change', function (e) { handleFile(e.target.files[0]); });
  ['dragenter', 'dragover'].forEach(function (ev) {
    drop.addEventListener(ev, function (e) { e.preventDefault(); drop.classList.add('over'); });
  });
  ['dragleave', 'drop'].forEach(function (ev) {
    drop.addEventListener(ev, function (e) { e.preventDefault(); drop.classList.remove('over'); });
  });
  drop.addEventListener('drop', function (e) {
    if (e.dataTransfer.files && e.dataTransfer.files[0]) handleFile(e.dataTransfer.files[0]);
  });

  document.addEventListener('paste', function (e) {
    var items = e.clipboardData && e.clipboardData.items;
    if (!items) return;
    for (var i = 0; i < items.length; i++) {
      if (items[i].type.indexOf('image') === 0) { handleFile(items[i].getAsFile()); return; }
    }
    var txt = e.clipboardData.getData('text');
    if (txt && txt.length > 60 && document.activeElement === document.body) applyParse(txt, 'পেস্ট করা লেখা');
  });

  $('pasteBtn').addEventListener('click', function () {
    var w = $('pasteWrap');
    w.style.display = w.style.display === 'none' ? 'block' : 'none';
    if (w.style.display === 'block') $('pasteText').focus();
  });
  $('parsePaste').addEventListener('click', function () { applyParse($('pasteText').value, 'পেস্ট করা লেখা'); });
  $('reparseBtn').addEventListener('click', function () { applyParse(rawText.value, 'সম্পাদিত লেখা'); });
  $('demoBtn').addEventListener('click', function () {
    applyParse(window.TicketSamples ? window.TicketSamples.SAUDIA : '', 'ডেমো টিকিট');
  });
  $('resetBtn').addEventListener('click', function () {
    SIMPLE_KEYS.forEach(function (k) { $(k).value = ''; });
    renderSegments([]);
    rawText.value = '';
    warnBox.style.display = 'none';
    setStatus('');
    setProgress(null);
  });
  $('addSeg').addEventListener('click', function () {
    var segs = collectSegments();
    segs.push({ flightNo: '', date: '', departureTime: '', arrivalTime: '', status: '', cabin: '', from: '', to: '', route: '', baggage: '' });
    renderSegments(segs);
  });
  segmentsEl.addEventListener('click', function (e) {
    var idx = e.target.getAttribute && e.target.getAttribute('data-remove');
    if (idx === null || idx === undefined) return;
    var segs = collectSegments();
    segs.splice(+idx, 1);
    renderSegments(segs);
  });
  // Auto-sync Route when From/To are edited
  segmentsEl.addEventListener('input', function (e) {
    var m = e.target.id && e.target.id.match(/^seg-(from|to)-(\d+)$/);
    if (!m) return;
    var i = m[2];
    var from = $('seg-from-' + i).value.trim(), to = $('seg-to-' + i).value.trim();
    if (from && to) $('seg-route-' + i).value = from + ' → ' + to;
  });

  $('copyJson').addEventListener('click', function () {
    var json = JSON.stringify(currentData(), null, 2);
    navigator.clipboard.writeText(json).then(
      function () { setStatus('JSON কপি হয়েছে ✅', 'ok'); },
      function () { setStatus('কপি করা যায়নি — ব্রাউজার অনুমতি দেয়নি।', 'err'); }
    );
  });
  $('downloadJson').addEventListener('click', function () {
    var blob = new Blob([JSON.stringify(currentData(), null, 2)], { type: 'application/json' });
    var a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'ticket-' + (($('bookingReference').value || 'data').replace(/\W/g, '')) + '.json';
    a.click();
    URL.revokeObjectURL(a.href);
  });
  $('printBtn').addEventListener('click', function () { window.print(); });

  renderSegments([]);
  setStatus('টিকিট দিন — তথ্য নিজে থেকেই আলাদা ফিল্ডে বসবে।');
})();
