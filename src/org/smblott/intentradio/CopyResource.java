package org.smblott.intentradio;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CopyResource extends Logger
{
   // source: http://stackoverflow.com/questions/8664468/copying-raw-file-into-sdcard
   //
   public static String copy(Context context, int id, String path)
   {
      log("CopyResource id: ", ""+id);
      log("CopyResource path: ", path);

      byte[] buff = new byte[1024];
      int read = 0;
      boolean success = true;

      InputStream in = null;
      FileOutputStream out = null;

      File sdcard = Environment.getExternalStorageDirectory();
      if ( sdcard == null )
         { toast_long("Error: SD card not found."); return null; }

      path = sdcard.getAbsolutePath() + "/" + path;
      log("CopyResource full path: ", path);

      File file = new File(path);
      File directory = new File(file.getParent());

      if ( ! directory.isDirectory() )
         { toast_long("Parent directory does not exist:\n" + file.getParent()); return null; }

      if ( file.exists() )
         { toast_long("File already exists, not copied.\n" + path); return null; }

      try
      {
         in = context.getResources().openRawResource(id);
         out = new FileOutputStream(path);

         while ((read = in.read(buff)) > 0)
            out.write(buff, 0, read);
      }
      catch ( Exception e)
      {
         success = false;
         toast_long("Failed to copy file:\n" + path);
      }
      finally
      {
         try
         {
            if ( in  != null ) in.close();
            if ( out != null ) out.close();
         }
         catch ( Exception e) {}
      }

      return success ? path : null;
   }
}
