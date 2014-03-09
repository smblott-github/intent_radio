package org.smblott.intentradio;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.text.SimpleDateFormat;

public class Build
{
   // source: http://stackoverflow.com/questions/7607165/how-to-write-build-time-stamp-into-apk
   //
   public static String getBuildDate(Context context)
   {
      String stamp = "Unknown";
      try
      {
         ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
         ZipFile zf = new ZipFile(ai.sourceDir);
         ZipEntry ze = zf.getEntry("classes.dex");
         long time = ze.getTime();
         stamp = SimpleDateFormat.getInstance().format(new java.util.Date(time));
      }
      catch(Exception e){ }

      return stamp;
   }
}
