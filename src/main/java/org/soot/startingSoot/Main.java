package org.soot.startingSoot;

import org.soot.startingSoot.dynamicCounter.GotoCounter;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        String[] restOfTheArgs = Arrays.copyOfRange(args, 0, args.length);
        if (args[0].equals("Circle"))
           GotoCounter.main(restOfTheArgs);
    }
}
