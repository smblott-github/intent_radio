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
   }

   /* ********************************************************************
    * Enable/disable...
    */

   public static void state(String how)
   {
      if ( how.equals("debug") || how.equals("yes") || how.equals("on") || how.equals("start") )
         { start(); return; }

      if ( how.equals("nodebug") || how.equals("no") || how.equals("off") || how.equals("stop") )
         { stop(); return; }

      Log.d(name, "Logger: invalid state: " + how);
   }

   /* ********************************************************************
    * State changes...
    */

   private static void start()
   {
      if ( debugging )
         return;

      if ( format == null )
         format = new SimpleDateFormat("HH:mm:ss ");

      debugging = true;

      try
      {
         File log_file = new File(context.getExternalFilesDir(null), context.getString(R.string.intent_log_file));
         file = new FileOutputStream(log_file);
      }
      catch (Exception e)
         { file = null; }

      log("Logger: -> on");
   }

   private static void stop()
   {
      log("Logger: -> off");

      if ( file != null )
      {
         try { file.close(); }
         catch (Exception e) { }
         file = null;
      }

      debugging = false;
   }

   /* ********************************************************************
    * Logging methods...
    */

   public static void log(String msg)
   {
      if ( ! debugging || msg == null )
         return;

      msg = format.format(new Date()) + msg;

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

   private static void log_file(String msg)
   {
      if ( file != null )
         try
         {
            file.write((msg + "\n").getBytes());
            file.flush();
         } catch (Exception e) {}
   }

}
