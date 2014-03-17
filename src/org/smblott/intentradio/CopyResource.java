package org.smblott.intentradio;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CopyResource extends Logger
{
   // source: http://stackoverflow.com/questions/8664468/copying-raw-file-into-sdcard
   //
   public void copy(Context context, int id, String path)
   {
      byte[] buff = new byte[1024];
      int read = 0;

      InputStream in = null;
      FileOutputStream out = null;

      File file = new File(path);
      File directory = new File(file.getParent());

      if ( ! directory.isDirectory() )
      {
         toast("Parent directory does not exist:\n" + file.getParent());
         return;
      }

      try
      {
         in = context.getResources().openRawResource(id);
         out = new FileOutputStream(path);

         while ((read = in.read(buff)) > 0)
            out.write(buff, 0, read);
      }
      catch ( Exception e) {}
      finally
      {
         try
         {
            toast("Failed to copy file:\n" + path);
            if ( in  != null ) in.close();
            if ( out != null ) out.close();
         }
         catch ( Exception e) {}
      }
   }
}
