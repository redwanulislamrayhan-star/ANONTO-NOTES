# PDF Import Engine V3 — Airline Ticket Parser

Section-based airline-ticket parser and receipt generator, shipped as a single
self-contained HTML app (`../FAMILY VISA PDF AUTO IMPORT V3.html`).

## What it does

```
PDF upload → PDF.js text extraction → Section map → Structured ticket → Review/Edit → Receipt
```

The engine reads a ticket in **sections** — Header → Passenger → Journey →
Fare/Ticket → Extra — instead of blind regex hunting:

- **Passenger Information**: Passenger Name, Passport No., Nationality,
  Airline (brand, not flight number), Airline Reference (PNR)
- **Smart passenger detection**: agent / agency / travel-office / issued-by
  names are never mistaken for the passenger
- **Journey**: unlimited flight segments, transit & layover computed,
  One Way / Round Trip / Journey with Transit / Multi-city auto-detected
- **AM/PM**: kept as printed on the ticket; 24-hour times converted (15:20 → 3:20 PM)
- **Ticket Information**: Ticket Number, Fare Basis, Baggage, Cabin, Booking
  Class, Seat, Terminal, Gate, Operating Carrier, Aircraft — only fields that
  exist are shown
- Handles Saudia, Oman Air, SalamAir, Emirates, Qatar Airways, Biman
  Bangladesh, US-Bangla, Flydubai, Air Arabia and more

## Files

| file | purpose |
|---|---|
| `parser.js` | the engine (pure JS, no DOM — runs in browser & Node) |
| `template.html` | app UI template |
| `build.py` | assembles the single-file app (inlines parser, samples, pdf.js) |
| `samples.js` | sample ticket texts (7 airlines) |
| `gen_samples.py` | generates real sample ticket PDFs into `samples/` |
| `test.js` | engine unit tests (`npm test`) |
| `test-e2e.js` | full pipeline test on the sample PDFs (`npm run test:e2e`) |

## Develop

```bash
npm install          # pdfjs-dist (inlined into the build)
python3 gen_samples.py
node test.js && node test-e2e.js
python3 build.py     # writes index.html + ../FAMILY VISA PDF AUTO IMPORT V3.html
```

Serve this folder (`python3 -m http.server`) to get the sample-PDF buttons.
