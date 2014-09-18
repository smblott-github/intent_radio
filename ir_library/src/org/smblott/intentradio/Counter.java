package org.smblott.intentradio;

public class Counter extends Logger
{
   private static volatile int counter = 1;

   public static int now()
      { return counter; }

   public static void time_passes()
      { counter += 1; }

   public static boolean still(int then)
      { return then == now(); }
}

