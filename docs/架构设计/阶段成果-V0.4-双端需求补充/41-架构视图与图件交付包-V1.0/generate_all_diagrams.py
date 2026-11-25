#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
智能阅读器架构视图全套交付图件生成脚本 (华为云 CodeArts Modeling 规范标准)
同时生成 PUML 源文件对应的 SVG 高清矢量图、200+ DPI PNG 位图以及标准 PDF 交付件。
"""

import os
import glob
import subprocess
import shutil
from PIL import Image

CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
PUML_DIR = os.path.join(CURRENT_DIR, "底稿-PlantUML")
SVG_DIR = os.path.join(CURRENT_DIR, "导出图-SVG")
PNG_DIR = os.path.join(CURRENT_DIR, "导出图-PNG")
PDF_DIR = os.path.join(CURRENT_DIR, "导出图-PDF")

# 探测 plantuml.jar 位置
CANDIDATE_JARS = [
    os.path.join(CURRENT_DIR, "plantuml.jar"),
    os.path.abspath(os.path.join(CURRENT_DIR, "../../../..", "plantuml.jar")),
    os.path.abspath(os.path.join(CURRENT_DIR, "../../../../..", "plantuml.jar")),
]
PLANTUML_JAR = next((jar for jar in CANDIDATE_JARS if os.path.exists(jar)), "plantuml.jar")

os.makedirs(SVG_DIR, exist_ok=True)
os.makedirs(PNG_DIR, exist_ok=True)
os.makedirs(PDF_DIR, exist_ok=True)

def main():
    puml_files = sorted(glob.glob(os.path.join(PUML_DIR, "*.puml")))
    print(f"[INFO] 正在基于华为云 CodeArts Modeling 规范生成全部 {len(puml_files)} 份架构图件...")
    print(f"[INFO] 使用 PlantUML 引擎: {PLANTUML_JAR}")

    for idx, puml in enumerate(puml_files, 1):
        base_name = os.path.splitext(os.path.basename(puml))[0]
        print(f"[{idx:02d}/{len(puml_files):02d}] 编译图件: {base_name} ...")
        
        # 1. 生成 SVG 矢量图
        subprocess.run(["java", "-jar", PLANTUML_JAR, "-tsvg", puml], check=True)
        gen_svg = os.path.join(PUML_DIR, f"{base_name}.svg")
        if os.path.exists(gen_svg):
            target_svg = os.path.join(SVG_DIR, f"{base_name}.svg")
            shutil.move(gen_svg, target_svg)

        # 2. 生成 PNG 高清图
        subprocess.run(["java", "-jar", PLANTUML_JAR, "-tpng", puml], check=True)
        gen_png = os.path.join(PUML_DIR, f"{base_name}.png")
        if os.path.exists(gen_png):
            target_png = os.path.join(PNG_DIR, f"{base_name}.png")
            shutil.move(gen_png, target_png)
            
            # 3. 转换为标准 PDF 交付件
            target_pdf = os.path.join(PDF_DIR, f"{base_name}.pdf")
            img = Image.open(target_png).convert("RGB")
            img.save(target_pdf, "PDF", resolution=150.0)

    print("[SUCCESS] 全量 14 份架构图件 (PUML / SVG / PNG / PDF) 生成完成！")

if __name__ == "__main__":
    main()
