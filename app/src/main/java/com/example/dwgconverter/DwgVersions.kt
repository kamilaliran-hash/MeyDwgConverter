package com.example.dwgconverter

import java.io.File

/** One entry in the "Save as" list. */
data class OutputFormat(
    val label: String,
    val target: String,     // passed to native code
    val asDxf: Boolean,
    val suffix: String,     // appended to the file name, e.g. "_R2000"
) {
    val extension get() = if (asDxf) "dxf" else "dwg"
    override fun toString() = label
}

object DwgVersions {

    /** What LibreDWG can write. R2000 is its most reliable DWG output. */
    val outputs = listOf(
        OutputFormat("DWG – AutoCAD 2000/2002 (AC1015)  ★ recommended", "R2000", false, "_R2000"),
        OutputFormat("DWG – AutoCAD R14 (AC1014)  experimental", "R14", false, "_R14"),
        OutputFormat("DWG – AutoCAD R13 (AC1012)  experimental", "R13", false, "_R13"),
        OutputFormat("DXF – AutoCAD 2018", "R2018", true, "_2018"),
        OutputFormat("DXF – AutoCAD 2013", "R2013", true, "_2013"),
        OutputFormat("DXF – AutoCAD 2010", "R2010", true, "_2010"),
        OutputFormat("DXF – AutoCAD 2007", "R2007", true, "_2007"),
        OutputFormat("DXF – AutoCAD 2004", "R2004", true, "_2004"),
        OutputFormat("DXF – AutoCAD 2000", "R2000", true, "_R2000"),
    )

    private val codes = mapOf(
        "MC0.0" to "AutoCAD R1.0", "AC1.2" to "AutoCAD R1.2", "AC1.40" to "AutoCAD R1.4",
        "AC1.50" to "AutoCAD R2.0", "AC2.10" to "AutoCAD R2.10",
        "AC1001" to "AutoCAD R2.2", "AC1002" to "AutoCAD R2.5", "AC1003" to "AutoCAD R2.6",
        "AC1004" to "AutoCAD R9", "AC1006" to "AutoCAD R10", "AC1009" to "AutoCAD R11/R12",
        "AC1012" to "AutoCAD R13", "AC1014" to "AutoCAD R14",
        "AC1015" to "AutoCAD 2000–2002", "AC1018" to "AutoCAD 2004–2006",
        "AC1021" to "AutoCAD 2007–2009", "AC1024" to "AutoCAD 2010–2012",
        "AC1027" to "AutoCAD 2013–2017", "AC1032" to "AutoCAD 2018–2025+",
    )

    /** Reads the 6-byte version code at the start of a DWG file, e.g. "AC1032". */
    fun detect(file: File): String? {
        val head = ByteArray(6)
        val n = file.inputStream().use { it.read(head) }
        if (n < 5) return null
        val code = String(head, 0, n, Charsets.US_ASCII).trimEnd('\u0000')
        if (!code.startsWith("AC") && !code.startsWith("MC")) return null
        val name = codes[code] ?: codes[code.take(5)] ?: "Unknown version"
        return "$name ($code)"
    }
}
