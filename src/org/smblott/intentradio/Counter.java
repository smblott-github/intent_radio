package org.smblott.intentradio;

public class Counter extends Logger
{
   private static volatile int counter = 1;

   public static int now()
      { return counter; }

   public static void time_passes()
      { counter += 1; }

   public static boolean still(int then)
   {
      boolean ok = (then == now());
      if ( ! ok )
         log("Counter: too late: then=", ""+then, " now=", ""+now());
      return ok;
   }
}

