package win.morodirule.eduzscl.teaching.lessons;

import win.morodirule.eduzscl.teaching.TeachingAgent;
import win.morodirule.eduzscl.teaching.TeachingAgent.Tip;

public class Lesson006_AttackBasics implements TeachingAgent.Lesson {
    @Override
    public String getId() { return "attack_basic_1"; }

    @Override
    public String getTitle() { return "Atak: jeden cel"; }

    @Override
    public String getGoal() { return "Podejdź do moba, zaatakuj go i po zwycięstwie połóż blok redstone."; }

    @Override
    public String getExample() {
        return """
// Podejdź dwa kroki i zaatakuj cel przed sobą, potem postaw redstone
agent.move(2);
agent.attack();
agent.place(\"redstone_block\");
""";
    }

    @Override
    public String getExplanation() {
        return """
agent.attack() uderza pierwszy byt w bloku przed agentem. Zapewnij, że cel jest w zasięgu, zanim położysz redstone.
""";
    }

    @Override
    public TeachingAgent.PracticeTask[] getTasks() {
        return new TeachingAgent.PracticeTask[] {
            new TeachingAgent.PracticeTask("guided", "Dojdź do moba, użyj attack() i połóż redstone po jego pokonaniu.", "Mob pokonany, redstone na mecie")
        };
    }

    @Override
    public TeachingAgent.Hint[] getHints() {
        return new TeachingAgent.Hint[] {
            new TeachingAgent.Hint("Ustaw się dokładnie przed celem przed wywołaniem agent.attack()."),
            new TeachingAgent.Hint("Jeśli mob stoi niżej, podejdź na sąsiedni blok przed atakiem.")
        };
    }

    @Override
    public String getVerification() { return "Mob przed agentem zniknął, redstone leży na końcu ścieżki."; }

    @Override
    public String getNextStep() { return "Powtórz atak na wielu mobach z użyciem pętli."; }

    @Override
    public int getMaxOperations() { return 120; }

    @Override
    public Tip[] getTips() {
        return new Tip[] {
            new Tip("Sprawdź kierunek przed atakiem, by nie marnować operacji.", "atak"),
            new Tip("Redstone kładź po zakończeniu walki.", "cel")
        };
    }
}
