package win.morodirule.eduzscl.teaching.lessons;

import win.morodirule.eduzscl.teaching.TeachingAgent;
import win.morodirule.eduzscl.teaching.TeachingAgent.Tip;

public class Lesson005_LoopsPath implements TeachingAgent.Lesson {
    @Override
    public String getId() { return "loops_intro_2"; }

    @Override
    public String getTitle() { return "Pętle: korytarz z zakrętem"; }

    @Override
    public String getGoal() { return "Wykorzystaj pętle do długiego korytarza i zakrętu, a na mecie połóż redstone."; }

    @Override
    public String getExample() {
        return """
// Idź 5 kroków, skręć, idź kolejne 3, wszystko w pętlach
for (var i = 0; i < 5; i++) {
    agent.move(1);
}
agent.turnLeft();
for (var j = 0; j < 3; j++) {
    agent.move(1);
}
agent.place(\"redstone_block\");
""";
    }

    @Override
    public String getExplanation() {
        return """
Możesz użyć kilku pętli pod rząd dla różnych odcinków trasy. Obrót rozdziela powtarzane fragmenty.
""";
    }

    @Override
    public TeachingAgent.PracticeTask[] getTasks() {
        return new TeachingAgent.PracticeTask[] {
            new TeachingAgent.PracticeTask("guided", "Zbuduj trasę z dwóch odcinków w pętlach i zakończ redstone.", "Redstone na końcu drugiego odcinka")
        };
    }

    @Override
    public TeachingAgent.Hint[] getHints() {
        return new TeachingAgent.Hint[] {
            new TeachingAgent.Hint("Stosuj osobną pętlę na każdy odcinek o innej długości."),
            new TeachingAgent.Hint("Po obrocie licznik pętli zaczynaj od 0 dla nowego odcinka.")
        };
    }

    @Override
    public String getVerification() { return "Redstone na końcu zakręconej trasy opartej na pętlach."; }

    @Override
    public String getNextStep() { return "Dodaj interakcję z mobami."; }

    @Override
    public int getMaxOperations() { return 120; }

    @Override
    public Tip[] getTips() {
        return new Tip[] {
            new Tip("Pętle skracają kod i ułatwiają poprawki długości.", "pętle"),
            new Tip("Redstone zawsze po ostatnim kroku.", "cel")
        };
    }
}
