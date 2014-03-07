package org.smblott.intentradio;

import java.io.File;
import java.io.FileOutputStream;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.widget.Toast;
import android.util.Log;

public class Logger
{
   private static Context context = null;

   private static boolean debug_file = true;
   private static boolean debug_log = true;

   private static String app_name = null;
   private static FileOutputStream log_file_stream = null;

   private static DateFormat format = null;

   /* ********************************************************************
    * Initialisation...
    */

   public static void init(Context acontext)
   {
      context = acontext;
      app_name = context.getString(R.string.app_name);

      if ( debug_file && log_file_stream == null )
         try
         {
            File log_file = new File(context.getExternalFilesDir(null), context.getString(R.string.intent_log_file));
            log_file_stream = new FileOutputStream(log_file);
         }
         catch (Exception e)
            { log_file_stream = null; }
   }

   public static void destroy()
   {
      if ( log_file_stream != null )
      {
         try { log_file_stream.close(); }
         catch (Exception e) { }
         log_file_stream = null;
      }
   }

   /* ********************************************************************
    * File logging...
    */

   private static void log_to_file(String msg)
   {
      if ( log_file_stream == null )
         return;

      if ( format == null )
         format = new SimpleDateFormat("HH:mm:ss ");

      String stamp = format.format(new Date());

      try
      {
         log_file_stream.write((stamp+msg+"\n").getBytes());
         log_file_stream.flush();
      } catch (Exception e) {}
   }

   public static void log(String msg)
   {
      if ( msg == null )
         return;

      log_to_file(msg);
      if ( debug_log )
         Log.d(app_name, msg);
   }

   public static void toast(String msg)
   {
      if ( msg == null )
         return;

      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
      log(msg);
   }

}
