package com.zawa.client.util

import com.zawa.client.data.BASE

/**
 * Minimal Markdown -> HTML converter tailored to zawa.uk article content.
 * Handles: images, links, headings, bold, italic, inline code, code fences,
 * blockquote, unordered lists, paragraphs, and hr. Relative image srcs are
 * rewritten to absolute https URLs against the zawa.uk base.
 */
object Markdown {

    fun toHtml(md: String): String {
        val lines = md.replace("\r\n", "\n").split("\n")
        val out = StringBuilder()
        var inUl = false
        var inOl = false
        var inCode = false
        var inBq = false

        fun closeLists() {
            if (inUl) { out.append("</ul>"); inUl = false }
            if (inOl) { out.append("</ol>"); inOl = false }
        }
        fun closeBq() { if (inBq) { out.append("</blockquote>"); inBq = false } }

        for (raw in lines) {
            val line = raw.trimEnd()
            if (line.startsWith("```")) { closeLists(); closeBq(); if (inCode) { out.append("</code></pre>"); inCode = false } else { out.append("<pre><code>"); inCode = true }; continue }
            if (inCode) { out.append(esc(line)).append("\n"); continue }

            if (line.isBlank()) { closeLists(); closeBq(); continue }

            if (line.startsWith("> ")) { if (!inBq) { out.append("<blockquote>"); inBq = true }; out.append("<p>").append(inline(line.substring(2))).append("</p>"); continue } else closeBq()

            if (line.startsWith("# ")) { closeLists(); out.append("<h1>").append(inline(line.substring(2))).append("</h1>"); continue }
            if (line.startsWith("## ")) { closeLists(); out.append("<h2>").append(inline(line.substring(3))).append("</h2>"); continue }
            if (line.startsWith("### ")) { closeLists(); out.append("<h3>").append(inline(line.substring(4))).append("</h3>"); continue }
            if (line.startsWith("#### ")) { closeLists(); out.append("<h4>").append(inline(line.substring(5))).append("</h4>"); continue }

            if (line.startsWith("- ") || line.startsWith("* ")) { if (!inUl) { closeLists(); out.append("<ul>"); inUl = true }; out.append("<li>").append(inline(line.substring(2))).append("</li>"); continue }
            val olMatch = Regex("^(\\d+)\\.\\s+(.*)").find(line)
            if (olMatch != null) { if (!inOl) { closeLists(); out.append("<ol>"); inOl = true }; out.append("<li>").append(inline(olMatch.groupValues[2])).append("</li>"); continue }

            if (line.matches(Regex("^(-{3,}|\\*{3,}|_{3,})$"))) { closeLists(); out.append("<hr/>"); continue }

            closeLists()
            out.append("<p>").append(inline(line)).append("</p>")
        }
        closeLists(); closeBq()
        if (inCode) out.append("</code></pre>")

        return out.toString()
    }

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    /** Inline formatting: images, links, bold, italic, code. */
    private fun inline(s: String): String {
        var t = esc(s)
        // images: ![alt](url)
        t = Regex("!\\[(.*?)]\\(([^)]+)\\)").replace(t) { m ->
            val alt = m.groupValues[1]; val src = abs(m.groupValues[2])
            "<img alt=\"${esc(alt)}\" src=\"$src\"/>"
        }
        // links: [text](url)
        t = Regex("\\[(.*?)]\\(([^)]+)\\)").replace(t) { m ->
            val txt = m.groupValues[1]; val href = abs(m.groupValues[2])
            "<a href=\"$href\">${esc(txt)}</a>"
        }
        // bold
        t = t.replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
        t = t.replace(Regex("__(.+?)__"), "<strong>$1</strong>")
        // italic
        t = t.replace(Regex("(?<![*\\w])\\*([^*\\n]+?)\\*(?!\\*)"), "<em>$1</em>")
        // inline code
        t = t.replace(Regex("`([^`]+)`"), "<code>$1</code>")
        return t
    }

    /** Make relative URLs absolute against zawa.uk base. */
    private fun abs(u: String): String =
        if (u.startsWith("http://") || u.startsWith("https://")) u else BASE + u

    fun wrapHtml(title: String, bodyHtml: String, dark: Boolean): String {
        val bg = if (dark) "#101114" else "#ffffff"
        val fg = if (dark) "#e8eaed" else "#1c1c1e"
        val a = "#19B67A"
        val muted = if (dark) "#9aa0a6" else "#6b7280"
        return """<!doctype html><html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
<style>
:root{color-scheme:${if (dark) "dark" else "light"}}
*{box-sizing:border-box}
html,body{margin:0;padding:0;background:$bg;color:$fg;
font-family:-apple-system,"Helvetica Neue","PingFang SC","Noto Sans CJK SC",sans-serif;
font-size:16px;line-height:1.75;word-wrap:break-word;overflow-wrap:break-word}
img{max-width:100%;height:auto;border-radius:8px;margin:8px 0;display:block}
a{color:$a;text-decoration:none;word-break:break-all}
h1{font-size:1.6em;line-height:1.3;margin:.6em 0}
h2{font-size:1.35em;margin:.6em 0}
h3,h4{font-size:1.15em;margin:.6em 0}
p{margin:.6em 0}
blockquote{margin:.6em 0;padding:.2em .9em;border-left:3px solid $a;background:${if (dark) "#1b1d22" else "#f2f4f7"};color:$muted;border-radius:4px}
code{font-family:ui-monospace,Menlo,Consolas,monospace;background:${if (dark) "#23262d" else "#f2f4f7"};padding:.1em .35em;border-radius:4px}
pre{background:${if (dark) "#16181d" else "#f6f8fa"};padding:12px;border-radius:8px;overflow-x:auto}
pre code{background:transparent;padding:0}
hr{border:none;border-top:1px solid ${if (dark) "#2a2d34" else "#e5e7eb"};margin:1em 0}
</style></head><body><h1>${esc(title)}</h1>$bodyHtml</body></html>"""
    }
}
