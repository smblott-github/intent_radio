package org.smblott.intentradio;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CopyResource extends Logger
{
   /* Install raw file resource id into location path on the SD card.
    *
    * Returns the full path on success (so the first character is '/'), or an
    * error message on failure.
    */

   public static String copy(Context context, int id, String path)
   {
      log("CopyResource id: ", ""+id);
      log("CopyResource path: ", path);

      byte[] buffer = new byte[1024];
      int count = 0;
      boolean success = true;

      InputStream input = null;
      FileOutputStream output = null;

      log("CopyResource SD card: ", Environment.getExternalStorageState(), ".");
      File sdcard = Environment.getExternalStorageDirectory();
      if ( sdcard == null || ! Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) )
         { return "Error:\nSD card not found or not ready."; }

      path = sdcard.getAbsolutePath() + "/" + path;
      log("CopyResource full path: ", path);

      File file = new File(path);
      File directory = new File(file.getParent());

      if ( ! directory.isDirectory() )
         { return "Error:\nParent directory does not exist...\n\n" + file.getParent(); }

      if ( file.exists() )
         { return "Error:\nFile already exists, not copied...\n\n" + path; }

      try
      {
         input = context.getResources().openRawResource(id);
         output = new FileOutputStream(path);

         while ( 0 < (count = input.read(buffer)) )
            output.write(buffer, 0, count);
      }
      catch ( Exception e)
      {
         success = false;
      }
      finally
      {
         try
         {
            if ( input  != null ) input.close();
            if ( output != null ) output.close();
         }
         catch ( Exception e) {}
      }

      return success ? path : "Unknown error.";
   }
}
