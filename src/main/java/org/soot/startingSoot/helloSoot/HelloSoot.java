package org.soot.startingSoot.helloSoot;


import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.options.Options;

import java.io.File;
import java.util.Collections;

public class HelloSoot {

    public static String sourceDirectory = System.getProperty("user.dir") + File.separator + "demo" + File.separator + "Circle";
    public static String circleClassName = "Circle";

    public static void setupSoot() {
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(sourceDirectory);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_process_dir(Collections.singletonList(sourceDirectory));
        Options.v().set_whole_program(true);
        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();
    }

    public static void main(String[] args) {
        setupSoot();
        SootClass circleClass = reportSootClassInfo();
        SootMethod areaMethod = reportSootMethodInfo(circleClass);
        JimpleBody body = (JimpleBody) areaMethod.getActiveBody();
        int c = 0;
        for (Unit u : body.getUnits()) {
            c++;
            Stmt stmt = (Stmt) u;
            System.out.println(String.format("(%d): %s", c, stmt));
        }
        for (Trap trap : body.getTraps()) {
            System.out.println(trap);
        }
    }

    private static SootClass reportSootClassInfo() {
        SootClass circleClass = Scene.v().getSootClass(circleClassName);
        return circleClass;
    }

    private static SootMethod reportSootMethodInfo(SootClass circleClass) {
        System.out.println("-----sootMethod-----");
        System.out.println(String.format("List of %s's methods:", circleClass.getName()));
        for (SootMethod sootMethod : circleClass.getMethods())
            System.out.println(String.format("- %s", sootMethod.getName()));
        SootMethod getCircleCountMethod = circleClass.getMethod("int getCircleCount()");
        System.out.println(String.format("Method Name: %s", getCircleCountMethod.getName()));
        return circleClass.getMethod("int area(boolean)");
    }
}
