#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Fillable PDF form generator: "করদাতার ডকুমেন্ট সংগ্রহ চেকলিস্ট (ফরম)"
===============================================================
- Bengali text is shaped correctly with HarfBuzz (uharfbuzz) using the
  original Noto Sans Bengali fonts (GSUB/GPOS applied).
- Shaped glyphs are drawn via PUA-remapped copies of the fonts, so the PDF
  embeds real vector text with correct conjuncts/positioning.
- Interactive AcroForm widgets (text fields + checkboxes) are added with
  reportlab, making this a real fillable PDF form.

Usage:  python3 build_tax_checklist_pdf.py [output.pdf]
Dependencies (pip): reportlab, fonttools, uharfbuzz
Fonts:  ~/.fonts/NotoSansBengali-Regular.ttf and -Bold.ttf (Noto project)
"""
import os
import sys
import tempfile

import uharfbuzz as hb
from fontTools.ttLib import TTFont
from fontTools.ttLib.tables._c_m_a_p import CmapSubtable

from reportlab.lib.colors import Color
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.acroform import AcroForm
from reportlab.pdfbase.ttfonts import TTFont as RLTTFont
from reportlab.pdfgen import canvas

# ----------------------------------------------------------------------------
# Fonts
# ----------------------------------------------------------------------------
FONT_DIR = os.path.expanduser("~/.fonts")
SRC_REG = os.path.join(FONT_DIR, "NotoSansBengali-Regular.ttf")
SRC_BOLD = os.path.join(FONT_DIR, "NotoSansBengali-Bold.ttf")
TMP = tempfile.mkdtemp(prefix="bnpdf_")
PUA_REG = os.path.join(TMP, "NotoBN-PUA-Regular.ttf")
PUA_BOLD = os.path.join(TMP, "NotoBN-PUA-Bold.ttf")
PUA_BASE = 0xE000


def build_pua_font(src, dst):
    """Remap every glyph to a PUA codepoint so we can draw shaped glyph IDs."""
    tt = TTFont(src)
    order = tt.getGlyphOrder()
    sub = CmapSubtable.newSubtable(4)
    sub.platformID = 3
    sub.platEncID = 1
    sub.language = 0
    sub.format = 4
    sub.cmap = {PUA_BASE + i: name for i, name in enumerate(order)}
    tt["cmap"].tables = [sub]
    tt["cmap"].tableVersion = 0
    for tag in ("GSUB", "GPOS", "GDEF", "kern", "morx", "kerx"):
        if tag in tt:
            del tt[tag]
    tt.save(dst)
    tt.close()


build_pua_font(SRC_REG, PUA_REG)
build_pua_font(SRC_BOLD, PUA_BOLD)

UPEM = TTFont(SRC_REG)["head"].unitsPerEm
pdfmetrics.registerFont(RLTTFont("BN", PUA_REG))
pdfmetrics.registerFont(RLTTFont("BN-Bold", PUA_BOLD))

# HarfBuzz font objects over the ORIGINAL fonts (with shaping tables)
_blob_r = hb.Blob.from_file_path(SRC_REG)
_blob_b = hb.Blob.from_file_path(SRC_BOLD)
HB_REG = hb.Font(hb.Face(_blob_r))
HB_BOLD = hb.Font(hb.Face(_blob_b))
HB_REG.scale = (UPEM, UPEM)
HB_BOLD.scale = (UPEM, UPEM)


def shape_run(hb_font, text):
    """Return list of (pua_codepoint, x_adv, x_off, y_off) in font units."""
    buf = hb.Buffer()
    buf.add_str(text)
    buf.guess_segment_properties()
    hb.shape(hb_font, buf)
    out = []
    for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
        out.append((PUA_BASE + info.codepoint, pos.x_advance, pos.x_offset, pos.y_offset))
    return out


def run_width(runs, size):
    return sum(r[1] for r in runs) * size / UPEM


def wrap_text(text, size, max_w, bold=False):
    hb_f = HB_BOLD if bold else HB_REG
    words = text.split(" ")
    lines, cur = [], ""

    def w(t):
        return run_width(shape_run(hb_f, t), size)

    for wd in words:
        cand = (cur + " " + wd).strip() if cur else wd
        if w(cand) <= max_w or not cur:
            cur = cand
        else:
            lines.append(cur)
            cur = wd
    if cur:
        lines.append(cur)
    return lines


def check_coverage(all_text):
    bad_found = False
    for path, label in ((SRC_REG, "regular"), (SRC_BOLD, "bold")):
        cmap = TTFont(path).getBestCmap()
        bad = sorted({ch for ch in set(all_text) if ord(ch) not in cmap and not ch.isspace()})
        if bad:
            bad_found = True
            print(f"[{label}] MISSING GLYPHS: {[f'U+{ord(c):04X} {c}' for c in bad]}")
    if not bad_found:
        print("Coverage OK (all characters present in Noto Sans Bengali).")


# ----------------------------------------------------------------------------
# Colors
# ----------------------------------------------------------------------------
ACCENT = Color(0.0588, 0.2980, 0.5059)    # #0F4C81
SOFT = Color(0.9333, 0.9529, 0.9765)      # #EEF3F9
WARN_BG = Color(1.0, 0.9686, 0.9020)      # #FFF7E6
WARN_BR = Color(0.9412, 0.8314, 0.5412)   # #F0D48A
WARN_TX = Color(0.4784, 0.3569, 0.0)      # #7A5B00
INK = Color(0.1019, 0.1019, 0.1765)       # #1A1A2E
GRAY = Color(0.3529, 0.3529, 0.4510)      # #5A5A73

BN_DIGITS = str.maketrans("0123456789", "০১২৩৪৫৬৭৮৯")


def bn_num(n):
    return str(n).translate(BN_DIGITS)


# ----------------------------------------------------------------------------
# Layout document
# ----------------------------------------------------------------------------
class Doc:
    def __init__(self, out_path):
        self.c = canvas.Canvas(out_path, pagesize=A4)
        self.c.acroForm  # ensure AcroForm exists (auto-created in reportlab 5)
        self.W, self.H = A4
        self.ML, self.MR, self.MT, self.MB = 42, 42, 44, 48
        self.CW = self.W - self.ML - self.MR
        self.y = self.MT
        self.page = 1
        self.cb_count = 0
        self.tf_count = 0

    # -- flow control -------------------------------------------------------
    def ensure(self, h):
        if self.y + h > self.H - self.MB:
            self.footer()
            self.c.showPage()
            self.page += 1
            self.y = self.MT

    def footer(self):
        self.c.setFont("BN", 7.5)
        self.c.setFillColor(GRAY)
        self.c.drawRightString(self.W - self.MR, 24, "করদাতার ডকুমেন্ট সংগ্রহ চেকলিস্ট")
        self.c.drawString(self.ML, 24, "পৃষ্ঠা " + bn_num(self.page))

    # -- primitives ---------------------------------------------------------
    def draw_text(self, x, baseline, text, size, bold=False, color=INK):
        hb_f = HB_BOLD if bold else HB_REG
        runs = shape_run(hb_f, text)
        self.c.setFont("BN-Bold" if bold else "BN", size)
        self.c.setFillColor(color)
        s = size / UPEM
        for cp, adv, xo, yo in runs:
            self.c.drawString(x + xo * s, baseline + yo * s, chr(cp))
            x += adv * s

    def textfield(self, name, x, y_top, width, height=16, fs=9):
        self.c.acroForm.textfield(
            name=name,
            x=x,
            y=self.H - y_top - height,
            width=width,
            height=height,
            fontName="Helvetica",
            fontSize=fs,
            borderColor=ACCENT,
            borderWidth=0.7,
            fillColor=Color(1, 1, 1),
            forceBorder=True,
        )

    def checkbox(self, x, baseline):
        self.c.acroForm.checkbox(
            name=f"cb{self.cb_count}",
            x=x,
            y=baseline - 3,
            size=10,
            checked=False,
            buttonStyle="check",
            borderColor=ACCENT,
            borderWidth=0.8,
        )
        self.cb_count += 1

    # -- content blocks -----------------------------------------------------
    def title(self):
        self.draw_text(self.ML, self.H - self.y - 17, "করদাতার ডকুমেন্ট সংগ্রহ চেকলিস্ট (ফরম)",
                       17, bold=True, color=ACCENT)
        self.y += 26
        self.draw_text(self.ML, self.H - self.y - 7, "ব্যক্তিগত আয়কর রিটার্ন দাখিল — বিশেষত: প্রবাস থেকে দেশে ফেরত আসা করদাতা",
                       9.5, color=GRAY)
        self.y += 18

    def warn_box(self, text):
        lines = wrap_text(text, 8.8, self.CW - 18)
        lh, pad = 12.0, 8
        h = len(lines) * lh + 2 * pad
        self.ensure(h + 6)
        self.c.setFillColor(WARN_BG)
        self.c.setStrokeColor(WARN_BR)
        self.c.setLineWidth(0.8)
        self.c.roundRect(self.ML, self.H - self.y - h, self.CW, h, 3, stroke=1, fill=1)
        for i, ln in enumerate(lines):
            self.draw_text(self.ML + 9, self.H - self.y - pad - 8 - i * lh, ln, 8.8, color=WARN_TX)
        self.y += h + 8

    def heading(self, text):
        self.ensure(30)
        self.y += 6
        top = self.H - self.y
        self.c.setFillColor(ACCENT)
        self.c.roundRect(self.ML, top - 20, self.CW, 20, 3, stroke=0, fill=1)
        self.draw_text(self.ML + 8, top - 11, text, 11, bold=True, color=Color(1, 1, 1))
        self.y += 26

    def sub_heading(self, text):
        self.ensure(20)
        self.y += 4
        self.draw_text(self.ML, self.H - self.y - 8, text, 10, bold=True, color=ACCENT)
        self.y += 15

    def item(self, text):
        lines = wrap_text(text, 9.5, self.CW - 16)
        lh = 13.2
        h = len(lines) * lh + 2.5
        self.ensure(h)
        baseline = self.H - self.y - 9.6
        for i, ln in enumerate(lines):
            self.draw_text(self.ML + 16, baseline - i * lh, ln, 9.5)
        self.checkbox(self.ML, baseline)
        self.y += h

    def meta_row(self, fields):
        gap = 12
        w = (self.CW - gap * (len(fields) - 1)) / len(fields)
        self.ensure(36)
        for i, (label, name) in enumerate(fields):
            x = self.ML + i * (w + gap)
            self.draw_text(x, self.H - self.y - 7.5, label, 8.5, color=GRAY)
            self.textfield(name, x, self.y + 12, w - 4, height=16, fs=9)
        self.y += 40

    def summary_table(self, rows):
        self.ensure(240)
        n_w, sub_w, cb_w, note_w = 26, 300, 26, self.CW - 26 - 300 - 26
        xs = [self.ML, self.ML + n_w, self.ML + n_w + sub_w, self.ML + n_w + sub_w + cb_w]
        col_h = 19
        header_h = 20
        total_h = header_h + len(rows) * col_h + 4
        self.ensure(total_h)
        top = self.H - self.y

        # header
        self.c.setFillColor(SOFT)
        self.c.rect(self.ML, top - header_h, self.CW, header_h, stroke=0, fill=1)
        headers = ["নং", "বিষয়", "আছে", "মন্তব্য"]
        hx = [self.ML + 5, self.ML + n_w + 5, self.ML + n_w + sub_w + 3, self.ML + n_w + sub_w + cb_w + 5]
        for t, x in zip(headers, hx):
            self.draw_text(x, top - 13, t, 9, bold=True)
        # rows
        for i, (no, subject) in enumerate(rows):
            row_top = top - header_h - i * col_h
            baseline = row_top - 13.5
            self.draw_text(self.ML + 5, baseline, no, 9)
            for ln, line in enumerate(wrap_text(subject, 9, sub_w - 8)):
                if ln > 1:
                    break
                self.draw_text(self.ML + n_w + 5, baseline - ln * 12.5, line, 9)
            self.checkbox(self.ML + n_w + sub_w + 3, baseline - 1)
            self.textfield(f"note{i + 1}", self.ML + n_w + sub_w + cb_w + 5, self.y + header_h + i * col_h + 3, note_w - 8, height=13, fs=8.5)
        # grid lines
        self.c.setStrokeColor(GRAY)
        self.c.setLineWidth(0.5)
        for x in xs + [self.W - self.MR]:
            self.c.line(x, top - total_h, x, top)
        for r in range(len(rows) + 1):
            yy = top - header_h - r * col_h
            self.c.line(self.ML, yy, self.W - self.MR, yy)
        self.y += total_h + 6

    def signature_block(self, x, width, caption, name_tf, date_tf):
        self.ensure(74)
        y_top = self.y
        line_y = self.H - y_top - 26
        self.c.setStrokeColor(INK)
        self.c.setLineWidth(0.9)
        self.c.line(x, line_y, x + width, line_y)
        self.draw_text(x, line_y - 12, caption, 9.5, bold=True)
        self.draw_text(x, line_y - 28, "নাম:", 8.5, color=GRAY)
        self.textfield(name_tf, x + 22, y_top + 44, 118, height=14, fs=8.5)
        self.draw_text(x + 152, line_y - 28, "তারিখ:", 8.5, color=GRAY)
        self.textfield(date_tf, x + 174, y_top + 44, 70, height=14, fs=8.5)

    def notes(self, lines):
        self.ensure(120)
        self.y += 4
        for text in lines:
            for ln in wrap_text(text, 8.2, self.CW):
                self.draw_text(self.ML, self.H - self.y - 8, ln, 8.2, color=GRAY)
                self.y += 10.5
            self.y += 2


# ----------------------------------------------------------------------------
# Content
# ----------------------------------------------------------------------------
META_ROW1 = [
    ("করদাতার নাম (ইংরেজি/বাংলা)", "name"),
    ("e-TIN নম্বর", "tin"),
    ("আয়কর বছর (যেমন: ২০২৫-২৬)", "tax_year"),
]
META_ROW2 = [
    ("বিদেশ থেকে দেশে ফেরার তারিখ", "return_date"),
    ("প্রিপেয়ার/পরামর্শকের নাম", "preparer"),
    ("মোবাইল নম্বর", "mobile"),
]

ITEMS_A = [
    "জাতীয় পরিচয়পত্র (NID) — সামনে ও পেছনে পরিষ্কার কপি",
    "পাসপোর্ট — তথ্য পৃষ্ঠা + সব স্ট্যাম্প/ভিসা পৃষ্ঠার কপি (বিদেশ যাওয়া-আসার দিন-গণনার জন্য — খুবই জরুরি)",
    "e-TIN সার্টিফিকেট — বর্তমান ও পুরনো (টিন বদলালে)",
    "সর্বশেষ রিটার্নের কপি + রিসিট (IT-10/অ্যাকনলেজমেন্ট) + অ্যাসেসমেন্ট নোটিশ (থাকলে)",
    "জন্ম সনদ / ড্রাইভিং লাইসেন্স — NID না থাকলে",
    "পাসপোর্ট সাইজ ছবি (২ কপি)",
    "যোগাযোগ — মোবাইল, ইমেইল, স্থায়ী ও বর্তমান ঠিকানা",
    "রিফান্ড পাওয়ার জন্য ব্যাংক অ্যাকাউন্টের তথ্য — অ্যাকাউন্ট নম্বর, ব্যাংক, শাখা",
]

NOTE_B = ("নিয়ম (Income Tax Act ২০২৩): আয়কর বছরে (জুলাই-জুন) বাংলাদেশে ১৮২ দিন বা তার বেশি থাকলে রেসিডেন্ট; "
          "অথবা বছরে ৯০ দিন + আগের ৪ বছরে মোট ৩৬৫ দিন। রেসিডেন্ট হলে বিশ্বব্যাপী আয় করযোগ্য — তাই দিনের হিসাব প্রমাণ করা জরুরি।")

ITEMS_B = [
    "পাসপোর্টের এন্ট্রি/এক্সিট স্ট্যাম্প — বছরের প্রতিটি যাওয়া-আসার তারিখ (দিন-গণনার ছক বানান)",
    "বিদেশি চাকরির নিয়োগপত্র/চুক্তিপত্র — শুরুর ও সমাপ্তির তারিখসহ",
    "ভিসা / ওয়ার্ক পারমিট / রেসিডেন্স পারমিট / গ্রিন কার্ড কপি",
    "দেশে ফেরার প্রমাণ — এয়ার টিকিট / ভিসা স্ট্যাম্প / জয়েনিং লেটার",
    "বর্তমান বাংলাদেশি চাকরির জয়েনিং লেটার (থাকলে)",
    "বিদেশে দাখিল করা ট্যাক্স রিটার্ন — USA: Form 1040 / UK: SA100-SA302 / মধ্যপ্রাচ্য: পে-ট্যাক্স",
    "বিদেশি কর কর্তৃপক্ষের অ্যাসেসমেন্ট/কর পরিশোধের রসিদ (ফরেন ট্যাক্স ক্রেডিটের জন্য)",
    "ট্রাভেল হিস্ট্রির ছক — কোন তারিখে কোথায় ছিলেন (করদাতা দিয়ে পূরণ করান)",
]

SUB_G1 = "বেতন (বর্তমান চাকরি)"
ITEMS_G1 = [
    "নিয়োগপত্র / অ্যাপয়েন্টমেন্ট লেটার",
    "বেতন বিবরণী (সব মাস) — বুনিয়াদি, বাড়ি ভাড়া, যাতায়াত, মেডিকেল, বোনাস, ওভারটাইম ইত্যাদি",
    "ব্যাংক স্টেটমেন্ট — বেতন ক্রেডিটের প্রমাণ",
    "উৎসে কর কর্তনের সনদ (TDS/Form-4) — নিয়োগকর্তার কাছ থেকে",
]
SUB_G2 = "বাড়ি ভাড়া (দেশি সম্পত্তি — প্রবাসে থেকেও করযোগ্য)"
ITEMS_G2 = [
    "ভাড়া চুক্তি (সব প্রপার্টি)",
    "হোল্ডিং ট্যাক্স / মিউটেশন রিসিট",
    "ভাড়া জমার ব্যাংক স্টেটমেন্ট / ভাড়াটিয়ার NID কপি",
    "বাড়ির ইউটিলিটি বিল (ঠিকানা প্রমাণে)",
]
SUB_G3 = "ব্যবসা / পেশা (থাকলে)"
ITEMS_G3 = [
    "ট্রেড লাইসেন্স + নিবন্ধন কাগজ",
    "আয়-ব্যয়ের হিসাব / প্রফিট-লস স্টেটমেন্ট",
    "ব্যবসায়িক ব্যাংক স্টেটমেন্ট",
]
SUB_G4 = "সুদ, সঞ্চয়, বিনিয়োগ ও মূলধনী আয়"
ITEMS_G4 = [
    "FDR/সঞ্চয়পত্রের সুদের ব্যাংক সার্টিফিকেট + TDS সার্টিফিকেট",
    "সঞ্চয়পত্র (NSD) সার্টিফিকেটের কপি",
    "শেয়ার ডিভিডেন্ড ওয়ারেন্ট / BO স্টেটমেন্ট / মিউচুয়াল-ICB স্টেটমেন্ট",
    "সম্পত্তি/শেয়ার বিক্রির দলিল-কন্ট্রাক্ট নোট (ক্যাপিটাল গেইন থাকলে)",
]

ITEMS_D = [
    "বিদেশি বেতন স্লিপ (পে-স্লিপ) — প্রবাসের প্রতিটি মাস",
    "বিদেশি W-2 / Form 16 / পে-ট্যাক্স / সার্ভিস লেটার",
    "বিদেশি চাকরির চুক্তিপত্র ও পুনর্নবীকরণ চিঠি",
    "বিদেশি কর পরিশোধের সনদ — ডাবল ট্যাক্স রিলিফ/ক্রেডিট দাবির জন্য",
    "বিদেশি ব্যাংক অ্যাকাউন্টের স্টেটমেন্ট (সারা বছর) + ব্যালেন্স সার্টিফিকেট",
    "বিদেশি পেনশন/রিটায়ারমেন্ট/সিকিউরিটিজ অ্যাকাউন্ট স্টেটমেন্ট",
    "বিদেশি সম্পত্তি/বিনিয়োগের কাগজ (থাকলে)",
    "বিদেশ থেকে বাংলাদেশে রেমিট্যান্সের ব্যাংক স্টেটমেন্ট / এনক্যাশমেন্ট সার্টিফিকেট",
    "প্রয়োজনে TRC (ট্যাক্স রেসিডেন্সি সার্টিফিকেট) আবেদনের কাগজ",
]

ITEMS_E = [
    "জীবন বীমা প্রিমিয়ামের রসিদ (সব পলিসি)",
    "DPS (ডিপোজিট পেনশন স্কিম) স্টেটমেন্ট",
    "সঞ্চয়পত্র ক্রয়ের রসিদ/সার্টিফিকেট",
    "প্রভিডেন্ট ফান্ড বিবরণী (নিয়োগকর্তার স্টেটমেন্ট)",
    "শেয়ার/মিউচুয়াল ফান্ড/ICB ক্রয়ের কাগজ + BO স্টেটমেন্ট",
    "বাড়ি নির্মাণ/জমি/ফ্ল্যাট কেনার রসিদ, দলিল, লোন স্টেটমেন্ট",
    "সরকারি সিকিউরিটিজ/বন্ড ক্রয়ের কাগজ",
    "অনুদানের রসিদ (NBR-অনুমোদিত প্রতিষ্ঠানে)",
    "যাকাত পরিশোধের রসিদ (যদি দাবি করেন)",
]

ITEMS_F = [
    "সব ব্যাংক অ্যাকাউন্টের স্টেটমেন্ট + ব্যালেন্স সার্টিফিকেট — দেশি ও বিদেশি (৩০ জুনের ব্যালেন্সসহ)",
    "FDR / সঞ্চয়পত্র / মিউচুয়াল ফান্ড / শেয়ার (BO) স্টেটমেন্ট",
    "রিয়েল এস্টেট: দলিল, মিউটেশন, হোল্ডিং ট্যাক্স রিসিট, বর্তমান বাজার মূল্যায়ন",
    "গাড়ি/মোটরসাইকেল — রেজিস্ট্রেশন পেপারের কপি",
    "স্বর্ণালংকার — পরিমাণ ও আনুমানিক মূল্যের তালিকা",
    "দোকান/ব্যবসায়িক অংশীদারি/ফিক্সড ডিপোজিটের কাগজ",
    "দায়/লোন: ব্যাংক লোন স্টেটমেন্ট, ক্রেডিট কার্ড স্টেটমেন্ট, অন্যান্য পাওনা",
]

ITEMS_G = [
    "করদাতার স্বাক্ষরযুক্ত অথরাইজেশন লেটার (আপনার মাধ্যমে রিটার্ন দাখিলের অনুমতি)",
    "e-Return পোর্টাল লগইন তথ্য (করদাতার সম্মতিতে; পাসওয়ার্ড গোপন রাখুন, শেষে বদলানোর পরামর্শ)",
    "OTP পাওয়ার জন্য করদাতার মোবাইল অ্যাক্সেস (ই-রিটার্ন সাবমিশনের সময়)",
    "দাখিলের পর রিসিট/অ্যাকনলেজমেন্ট কপি করদাতাকে দেওয়া",
]

TABLE_ROWS = [
    ("১", "বাংলাদেশে অবস্থানের দিনের হিসাব (পাসপোর্ট)"),
    ("২", "দেশি বেতন + TDS সনদ"),
    ("৩", "বিদেশি আয় + বিদেশি করের প্রমাণ"),
    ("৪", "বাড়ি ভাড়া (দেশি সম্পত্তি)"),
    ("৫", "সুদ / সঞ্চয়পত্র / FDR"),
    ("৬", "শেয়ার / ডিভিডেন্ড / ক্যাপিটাল গেইন"),
    ("৭", "ব্যবসা / পেশা আয়"),
    ("৮", "বিনিয়োগ রিবেটের রসিদ"),
    ("৯", "সম্পদের বিবরণীর সব কাগজ"),
    ("১০", "রিটার্ন দাখিলের অথরাইজেশন"),
]

NOTES = [
    "মনে রাখবেন: ১) রেসিডেন্সি ঠিক না হলে পুরো হিসাব ভুল হয় — পাসপোর্ট স্ট্যাম্প ও ট্রাভেল হিস্ট্রি অগ্রাধিকার নিয়ে সংগ্রহ করুন।",
    "২) ফরেন ট্যাক্স ক্রেডিট দাবির সময়সীমা আছে (DTAA অনুযায়ী) — বিদেশি কর পরিশোধের সনদপত্র আবশ্যক। ৩) প্রবাসকালেও দেশি সম্পত্তির ভাড়া করযোগ্য।",
    "৪) ব্যক্তিগত তথ্য (NID, পাসপোর্ট, ব্যাংক স্টেটমেন্ট) নিরাপদে রাখুন — করদাতার অনুমতি ছাড়া ব্যবহার করবেন না। ৫) আইন ও ফরম প্রতি বছর বদলায় — দাখিলের আগে বর্তমান NBR নিয়ম মিলিয়ে নিন।",
    "দ্রষ্টব্য: এটি একটি কাজের চেকলিস্ট — আইনি/করের চূড়ান্ত সিদ্ধান্তের জন্য যোগ্য কর পরামর্শকের শরণাপন্ন হোন।",
]


def build(out_path):
    d = Doc(out_path)
    c = d.c
    d.title()

    d.meta_row(META_ROW1)
    d.meta_row(META_ROW2)

    # ক
    d.heading("ক. মৌলিক পরিচয় ও নিবন্ধন")
    for t in ITEMS_A:
        d.item(t)

    # খ
    d.heading("খ. রেসিডেন্সি নির্ধারণের কাগজ (প্রবাস থেকে ফেরত — সবচেয়ে জরুরি)")
    d.warn_box(NOTE_B)
    for t in ITEMS_B:
        d.item(t)

    # গ
    d.heading("গ. দেশে অর্জিত আয়ের কাগজ")
    d.sub_heading("গ.১ " + SUB_G1)
    for t in ITEMS_G1:
        d.item(t)
    d.sub_heading("গ.২ " + SUB_G2)
    for t in ITEMS_G2:
        d.item(t)
    d.sub_heading("গ.৩ " + SUB_G3)
    for t in ITEMS_G3:
        d.item(t)
    d.sub_heading("গ.৪ " + SUB_G4)
    for t in ITEMS_G4:
        d.item(t)

    # ঘ
    d.heading("ঘ. বিদেশে অর্জিত আয়ের কাগজ (ফরেন ট্যাক্স ক্রেডিটের জন্য)")
    for t in ITEMS_D:
        d.item(t)

    # ঙ
    d.heading("ঙ. কর রিবেট / ছাড়ের কাগজ (বিনিয়োগ)")
    for t in ITEMS_E:
        d.item(t)

    # চ
    d.heading("চ. সম্পদের বিবরণীর কাগজ (ওয়েলথ স্টেটমেন্ট)")
    for t in ITEMS_F:
        d.item(t)

    # ছ
    d.heading("ছ. রিটার্ন দাখিল ও অথরাইজেশন")
    for t in ITEMS_G:
        d.item(t)

    # ঝ
    d.heading("ঝ. চূড়ান্ত হিসাব — কিছু বাদ গেল কি না")
    d.summary_table(TABLE_ROWS)

    # signature
    d.ensure(80)
    d.y += 6
    half = (d.CW - 20) / 2
    d.signature_block(d.ML, half, "করদাতার স্বাক্ষর ও তারিখ", "sig_name_1", "sig_date_1")
    d.signature_block(d.ML + half + 20, half, "প্রিপেয়ার/পরামর্শকের স্বাক্ষর ও তারিখ", "sig_name_2", "sig_date_2")
    d.y += 86

    d.notes(NOTES)

    d.footer()
    c.save()
    print(f"PDF written: {out_path}  ({d.page} page(s), {d.cb_count} checkboxes)")


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "docs/tax-return-document-checklist.pdf"
    # coverage check on all text used
    all_text = "".join(
        [
            "করদাতার ডকুমেন্ট সংগ্রহ চেকলিস্ট (ফরম)",
            "ব্যক্তিগত আয়কর রিটার্ন দাখিল — বিশেষত: প্রবাস থেকে দেশে ফেরত আসা করদাতা",
            "পৃষ্ঠা ১২৩৪৫৬৭৮৯০",
        ]
        + [META_ROW1[0][0], META_ROW1[1][0], META_ROW1[2][0], META_ROW2[0][0], META_ROW2[1][0], META_ROW2[2][0]]
        + ["ক. মৌলিক পরিচয় ও নিবন্ধন"] + ITEMS_A
        + ["খ. রেসিডেন্সি নির্ধারণের কাগজ (প্রবাস থেকে ফেরত — সবচেয়ে জরুরি)", NOTE_B] + ITEMS_B
        + ["গ. দেশে অর্জিত আয়ের কাগজ", "গ.১ " + SUB_G1] + ITEMS_G1 + ["গ.২ " + SUB_G2] + ITEMS_G2 + ["গ.৩ " + SUB_G3] + ITEMS_G3 + ["গ.৪ " + SUB_G4] + ITEMS_G4
        + ["ঘ. বিদেশে অর্জিত আয়ের কাগজ (ফরেন ট্যাক্স ক্রেডিটের জন্য)"] + ITEMS_D
        + ["ঙ. কর রিবেট / ছাড়ের কাগজ (বিনিয়োগ)"] + ITEMS_E
        + ["চ. সম্পদের বিবরণীর কাগজ (ওয়েলথ স্টেটমেন্ট)"] + ITEMS_F
        + ["ছ. রিটার্ন দাখিল ও অথরাইজেশন"] + ITEMS_G
        + ["ঝ. চূড়ান্ত হিসাব — কিছু বাদ গেল কি না", "নং", "বিষয়", "আছে", "মন্তব্য"]
        + [no + sub for no, sub in TABLE_ROWS]
        + NOTES
        + ["করদাতার স্বাক্ষর ও তারিখ", "প্রিপেয়ার/পরামর্শকের স্বাক্ষর ও তারিখ", "নাম:", "তারিখ:"]
    )
    check_coverage(all_text)
    build(out)
