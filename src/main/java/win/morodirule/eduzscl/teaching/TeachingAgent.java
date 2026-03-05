package win.morodirule.eduzscl.teaching;

import net.minecraft.server.level.ServerPlayer;
import dev.latvian.mods.rhino.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import win.morodirule.eduzscl.teaching.lessons.Lesson001_HelloWorld;

public class TeachingAgent {
    private static final Logger LOGGER = LoggerFactory.getLogger("TeachingAgent");
    
    private static final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();
    private static volatile ServerPlayer currentPlayer = null;
    private static final List<Lesson> lessons = new ArrayList<>();
    
    static {
        Lesson001_HelloWorld lesson001 = new Lesson001_HelloWorld();
        lessons.add(lesson001);
        LessonManager.registerLesson(lesson001);
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
        
        Lesson lesson = LessonManager.getPlayerLesson(player.getUUID());
        if (lesson == null) {
            lesson = LessonManager.getDefaultLesson();
        }
        
        try {
            RhinoContext rhino = new RhinoContext(lesson);
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
        default int getMaxOperations() { return 100; }
        default Tip[] getTips() { return new Tip[0]; }
    }
    
    public record Tip(String text, String category) {
        public Tip(String text) { this(text, "general"); }
    }
    
    public record PracticeTask(String type, String prompt, String expectedOutcome) {}
    public record Hint(String text) {}
    
    public static class LessonManager {
        private static final Map<String, Lesson> lessonRegistry = new ConcurrentHashMap<>();
        
        public static void registerLesson(Lesson lesson) {
            lessonRegistry.put(lesson.getId(), lesson);
            LOGGER.info("Registered lesson: {} - {}", lesson.getId(), lesson.getTitle());
        }
        
        public static Lesson getLesson(String id) {
            return lessonRegistry.get(id);
        }
        
        public static List<Lesson> getAllLessons() {
            return new ArrayList<>(lessonRegistry.values());
        }
        
        public static void setPlayerLesson(UUID playerId, String lessonId) {
            PlayerSession session = sessions.get(playerId);
            if (session != null) {
                session.setCurrentLessonId(lessonId);
            }
        }
        
        public static Lesson getPlayerLesson(UUID playerId) {
            PlayerSession session = sessions.get(playerId);
            if (session != null && session.getCurrentLessonId() != null) {
                return lessonRegistry.get(session.getCurrentLessonId());
            }
            return null;
        }
        
        public static Lesson getDefaultLesson() {
            return lessonRegistry.isEmpty() ? null : lessonRegistry.values().iterator().next();
        }
    }
    
    public static class PlayerSession {
        private final UUID playerId;
        private int currentLessonIndex = 0;
        private String currentLessonId = null;
        private int hintsUsed = 0;
        private final List<String> completedLessons = new ArrayList<>();
        
        public PlayerSession(UUID playerId) {
            this.playerId = playerId;
        }
        
        public UUID getPlayerId() { return playerId; }
        public int getCurrentLessonIndex() { return currentLessonIndex; }
        public void setCurrentLessonIndex(int index) { this.currentLessonIndex = index; }
        public String getCurrentLessonId() { return currentLessonId; }
        public void setCurrentLessonId(String id) { this.currentLessonId = id; }
        public int getHintsUsed() { return hintsUsed; }
        public void incrementHints() { this.hintsUsed++; }
        public void resetHints() { this.hintsUsed = 0; }
        public List<String> getCompletedLessons() { return completedLessons; }
        public void completeLesson(String lessonId) { completedLessons.add(lessonId); }
    }
}
