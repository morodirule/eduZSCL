package win.morodirule.eduzscl.teaching.lessons;

import win.morodirule.eduzscl.teaching.TeachingAgent;

public class Lesson009_FreePlay implements TeachingAgent.Lesson {
    @Override
    public String getId() { return "free_play"; }

    @Override
    public String getTitle() { return "Swobodna budowa"; }

    @Override
    public String getGoal() { return "Twoja mapa, Twój cel – zakończ kładąc blok redstone."; }

    @Override
    public String getExample() {
        return """
// Zaprojektuj własny kod! Pamiętaj tylko o bloku redstone na końcu.
""";
    }

    @Override
    public String getExplanation() {
        return """
Brak narzuconej trasy. Wykorzystaj ruch, pętle, ataki i budowanie według własnego pomysłu. Zakończ redstone.
""";
    }

    @Override
    public TeachingAgent.PracticeTask[] getTasks() {
        return new TeachingAgent.PracticeTask[] {
            new TeachingAgent.PracticeTask("open", "Stwórz własne wyzwanie i rozwiąż je skryptem.", "Redstone na zaprojektowanej mecie")
        };
    }

    @Override
    public TeachingAgent.Hint[] getHints() {
        return new TeachingAgent.Hint[] {
            new TeachingAgent.Hint("Połącz elementy z poprzednich lekcji: ruch, pętle, attack(), place(), remove().")
        };
    }

    @Override
    public String getVerification() { return "Twój skrypt kończy się położeniem redstone na wybranej mecie."; }

    @Override
    public String getNextStep() { return "Eksperymentuj dalej!"; }
}
