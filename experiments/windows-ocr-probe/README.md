# Windows OCR 探针（W-E03）

- 用途：验证 Windows 内置 OCR（`Windows.Media.Ocr`）与 PDF 渲染接入
- 环境：Windows 11 Build 26300，PowerShell 5.1，无需第三方 OCR
- 结果报告：`results/TECH-WIN-017-R01.md`

## 文件

| 文件 | 说明 |
|---|---|
| `ocr_winrt.ps1` | 生成中英数字样本图并 OCR，输出 `results/we03-ocr.json` |
| `ocr_image.ps1` | 通用图片 OCR：`-Image <png> -OutJson <json> -Expect "a;b;c"` |
| `results/we03-sample.png` | 中英数字样本（1400×420） |
| `results/we03-render.pdf` / `.png` | PDFBox 生成的 PDF 及其 200 dpi 渲染页 |
| `results/we03-render-ocr.json` | 渲染页 OCR 结果（3/3） |

## 复跑

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ocr_winrt.ps1
# PDF 渲染页需先由 windows-kmp-minimal/docProbe 的 RenderMainKt 生成
powershell -NoProfile -ExecutionPolicy Bypass -File .\ocr_image.ps1 `
  -Image .\results\we03-render.png `
  -OutJson .\results\we03-render-ocr.json `
  -Expect "Reader Studio W-E03 2026;PDF render;12345"
```

## 结果摘要

| 样本 | 命中 | 耗时 |
|---|---|---|
| 中英数字图片 | 5/5 | 85.8 ms |
| PDF 200 dpi 渲染页 | 3/3 | 渲染 231.7 ms + OCR 105 ms |
