package com.example.dwgconverter

/** Thin JNI bridge to LibreDWG (see app/src/main/cpp/dwgconv.c). */
object DwgNative {
    init {
        System.loadLibrary("dwgconv")
    }

    /**
     * Converts [inPath] to [outPath].
     * @param target LibreDWG version id: "R13", "R14", "R2000", "R2004", "R2007", "R2010", "R2013", "R2018"
     * @param asDxf  true = write DXF, false = write DWG
     * @return null on success, otherwise an error message
     */
    @JvmStatic
    external fun convert(inPath: String, outPath: String, target: String, asDxf: Boolean): String?

    /** LibreDWG library version string, for the About line. */
    @JvmStatic
    external fun libVersion(): String
}
