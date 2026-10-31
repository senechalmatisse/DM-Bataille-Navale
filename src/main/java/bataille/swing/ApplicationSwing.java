package bataille.swing;

import bataille.donnees.Dossiers;
import java.util.Random;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/** Lance le jeu dans une fenêtre. */
public final class ApplicationSwing {

    private ApplicationSwing() {
    }

    /**
     * Ouvre la fenêtre du jeu sur l'écran d'accueil, sur le fil d'affichage de Swing.
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
            new FenetreJeu(new Random(), Dossiers.parDefaut()).setVisible(true);
        });
    }
}
