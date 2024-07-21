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

    public static int counter = 0;

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
            instrumentGotoStatements(circleClass);
            System.out.println("Number of gotos in execution:" + counter);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1); // Exit with error code 1
        }
    }

    private static SootClass reportSootClassInfo() {
        SootClass circleClass = Scene.v().getSootClass(circleClassName);
        return circleClass;
    }

    private static void instrumentGotoStatements(SootClass circleClass) {
        try {
            SootField gotoCounterField = new SootField("gotoCounter", IntType.v(), Modifier.PUBLIC | Modifier.STATIC);
            circleClass.addField(gotoCounterField);

            for (SootMethod method : circleClass.getMethods()) {
                if (method.isConcrete()) {
                    JimpleBody body = (JimpleBody) method.retrieveActiveBody();
                    PatchingChain<Unit> units = body.getUnits();

                    // Collect GotoStmt units to avoid concurrent modification
                    List<GotoStmt> gotoStmts = new ArrayList<>();
                    for (Unit unit : units) {
                        if (unit instanceof GotoStmt) {
                            gotoStmts.add((GotoStmt) unit);
                        }
                    }

                    for (GotoStmt stmt : gotoStmts) {
                        SootFieldRef gotoCounterFieldRef = gotoCounterField.makeRef();
                        Local tmpLocal = Jimple.v().newLocal("tmpCounter", IntType.v());
                        body.getLocals().add(tmpLocal);

                        AssignStmt readCounter = Jimple.v().newAssignStmt(tmpLocal, Jimple.v().newStaticFieldRef(gotoCounterFieldRef));
                        AssignStmt incrementCounter = Jimple.v().newAssignStmt(tmpLocal, Jimple.v().newAddExpr(tmpLocal, IntConstant.v(1)));
                        AssignStmt writeCounter = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(gotoCounterFieldRef), tmpLocal);

                        units.insertBefore(readCounter, stmt);
                        units.insertBefore(incrementCounter, stmt);
                        units.insertBefore(writeCounter, stmt);
                        for (Unit x : units) {
                            if (x instanceof GotoStmt) {
                                counter++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
