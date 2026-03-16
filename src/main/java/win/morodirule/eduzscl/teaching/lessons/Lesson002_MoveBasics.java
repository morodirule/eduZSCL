package win.morodirule.eduzscl.teaching.lessons;

import win.morodirule.eduzscl.teaching.TeachingAgent;
import win.morodirule.eduzscl.teaching.TeachingAgent.Tip;

public class Lesson002_MoveBasics implements TeachingAgent.Lesson {
    @Override
    public String getId() { return "move_basic_1"; }

    @Override
    public String getTitle() { return "Ruch: prosta trasa"; }

    @Override
    public String getGoal() { return "Przemieść agenta do wyznaczonego miejsca i połóż blok redstone na końcu."; }

    @Override
    public String getExample() {
        return """
// Idź 3 kroki na północ i postaw redstone z przodu
agent.move(3);
agent.place("redstone_block");
""";
    }

    @Override
    public String getExplanation() {
        return """
Użyj agent.move(kroki), aby przejść prosto. Na końcu postaw blok redstone funkcją agent.place("redstone_block").
""";
    }

    @Override
    public TeachingAgent.PracticeTask[] getTasks() {
        return new TeachingAgent.PracticeTask[] {
            new TeachingAgent.PracticeTask("guided", "Przejdź po prostej do celu i połóż blok redstone.", "Redstone leży w punkcie docelowym")
        };
    }

    @Override
    public TeachingAgent.Hint[] getHints() {
        return new TeachingAgent.Hint[] {
            new TeachingAgent.Hint("agent.move(liczba) przesuwa w kierunku, w którym patrzy agent."),
            new TeachingAgent.Hint("agent.place(\"redstone_block\") kładzie blok dokładnie przed agentem.")
        };
    }

    @Override
    public String getVerification() { return "Redstone leży na końcu wyznaczonej trasy."; }

    @Override
    public String getNextStep() { return "Ponów trasę z zakrętami."; }

    @Override
    public int getMaxOperations() { return 80; }

    @Override
    public Tip[] getTips() {
        return new Tip[] {
            new Tip("Zacznij od sprawdzenia kierunku: agent.getDirection() w GUI.", "ruch"),
            new Tip("Redstone musi być położony na ostatnim polu trasy.", "cel")
        };
    }
}
