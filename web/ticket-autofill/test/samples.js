/* Sample OCR / PDF-text dumps used by the parser tests and the demo button. */

// 1) Saudia e-ticket, labelled layout (the exact case from the bug report)
var SAUDIA = [
  'SAUDI ARABIAN AIRLINES',
  'ELECTRONIC TICKET RECEIPT',
  '',
  'Passenger Name   : MR. MOHAMMAD FAYSAL',
  'Passport No      : A12345678',
  'Nationality      : BANGLADESH',
  'Booking Reference: XK7Q2M',
  'Ticket Number    : 065-1234567890',
  'Issue Date       : 02 Oct 2026',
  'Airline          : Saudia (SV)',
  '',
  'TICKET DETAILS',
  'Flight: SV804   Date: 15 Oct 2026   Dep: 23:05   Arr: 09:30   Status: Confirmed',
  'From: Buraydah (ELQ)   To: Dhaka (DAC)',
  'Class: Economy   Baggage: 2 x 23 KG',
  '',
  'Flight: SV807   Date: 05 Dec 2026   Dep: 16:05   Arr: 20:40   Status: Confirmed',
  'From: Dhaka (DAC)   To: Buraydah (ELQ)',
  'Class: Economy   Baggage: 2 x 23 KG',
  '',
  'FARE DETAILS',
  'Base Fare    : SAR 1990',
  'Taxes & Fees : SAR 400',
  'Total Amount : SAR 2390'
].join('\n');

// 2) Biman, classic GDS/console layout (no labels, bare times, DDMMMYY dates)
var BIMAN_GDS = [
  'BIMAN BANGLADESH AIRLINES',
  'E-TICKET ITINERARY RECEIPT',
  'PASSENGER: RAHMAN/ABDUR MR          ADT',
  'PNR: 4KL9ZT        TKT: 9971234567890',
  'DATE OF ISSUE: 12SEP26',
  '',
  'BG 348  Y  25SEP26  DAC JED  0310  0730  OK',
  'BG 349  Y  20NOV26  JED DAC  0930  2035  OK',
  '',
  'FARE   BDT 85,400',
  'TAX    BDT 12,250',
  'TOTAL  BDT 97,650',
  'BAGGAGE 2PC'
].join('\n');

// 3) Messy OCR: broken spacing, value on the next line, amount glued near a date
var MESSY_OCR = [
  'emirates',
  'Passenger',
  'KHATUN/MST SHIRIN MRS',
  'Booking Reference',
  'J8PL2D',
  'Passport Number BX0912345',
  'Nationality: Bangladeshi',
  '',
  'Departure  EK 585   10/11/2026   02:45   Dhaka (DAC) -> Dubai (DXB)   Confirmed',
  'Arrival    07:05   Terminal 3   Economy   Free Baggage 30 KG',
  '',
  'Grand Total USD 612.50',
  'Taxes USD 98.50'
].join('\n');

if (typeof module === 'object' && module.exports) {
  module.exports = { SAUDIA: SAUDIA, BIMAN_GDS: BIMAN_GDS, MESSY_OCR: MESSY_OCR };
}
if (typeof window !== 'undefined') {
  window.TicketSamples = { SAUDIA: SAUDIA, BIMAN_GDS: BIMAN_GDS, MESSY_OCR: MESSY_OCR };
}
