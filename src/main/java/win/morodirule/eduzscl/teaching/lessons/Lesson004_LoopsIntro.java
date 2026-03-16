package win.morodirule.eduzscl.teaching.lessons;

import win.morodirule.eduzscl.teaching.TeachingAgent;
import win.morodirule.eduzscl.teaching.TeachingAgent.Tip;

public class Lesson004_LoopsIntro implements TeachingAgent.Lesson {
    @Override
    public String getId() { return "loops_intro_1"; }

    @Override
    public String getTitle() { return "Pętle: powtarzanie ruchów"; }

    @Override
    public String getGoal() { return "Użyj pętli, by pokonać powtarzalny odcinek i na końcu postawić blok redstone."; }

    @Override
    public String getExample() {
        return """
// Przejdź 4 razy po 1 kroku używając pętli for, potem postaw redstone
for (var i = 0; i < 4; i++) {
    agent.move(1);
}
agent.place(\"redstone_block\");
""";
    }

    @Override
    public String getExplanation() {
        return """
Pętle for pozwalają skrócić kod, gdy powtarzasz te same ruchy. Ustaw licznik, warunek i inkrementację.
""";
    }

    @Override
    public TeachingAgent.PracticeTask[] getTasks() {
        return new TeachingAgent.PracticeTask[] {
            new TeachingAgent.PracticeTask("guided", "Użyj pętli, by powtórzyć ruchy i dotrzeć do mety.", "Redstone położony po wyjściu z pętli")
        };
    }

    @Override
    public TeachingAgent.Hint[] getHints() {
        return new TeachingAgent.Hint[] {
            new TeachingAgent.Hint("for (var i = 0; i < N; i++) { agent.move(1); } powtarza ruch N razy."),
            new TeachingAgent.Hint("Możesz w pętli też obracać agenta, jeśli trasa jest łamana.")
        };
    }

    @Override
    public String getVerification() { return "Redstone stoi w punkcie końcowym, a ruch wykorzystał pętlę."; }

    @Override
    public String getNextStep() { return "Łącz pętle z zakrętami lub różnymi krokami."; }

    @Override
    public int getMaxOperations() { return 100; }

    @Override
    public Tip[] getTips() {
        return new Tip[] {
            new Tip("Zmieniaj warunek pętli, by dopasować długość trasy.", "pętle"),
            new Tip("Nie zapomnij o redstone po wyjściu z pętli.", "cel")
        };
    }
}
