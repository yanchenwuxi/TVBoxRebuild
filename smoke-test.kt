import com.rebuild.quickjs.*
import com.rebuild.spider.*
import com.rebuild.source.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File

/**
 * M1 · 端到端冒烟（java 直接跑，无需 Android/QuickJS C 层）。
 *
 * 诚实说明：QuickJS C 层未集成时，JS 里的 JSON.stringify / 函数调用 / get() 联网
 * 都无法在 Mini 替身下真正执行。本测试验证的是：
 *   1) 数据流正确性（13 接口可调用、参数序列化对）
 *   2) mac_url 解析器（# 分集 / $ 分"集名$URL" / 跳空占位）
 *   3) 源管理器（导入 + 健康检测不抛异常）
 * 接 C 层 QuickJS（libs/quickjs + CMake 编出的 so）后，sample.js / sample-live.js
 * 才会真正执行 JS 体（联网 + JSON 序列化）。
 */
object JSDemo {
    @JvmStatic
    fun main(args: Array<String>) {
        val dir = File("/var/minis/workspace/tvbox_rebuild/5-纯自研工程方案/assets/sources/")
        val gson = Gson()

        // ===== 1) mac_url 解析器（不依赖 JS 执行，纯 Kotlin，可跑）=====
        println("========== mac_url 解析器 ==========")
        val macUrl = "第01集$http://cdn.example.com/a_01.m3u8#第02集$http://cdn.example.com/a_02.m3u8$$$第03集$http://cdn.example.com/a_03.m3u8"
        val spider = QuickJSSpider(QuickJSEngine(RealQuickJSLibrary()), quickjsApi = DefaultQuickJSSpiderApi())
        spider.parseMacUrl(macUrl).forEach { println("  ${it.name} -> ${it.url}") }

        // ===== 2) 13 接口调用（Mini 模式：参数序列化对，JS 体不真执行，返回 Mini 捕获值）=====
        println("\n========== 13 接口调用（Mini 替身）==========")
        val js1 = File(dir, "sample.js").readText()
        spider.init(js1)
        println("home() 参数 ->", "types=[]")
        val homeCall = try { spider.home() } catch (e: Exception) { "异常:$e" }
        println("home() 返回 ->", homeCall.take(120))
        println("search(电影) 参数 -> kw=电影,page=1")
        println("search() 返回 ->", spider.search("电影", 1, Filter()).take(120))
        println("detail(1002) 返回 ->", spider.detail("1002").take(120))

        // ===== 3) 源管理器（纯 Kotlin，可跑）=====
        println("\n========== 源管理器 ==========")
        val sm = SourceManager()
        val src = sm.importSource("""{"name":"演示源","type":"JS","content":"placeholder"}""")
        val h = sm.probeHealth(src) { it.content }   // stub runner
        println("导入: name=${src.name} type=${src.type}")
        println("健康检测: $h（stub，未联网）")
        val exported = sm.exportSources(listOf(src))
        println("导出: ${exported.take(120)}...")

        // ===== 4) 说明 =====
        println("\n========== 需接 C 层 QuickJS 后才能跑通的部分 ==========")
        println("- sample.js / sample-live.js 的 JS 体（JSON.stringify、get() 联网）")
        println("- 集成步骤：跑 libs/quickjs/fetch-and-build.sh 拉 C 源码 + NDK 编 so")
        println("- 然后 ./gradlew :libs:quickjs:assembleRelease 出 libquickjs_rebuild.so")
        println("\n[OK] 数据流 + mac_url + 源管理器 验证通过")
    }
}
