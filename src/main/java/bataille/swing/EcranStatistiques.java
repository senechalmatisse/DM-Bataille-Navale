package bataille.swing;

import bataille.donnees.ResumeStatistiques;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** L'écran des statistiques du joueur, d'une partie à l'autre. */
final class EcranStatistiques extends JPanel {
    private static final long serialVersionUID = 1L;

    /** Le tableau des statistiques. */
    private final JPanel tableau = new JPanel(new GridBagLayout());
    /** Efface les statistiques. */
    private final JButton boutonReinitialiser = new JButton("Réinitialiser…");

    /**
     * Construit l'écran.
     *
     * @param retour revient à l'accueil
     * @param reinitialiser efface les statistiques, après confirmation
     */
    EcranStatistiques(Runnable retour, Runnable reinitialiser) {
        super(new GridBagLayout());
        Objects.requireNonNull(retour, "retour");
        Objects.requireNonNull(reinitialiser, "reinitialiser");
        JPanel contenu = new JPanel(new BorderLayout(0, 16));
        JLabel titre = new JLabel("Vos statistiques");
        titre.setFont(titre.getFont().deriveFont(Font.BOLD, 24f));
        contenu.add(titre, BorderLayout.NORTH);
        tableau.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        contenu.add(tableau, BorderLayout.CENTER);
        JButton accueil = new JButton("Retour à l'accueil");
        accueil.addActionListener(evenement -> retour.run());
        boutonReinitialiser.addActionListener(evenement -> reinitialiser.run());
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        boutons.add(accueil);
        boutons.add(Box.createHorizontalStrut(10));
        boutons.add(boutonReinitialiser);
        contenu.add(boutons, BorderLayout.SOUTH);
        add(contenu);
    }

    /**
     * Affiche les statistiques.
     *
     * @param lignes une ligne par donnée connue ; vide si aucune partie n'a été jouée
     * @param erreur le message à afficher si les statistiques n'ont pas pu être lues, sinon {@code null}
     */
    void afficher(List<ResumeStatistiques.Ligne> lignes, String erreur) {
        tableau.removeAll();
        GridBagConstraints contraintes = new GridBagConstraints();
        contraintes.anchor = GridBagConstraints.LINE_START;
        contraintes.insets = new Insets(3, 0, 3, 24);
        if (erreur != null || lignes.isEmpty()) {
            String texte = erreur != null ? "Les statistiques ne peuvent pas être lues : " + erreur
                    : "Aucune partie terminée pour l'instant : vos statistiques s'afficheront ici.";
            tableau.add(new JLabel(Textes.paragraphe(texte, 420)), contraintes);
        }
        int ligne = 0;
        for (ResumeStatistiques.Ligne donnee : lignes) {
            contraintes.gridy = ligne++;
            contraintes.gridx = 0;
            JLabel libelle = new JLabel(donnee.libelle());
            libelle.setFont(libelle.getFont().deriveFont(14f));
            tableau.add(libelle, contraintes);
            contraintes.gridx = 1;
            JLabel valeur = new JLabel(donnee.valeur());
            valeur.setFont(valeur.getFont().deriveFont(Font.BOLD, 14f));
            tableau.add(valeur, contraintes);
        }
        boutonReinitialiser.setEnabled(!lignes.isEmpty());
        revalidate();
        repaint();
    }
}
