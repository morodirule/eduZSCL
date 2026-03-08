package win.morodirule.eduzscl.teaching;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class JsonLessonLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("JsonLessonLoader");
    private static final String EXAMPLE_FILE = "lesson_example.json";

    private static final String DEFAULT_EXAMPLE = """
{
  "id": "intro_json_01",
  "title": "JSON Lesson Example",
  "goal": "Print text and move the agent one step",
  "example": "console.log(\\"Hello from JSON lesson!\\");\\nagent.move(1);",
  "explanation": "This lesson is loaded from config/eduzscl/lessons/*.json",
  "verification": "A message appears and the agent moves",
  "nextStep": "Add more API calls to allowedApiCalls",
  "maxOperations": 50,
  "tasks": [
    {
      "type": "guided",
      "prompt": "Run the example code",
      "expectedOutcome": "Agent moves and message is printed"
    }
  ],
  "hints": [
    { "text": "Use console.log(\\"text\\") for chat output" }
  ],
  "tips": [
    { "text": "Autocomplete follows allowedApiCalls", "category": "editor" }
  ],
  "allowedApiCalls": [
    "console.log",
    "agent.move",
    "agent.turnLeft"
  ]
}
""";

    private JsonLessonLoader() {
    }

    public static List<TeachingAgent.Lesson> loadLessons() {
        Path lessonsDir = getLessonsDir();
        ensureFolderAndExample(lessonsDir);

        List<TeachingAgent.Lesson> lessons = new ArrayList<>();
        if (!Files.isDirectory(lessonsDir)) {
            return lessons;
        }

        try (Stream<Path> stream = Files.list(lessonsDir)) {
            stream
                .filter(path -> path.toString().endsWith(".json"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .forEach(path -> {
                    TeachingAgent.Lesson lesson = readLesson(path);
                    if (lesson != null) {
                        lessons.add(lesson);
                    }
                });
        } catch (IOException e) {
            LOGGER.error("Failed to list JSON lessons from {}: {}", lessonsDir, e.getMessage());
        }

        return lessons;
    }

    private static Path getLessonsDir() {
        try {
            return FMLPaths.CONFIGDIR.get().resolve("eduzscl").resolve("lessons");
        } catch (Throwable ignored) {
            return Path.of("config", "eduzscl", "lessons");
        }
    }

    private static void ensureFolderAndExample(Path lessonsDir) {
        try {
            Files.createDirectories(lessonsDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create lesson directory {}: {}", lessonsDir, e.getMessage());
            return;
        }

        try (Stream<Path> stream = Files.list(lessonsDir)) {
            boolean hasJson = stream.anyMatch(path -> path.toString().endsWith(".json"));
            if (!hasJson) {
                Path examplePath = lessonsDir.resolve(EXAMPLE_FILE);
                Files.writeString(examplePath, DEFAULT_EXAMPLE);
                LOGGER.info("Created example JSON lesson: {}", examplePath);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to ensure example JSON lesson in {}: {}", lessonsDir, e.getMessage());
        }
    }

    private static TeachingAgent.Lesson readLesson(Path path) {
        try {
            String content = Files.readString(path);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            return parseLesson(root, path);
        } catch (IOException | JsonParseException | IllegalStateException e) {
            LOGGER.error("Failed to parse lesson JSON {}: {}", path, e.getMessage());
            return null;
        }
    }

    private static TeachingAgent.Lesson parseLesson(JsonObject root, Path path) {
        String id = getRequiredString(root, "id", path);
        String title = getRequiredString(root, "title", path);
        if (id == null || title == null) {
            return null;
        }

        String goal = getString(root, "goal", "");
        String example = getString(root, "example", "");
        String explanation = getString(root, "explanation", "");
        String verification = getString(root, "verification", "");
        String nextStep = getString(root, "nextStep", "");
        int maxOperations = getInt(root, "maxOperations", 100);

        TeachingAgent.PracticeTask[] tasks = parseTasks(root.getAsJsonArray("tasks"));
        TeachingAgent.Hint[] hints = parseHints(root.getAsJsonArray("hints"));
        TeachingAgent.Tip[] tips = parseTips(root.getAsJsonArray("tips"));
        List<TeachingAgent.LessonApiCall> allowedApiCalls = parseAllowedApiCalls(root);

        return new JsonLesson(
            id,
            title,
            goal,
            example,
            explanation,
            tasks,
            hints,
            verification,
            nextStep,
            maxOperations,
            tips,
            allowedApiCalls
        );
    }

    private static TeachingAgent.PracticeTask[] parseTasks(JsonArray array) {
        if (array == null) {
            return new TeachingAgent.PracticeTask[0];
        }

        List<TeachingAgent.PracticeTask> tasks = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            tasks.add(new TeachingAgent.PracticeTask(
                getString(obj, "type", "open"),
                getString(obj, "prompt", ""),
                getString(obj, "expectedOutcome", "")
            ));
        }
        return tasks.toArray(new TeachingAgent.PracticeTask[0]);
    }

    private static TeachingAgent.Hint[] parseHints(JsonArray array) {
        if (array == null) {
            return new TeachingAgent.Hint[0];
        }

        List<TeachingAgent.Hint> hints = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                hints.add(new TeachingAgent.Hint(element.getAsString()));
                continue;
            }
            if (element.isJsonObject()) {
                hints.add(new TeachingAgent.Hint(getString(element.getAsJsonObject(), "text", "")));
            }
        }
        return hints.toArray(new TeachingAgent.Hint[0]);
    }

    private static TeachingAgent.Tip[] parseTips(JsonArray array) {
        if (array == null) {
            return new TeachingAgent.Tip[0];
        }

        List<TeachingAgent.Tip> tips = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                tips.add(new TeachingAgent.Tip(element.getAsString()));
                continue;
            }
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                tips.add(new TeachingAgent.Tip(
                    getString(obj, "text", ""),
                    getString(obj, "category", "general")
                ));
            }
        }
        return tips.toArray(new TeachingAgent.Tip[0]);
    }

    private static List<TeachingAgent.LessonApiCall> parseAllowedApiCalls(JsonObject root) {
        if (root == null || !root.has("allowedApiCalls") || root.get("allowedApiCalls").isJsonNull()) {
            return null;
        }

        JsonArray array = root.getAsJsonArray("allowedApiCalls");
        if (array == null) {
            return List.of();
        }

        List<TeachingAgent.LessonApiCall> calls = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                String name = element.getAsString();
                if (!name.isBlank()) {
                    calls.add(new TeachingAgent.LessonApiCall(name));
                }
                continue;
            }
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                String name = getString(obj, "name", "");
                if (!name.isBlank()) {
                    calls.add(new TeachingAgent.LessonApiCall(
                        name,
                        getNullableString(obj, "insertText"),
                        getNullableString(obj, "description")
                    ));
                }
            }
        }
        return List.copyOf(calls);
    }

    private static String getRequiredString(JsonObject obj, String key, Path path) {
        String value = getNullableString(obj, key);
        if (value == null || value.isBlank()) {
            LOGGER.error("Missing required field '{}' in lesson JSON {}", key, path);
            return null;
        }
        return value;
    }

    private static String getString(JsonObject obj, String key, String fallback) {
        String value = getNullableString(obj, key);
        return value != null ? value : fallback;
    }

    private static String getNullableString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        JsonElement element = obj.get(key);
        return element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static int getInt(JsonObject obj, String key, int fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static final class JsonLesson implements TeachingAgent.Lesson {
        private final String id;
        private final String title;
        private final String goal;
        private final String example;
        private final String explanation;
        private final TeachingAgent.PracticeTask[] tasks;
        private final TeachingAgent.Hint[] hints;
        private final String verification;
        private final String nextStep;
        private final int maxOperations;
        private final TeachingAgent.Tip[] tips;
        private final List<TeachingAgent.LessonApiCall> allowedApiCalls;

        private JsonLesson(
            String id,
            String title,
            String goal,
            String example,
            String explanation,
            TeachingAgent.PracticeTask[] tasks,
            TeachingAgent.Hint[] hints,
            String verification,
            String nextStep,
            int maxOperations,
            TeachingAgent.Tip[] tips,
            List<TeachingAgent.LessonApiCall> allowedApiCalls
        ) {
            this.id = id;
            this.title = title;
            this.goal = goal;
            this.example = example;
            this.explanation = explanation;
            this.tasks = tasks;
            this.hints = hints;
            this.verification = verification;
            this.nextStep = nextStep;
            this.maxOperations = maxOperations;
            this.tips = tips;
            this.allowedApiCalls = allowedApiCalls;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getGoal() {
            return goal;
        }

        @Override
        public String getExample() {
            return example;
        }

        @Override
        public String getExplanation() {
            return explanation;
        }

        @Override
        public TeachingAgent.PracticeTask[] getTasks() {
            return tasks;
        }

        @Override
        public TeachingAgent.Hint[] getHints() {
            return hints;
        }

        @Override
        public String getVerification() {
            return verification;
        }

        @Override
        public String getNextStep() {
            return nextStep;
        }

        @Override
        public int getMaxOperations() {
            return maxOperations;
        }

        @Override
        public TeachingAgent.Tip[] getTips() {
            return tips;
        }

        @Override
        public List<TeachingAgent.LessonApiCall> getAllowedApiCalls() {
            return allowedApiCalls;
        }
    }
}
