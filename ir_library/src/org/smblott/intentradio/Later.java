package org.smblott.intentradio;

import android.os.AsyncTask;
import java.lang.Thread;

public abstract class Later extends AsyncTask<Integer, Void, Void>
{
   private static final int default_seconds = 120;
   private int seconds = default_seconds;
   private int then;

   // secs <  0: execute immediately
   // secs == 0: delay for default_seconds
   // otherwise: delay for secs
   //
   Later(int secs)
   {
      super();
      if ( secs == 0 )
         secs = default_seconds;
      seconds = secs;
      then = Counter.now();
   }

   Later()
      { this(default_seconds); }

   public abstract void later();

   protected Void doInBackground(Integer... args)
   {
      try { if ( 0 < seconds ) Thread.sleep(seconds * 1000); }
      catch ( Exception e ) { }
      return null;
   }

   protected void onPostExecute(Void ignored)
   {
      if ( ! isCancelled() && Counter.still(then) )
         later();
   }

   public AsyncTask<Integer,Void,Void> start()
      { return executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); }
}
