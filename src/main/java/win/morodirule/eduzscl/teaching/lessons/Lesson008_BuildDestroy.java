package win.morodirule.eduzscl.teaching.lessons;

import win.morodirule.eduzscl.teaching.TeachingAgent;
import win.morodirule.eduzscl.teaching.TeachingAgent.Tip;

public class Lesson008_BuildDestroy implements TeachingAgent.Lesson {
    @Override
    public String getId() { return "build_destroy_1"; }

    @Override
    public String getTitle() { return "Buduj i niszcz"; }

    @Override
    public String getGoal() { return "Usuń przeszkodę, zbuduj most i na końcu połóż blok redstone."; }

    @Override
    public String getExample() {
        return """
// Usuń blok przed sobą, postaw 2 bloki, przejdź i połóż redstone
agent.remove();
agent.place(\"stone\");
agent.placeUp(\"stone\"); // przykład innego kierunku
agent.move(1);
agent.place(\"redstone_block\");
""";
    }

    @Override
    public String getExplanation() {
        return """
agent.remove() usuwa blok przed agentem, a agent.place() stawia nowy. Możesz budować mosty lub korytarze i kończyć redstone.
""";
    }

    @Override
    public TeachingAgent.PracticeTask[] getTasks() {
        return new TeachingAgent.PracticeTask[] {
            new TeachingAgent.PracticeTask("guided", "Usuń przeszkodę, postaw potrzebne bloki i zakończ redstone.", "Przejście zbudowane, redstone na mecie")
        };
    }

    @Override
    public TeachingAgent.Hint[] getHints() {
        return new TeachingAgent.Hint[] {
            new TeachingAgent.Hint("agent.removeDown() i agent.placeDown() pomogą przy lukach w podłodze."),
            new TeachingAgent.Hint("Upewnij się, że miejsce przed agentem jest puste zanim postawisz redstone.")
        };
    }

    @Override
    public String getVerification() { return "Przeszkoda usunięta, konstrukcja gotowa, redstone na końcu."; }

    @Override
    public String getNextStep() { return "Ostatnia lekcja: pełna swoboda."; }

    @Override
    public int getMaxOperations() { return 180; }

    @Override
    public Tip[] getTips() {
        return new Tip[] {
            new Tip("Sprawdzaj, czy blok można usunąć — niektóre materiały mogą być zablokowane.", "niszczenie"),
            new Tip("Redstone połóż dopiero gdy ścieżka jest bezpieczna.", "cel")
        };
    }
}
