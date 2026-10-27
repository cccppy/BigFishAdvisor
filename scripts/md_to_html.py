#!/usr/bin/env python3
"""Small dependency-free Markdown -> standalone HTML converter for GUIDES.md."""
import html
import re
import sys
from pathlib import Path


def inline(s: str) -> str:
    s = html.escape(s, quote=False)
    s = re.sub(r"`([^`]+)`", r"<code>\1</code>", s)
    s = re.sub(r"\[([^\]]+)\]\(([^)]+)\)",
               r'<a href="\2" target="_blank" rel="noopener">\1</a>', s)
    s = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", s)
    s = re.sub(r"\*([^*]+)\*", r"<em>\1</em>", s)
    return s


def convert(md: str) -> str:
    lines = md.splitlines()
    out = []
    i = 0
    in_ul = False
    in_ol = False
    in_code = False
    para = []

    def flush_para():
        nonlocal para
        if para:
            out.append("<p>" + inline(" ".join(para)) + "</p>")
            para = []

    def close_lists():
        nonlocal in_ul, in_ol
        if in_ul:
            out.append("</ul>")
            in_ul = False
        if in_ol:
            out.append("</ol>")
            in_ol = False

    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        if stripped.startswith("```"):
            flush_para()
            close_lists()
            if in_code:
                out.append("</code></pre>")
                in_code = False
            else:
                out.append("<pre><code>")
                in_code = True
            i += 1
            continue
        if in_code:
            out.append(html.escape(line))
            i += 1
            continue
        if not stripped:
            flush_para()
            close_lists()
            i += 1
            continue
        m = re.match(r"^(#{1,6})\s+(.*)$", stripped)
        if m:
            flush_para()
            close_lists()
            level = len(m.group(1))
            out.append(f"<h{level}>{inline(m.group(2))}</h{level}>")
            i += 1
            continue
        if stripped.startswith(">"):
            flush_para()
            close_lists()
            out.append("<blockquote>" + inline(stripped[1:].strip()) + "</blockquote>")
            i += 1
            continue
        if re.match(r"^---+\s*$", stripped):
            flush_para()
            close_lists()
            out.append("<hr>")
            i += 1
            continue
        if stripped.startswith("|") and i + 1 < len(lines) and re.match(r"^\s*\|?[\s:|-]+\|?\s*$", lines[i + 1]):
            flush_para()
            close_lists()
            header = [c.strip() for c in stripped.strip("|").split("|")]
            out.append("<table><thead><tr>" + "".join(f"<th>{inline(c)}</th>" for c in header)
                       + "</tr></thead><tbody>")
            i += 2
            while i < len(lines) and lines[i].strip().startswith("|"):
                cells = [c.strip() for c in lines[i].strip().strip("|").split("|")]
                out.append("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in cells) + "</tr>")
                i += 1
            out.append("</tbody></table>")
            continue
        m = re.match(r"^[-*]\s+(.*)$", stripped)
        if m:
            flush_para()
            if in_ol:
                out.append("</ol>")
                in_ol = False
            if not in_ul:
                out.append("<ul>")
                in_ul = True
            out.append("<li>" + inline(m.group(1)) + "</li>")
            i += 1
            continue
        m = re.match(r"^\d+[.)]\s+(.*)$", stripped)
        if m:
            flush_para()
            if in_ul:
                out.append("</ul>")
                in_ul = False
            if not in_ol:
                out.append("<ol>")
                in_ol = True
            out.append("<li>" + inline(m.group(1)) + "</li>")
            i += 1
            continue
        para.append(stripped)
        i += 1

    flush_para()
    close_lists()
    if in_code:
        out.append("</code></pre>")
    return "\n".join(out)


def main():
    src = Path(sys.argv[1])
    dst = Path(sys.argv[2])
    body = convert(src.read_text(encoding="utf-8"))
    page = f"""<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>大肥鱼助手 · 攻略依据</title>
<style>
:root {{ --ink:#1d2a44; --muted:#5e6b82; --cream:#fff8e8; --gold:#c99a2e; --red:#b83b3b; }}
* {{ box-sizing: border-box; }}
body {{ margin:0; background:#f3ead8; color:var(--ink);
  font:17px/1.75 "Segoe UI","Microsoft YaHei",system-ui,sans-serif; }}
main {{ max-width: 980px; margin: 32px auto; background:var(--cream);
  border: 2px solid var(--ink); border-radius: 18px; padding: 28px 44px 48px;
  box-shadow: 0 18px 45px rgba(45,35,20,.18); }}
h1,h2,h3,h4 {{ line-height:1.3; color:var(--ink); margin:1.4em 0 .55em; }}
h1 {{ font-size:2.0em; border-bottom:3px solid var(--gold); padding-bottom:.35em; }}
h2 {{ font-size:1.5em; border-left:6px solid var(--gold); padding-left:.55em; }}
h3 {{ font-size:1.2em; }}
a {{ color:#8a5a00; }}
code {{ background:#f0e2c4; padding:.08em .35em; border-radius:5px; font-size:.92em; }}
pre {{ background:#1d2a44; color:#fff8e8; padding:16px; border-radius:10px; overflow:auto; }}
pre code {{ background:transparent; padding:0; }}
blockquote {{ margin:1em 0; padding:.7em 1em; background:#f7e8c6;
  border-left:5px solid var(--gold); border-radius:8px; color:#4a3a1c; }}
table {{ border-collapse:collapse; width:100%; margin:1em 0; background:#fffdf6; }}
th,td {{ border:1px solid #d8c49a; padding:.55em .7em; text-align:left; vertical-align:top; }}
th {{ background:#f0dfb9; }}
ul,ol {{ padding-left:1.6em; }}
li {{ margin:.25em 0; }}
hr {{ border:0; border-top:1px solid #d8c49a; margin:2em 0; }}
.footer {{ margin-top:2.5em; color:var(--muted); font-size:.9em; }}
@media (max-width:700px) {{ main {{ margin:0; border-radius:0; padding:20px; }} }}
</style>
</head>
<body><main>
{body}
<div class="footer">大肥鱼助手 · 攻略依据 · 生成自 GUIDES.md</div>
</main></body></html>"""
    dst.write_text(page, encoding="utf-8")
    print(dst)


if __name__ == "__main__":
    main()
