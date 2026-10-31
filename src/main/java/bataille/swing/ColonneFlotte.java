package bataille.swing;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

/**
 * Une colonne de l'écran, sous un titre : une grille aussi grande que la place le permet, toujours carrée, et le
 * suivi de sa flotte juste en dessous.
 */
final class ColonneFlotte extends JPanel {
    private static final long serialVersionUID = 1L;
    /** L'espace entre la grille et le suivi de la flotte. */
    private static final int ECART = 10;
    /** Le retrait du suivi de la flotte, pour l'aligner sur la grille plutôt que sur ses repères. */
    private static final int RETRAIT = 20;

    /** La grille. */
    private final PanneauGrille grille;
    /** Le suivi de la flotte. */
    private final PanneauFlotte flotte;

    /**
     * Construit la colonne.
     *
     * @param grille la grille
     * @param flotte le suivi de la flotte
     * @param titre le titre, que l'écran change au fil de la partie
     */
    ColonneFlotte(PanneauGrille grille, PanneauFlotte flotte, TitledBorder titre) {
        super(null);
        this.grille = Objects.requireNonNull(grille, "grille");
        this.flotte = Objects.requireNonNull(flotte, "flotte");
        Font police = UIManager.getFont("TitledBorder.font");
        if (police != null) {
            titre.setTitleFont(police.deriveFont(Font.BOLD, 14f));
        }
        setBorder(BorderFactory.createCompoundBorder(titre, BorderFactory.createEmptyBorder(4, 6, 8, 6)));
        add(grille);
        add(flotte);
    }

    @Override
    public void doLayout() {
        Insets marges = getInsets();
        int largeur = getWidth() - marges.left - marges.right;
        Dimension suivi = flotte.getPreferredSize();
        int hauteur = getHeight() - marges.top - marges.bottom - suivi.height - ECART;
        int cote = Math.max(0, Math.min(largeur, hauteur));
        int x = marges.left + (largeur - cote) / 2;
        grille.setBounds(x, marges.top, cote, cote);
        flotte.setBounds(x + RETRAIT, marges.top + cote + ECART, Math.max(0, largeur - (x - marges.left) - RETRAIT),
                suivi.height);
    }

    @Override
    public Dimension getPreferredSize() {
        return taille(grille.getPreferredSize());
    }

    @Override
    public Dimension getMinimumSize() {
        return taille(grille.getMinimumSize());
    }

    private Dimension taille(Dimension tailleGrille) {
        Insets marges = getInsets();
        Dimension suivi = flotte.getPreferredSize();
        return new Dimension(Math.max(tailleGrille.width, suivi.width + RETRAIT) + marges.left + marges.right,
                tailleGrille.height + ECART + suivi.height + marges.top + marges.bottom);
    }
}
