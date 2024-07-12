package org.soot.startingSoot;

import org.soot.startingSoot.helloSoot.HelloSoot;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        String[] restOfTheArgs = Arrays.copyOfRange(args, 1, args.length);
        if (args[0].equals("Circle"))
            HelloSoot.main(restOfTheArgs);
    }
}
