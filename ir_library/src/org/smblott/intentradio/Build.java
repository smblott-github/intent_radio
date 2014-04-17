package org.smblott.intentradio;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.text.SimpleDateFormat;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

public class Build
{
   static private String build = null;

   // source: http://stackoverflow.com/questions/7607165/how-to-write-build-time-stamp-into-apk
   //
   public static String getBuildDate(Context context)
   {
      if ( build != null )
         return build;

      try
      {
         ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
         ZipFile file = new ZipFile(info.sourceDir);
         ZipEntry entry = file.getEntry("classes.dex");
         long time = entry.getTime();
         build = SimpleDateFormat.getInstance().format(new java.util.Date(time));
      }
      catch (Exception e)
         { build = "Unknown"; }

      if ( debug_build(context)  )
         build += " [debug]";

      return build;
   }

   public static boolean debug_build(Context context)
   {
      int DEBUGGABLE = ApplicationInfo.FLAG_DEBUGGABLE;
      return (context.getApplicationInfo().flags & DEBUGGABLE) == DEBUGGABLE;
   }

   public static String version_string(Context context)
   {
      try
      {
         PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
         int version_code = pinfo.versionCode;
         String version_name = pinfo.versionName;
         return version_code + "-" + version_name;
      }
      catch (Exception e) {}

      return "Unknown";
   }
}
