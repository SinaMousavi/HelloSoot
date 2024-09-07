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
    public static int dynamicCounter = 0;

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

            /* we can change the second input of this method to count any kind of Jimple stmts for example instead of
            GotoStmt.class just put IfStmt.class */
            instrumentGotoStatements(circleClass, GotoStmt.class);


            System.out.println("Number of gotos in execution:" + counter);
            System.out.println("Number of dynamicCounter in execution:" + dynamicCounter);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1); // Exit with error code 1
        }
    }

    private static SootClass reportSootClassInfo() {
        SootClass circleClass = Scene.v().getSootClass(circleClassName);
        return circleClass;
    }

    private static void instrumentGotoStatements(SootClass circleClass, Class inputStmt) {
        try {
            SootField gotoCounterField = new SootField("gotoCounter", IntType.v(), Modifier.PUBLIC | Modifier.STATIC);
            circleClass.addField(gotoCounterField);

            for (SootMethod method : circleClass.getMethods()) {
                if (method.isConcrete()) {
                    JimpleBody body = (JimpleBody) method.retrieveActiveBody();
                    PatchingChain<Unit> units = body.getUnits();

                    List<GotoStmt> gotoStmts = new ArrayList<>();
                    List<Unit> stmts = new ArrayList<>();
                    for (Unit unit : units) {
                        if (unit instanceof GotoStmt) {
                            gotoStmts.add((GotoStmt) unit);
                        }
                        if (inputStmt.isInstance(unit)) {
                            stmts.add(unit);
                        }
                    }
//this fragmentation of code is the previous one which only counts the gotoStatements
                    for (GotoStmt stmt : gotoStmts) {
                        addStatements(gotoCounterField, body, units, stmt);
                        for (Unit x : units) {
                            if (x instanceof GotoStmt) {
                                counter++;
                            }
                        }
                    }
/* this fragmentation of the code can count all the statements type like gotoStmts, ifStmts, returnVoidStmts, etc. dynamically
by just adjusting the input of the method in main */
                    for (Unit stmt : stmts) {
                        addStatements(gotoCounterField, body, units, stmt);
                        for (Unit x : units) {
                            if (inputStmt.isInstance(x)) {
                                dynamicCounter++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addStatements(SootField counterField, JimpleBody body, PatchingChain<Unit> units, Unit stmt) {
        SootFieldRef gotoCounterFieldRef = counterField.makeRef();
        Local tmpLocal = Jimple.v().newLocal("tmpCounter", IntType.v());
        body.getLocals().add(tmpLocal);

        AssignStmt readCounter = Jimple.v().newAssignStmt(tmpLocal, Jimple.v().newStaticFieldRef(gotoCounterFieldRef));
        AssignStmt incrementCounter = Jimple.v().newAssignStmt(tmpLocal, Jimple.v().newAddExpr(tmpLocal, IntConstant.v(1)));
        AssignStmt writeCounter = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(gotoCounterFieldRef), tmpLocal);

        units.insertBefore(readCounter, stmt);
        units.insertBefore(incrementCounter, stmt);
        units.insertBefore(writeCounter, stmt);
    }
}
