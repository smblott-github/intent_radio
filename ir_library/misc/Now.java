package org.smblott.intentradio;

import android.os.AsyncTask;
import java.lang.Thread;

public abstract class Now extends AsyncTask<Object, Void, Void>
{
   Now()
      { super(); }

   public abstract void now();

   protected Void doInBackground(Integer... args)
      { return null; }

   protected void onPostExecute(Void ignored)
   {
      if ( ! isCancelled() )
         now();
   }

   public AsyncTask<Object,Void,Void> start()
      { return executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); }
}
