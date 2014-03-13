package org.smblott.intentradio;

import java.io.File;
import java.io.FileOutputStream;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import android.text.TextUtils;

import android.content.Context;
import android.widget.Toast;
import android.util.Log;

public class Logger
{
   private static Context context = null;
   private static String name = null;
   private static boolean debugging = false;

   /* ********************************************************************
    * Initialisation...
    */

   public static void init(Context a_context)
   {
      context = a_context;
      name = context.getString(R.string.app_name);
   }

   /* ********************************************************************
    * Enable/disable...
    */

   public static void state(String s)
   {
      if ( s.equals("debug") || s.equals("yes") || s.equals("on") || s.equals("start") )
         { start(); return; }

      if ( s.equals("nodebug") || s.equals("no") || s.equals("off") || s.equals("stop") )
         { stop(); return; }

      Log.d(name, "Logger: invalid state change: " + s);
   }

   /* ********************************************************************
    * State changes...
    */

   private static DateFormat format = null;
   private static FileOutputStream file = null;

   private static void start()
   {
      if ( debugging )
         return;

      debugging = true;

      if ( format == null )
         format = new SimpleDateFormat("HH:mm:ss ");

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

      debugging = false;

      if ( file != null )
      {
         try { file.close(); }
         catch (Exception e) { }
         file = null;
      }
   }

   /* ********************************************************************
    * Public logging methods...
    */

   public static void log(String... msg)
   {
      if ( ! debugging || msg == null )
         return;

      String text = format.format(new Date()) + TextUtils.join("",msg);

      Log.d(name, text);
      log_file(text);
   }

   public static void toast(String msg)
   {
      if ( msg == null )
         return;

      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
      log(msg);
   }

   /* ********************************************************************
    * Private logging method...
    */

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
