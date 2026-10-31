package bataille.swing;

import bataille.modele.EtatCase;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.util.Objects;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * L'écran d'accueil : jouer contre l'ordinateur ou à deux, reprendre la partie interrompue, revoir la dernière
 * partie, ouvrir une partie enregistrée, consulter ses statistiques.
 */
final class EcranAccueil extends JPanel {
    private static final long serialVersionUID = 1L;
    /** La largeur des boutons. */
    private static final int LARGEUR_BOUTONS = 340;

    /** Ce que propose l'accueil. */
    interface Actions {
        /** Lance une partie contre l'ordinateur. */
        void jouerContreOrdinateur();

        /** Lance une partie à deux sur le même écran. */
        void jouerADeux();

        /** Reprend la partie interrompue. */
        void reprendre();

        /** Revoit la dernière partie coup par coup. */
        void revoirDernierePartie();

        /** Ouvre une partie enregistrée. */
        void ouvrir();

        /** Affiche les statistiques. */
        void statistiques();

        /** Ferme le jeu. */
        void quitter();
    }

    /** Reprend la partie interrompue. */
    private final JButton boutonReprendre = new JButton("Reprendre la partie interrompue");
    /** Décrit la partie interrompue. */
    private final JLabel detailReprise = new JLabel(" ");
    /** Revoit la dernière partie. */
    private final JButton boutonRevoir = new JButton("Revoir la dernière partie");
    /** Résume les statistiques. */
    private final JLabel resume = new JLabel(" ");

    /**
     * Construit l'écran.
     *
     * @param actions ce que font les boutons
     */
    EcranAccueil(Actions actions) {
        super(new GridBagLayout());
        Objects.requireNonNull(actions, "actions");
        JPanel colonne = new JPanel();
        colonne.setLayout(new BoxLayout(colonne, BoxLayout.Y_AXIS));
        colonne.add(new Banniere());
        colonne.add(Box.createVerticalStrut(24));
        colonne.add(bouton("Jouer contre l'ordinateur", true, actions::jouerContreOrdinateur));
        colonne.add(Box.createVerticalStrut(8));
        colonne.add(bouton("Jouer à deux sur le même écran", true, actions::jouerADeux));
        colonne.add(Box.createVerticalStrut(18));
        configurer(boutonReprendre, false, actions::reprendre);
        colonne.add(boutonReprendre);
        detailReprise.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailReprise.setForeground(Color.GRAY);
        colonne.add(detailReprise);
        colonne.add(Box.createVerticalStrut(8));
        configurer(boutonRevoir, false, actions::revoirDernierePartie);
        colonne.add(boutonRevoir);
        colonne.add(Box.createVerticalStrut(8));
        colonne.add(bouton("Ouvrir une partie enregistrée…", false, actions::ouvrir));
        colonne.add(Box.createVerticalStrut(8));
        colonne.add(bouton("Statistiques", false, actions::statistiques));
        colonne.add(Box.createVerticalStrut(8));
        colonne.add(bouton("Quitter", false, actions::quitter));
        colonne.add(Box.createVerticalStrut(20));
        resume.setAlignmentX(Component.CENTER_ALIGNMENT);
        resume.setForeground(Color.GRAY);
        colonne.add(resume);
        add(colonne);
    }

    private static JButton bouton(String texte, boolean principal, Runnable action) {
        JButton bouton = new JButton(texte);
        configurer(bouton, principal, action);
        return bouton;
    }

    private static void configurer(JButton bouton, boolean principal, Runnable action) {
        bouton.setAlignmentX(Component.CENTER_ALIGNMENT);
        bouton.setFont(bouton.getFont().deriveFont(principal ? Font.BOLD : Font.PLAIN, principal ? 16f : 14f));
        Dimension taille = new Dimension(LARGEUR_BOUTONS, principal ? 44 : 34);
        bouton.setPreferredSize(taille);
        bouton.setMaximumSize(taille);
        bouton.addActionListener(evenement -> action.run());
    }

    /**
     * Met à jour ce qui dépend des fichiers du jeu.
     *
     * @param partieInterrompue la description de la partie à reprendre, ou une valeur vide s'il n'y en a pas
     * @param dernierePartie si une dernière partie peut être revue
     * @param statistiques le résumé des statistiques, ou une valeur vide sans partie jouée
     */
    void actualiser(Optional<String> partieInterrompue, boolean dernierePartie, Optional<String> statistiques) {
        boutonReprendre.setEnabled(partieInterrompue.isPresent());
        detailReprise.setText(partieInterrompue.map(Textes::html).orElse(" "));
        boutonRevoir.setEnabled(dernierePartie);
        resume.setText(statistiques.map(texte -> "Contre l'ordinateur : " + texte).orElse(" "));
    }

    /** Le titre du jeu, sur une mer stylisée où un bateau vient d'être touché. */
    private static final class Banniere extends JComponent {
        private static final long serialVersionUID = 1L;
        /** Le côté des cases décoratives. */
        private static final int COTE = 26;
        /** Les cases décoratives, ligne par ligne. */
        private static final EtatCase[][] CASES = {
            {EtatCase.EAU, EtatCase.MANQUE, EtatCase.EAU, EtatCase.EAU, EtatCase.EAU, EtatCase.EAU, EtatCase.EAU,
                EtatCase.MANQUE, EtatCase.EAU, EtatCase.EAU},
            {EtatCase.EAU, EtatCase.BATEAU, EtatCase.BATEAU, EtatCase.TOUCHE, EtatCase.BATEAU, EtatCase.EAU,
                EtatCase.COULE, EtatCase.COULE, EtatCase.EAU, EtatCase.MANQUE},
        };

        Banniere() {
            Dimension taille = new Dimension(460, 150);
            setPreferredSize(taille);
            setMaximumSize(taille);
            setAlignmentX(Component.CENTER_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(0x123B5D), 0, getHeight(), new Color(0x0B7285)));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g.setColor(Color.WHITE);
            g.setFont(getFont().deriveFont(Font.BOLD, 34f));
            FontMetrics mesures = g.getFontMetrics();
            String titre = "Bataille navale";
            g.drawString(titre, (getWidth() - mesures.stringWidth(titre)) / 2, 50);
            g.setFont(getFont().deriveFont(Font.PLAIN, 14f));
            mesures = g.getFontMetrics();
            String sousTitre = "Coulez la flotte adverse avant qu'elle ne coule la vôtre.";
            g.setColor(new Color(0xD0EBFF));
            g.drawString(sousTitre, (getWidth() - mesures.stringWidth(sousTitre)) / 2, 76);
            int gauche = (getWidth() - CASES[0].length * COTE) / 2;
            for (int ligne = 0; ligne < CASES.length; ligne++) {
                for (int colonne = 0; colonne < CASES[ligne].length; colonne++) {
                    int x = gauche + colonne * COTE;
                    int y = 90 + ligne * COTE;
                    PanneauGrille.dessinerCase(g, CASES[ligne][colonne], x, y, COTE);
                    g.setColor(PanneauGrille.LIGNES);
                    g.drawRect(x, y, COTE, COTE);
                }
            }
            g.dispose();
        }
    }
}
