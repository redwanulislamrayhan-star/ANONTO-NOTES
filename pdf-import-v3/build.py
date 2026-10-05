#!/usr/bin/env python3
"""Assemble the single-file app:
   template.html + parser.js + samples.js + pdf.js (inlined, offline)  →  final HTML"""
import os
import re
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT_NAME = 'FAMILY VISA PDF AUTO IMPORT V3.html'

def vendor(path, cache_name):
    """Vendor file from local node_modules (npm install pdfjs-dist@3.11.174),
    falling back to a previously cached copy next to this script."""
    local = os.path.join(HERE, 'node_modules', 'pdfjs-dist', 'build', path)
    cache = os.path.join(HERE, cache_name)
    if os.path.exists(local) and os.path.getsize(local) > 10000:
        data = open(local, encoding='utf-8').read()
        open(cache, 'w', encoding='utf-8').write(data)
        return data
    if os.path.exists(cache) and os.path.getsize(cache) > 10000:
        return open(cache, encoding='utf-8').read()
    raise SystemExit('missing vendor file — run: npm install pdfjs-dist@3.11.174')

def safe_inline(js):
    """</script inside an inline <script> block would close it early."""
    if '</script' in js.lower():
        js = re.sub(r'</script', r'<\\/script', js, flags=re.I)
    return js

def main():
    template = open(os.path.join(HERE, 'template.html'), encoding='utf-8').read()
    parser = open(os.path.join(HERE, 'parser.js'), encoding='utf-8').read()
    samples = open(os.path.join(HERE, 'samples.js'), encoding='utf-8').read()

    pdfjs = safe_inline(vendor('pdf.min.js', 'vendor-pdf.min.js'))
    worker = safe_inline(vendor('pdf.worker.min.js', 'vendor-pdf.worker.min.js'))

    html = template
    html = html.replace('<!--__PDFJS__-->', '<script>\n' + pdfjs + '\n</script>')
    html = html.replace('<!--__PDFWORKER__-->', '<script id="pdf-worker-src">\n' + worker + '\n</script>')
    html = html.replace('/*__PARSER__*/', parser)
    html = html.replace('/*__SAMPLES__*/', samples)

    for marker in ('__PDFJS__', '__PDFWORKER__', '/*__PARSER__*/', '/*__SAMPLES__*/'):
        if marker in html:
            print('ERROR: marker not replaced:', marker); sys.exit(1)

    for out in (os.path.join(HERE, 'index.html'), os.path.join(ROOT, OUT_NAME)):
        open(out, 'w', encoding='utf-8').write(html)
        print('wrote', out, f'{len(html)/1024:.0f} KB')

if __name__ == '__main__':
    main()
