package org.smblott.intentradio;

import android.os.AsyncTask;
import java.lang.Thread;

public abstract class Later extends AsyncTask<Integer, Void, Void>
{
   private static final int default_seconds = 5;
   private int seconds = default_seconds;
   private int then;

   Later(int secs)
   {
      super();
      seconds = 0 < secs ? secs : default_seconds;
      then = Counter.now();
   }

   Later()
      { this(default_seconds); }

   public abstract void later();

   protected Void doInBackground(Integer... args)
   {
      try { Thread.sleep(seconds * 1000); }
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
