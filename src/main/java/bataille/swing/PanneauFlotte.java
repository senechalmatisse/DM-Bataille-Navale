package bataille.swing;

import bataille.modele.Bateau;
import bataille.modele.Plateau;
import bataille.modele.TypeBateau;
import bataille.modele.VuePlateau;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;

/**
 * Le suivi d'une flotte : un bateau par ligne, avec une case par case du bateau. Pour sa propre flotte, le joueur
 * voit les cases touchées ; pour la flotte adverse, il ne sait que quels bateaux sont coulés.
 */
final class PanneauFlotte extends JComponent {
    private static final long serialVersionUID = 1L;
    /** La hauteur d'une ligne. */
    private static final int HAUTEUR_LIGNE = 20;
    /** Le côté d'une case. */
    private static final int COTE_CASE = 13;
    /** La largeur réservée au nom du bateau. */
    private static final int LARGEUR_NOM = 128;
    /** Le fond d'une case dont on ne sait rien. */
    private static final Color INCONNUE = new Color(0xEEF3F8);

    /** Les lignes affichées. */
    private transient List<Ligne> lignes = List.of();

    /**
     * Change les bateaux affichés.
     *
     * @param nouvelles une ligne par bateau
     */
    void afficher(List<Ligne> nouvelles) {
        lignes = List.copyOf(nouvelles);
        revalidate();
        repaint();
    }

    /**
     * Décrit sa propre flotte : chaque bateau avec ses cases touchées.
     *
     * @param plateau le plateau du joueur
     * @return une ligne par bateau, du plus long au plus court
     */
    static List<Ligne> pourProprietaire(Plateau plateau) {
        List<Ligne> lignes = new ArrayList<>();
        for (Bateau bateau : plateau.getBateaux()) {
            List<Boolean> touches = bateau.getCases().stream().map(bateau::estTouche).toList();
            lignes.add(new Ligne(bateau.getType(), touches, bateau.estCoule(), true));
        }
        lignes.sort(Comparator.comparingInt((Ligne ligne) -> ligne.type().getLongueur()).reversed());
        return lignes;
    }

    /**
     * Décrit une flotte en cours de placement : les bateaux posés, puis ceux qui restent à poser.
     *
     * @param flotte la flotte complète
     * @param plateau le plateau en cours de placement
     * @return une ligne par bateau de la flotte
     */
    static List<Ligne> pourPlacement(List<TypeBateau> flotte, Plateau plateau) {
        List<TypeBateau> poses = new ArrayList<>(plateau.getTypes());
        List<Ligne> lignes = new ArrayList<>();
        for (TypeBateau type : flotte) {
            boolean pose = poses.remove(type);
            lignes.add(new Ligne(type, Collections.nCopies(type.getLongueur(), false), false, pose));
        }
        return lignes;
    }

    /**
     * Décrit la flotte adverse telle que le joueur la connaît : coulée ou non, bateau par bateau.
     *
     * @param flotte la composition de la flotte, connue des deux joueurs
     * @param vue ce que le joueur sait du plateau adverse
     * @return une ligne par bateau de la flotte
     */
    static List<Ligne> pourAdversaire(List<TypeBateau> flotte, VuePlateau vue) {
        List<TypeBateau> coules = new ArrayList<>(vue.getBateauxCoules().stream().map(Bateau::getType).toList());
        List<Ligne> lignes = new ArrayList<>();
        for (TypeBateau type : flotte) {
            boolean coule = coules.remove(type);
            lignes.add(new Ligne(type, Collections.nCopies(type.getLongueur(), false), coule, false));
        }
        return lignes;
    }

    @Override
    public Dimension getPreferredSize() {
        int cases = lignes.stream().mapToInt(ligne -> ligne.type().getLongueur()).max().orElse(5);
        return new Dimension(LARGEUR_NOM + cases * (COTE_CASE + 3), Math.max(1, lignes.size()) * HAUTEUR_LIGNE + 4);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font police = getFont().deriveFont(12f);
        g.setFont(police);
        FontMetrics mesures = g.getFontMetrics();
        int y = 2;
        for (Ligne ligne : lignes) {
            int milieu = y + HAUTEUR_LIGNE / 2;
            String nom = ligne.type().getNom();
            g.setColor(ligne.coule() ? Color.GRAY : getForeground());
            g.drawString(nom, 0, milieu + mesures.getAscent() / 2 - 1);
            if (ligne.coule()) {
                g.drawLine(0, milieu, mesures.stringWidth(nom), milieu);
            }
            for (int i = 0; i < ligne.touches().size(); i++) {
                int x = LARGEUR_NOM + i * (COTE_CASE + 3);
                int haut = milieu - COTE_CASE / 2;
                Color fond;
                if (ligne.coule()) {
                    fond = PanneauGrille.COULE;
                } else if (ligne.touches().get(i)) {
                    fond = PanneauGrille.TOUCHE;
                } else {
                    fond = ligne.visible() ? PanneauGrille.BATEAU : INCONNUE;
                }
                g.setColor(fond);
                g.fillRoundRect(x, haut, COTE_CASE, COTE_CASE, 4, 4);
                g.setColor(PanneauGrille.LIGNES);
                g.setStroke(new BasicStroke(1f));
                g.drawRoundRect(x, haut, COTE_CASE, COTE_CASE, 4, 4);
            }
            y += HAUTEUR_LIGNE;
        }
        g.dispose();
    }

    /**
     * Un bateau du suivi.
     *
     * @param type le type du bateau
     * @param touches pour chaque case du bateau, si elle est touchée
     * @param coule si le bateau est coulé
     * @param visible si ses cases intactes sont connues du joueur
     */
    record Ligne(TypeBateau type, List<Boolean> touches, boolean coule, boolean visible) {

        /**
         * Copie la liste des touches.
         *
         * @param type le type du bateau
         * @param touches pour chaque case, si elle est touchée
         * @param coule si le bateau est coulé
         * @param visible si ses cases intactes sont connues
         */
        Ligne {
            Objects.requireNonNull(type, "type");
            touches = List.copyOf(touches);
        }
    }
}
