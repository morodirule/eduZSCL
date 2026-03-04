package win.morodirule.eduzscl.teaching;

import net.minecraft.server.level.ServerPlayer;
import dev.latvian.mods.rhino.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeachingAgent {
    private static final Logger LOGGER = LoggerFactory.getLogger("TeachingAgent");
    
    private static final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();
    private static volatile ServerPlayer currentPlayer = null;
    private static final List<Lesson> lessons = new ArrayList<>();
    
    static {
        lessons.add(new Lesson001_HelloWorld());
    }
    
    public static void setCurrentPlayer(ServerPlayer player) {
        currentPlayer = player;
    }
    
    public static ServerPlayer getCurrentPlayer() {
        return currentPlayer;
    }
    
    public static void onPlayerJoin(ServerPlayer player) {
        sessions.putIfAbsent(player.getUUID(), new PlayerSession(player.getUUID()));
        sendMessageToPlayer(player, "§aWelcome to eduZSCL! Type '/js help' to start learning JavaScript.");
    }
    
    public static void handleCommand(ServerPlayer player, String command) {
        LOGGER.info("handleCommand called with: {}", command);
        
        if (command.startsWith("/js ")) {
            String code = command.substring(4).trim();
            LOGGER.info("Executing code: {}", code);
            executeCode(player, code);
        } else if (command.equals("/js") || command.equals("/js help")) {
            showHelp(player);
        } else {
            sendMessageToPlayer(player, "§cUnknown command. Try /js help");
        }
    }
    
    public static void showHelp(ServerPlayer player) {
        sendMessageToPlayer(player, "§6=== JavaScript Teaching Agent ===");
        sendMessageToPlayer(player, "§e/js <code> - Run JavaScript code");
        sendMessageToPlayer(player, "§e/js help - Show this help");
        sendMessageToPlayer(player, "§e/js lesson - Start current lesson");
        sendMessageToPlayer(player, "§e/js hint - Get a hint");
        sendMessageToPlayer(player, "§e/js progress - Show your progress");
        sendMessageToPlayer(player, "§6================================");
    }
    
    public static void executeCode(ServerPlayer player, String code) {
        setCurrentPlayer(player);
        
        try {
            RhinoContext rhino = new RhinoContext();
            Scriptable scope = rhino.createScope();
            
            Object result = rhino.execute(code, scope);
            
            if (result != null) {
                sendMessageToPlayer(player, "§7=> " + rhino.getRuntime().toString(result));
            }
            
        } catch (Exception e) {
            sendMessageToPlayer(player, "§cError: " + e.getMessage());
            LOGGER.error("JS Execution error for {}: {}", player.getName().getString(), e.getMessage());
        }
    }
    
    public static void sendMessage(String message) {
        if (currentPlayer != null) {
            sendMessageToPlayer(currentPlayer, message);
        }
    }
    
    public static void sendMessageToPlayer(ServerPlayer player, String message) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
    }
    
    public static PlayerSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }
    
    public static List<Lesson> getLessons() {
        return lessons;
    }
    
    public static class PlayerSession {
        private final UUID playerId;
        private int currentLessonIndex = 0;
        private int hintsUsed = 0;
        private final List<String> completedLessons = new ArrayList<>();
        
        public PlayerSession(UUID playerId) {
            this.playerId = playerId;
        }
        
        public UUID getPlayerId() { return playerId; }
        public int getCurrentLessonIndex() { return currentLessonIndex; }
        public void setCurrentLessonIndex(int index) { this.currentLessonIndex = index; }
        public int getHintsUsed() { return hintsUsed; }
        public void incrementHints() { this.hintsUsed++; }
        public void resetHints() { this.hintsUsed = 0; }
        public List<String> getCompletedLessons() { return completedLessons; }
        public void completeLesson(String lessonId) { completedLessons.add(lessonId); }
    }
    
    public interface Lesson {
        String getId();
        String getTitle();
        String getGoal();
        String getExample();
        String getExplanation();
        PracticeTask[] getTasks();
        Hint[] getHints();
        String getVerification();
        String getNextStep();
    }
    
    public record PracticeTask(String type, String prompt, String expectedOutcome) {}
    public record Hint(String text) {}
}
