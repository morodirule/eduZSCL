package win.morodirule.eduzscl.teaching;

public class ConsoleAPI {
    
    public void log(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(arg != null ? arg.toString() : "null");
        }
        TeachingAgent.sendMessage("§7" + sb.toString());
    }
    
    public void error(Object... args) {
        StringBuilder sb = new StringBuilder("§c[ERROR] ");
        for (Object arg : args) {
            if (sb.length() > 9) sb.append(" ");
            sb.append(arg != null ? arg.toString() : "null");
        }
        TeachingAgent.sendMessage(sb.toString());
    }
    
    public void warn(Object... args) {
        StringBuilder sb = new StringBuilder("§e[WARN] ");
        for (Object arg : args) {
            if (sb.length() > 9) sb.append(" ");
            sb.append(arg != null ? arg.toString() : "null");
        }
        TeachingAgent.sendMessage(sb.toString());
    }
    
    public void clear() {
    }
}
