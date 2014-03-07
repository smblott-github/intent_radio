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

   private static boolean debugging = false;
   private static boolean done_init = false;

   private static String name = null;
   private static FileOutputStream file = null;

   private static DateFormat format = null;

   /* ********************************************************************
    * Initialisation...
    */

   public static void init(Context acontext)
   {
      context = acontext;
      name = context.getString(R.string.app_name);

      Log.d(name, "Logger init");
   }

   public static void start()
   {
      if ( done_init )
         return;

      Log.d(name, "Logger start");

      done_init = true;
      debugging = true;

      format = new SimpleDateFormat("HH:mm:ss ");

      try
      {
         File log_file = new File(context.getExternalFilesDir(null), context.getString(R.string.intent_log_file));
         file = new FileOutputStream(log_file);
      }
      catch (Exception e)
         { file = null; }
   }

   public static void destroy()
   {
      if ( file != null )
      {
         try { file.close(); }
         catch (Exception e) { }
         file = null;
      }

      debugging = false;
      done_init = false;
   }

   /* ********************************************************************
    * File logging...
    */

   private static void log_file(String msg)
   {
      if ( file == null )
         return;

      String stamp = format.format(new Date());

      try
      {
         file.write((stamp+msg+"\n").getBytes());
         file.flush();
      } catch (Exception e) {}
   }

   public static void log(String msg)
   {
      if ( ! debugging || msg == null )
         return;

      Log.d(name, msg);
      log_file(msg);
   }

   public static void toast(String msg)
   {
      if ( msg == null )
         return;

      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
      log(msg);
   }

}
