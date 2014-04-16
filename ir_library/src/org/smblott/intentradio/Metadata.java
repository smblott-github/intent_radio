package org.smblott.intentradio;

import android.content.Context;
import android.os.AsyncTask;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

public class Metadata extends AsyncTask<Void, Void, String>
{
   private int then = 0;
   private Context context = null;
   private String url = null;

   Metadata(Context a_context, String a_url)
   {
      super();
      then = Counter.now();
      context = a_context;
      url = a_url;
      log("Metadata: then=" + then);
   }

   public void start()
      { executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); }

   @Override
   protected String doInBackground(Void... args)
   {
      try
      {
         log("Metadata start: ", url);
         MediaMetadataRetriever retriever = new MediaMetadataRetriever();
         log("Metadata 1.");
         retriever.setDataSource(context,Uri.parse(url));
         // FIXME:
         // This is broken!
         // Never reaches here!
         log("Metadata 2.");
         String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
         log("Metadata done.");
         return title != null ? title : null;
      }
      catch (Exception e)
         { return null; }
   }

   @Override
   protected void onPostExecute(String title)
   {
      if ( title != null && ! isCancelled() && Counter.still(then) )
      {
         Notify.name("XX" + title);
         Notify.note();
      }
   }

   /* ********************************************************************
    * Logging...
    */

   private static void log(String... msg)
      { Logger.log(msg); }
}
