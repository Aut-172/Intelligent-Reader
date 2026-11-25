#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
智能阅读器架构视图全套交付图件生成脚本 (华为云 CodeArts Modeling 规范标准)
同时生成 PUML 源文件对应的 SVG 高清矢量图、PNG 位图以及标准 PDF 交付件。
支持主架构图件与课堂汇报拆分图件的全量递归生成。
"""

import os
import subprocess
import shutil
from pathlib import Path
from PIL import Image

CURRENT_DIR = Path(__file__).resolve().parent
PUML_DIR = CURRENT_DIR / "底稿-PlantUML"
SVG_DIR = CURRENT_DIR / "导出图-SVG"
PNG_DIR = CURRENT_DIR / "导出图-PNG"
PDF_DIR = CURRENT_DIR / "导出图-PDF"

# 探测 plantuml.jar 位置
CANDIDATE_JARS = [
    CURRENT_DIR / "plantuml.jar",
    CURRENT_DIR.parents[3] / "plantuml.jar",
    CURRENT_DIR.parents[4] / "plantuml.jar",
    Path("D:/北航事务/大三上/软分/软分项目/plantuml.jar"),
]
PLANTUML_JAR = next((str(jar) for jar in CANDIDATE_JARS if jar.exists()), "plantuml.jar")

def main():
    puml_files = sorted(PUML_DIR.rglob("*.puml"))
    print(f"[INFO] 正在基于华为云 CodeArts Modeling 规范生成全部 {len(puml_files)} 份架构图件...")
    print(f"[INFO] 使用 PlantUML 引擎: {PLANTUML_JAR}")

    for idx, puml_path in enumerate(puml_files, 1):
        rel_path = puml_path.relative_to(PUML_DIR)
        rel_dir = rel_path.parent
        base_name = puml_path.stem

        target_svg_dir = SVG_DIR / rel_dir
        target_png_dir = PNG_DIR / rel_dir
        target_pdf_dir = PDF_DIR / rel_dir

        target_svg_dir.mkdir(parents=True, exist_ok=True)
        target_png_dir.mkdir(parents=True, exist_ok=True)
        target_pdf_dir.mkdir(parents=True, exist_ok=True)

        print(f"[{idx:02d}/{len(puml_files):02d}] 编译图件: {rel_path} ...")

        # 1. 生成 SVG 矢量图
        subprocess.run(["java", "-jar", PLANTUML_JAR, "-tsvg", str(puml_path)], check=True)
        gen_svg = puml_path.with_suffix(".svg")
        if gen_svg.exists():
            shutil.move(str(gen_svg), str(target_svg_dir / f"{base_name}.svg"))

        # 2. 生成 PNG 高清图
        subprocess.run(["java", "-jar", PLANTUML_JAR, "-tpng", str(puml_path)], check=True)
        gen_png = puml_path.with_suffix(".png")
        if gen_png.exists():
            dest_png = target_png_dir / f"{base_name}.png"
            shutil.move(str(gen_png), str(dest_png))

            # 3. 转换为标准 PDF 交付件 (150 DPI)
            target_pdf = target_pdf_dir / f"{base_name}.pdf"
            with Image.open(dest_png) as img:
                img_rgb = img.convert("RGB")
                img_rgb.save(str(target_pdf), "PDF", resolution=150.0)

        # 清理可能生成的临时 cmapx 文件
        gen_cmapx = puml_path.with_suffix(".cmapx")
        if gen_cmapx.exists():
            gen_cmapx.unlink()

    print(f"[SUCCESS] 全量 {len(puml_files)} 份架构图件 (PUML / SVG / PNG / PDF) 生成完成！")

if __name__ == "__main__":
    main()
