# e-Ticket Auto Fill — field-by-field parser

OCR/PDF থেকে পাওয়া লেখা **এক বড় textarea-তে ঢেলে Apply** করার বদলে প্রতিটি তথ্য
pattern/keyword ধরে **নিজের ফিল্ডে** বসায়।

## যে সমস্যাগুলো ঠিক হয়েছে

| আগে (বাগ) | এখন |
|---|---|
| Travel Date → `00 SAR 2390` | টাকার অঙ্ক **সবার আগে** আলাদা করে ফেলা হয় (`maskMoney`), তাই Date/Flight/Time-এ কখনো ঢুকতে পারে না |
| Flight No. আলাদা হতো না | প্রতিটি লেগ আলাদা: `SV804`, `SV807` |
| Route এক জায়গায় জমা হতো | `Buraydah (ELQ) → Dhaka (DAC)` আর `Dhaka (DAC) → Buraydah (ELQ)` আলাদা |
| সব তথ্য "Imported Ticket Information" বক্সে | raw লেখা শুধু একটা collapsed ডিবাগ বক্সে; ফিল্ড আগেই ভরা থাকে |

## কী কী ফিল্ড ধরা হয়

Passenger Name · Passport No. · Nationality · Booking Reference/PNR · Ticket Number ·
Issue Date · Airline · Destination · Travel Date · Return Date · Trip Type ·
প্রতি সেগমেন্টে Flight No. / Date / Departure Time / Arrival Time / Status / Class /
From / To / Route / Baggage / Terminal · Currency · Total Amount · Base Fare · Taxes · Baggage.

## চালানো

```bash
cd web/ticket-autofill
python3 -m http.server 8000      # তারপর http://localhost:8000
```

PDF হলে pdf.js দিয়ে টেক্সট লেয়ার পড়া হয়; টেক্সট লেয়ার না থাকলে (স্ক্যান করা PDF) বা
ছবি দিলে Tesseract.js দিয়ে OCR চলে। “ডেমো টিকিট” বাটনে ক্লিক করলে ইন্টারনেট ছাড়াই পরখ করা যায়।

## নিজের অ্যাপে যোগ করা

```html
<script src="ticket-parser.js"></script>
<script>
  const r = TicketParser.parseTicket(ocrText);

  document.getElementById('passengerName').value = r.passengerName;  // MR. Mohammad Faysal
  document.getElementById('passportNo').value    = r.passportNo;     // A12345678
  document.getElementById('nationality').value   = r.nationality;    // Bangladeshi
  document.getElementById('destination').value   = r.destination;    // Dhaka (DAC)
  document.getElementById('travelDate').value    = r.travelDate;     // 15 Oct 2026
  document.getElementById('totalAmount').value   = r.totalAmountText;// SAR 2,390

  r.segments.forEach((s, i) => {
    // s.flightNo "SV804" | s.date "15 Oct 2026" | s.departureTime "23:05"
    // s.status "Confirmed" | s.route "Buraydah (ELQ) → Dhaka (DAC)"
  });
</script>
```

Node/বান্ডলারেও চলে: `const { parseTicket } = require('./ticket-parser.js');`

### `parseTicket(text)` এর রিটার্ন

```jsonc
{
  "passengerName": "MR. Mohammad Faysal",
  "passportNo": "A12345678",
  "nationality": "Bangladeshi",
  "bookingReference": "XK7Q2M",
  "ticketNumber": "065-1234567890",
  "issueDate": "02 Oct 2026",
  "airline": "Saudia",
  "origin": "Buraydah (ELQ)",
  "destination": "Dhaka (DAC)",
  "travelDate": "15 Oct 2026",
  "returnDate": "05 Dec 2026",
  "tripType": "Round Trip",
  "currency": "SAR",
  "totalAmount": 2390,
  "totalAmountText": "SAR 2,390",
  "baseFare": "SAR 1,990",
  "taxes": "SAR 400",
  "baggage": "2 x 23 Kg",
  "segments": [
    { "flightNo": "SV804", "date": "15 Oct 2026", "departureTime": "23:05",
      "arrivalTime": "09:30", "status": "Confirmed",
      "from": "Buraydah (ELQ)", "to": "Dhaka (DAC)",
      "route": "Buraydah (ELQ) → Dhaka (DAC)", "baggage": "2 x 23 Kg" }
  ],
  "warnings": [], "missingFields": []
}
```

## কীভাবে ভুল ঠেকানো হয়

1. **Money first** — `SAR/BDT/USD/৳/$ …` + সংখ্যা আগে খুঁজে বের করে সেই অংশটুকু
   স্পেস দিয়ে ঢেকে দেওয়া হয়। এরপর date/flight/time খোঁজা হয়, তাই `SAR 2390`
   কখনো Date বা Flight হতে পারে না। `00 SAR 2390`-এ `SAR 2390` priority পায়।
2. **Flight No.** — `\b(AA|A1|1A)\s?\d{1,4}\b` + এয়ারলাইন কোড যাচাই; passport
   `A12345678` বা PNR `XK7Q2M` ম্যাচ করে না, currency কোড (SAR/TK) বাদ।
3. **Segment window** — একটা ফ্লাইট লাইন থেকে পরের ফ্লাইট লাইনের আগ পর্যন্ত লেখাই
   ওই লেগের তথ্য, তাই SV804-এর তারিখ SV807-এ যায় না।
4. **Dates** — `15 Oct 2026`, `15OCT26`, `15/10/2026`, `2026-10-15`, `Oct 15, 2026`,
   বাংলা সংখ্যা (`১৫ অক্টো ২০২৬`) — সব সাপোর্টেড।
5. **Times** — `23:05`, `11:05 PM`, `2305 HRS`, আর GDS স্টাইল খালি `0310`
   (date/flight মাস্ক করার পর, তাই `2026` কখনো `20:26` হয় না)।
6. **Labels** — `grabLabel` ভুল লাইন (যেমন ব্যানারের “ELECTRONIC TICKET RECEIPT”)
   বাদ দিয়ে validator-পাস করা মান খোঁজে; মান পরের লাইনে থাকলেও ধরে।

## টেস্ট

```bash
node --test web/ticket-autofill/test/parser.test.js
```

Saudia (labelled), Biman (GDS console), Emirates (messy OCR), বাংলা সংখ্যা এবং
“SAR 2390 যেন Date-এ না যায়” — সব কেস কভার করা আছে।
