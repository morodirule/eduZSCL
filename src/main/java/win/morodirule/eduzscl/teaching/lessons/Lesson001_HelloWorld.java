package win.morodirule.eduzscl.teaching.lessons;

import win.morodirule.eduzscl.teaching.TeachingAgent;
import win.morodirule.eduzscl.teaching.TeachingAgent.Tip;

public class Lesson001_HelloWorld implements TeachingAgent.Lesson {
    
    @Override
    public String getId() {
        return "intro_01";
    }
    
    @Override
    public String getTitle() {
        return "Hello World";
    }
    
    @Override
    public String getGoal() {
        return "Print a message and place a single stone block";
    }
    
    @Override
    public String getExample() {
        return """
// This is your first JavaScript!
// The console.log() function prints to chat
console.log("Hello, Minecraft!");

// Get your position
var pos = player.getPosition();
console.log("My position:", pos.x, pos.y, pos.z);

// Place a stone block 1 block in front of you
mod.placeBlock(Math.floor(pos.x) + 1, Math.floor(pos.y), Math.floor(pos.z), "stone");
""";
    }
    
    @Override
    public String getExplanation() {
        return """
Line 1: console.log() sends text to the in-game chat.
Line 4: player.getPosition() returns an object with x, y, z.
Line 5: We log each coordinate separated by commas.
Line 8: mod.placeBlock(x, y, z, "blockId") places a block at coordinates.
Math.floor() converts decimal numbers to whole numbers.
""";
    }
    
    @Override
    public TeachingAgent.PracticeTask[] getTasks() {
        return new TeachingAgent.PracticeTask[] {
            new TeachingAgent.PracticeTask("guided", 
                "Try running the example above. You should see a message in chat and a stone block should appear in front of you.",
                "Stone block placed, message in chat"),
            new TeachingAgent.PracticeTask("open",
                "Challenge: Change the code to place a different block type (try 'oak_planks' or 'dirt')",
                "Different block type placed")
        };
    }
    
    @Override
    public TeachingAgent.Hint[] getHints() {
        return new TeachingAgent.Hint[] {
            new TeachingAgent.Hint("Block IDs follow the pattern 'namespace:block' - try 'minecraft:stone' or just 'stone'"),
            new TeachingAgent.Hint("You can use Math.round() instead of Math.floor() to round numbers"),
            new TeachingAgent.Hint("To change direction, add to a different axis: z instead of x")
        };
    }
    
    @Override
    public String getVerification() {
        return "Check that stone block appears at player's position +1 on x axis";
    }
    
    @Override
    public String getNextStep() {
        return "Variables - store values and use them in loops";
    }
    
    @Override
    public int getMaxOperations() {
        return 50;
    }
    
    @Override
    public Tip[] getTips() {
        return new Tip[] {
            new Tip("Use Math.floor() to convert decimal coordinates to integers", "coordinates"),
            new Tip("Block IDs can be simple like 'stone' or full like 'minecraft:stone'", "blocks"),
            new Tip("Get your position with player.getPosition() before placing blocks", "player"),
            new Tip("Add to x for east/west, y for up/down, z for north/south", "coordinates")
        };
    }
}
