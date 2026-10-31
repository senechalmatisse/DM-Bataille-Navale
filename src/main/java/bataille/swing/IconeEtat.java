package bataille.swing;

import bataille.modele.EtatCase;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/** Une petite case dessinée comme sur la grille, pour la légende. */
final class IconeEtat implements Icon {
    /** L'état dessiné. */
    private final EtatCase etat;
    /** La largeur de l'icône. */
    private final int cote;

    /**
     * Construit l'icône d'un état.
     *
     * @param etat l'état à dessiner
     * @param cote la largeur de l'icône, en pixels
     */
    IconeEtat(EtatCase etat, int cote) {
        this.etat = etat;
        this.cote = cote;
    }

    @Override
    public void paintIcon(Component composant, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        PanneauGrille.dessinerCase(g, etat, x, y, cote);
        g.setColor(PanneauGrille.LIGNES);
        g.drawRect(x, y, cote - 1, cote - 1);
        g.dispose();
    }

    @Override
    public int getIconWidth() {
        return cote;
    }

    @Override
    public int getIconHeight() {
        return cote;
    }
}
