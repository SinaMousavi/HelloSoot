package org.soot.startingSoot.helloSoot;

import soot.*;
import soot.jimple.*;
import soot.options.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HelloSoot {

    public static String sourceDirectory = System.getProperty("user.dir") + File.separator + "demo" + File.separator + "Circle";
    public static String circleClassName = "Circle";
    public static int gotoCounter = 0;

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
        try {
            setupSoot();
            SootClass circleClass = reportSootClassInfo();
            gotoStatementsCollector(circleClass);
            System.out.println("The number of goto statements = " + gotoCounter);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1); // Exit with error code 1
        }
    }

    private static SootClass reportSootClassInfo() {
        SootClass circleClass = Scene.v().getSootClass(circleClassName);
        return circleClass;
    }

    private static void gotoStatementsCollector(SootClass circleClass) {
        try {
            for (SootMethod method : circleClass.getMethods()) {
                if (method.isConcrete()) {
                    JimpleBody body = (JimpleBody) method.retrieveActiveBody();
                    PatchingChain<Unit> units = body.getUnits();

                    // Collect GotoStmt units to avoid concurrent modification
                    List<GotoStmt> gotoStmts = new ArrayList<>();
                    for (Unit unit : units) {
                        if (unit instanceof GotoStmt) {
                            gotoStmts.add((GotoStmt) unit);
                            gotoCounter++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
