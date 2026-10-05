/* Sample ticket texts (PDF text-dump style) used for engine tests and demos. */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory();
  else root.TICKET_SAMPLES = factory();
})(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  return {

    omanAir: {
      label: 'Oman Air — Dhaka → Muscat → Salalah (transit, AM/PM)',
      text: [
        'OMAN AIR',
        'ELECTRONIC TICKET - PASSENGER ITINERARY AND RECEIPT',
        '',
        'Agent Name: AL MUSAFIR TRAVELS LTD',
        'Agency: MUSCAT TRAVEL AND TOURISM',
        'Issued By: TRAVEL OFFICE DHAKA',
        'Issue Date: 02 Oct 2025',
        '',
        'WEB CHECK-IN',
        'Booking Reference (PNR): G8YQ4P',
        '',
        'TRAVEL INFORMATION',
        'Passenger Name: MD MAINUL ISLAM',
        'Passport No: BR0987654',
        'Nationality: BANGLADESHI',
        'Airline: OMAN AIR',
        '',
        'JOURNEY DETAILS',
        'From: Dhaka (DAC)',
        'To: Salalah (SLL) via Muscat',
        '',
        'Flight WY684 | Date: 12 Oct 2025',
        'Departure: 08:20 AM Dhaka (DAC)',
        'Arrival: 11:15 AM Muscat (MCT)',
        'Flight Time: 5h 45m',
        'Class: Economy (O)',
        'Baggage: 30KG',
        '',
        'Transit in Muscat (MCT): 2h 30m',
        '',
        'Flight WY225 | Date: 12 Oct 2025',
        'Departure: 01:45 PM Muscat (MCT)',
        'Arrival: 03:20 PM Salalah (SLL)',
        'Flight Time: 1h 35m',
        'Class: Economy (O)',
        'Baggage: 30KG',
        '',
        'FARE INFORMATION',
        'Fare Basis: OLOWBD',
        'Base Fare: BDT 42,000',
        'Total: BDT 58,500',
        '',
        'TICKET INFORMATION',
        'Ticket Number: 920-2412345678',
        'Operating Carrier: Oman Air',
        'Aircraft: Boeing 737-800'
      ].join('\n')
    },

    saudia: {
      label: 'Saudia — Dhaka → Jeddah (24-hour, SURNAME/GIVEN name)',
      text: [
        'SAUDIA',
        'ELECTRONIC TICKET',
        'Booking reference: MMMR2L',
        '',
        'Agent: AL MUSAFIR TRAVELS LTD',
        'Issued by: TRAVEL OFFICE DHAKA',
        'Issue Date: 05 OCT 2025',
        '',
        'TRAVEL INFORMATION',
        'Passenger: MR ALAM/MOHAMMED',
        'Passport No: A01234567',
        'Nationality: BANGLADESH (BD)',
        '',
        'FLIGHT DETAILS',
        'Flight SV 772 Date 05 NOV 2025',
        'From Dhaka (DAC) 03:30',
        'To Jeddah (JED) 08:10',
        'Duration 8h 40m',
        'Cabin: Economy',
        'Baggage: 2PC',
        'Terminal 1',
        '',
        'FARE DETAILS',
        'Fare Basis: YRTBD',
        'Total: BDT 65,000',
        '',
        'TICKET INFORMATION',
        'Ticket No: 065-2412345678'
      ].join('\n')
    },

    qatar: {
      label: 'Qatar Airways — Round trip (compact rows)',
      text: [
        'QATAR AIRWAYS',
        'ELECTRONIC TICKET / RECEIPT',
        '',
        'Booking Reference: Q8R5T2',
        'Issued: 01 OCT 2025',
        '',
        'PASSENGER INFORMATION',
        'Passenger Name: MRS AKTER/SABINA',
        'Passport Number: EF0123456',
        'Nationality: BANGLADESHI',
        '',
        'ITINERARY',
        'QR 635  20 OCT 2025  DAC 02:45  DOH 05:35  Economy (S)  Duration 5h 50m',
        'Transit: Doha 2h 5m',
        'QR 638  20 OCT 2025  DOH 07:40  DAC 19:55  Economy (S)  Duration 5h 15m',
        '',
        'FARE SUMMARY',
        'Fare Basis: SRTBG',
        'Total Amount: BDT 71,000',
        '',
        'TICKET INFORMATION',
        'Ticket Number: 157-2412345678',
        'Baggage: 2PC',
        'Cabin: Economy'
      ].join('\n')
    },

    emirates: {
      label: 'Emirates — Dhaka → Dubai → London (overnight transit)',
      text: [
        'EMIRATES',
        'BOOKING CONFIRMATION',
        '',
        'Confirmation Number: KB4T7V',
        'Passenger Name: MS RAHMAN/NUSRAT',
        'Passport No: A1B2C3456',
        'Nationality: BANGLADESHI',
        '',
        'JOURNEY',
        'EK 585  15 DEC 2025  Dhaka (DAC) 09:45  Dubai (DXB) 13:00  Terminal 3',
        'EK 029  16 DEC 2025  Dubai (DXB) 10:05  London (LHR) 14:20  Terminal 3',
        '',
        'FLIGHT INFORMATION',
        'Cabin: Economy',
        'Booking Class: T',
        'Baggage: 30KG',
        'Aircraft: Boeing 777',
        'Operating Carrier: Emirates',
        '',
        'FARE',
        'Fare Basis: TLBD2',
        'Ticket Number: 176-2412345678'
      ].join('\n')
    },

    biman: {
      label: 'Biman — Chattogram → Dhaka (one way, 24-hour)',
      text: [
        'BIMAN BANGLADESH AIRLINES',
        'E-TICKET',
        '',
        'Booking Reference No: XL9M2K',
        'Travel Office: BIMAN SALES OFFICE DHAKA',
        'Issue Date: 10 OCT 2025',
        '',
        'TRAVEL INFORMATION',
        'Passenger Name: MOHAMMAD AL AMIN',
        'Passport No: DP0123456',
        'Nationality: BANGLADESHI',
        '',
        'FLIGHT DETAILS',
        'Flight: BG 435',
        'Date: 12 OCT 2025',
        'From: Chattogram (CGP)',
        'To: Dhaka (DAC)',
        'Departure: 16:20',
        'Arrival: 17:15',
        'Duration: 0h 55m',
        'Cabin: Economy',
        'Booking Class: Y',
        'Baggage: 20KG',
        'Ticket Number: 997-2401234567'
      ].join('\n')
    },

    usBangla: {
      label: 'US-Bangla — Round trip (compact, one line per flight)',
      text: [
        'US-BANGLA AIRLINES',
        'E-TICKET RECEIPT',
        '',
        'Booking Ref: BS7G1H',
        '',
        'TRAVEL INFORMATION',
        'Name: MRS CHOWDHURY/FARZANA',
        'Passport: B12345678',
        'Nationality: BANGLADESHI',
        '',
        'JOURNEY DETAILS',
        'BS 321  08 NOV 2025  DAC 10:30  CGP 11:35  Economy  1h 5m  Baggage 20KG',
        'BS 326  22 NOV 2025  CGP 17:40  DAC 18:45  Economy  1h 5m  Baggage 20KG',
        '',
        'FARE',
        'Fare Basis: EL2BG',
        'Ticket No: 555-2401234567'
      ].join('\n')
    },

    salamAir: {
      label: 'SalamAir — Dhaka → Muscat (overnight arrival +1)',
      text: [
        'SALAMAIR',
        'TRAVEL ITINERARY',
        '',
        'Booking Reference (PNR): OV9K3L',
        'Passenger Name: MR SHAKIB/RAKIB',
        'Nationality: BANGLADESHI',
        'Passport No: A7654321',
        '',
        'FLIGHT DETAILS',
        'OV 537  02 DEC 2025',
        'DAC 21:10  MCT 00:25 (+1)',
        'Duration: 3h 15m',
        '',
        'Baggage: 15KG',
        'Cabin: Economy',
        'Ticket Number: 705-2401234567'
      ].join('\n')
    }
  };
});
