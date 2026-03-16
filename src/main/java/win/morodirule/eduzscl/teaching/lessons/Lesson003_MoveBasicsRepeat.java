package win.morodirule.eduzscl.teaching.lessons;

import win.morodirule.eduzscl.teaching.TeachingAgent;
import win.morodirule.eduzscl.teaching.TeachingAgent.Tip;

public class Lesson003_MoveBasicsRepeat implements TeachingAgent.Lesson {
    @Override
    public String getId() { return "move_basic_2"; }

    @Override
    public String getTitle() { return "Ruch: zakręty"; }

    @Override
    public String getGoal() { return "Dotrzyj do celu z jednym zakrętem i połóż blok redstone na końcu."; }

    @Override
    public String getExample() {
        return """
// Idź 2 kroki, skręć w prawo i idź 2 kroki, potem postaw redstone
agent.move(2);
agent.turnRight();
agent.move(2);
agent.place("redstone_block");
""";
    }

    @Override
    public String getExplanation() {
        return """
Łącz agent.move() z agent.turnRight()/agent.turnLeft(), by zmienić kierunek. Na końcu połóż redstone.
""";
    }

    @Override
    public TeachingAgent.PracticeTask[] getTasks() {
        return new TeachingAgent.PracticeTask[] {
            new TeachingAgent.PracticeTask("guided", "Zrób jeden zakręt i postaw redstone na końcu trasy.", "Redstone stoi w punkcie mety")
        };
    }

    @Override
    public TeachingAgent.Hint[] getHints() {
        return new TeachingAgent.Hint[] {
            new TeachingAgent.Hint("agent.turnRight() obraca o 90° zgodnie z ruchem wskazówek zegara."),
            new TeachingAgent.Hint("Zaplanuj trasę: licz kroki przed i po zakręcie.")
        };
    }

    @Override
    public String getVerification() { return "Redstone stoi na mecie po zakręcie."; }

    @Override
    public String getNextStep() { return "Powtórz ruchy w pętli, by skrócić kod."; }

    @Override
    public int getMaxOperations() { return 80; }

    @Override
    public Tip[] getTips() {
        return new Tip[] {
            new Tip("Jeśli brakuje miejsca, skręć wcześniej i dostosuj długości odcinków.", "ruch"),
            new Tip("Redstone zawsze kładź na końcu, nie po drodze.", "cel")
        };
    }
}
