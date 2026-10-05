#!/usr/bin/env python3
"""Generate sample airline-ticket PDFs (real extractable text) for the demo."""
import os
from fpdf import FPDF

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, 'samples')
os.makedirs(OUT, exist_ok=True)

# (filename, title, lines, two-column rows: (left, right))
TICKETS = [
    {
        'file': 'oman-air.pdf',
        'title': 'OMAN AIR - ELECTRONIC TICKET',
        'lines': [
            ('b', 'OMAN AIR'),
            ('', 'ELECTRONIC TICKET - PASSENGER ITINERARY AND RECEIPT'),
            ('', ''),
            ('', 'Agent Name: AL MUSAFIR TRAVELS LTD'),
            ('', 'Issued By: TRAVEL OFFICE DHAKA'),
            ('', 'Issue Date: 02 Oct 2025'),
            ('', ''),
            ('b', 'WEB CHECK-IN'),
            ('', 'Booking Reference (PNR): G8YQ4P'),
            ('', ''),
            ('b', 'TRAVEL INFORMATION'),
            ('', 'Passenger Name: MD MAINUL ISLAM'),
            ('', 'Passport No: BR0987654'),
            ('', 'Nationality: BANGLADESHI'),
            ('', 'Airline: OMAN AIR'),
            ('', ''),
            ('b', 'JOURNEY DETAILS'),
            ('', 'From: Dhaka (DAC)'),
            ('', 'To: Salalah (SLL) via Muscat'),
            ('', ''),
            ('', 'Flight WY684  |  Date: 12 Oct 2025'),
            ('', 'Departure: 08:20 AM  Dhaka (DAC)'),
            ('', 'Arrival: 11:15 AM  Muscat (MCT)'),
            ('', 'Flight Time: 5h 45m'),
            ('', 'Class: Economy (O)'),
            ('', 'Baggage: 30KG'),
            ('', ''),
            ('', 'Transit in Muscat (MCT): 2h 30m'),
            ('', ''),
            ('', 'Flight WY225  |  Date: 12 Oct 2025'),
            ('', 'Departure: 01:45 PM  Muscat (MCT)'),
            ('', 'Arrival: 03:20 PM  Salalah (SLL)'),
            ('', 'Flight Time: 1h 35m'),
            ('', 'Class: Economy (O)'),
            ('', 'Baggage: 30KG'),
            ('', ''),
            ('b', 'FARE INFORMATION'),
            ('', 'Fare Basis: OLOWBD'),
            ('', 'Total: BDT 58,500'),
            ('', ''),
            ('b', 'TICKET INFORMATION'),
            ('', 'Ticket Number: 920-2412345678'),
            ('', 'Operating Carrier: Oman Air'),
            ('', 'Aircraft: Boeing 737-800'),
        ],
    },
    {
        'file': 'saudia.pdf',
        'title': 'SAUDIA - ELECTRONIC TICKET',
        'lines': [
            ('b', 'SAUDIA'),
            ('', 'ELECTRONIC TICKET'),
            ('', 'Booking reference: MMMR2L'),
            ('', ''),
            ('', 'Agent: AL MUSAFIR TRAVELS LTD'),
            ('', 'Issued by: TRAVEL OFFICE DHAKA'),
            ('', 'Issue Date: 05 OCT 2025'),
            ('', ''),
            ('b', 'TRAVEL INFORMATION'),
            ('', 'Passenger: MR ALAM/MOHAMMED'),
            ('', 'Passport No: A01234567'),
            ('', 'Nationality: BANGLADESH (BD)'),
            ('', ''),
            ('b', 'FLIGHT DETAILS'),
            ('', 'Flight SV 772  Date 05 NOV 2025'),
            ('', 'From Dhaka (DAC)  03:30'),
            ('', 'To Jeddah (JED)  08:10'),
            ('', 'Duration 8h 40m'),
            ('', 'Cabin: Economy'),
            ('', 'Baggage: 2PC'),
            ('', 'Terminal 1'),
            ('', ''),
            ('b', 'FARE DETAILS'),
            ('', 'Fare Basis: YRTBD'),
            ('', 'Total: BDT 65,000'),
            ('', ''),
            ('b', 'TICKET INFORMATION'),
            ('', 'Ticket No: 065-2412345678'),
        ],
    },
    {
        'file': 'qatar-airways.pdf',
        'title': 'QATAR AIRWAYS - ELECTRONIC TICKET',
        'lines': [
            ('b', 'QATAR AIRWAYS'),
            ('', 'ELECTRONIC TICKET / RECEIPT'),
            ('', ''),
            ('', 'Booking Reference: Q8R5T2'),
            ('', 'Issued: 01 OCT 2025'),
            ('', ''),
            ('b', 'PASSENGER INFORMATION'),
            ('', 'Passenger Name: MRS AKTER/SABINA'),
            ('', 'Passport Number: EF0123456'),
            ('', 'Nationality: BANGLADESHI'),
            ('', ''),
            ('b', 'ITINERARY'),
            ('', 'QR 635   20 OCT 2025   DAC 02:45   DOH 05:35   Economy (S)   Duration 5h 50m'),
            ('', 'Transit: Doha 2h 5m'),
            ('', 'QR 638   20 OCT 2025   DOH 07:40   DAC 19:55   Economy (S)   Duration 5h 15m'),
            ('', ''),
            ('b', 'FARE SUMMARY'),
            ('', 'Fare Basis: SRTBG'),
            ('', 'Total Amount: BDT 71,000'),
            ('', ''),
            ('b', 'TICKET INFORMATION'),
            ('', 'Ticket Number: 157-2412345678'),
            ('', 'Baggage: 2PC'),
            ('', 'Cabin: Economy'),
        ],
    },
]

for t in TICKETS:
    pdf = FPDF()
    pdf.set_auto_page_break(auto=True, margin=18)
    pdf.add_page()
    pdf.set_margins(16, 16, 16)
    y = 16
    for style, text in t['lines']:
        if not text:
            y += 3
            continue
        pdf.set_font('helvetica', 'B' if style == 'b' else '', 11 if style == 'b' else 10)
        pdf.set_xy(16, y)
        pdf.cell(180, 6, text, new_x='LMARGIN', new_y='NEXT')
        y = pdf.get_y() + 1.2
    path = os.path.join(OUT, t['file'])
    pdf.output(path)
    print('wrote', path, os.path.getsize(path), 'bytes')

print('done')
