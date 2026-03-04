package win.morodirule.eduzscl.teaching;

import dev.latvian.mods.rhino.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class RhinoContext {
    private static final Logger LOGGER = LoggerFactory.getLogger("RhinoContext");
    private static final int MAX_OPERATIONS = 1000;
    
    private final AtomicInteger operationCount = new AtomicInteger(0);
    private final Context runtime;
    
    public RhinoContext() {
        runtime = new Context(new ContextFactory());
    }
    
    public Context getRuntime() {
        return runtime;
    }
    
    public boolean incrementOperations() {
        return operationCount.incrementAndGet() <= MAX_OPERATIONS;
    }
    
    public int getOperationCount() {
        return operationCount.get();
    }
    
    public void resetOperations() {
        operationCount.set(0);
    }
    
    public Object execute(String code, Scriptable scope) {
        try {
            resetOperations();
            
            if (scope == null) {
                scope = runtime.initStandardObjects();
                setupGlobals(scope);
            }
            
            Object result = runtime.evaluateString(
                scope,
                code,
                "<player>",
                1,
                null
            );
            
            return result != Context.getUndefinedValue() ? result : null;
            
        } catch (RhinoException e) {
            String msg = e.getMessage();
            if (e.lineNumber() > 0) {
                msg += " (line " + e.lineNumber();
                if (e.columnNumber() > 0) {
                    msg += ", col " + e.columnNumber();
                }
                msg += ")";
            }
            throw new ScriptExecutionException(msg);
        } catch (Exception e) {
            throw new ScriptExecutionException(e.getMessage());
        }
    }
    
     public void setupGlobals(Scriptable scope) {
         ScriptableObject.putProperty(scope, "mod", new ModAPI(this), runtime);
         ScriptableObject.putProperty(scope, "player", new PlayerAPI(), runtime);
         ScriptableObject.putProperty(scope, "console", new ConsoleAPI(), runtime);
     }
    
    public Scriptable createScope() {
        Context cx = new Context(new ContextFactory());
        Scriptable scope = cx.initStandardObjects();
        setupGlobals(scope);
        return scope;
    }
    
    public static class ScriptExecutionException extends RuntimeException {
        public ScriptExecutionException(String message) {
            super(message);
        }
    }
}
