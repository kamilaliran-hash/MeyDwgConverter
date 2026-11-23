/*
 * JNI wrapper around LibreDWG: read any DWG, write DWG (R13–R2000) or DXF.
 * Licensed GPLv3+ (because LibreDWG is GPLv3+).
 */
#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <android/log.h>

#include "config.h"   /* LibreDWG internal: PACKAGE_VERSION etc. */
#include <dwg.h>
#include "bits.h"     /* Bit_Chain */
#include "out_dxf.h"  /* dwg_write_dxf */

/* Same steps as LibreDWG's programs/dwg2dxf.c */
static int write_dxf(const char *path, Dwg_Data *dwg)
{
  Bit_Chain dat = { 0 };
  dat.version = dwg->header.version;
  dat.from_version = dwg->header.from_version;
  dat.codepage = dwg->header.codepage;
  dat.opts = dwg->opts;
  dat.fh = fopen(path, "wb");
  if (!dat.fh)
    return DWG_ERR_IOERROR;
  int err = dwg_write_dxf(&dat, dwg);
  fclose(dat.fh);
  return err;
}

#define TAG "dwgconv"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static Dwg_Version_Type parse_version(const char *s)
{
  if (!strcmp(s, "R13"))   return R_13;
  if (!strcmp(s, "R14"))   return R_14;
  if (!strcmp(s, "R2000")) return R_2000;
  if (!strcmp(s, "R2004")) return R_2004;
  if (!strcmp(s, "R2007")) return R_2007;
  if (!strcmp(s, "R2010")) return R_2010;
  if (!strcmp(s, "R2013")) return R_2013;
  if (!strcmp(s, "R2018")) return R_2018;
  return R_INVALID;
}

JNIEXPORT jstring JNICALL
Java_com_example_dwgconverter_DwgNative_convert(JNIEnv *env, jclass clazz,
                                                jstring jin, jstring jout,
                                                jstring jtarget, jboolean asDxf)
{
  (void)clazz;
  char msg[256] = { 0 };
  const char *in = (*env)->GetStringUTFChars(env, jin, NULL);
  const char *out = (*env)->GetStringUTFChars(env, jout, NULL);
  const char *target = (*env)->GetStringUTFChars(env, jtarget, NULL);

  Dwg_Version_Type version = parse_version(target);
  if (version == R_INVALID)
    {
      snprintf(msg, sizeof msg, "Unknown target version %s", target);
      goto done;
    }
  if (!asDxf && version > R_2000)
    {
      snprintf(msg, sizeof msg, "LibreDWG can only write DWG up to R2000");
      goto done;
    }

  Dwg_Data dwg;
  memset(&dwg, 0, sizeof dwg);
  dwg.opts = 0; /* log level 0 = quiet */

  int err = dwg_read_file(in, &dwg);
  if (err >= DWG_ERR_CRITICAL)
    {
      LOGE("dwg_read_file failed: 0x%x", err);
      snprintf(msg, sizeof msg,
               "Could not read the drawing (error 0x%x). The file may be "
               "damaged or use features LibreDWG doesn't support.", err);
      dwg_free(&dwg);
      goto done;
    }

  /* The reader sets from_version = the original file version.
     Setting header.version selects the version to write. */
  dwg.header.version = version;

  if (asDxf)
    err = write_dxf(out, &dwg);
  else
    err = dwg_write_file(out, &dwg);

  if (err >= DWG_ERR_CRITICAL)
    {
      LOGE("write failed: 0x%x", err);
      snprintf(msg, sizeof msg, "Could not write the %s file (error 0x%x).",
               asDxf ? "DXF" : "DWG", err);
    }
  dwg_free(&dwg);

done:
  (*env)->ReleaseStringUTFChars(env, jin, in);
  (*env)->ReleaseStringUTFChars(env, jout, out);
  (*env)->ReleaseStringUTFChars(env, jtarget, target);
  return msg[0] ? (*env)->NewStringUTF(env, msg) : NULL;
}

JNIEXPORT jstring JNICALL
Java_com_example_dwgconverter_DwgNative_libVersion(JNIEnv *env, jclass clazz)
{
  (void)clazz;
  return (*env)->NewStringUTF(env, "LibreDWG " PACKAGE_VERSION);
}
