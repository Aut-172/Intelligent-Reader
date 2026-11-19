#!/usr/bin/env python3
"""Generate A-E02 supplementary PDF samples: rotated page, font specialization, images, large-page."""
from pathlib import Path
import hashlib, json

from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "samples" / "self-authored" / "pdf"
OUT.mkdir(parents=True, exist_ok=True)
ANN = ROOT / "samples" / "self-authored" / "annotations"
ANN.mkdir(parents=True, exist_ok=True)


def find_font():
    candidates = [
        Path("C:/Windows/Fonts/msyh.ttc"),
        Path("C:/Windows/Fonts/simhei.ttf"),
        Path("C:/Windows/Fonts/simsun.ttc"),
    ]
    return next((c for c in candidates if c.exists()), None)


def register_cjk():
    fp = find_font()
    if fp is None:
        raise RuntimeError("no CJK font found")
    name = "CJK"
    try:
        pdfmetrics.registerFont(TTFont(name, str(fp), subfontIndex=0))
    except TypeError:
        pdfmetrics.registerFont(TTFont(name, str(fp)))
    return name


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


results = []


def gen_rotated():
    p = OUT / "SELF-013-rotated.pdf"
    c = canvas.Canvas(str(p), pagesize=A4)
    c.setPageRotation(90)
    cjk = register_cjk()
    c.setFont(cjk, 14)
    c.drawString(100, 720, "旋转页样本：本页设置了 /Rotate 90")
    c.drawString(100, 700, "解析器应报告页面旋转并正确处理坐标。")
    c.setFont("Helvetica", 12)
    c.drawString(100, 680, "Rotated page anchor: English")
    c.showPage()
    c.save()
    results.append(("SELF-013", p, ["旋转页", "Rotated page anchor"]))


def gen_fonts():
    p = OUT / "SELF-014-fonts.pdf"
    c = canvas.Canvas(str(p), pagesize=A4)
    cjk = register_cjk()
    c.setFont(cjk, 12)
    c.drawString(100, 720, "字体专项：中文用 CJK 字体")
    c.setFont("Helvetica", 12)
    c.drawString(100, 700, "Font anchor: Helvetica Latin")
    c.setFont("Helvetica-Bold", 12)
    c.drawString(100, 680, "Bold anchor: bold Latin text")
    c.setFont("Times-Roman", 12)
    c.drawString(100, 660, "Serif anchor: Times Roman")
    c.showPage()
    c.save()
    results.append(("SELF-014", p, ["字体专项", "Font anchor", "Bold anchor", "Serif anchor"]))


def gen_images():
    p = OUT / "SELF-015-images.pdf"
    c = canvas.Canvas(str(p), pagesize=A4)
    cjk = register_cjk()
    c.setFont(cjk, 12)
    c.drawString(100, 720, "图片样本：正文与嵌入图片混合")
    img = Image.new("RGB", (300, 150), "white")
    d = ImageDraw.Draw(img)
    d.rectangle((10, 10, 290, 140), outline="black", width=3)
    d.text((30, 60), "Embedded figure", fill="black")
    tmp = OUT / "_fig.png"
    img.save(tmp)
    c.drawImage(str(tmp), 100, 500, width=300, height=150)
    c.drawString(100, 470, "图 1：嵌入图片示例（应保留为图片资产或降级信息）")
    c.showPage()
    c.save()
    tmp.unlink()
    results.append(("SELF-015", p, ["图片样本", "嵌入图片"]))


def gen_large():
    p = OUT / "SELF-016-large.pdf"
    c = canvas.Canvas(str(p), pagesize=A4)
    cjk = register_cjk()
    for i in range(1, 31):
        c.setFont(cjk, 12)
        c.drawString(100, 720, f"大页样本：第 {i} 页 / 30")
        c.drawString(100, 700, "本页用于验证多页文档的解析时间与资源预算。")
        c.setFont("Helvetica", 10)
        c.drawString(100, 680, f"page {i} anchor")
        c.showPage()
    c.save()
    results.append(("SELF-016", p, ["大页样本", "page 30 anchor"]))


gen_rotated()
gen_fonts()
gen_images()
gen_large()

manifest_rows = []
for sid, path, anchors in results:
    annotation = {
        "sample_id": sid,
        "fixed_queries": anchors,
        "expected_evidence_fragments": anchors,
        "reference_notes": "A-E02 supplementary sample",
        "annotation_version": 1,
        "source": "project-authored",
    }
    (ANN / f"{sid}.json").write_text(json.dumps(annotation, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    manifest_rows.append({"sample_id": sid, "path": str(path), "sha256": sha256(path), "pages": None, "bytes": path.stat().st_size})

print(json.dumps(manifest_rows, ensure_ascii=False, indent=2))
