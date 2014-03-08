package org.smblott.intentradio;

import android.content.Intent;
import android.content.Context;

import android.os.AsyncTask;
import java.lang.Thread;

public class Later extends AsyncTask<Integer, Void, Void>
{
   private static final int default_seconds = 30;

   private Context context = null;
   private Intent intent = null;
   private int seconds = default_seconds;

   Later(Context ctx, Intent pintent, int secs)
   {
      context = ctx;
      intent = pintent;
      seconds = secs;
   }

   Later(Context ctx, Intent pintent)
   {
      this(ctx,pintent,default_seconds);
   }

   protected Void doInBackground(Integer... args)
   {
      try
      {
         Thread.sleep(seconds * 1000);
         if ( ! isCancelled() )
         {
            String msg = intent.hasExtra("action") ? intent.getStringExtra("action") : "something";
            log("Later: " + msg);
            context.startService(intent);
         }
      }
      catch ( Exception e ) { }

      return null;
   }

   /* ********************************************************************
    * Logging...
    */

   private void log(String msg)
      { Logger.log(msg); }

}
