package reader.androidmin.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * PdfBox-Android 需要先初始化资源加载器（glyphlist/CMap/字体），否则解析 PDF 时
 * 报 GlyphList not found。必须作为 Application 注册到 Manifest。
 */
class ProbeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
    }
}
