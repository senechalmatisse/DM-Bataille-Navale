package bataille.swing;

import java.util.Random;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/** Lance le jeu dans une fenêtre. */
public final class ApplicationSwing {

    private ApplicationSwing() {
    }

    /**
     * Ouvre la fenêtre du jeu, sur le fil d'affichage de Swing, puis demande les paramètres de la partie.
     *
     * @param args non utilisés
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
                // l'apparence par défaut de Swing convient aussi
            }
            FenetreJeu fenetre = new FenetreJeu(new Random());
            fenetre.setVisible(true);
            fenetre.nouvellePartie();
        });
    }
}
