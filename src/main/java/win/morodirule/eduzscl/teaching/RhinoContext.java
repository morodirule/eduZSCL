package win.morodirule.eduzscl.teaching;

import dev.latvian.mods.rhino.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import win.morodirule.eduzscl.api.ConsoleAPI;
import win.morodirule.eduzscl.api.ModAPI;
import win.morodirule.eduzscl.api.PlayerAPI;
import win.morodirule.eduzscl.teaching.TeachingAgent.Lesson;

import java.util.concurrent.atomic.AtomicInteger;

public class RhinoContext {
    private static final Logger LOGGER = LoggerFactory.getLogger("RhinoContext");
    private static final int DEFAULT_MAX_OPERATIONS = 1000;
    
    private final AtomicInteger operationCount = new AtomicInteger(0);
    private final Context runtime;
    private final int maxOperations;
    private Lesson currentLesson;
    
    public RhinoContext() {
        this(null);
    }
    
    public RhinoContext(Lesson lesson) {
        this.runtime = new Context(new ContextFactory());
        this.currentLesson = lesson;
        this.maxOperations = lesson != null ? lesson.getMaxOperations() : DEFAULT_MAX_OPERATIONS;
    }
    
    public Context getRuntime() {
        return runtime;
    }
    
    public boolean incrementOperations() {
        return operationCount.incrementAndGet() <= maxOperations;
    }
    
    public int getOperationCount() {
        return operationCount.get();
    }
    
    public int getMaxOperations() {
        return maxOperations;
    }
    
    public Lesson getCurrentLesson() {
        return currentLesson;
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
