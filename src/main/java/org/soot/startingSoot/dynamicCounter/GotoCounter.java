package org.soot.startingSoot.dynamicCounter;

import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.Iterator;
import java.util.Map;

public class GotoCounter {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Syntax: java org.soot.startingSoot.dynamicCounter.Main [soot options]");
            System.exit(0);
        }

        PackManager.v().getPack("jtp").add(new Transform("jtp.instrumenter", GotoCounterTransformer.v()));

        // Just in case, resolve the PrintStream and System SootClasses.
        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);
        soot.Main.main(args);
    }
}


class GotoCounterTransformer extends BodyTransformer {

    // Singleton instance
    private static final GotoCounterTransformer instance = new GotoCounterTransformer();
    private boolean isMainFieldAdded = false;
    private SootClass printStreamClass;

    // Private constructor for singleton pattern
    private GotoCounterTransformer() {
    }

    // Get the singleton instance
    public static GotoCounterTransformer v() {
        return instance;
    }

    // Create a temporary reference for PrintStream
    private Local createPrintStreamLocal(Body body) {
        Local tempRef = Jimple.v().newLocal("printStreamRef", RefType.v("java.io.PrintStream"));
        body.getLocals().add(tempRef);
        return tempRef;
    }

    // Create a temporary long variable
    private Local createLongLocal(Body body) {
        Local tempLong = Jimple.v().newLocal("counterLong", LongType.v());
        body.getLocals().add(tempLong);
        return tempLong;
    }

    // Insert statements to log the goto counter
    private void insertLogStatements(Chain<Unit> units, Stmt stmt, SootField counterField, Local printStreamRef, Local counterLong) {
        // Insert statement: printStreamRef = System.out;
        units.insertBefore(Jimple.v().newAssignStmt(printStreamRef, Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), stmt);

        // Insert statement: counterLong = gotoCounter;
        units.insertBefore(Jimple.v().newAssignStmt(counterLong, Jimple.v().newStaticFieldRef(counterField.makeRef())), stmt);

        // Insert statement: printStreamRef.println(counterLong);
        SootMethod printlnMethod = printStreamClass.getMethod("void println(long)");
        units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(printStreamRef, printlnMethod.makeRef(), counterLong)), stmt);
    }

    // Main transformation logic
    @Override
    protected void internalTransform(Body body, String phase, Map<String, String> options) {
        SootClass declaringClass = body.getMethod().getDeclaringClass();
        SootField gotoCounterField = null;
        boolean localsInitialized = false;
        Local printStreamRef = null, counterLong = null;
        Chain<Unit> units = body.getUnits();

        // Ensure the main class contains a goto counter field and load PrintStream class
        synchronized (this) {
            if (!Scene.v().getMainClass().declaresMethod("void main(java.lang.String[])")) {
                throw new RuntimeException("Main method not found in the specified class.");
            }

            if (isMainFieldAdded) {
                gotoCounterField = Scene.v().getMainClass().getFieldByName("gotoCount");
            } else {
                // Add the goto counter field
                gotoCounterField = new SootField("gotoCount", LongType.v(), Modifier.STATIC);
                Scene.v().getMainClass().addField(gotoCounterField);

                // Load the PrintStream class
                printStreamClass = Scene.v().getSootClass("java.io.PrintStream");

                isMainFieldAdded = true;
            }
        }

        // Instrument code to increment the goto counter whenever a goto statement is encountered
        boolean isMainMethod = body.getMethod().getSubSignature().equals("void main(java.lang.String[])");

        Local tempCounter = Jimple.v().newLocal("tempCounter", LongType.v());
        body.getLocals().add(tempCounter);

        Iterator<Unit> stmtIterator = units.snapshotIterator();

        while (stmtIterator.hasNext()) {
            Stmt stmt = (Stmt) stmtIterator.next();

            if (stmt instanceof GotoStmt) {
                // Insert statements to increment the goto counter
                units.insertBefore(Jimple.v().newAssignStmt(tempCounter, Jimple.v().newStaticFieldRef(gotoCounterField.makeRef())), stmt);
                units.insertBefore(Jimple.v().newAssignStmt(tempCounter, Jimple.v().newAddExpr(tempCounter, LongConstant.v(1L))), stmt);
                units.insertBefore(Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(gotoCounterField.makeRef()), tempCounter), stmt);
            } else if (stmt instanceof InvokeStmt) {
                // Check for System.exit() calls and log the counter before exiting
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                if (invokeExpr instanceof StaticInvokeExpr) {
                    SootMethod targetMethod = invokeExpr.getMethod();
                    if (targetMethod.getSignature().equals("<java.lang.System: void exit(int)>")) {
                        if (!localsInitialized) {
                            printStreamRef = createPrintStreamLocal(body);
                            counterLong = createLongLocal(body);
                            localsInitialized = true;
                        }
                        insertLogStatements(units, stmt, gotoCounterField, printStreamRef, counterLong);
                    }
                }
            } else if (isMainMethod && (stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt)) {
                // Log the counter before returning from the main method
                if (!localsInitialized) {
                    printStreamRef = createPrintStreamLocal(body);
                    counterLong = createLongLocal(body);
                    localsInitialized = true;
                }
                insertLogStatements(units, stmt, gotoCounterField, printStreamRef, counterLong);
            }
        }
    }
}
