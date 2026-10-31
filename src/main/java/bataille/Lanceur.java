package bataille;

import bataille.console.JeuConsole;
import bataille.swing.ApplicationSwing;
import java.awt.GraphicsEnvironment;
import java.util.List;

/**
 * Le point d'entrée du programme : ouvre la fenêtre du jeu, ou joue en console avec l'option {@code --console}
 * (ou quand aucun écran n'est disponible).
 */
public final class Lanceur {

    private Lanceur() {
    }

    /**
     * Lance le jeu.
     *
     * @param args {@code --console} pour jouer dans le terminal, {@code --aide} pour afficher l'aide
     */
    public static void main(String[] args) {
        List<String> options = List.of(args);
        if (options.contains("--aide") || options.contains("-h") || options.contains("--help")) {
            System.out.println("Bataille navale");
            System.out.println("  java -jar bataille-navale.jar            ouvre la fenêtre du jeu");
            System.out.println("  java -jar bataille-navale.jar --console  joue dans le terminal");
        } else if (options.contains("--console") || GraphicsEnvironment.isHeadless()) {
            JeuConsole.main(args);
        } else {
            ApplicationSwing.main(args);
        }
    }
}
