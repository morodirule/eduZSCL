package win.morodirule.eduzscl.teaching.lessons;

import win.morodirule.eduzscl.teaching.TeachingAgent;
import win.morodirule.eduzscl.teaching.TeachingAgent.Tip;

public class Lesson007_AttackLoops implements TeachingAgent.Lesson {
    @Override
    public String getId() { return "attack_loop_1"; }

    @Override
    public String getTitle() { return "Atak w pętli"; }

    @Override
    public String getGoal() { return "Pokonaj kilka mobów w szeregu, używając pętli i zakończ kładąc redstone."; }

    @Override
    public String getExample() {
        return """
// Idź naprzód 3 razy, po każdym kroku zaatakuj, na końcu połóż redstone
for (var i = 0; i < 3; i++) {
    agent.move(1);
    agent.attack();
}
agent.place(\"redstone_block\");
""";
    }

    @Override
    public String getExplanation() {
        return """
Łącz pętle z agent.attack(), by powtarzać sekwencję ruch + atak. Przyda się, gdy w korytarzu stoją kolejne moby.
""";
    }

    @Override
    public TeachingAgent.PracticeTask[] getTasks() {
        return new TeachingAgent.PracticeTask[] {
            new TeachingAgent.PracticeTask("guided", "Użyj pętli do powtarzania ruchu i ataku na kilku mobach.", "Wszystkie cele pokonane, redstone na mecie")
        };
    }

    @Override
    public TeachingAgent.Hint[] getHints() {
        return new TeachingAgent.Hint[] {
            new TeachingAgent.Hint("Jeśli mob jest dalej, zwiększ krok w agent.move()."),
            new TeachingAgent.Hint("Pętla może też zawierać skręt, jeśli korytarz zakręca.")
        };
    }

    @Override
    public String getVerification() { return "Seria mobów pokonana, redstone leży na końcu trasy."; }

    @Override
    public String getNextStep() { return "Buduj i niszcz bloki po drodze."; }

    @Override
    public int getMaxOperations() { return 150; }

    @Override
    public Tip[] getTips() {
        return new Tip[] {
            new Tip("Kontroluj liczbę iteracji, by nie zabrakło operacji.", "pętle"),
            new Tip("Redstone dopiero po ostatnim ataku.", "cel")
        };
    }
}
