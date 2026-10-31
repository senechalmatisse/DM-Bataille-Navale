package bataille.swing;

import bataille.modele.Capacite;
import bataille.modele.Coordonnee;
import bataille.modele.Plateau;
import bataille.modele.Tir;
import bataille.partie.Annonces;
import bataille.partie.Coup;
import bataille.partie.EnregistrementPartie;
import bataille.partie.Joueur;
import bataille.partie.Partie;
import bataille.partie.Rejeu;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

/**
 * Le rejeu d'une partie, coup par coup : les deux flottes dévoilées, le coup en cours encadré, et les commandes pour
 * avancer, reculer ou laisser la partie se rejouer seule.
 */
final class EcranRejeu extends JPanel {
    private static final long serialVersionUID = 1L;
    /** La largeur de la zone centrale. */
    private static final int LARGEUR_CENTRE = 300;
    /** Le délai entre deux coups en lecture automatique, de la vitesse la plus lente à la plus rapide. */
    private static final int[] DELAIS_MS = {1600, 1000, 650, 380, 200};

    /** La grille du joueur 1. */
    private final PanneauGrille grille1 = new PanneauGrille(10);
    /** La grille du joueur 2. */
    private final PanneauGrille grille2 = new PanneauGrille(10);
    /** Le suivi de la flotte du joueur 1. */
    private final PanneauFlotte flotte1 = new PanneauFlotte();
    /** Le suivi de la flotte du joueur 2. */
    private final PanneauFlotte flotte2 = new PanneauFlotte();
    /** Le titre de la colonne du joueur 1. */
    private final TitledBorder titre1 = BorderFactory.createTitledBorder(" ");
    /** Le titre de la colonne du joueur 2. */
    private final TitledBorder titre2 = BorderFactory.createTitledBorder(" ");
    /** La position dans la partie. */
    private final JLabel position = new JLabel(" ");
    /** Le coup affiché. */
    private final JLabel description = new JLabel(" ");
    /** Lance ou arrête la lecture automatique. */
    private final JButton boutonLecture = new JButton("Lecture ▶");
    /** Les coups de la partie. */
    private final DefaultListModel<String> coups = new DefaultListModel<>();
    /** La liste des coups, où le coup affiché est sélectionné. */
    private final JList<String> listeCoups = new JList<>(coups);
    /** La vitesse de lecture. */
    private final JSlider vitesse = new JSlider(1, DELAIS_MS.length, 3);
    /** Le minuteur de la lecture automatique. */
    private final Timer lecture;
    /** Le rejeu en cours, ou {@code null}. */
    private transient Rejeu rejeu;
    /** Si les animations sont jouées quand la partie avance d'un coup. */
    private boolean animations = true;
    /** Vrai pendant que l'écran met à jour la sélection de la liste, pour ne pas y réagir. */
    private boolean miseAJour;

    /**
     * Construit l'écran.
     *
     * @param retour revient à l'accueil
     */
    EcranRejeu(Runnable retour) {
        super(new GridBagLayout());
        Objects.requireNonNull(retour, "retour");
        setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        lecture = new Timer(DELAIS_MS[2], evenement -> lireUnCoup());
        GridBagConstraints contraintes = new GridBagConstraints();
        contraintes.fill = GridBagConstraints.BOTH;
        contraintes.weighty = 1;
        contraintes.weightx = 0.5;
        add(new ColonneFlotte(grille1, flotte1, titre1), contraintes);
        contraintes.gridx = 1;
        contraintes.weightx = 0;
        contraintes.insets = new Insets(6, 14, 0, 14);
        add(creerCentre(retour), contraintes);
        contraintes.gridx = 2;
        contraintes.weightx = 0.5;
        contraintes.insets = new Insets(0, 0, 0, 0);
        add(new ColonneFlotte(grille2, flotte2, titre2), contraintes);
    }

    private JPanel creerCentre(Runnable retour) {
        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        JLabel titre = new JLabel("Rejeu de la partie");
        titre.setFont(titre.getFont().deriveFont(Font.BOLD, 17f));
        position.setFont(position.getFont().deriveFont(Font.BOLD, 13f));
        description.setVerticalAlignment(JLabel.TOP);

        JButton debut = new JButton("|◀ Début");
        debut.addActionListener(evenement -> aller(0));
        JButton precedent = new JButton("◀ Précédent");
        precedent.addActionListener(evenement -> reculer());
        JButton suivant = new JButton("Suivant ▶");
        suivant.addActionListener(evenement -> avancer());
        JButton fin = new JButton("Fin ▶|");
        fin.addActionListener(evenement -> aller(Integer.MAX_VALUE));
        boutonLecture.addActionListener(evenement -> basculerLecture());
        JPanel commandes = new JPanel(new GridLayout(0, 2, 6, 6));
        commandes.add(debut);
        commandes.add(fin);
        commandes.add(precedent);
        commandes.add(suivant);
        JPanel lectureEtVitesse = new JPanel(new BorderLayout(0, 4));
        lectureEtVitesse.add(boutonLecture, BorderLayout.NORTH);
        Hashtable<Integer, JComponent> reperes = new Hashtable<>();
        reperes.put(1, new JLabel("Lent"));
        reperes.put(DELAIS_MS.length, new JLabel("Rapide"));
        vitesse.setLabelTable(reperes);
        vitesse.setPaintLabels(true);
        vitesse.setSnapToTicks(true);
        vitesse.addChangeListener(evenement -> lecture.setDelay(DELAIS_MS[vitesse.getValue() - 1]));
        lectureEtVitesse.add(vitesse, BorderLayout.CENTER);

        listeCoups.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listeCoups.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> liste, Object valeur, int indice,
                    boolean selectionne, boolean focus) {
                return super.getListCellRendererComponent(liste, Textes.paragraphe(String.valueOf(valeur),
                        LARGEUR_CENTRE - 50), indice, selectionne, focus);
            }
        });
        listeCoups.addListSelectionListener(evenement -> {
            if (!miseAJour && !evenement.getValueIsAdjusting() && listeCoups.getSelectedIndex() >= 0) {
                arreterLecture();
                aller(listeCoups.getSelectedIndex() + 1);
            }
        });
        JScrollPane defilement = new JScrollPane(listeCoups);
        defilement.setBorder(BorderFactory.createTitledBorder("Coups"));
        defilement.setPreferredSize(new Dimension(LARGEUR_CENTRE, 220));

        JButton accueil = new JButton("Retour à l'accueil");
        accueil.addActionListener(evenement -> {
            arreter();
            retour.run();
        });

        List<JComponent> composants = List.of(titre, position, description, commandes, lectureEtVitesse,
                defilement, accueil);
        for (JComponent composant : composants) {
            composant.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        centre.add(titre);
        centre.add(Box.createVerticalStrut(6));
        centre.add(position);
        centre.add(Box.createVerticalStrut(4));
        centre.add(description);
        centre.add(Box.createVerticalStrut(12));
        centre.add(commandes);
        centre.add(Box.createVerticalStrut(8));
        centre.add(lectureEtVitesse);
        centre.add(Box.createVerticalStrut(10));
        centre.add(defilement);
        centre.add(Box.createVerticalStrut(10));
        centre.add(accueil);
        commandes.setMaximumSize(new Dimension(LARGEUR_CENTRE, commandes.getPreferredSize().height));
        lectureEtVitesse.setMaximumSize(new Dimension(LARGEUR_CENTRE, lectureEtVitesse.getPreferredSize().height));
        centre.setPreferredSize(new Dimension(LARGEUR_CENTRE, centre.getPreferredSize().height));
        return centre;
    }

    /**
     * Ouvre une partie au début.
     *
     * @param enregistrement la partie à revoir
     * @throws IllegalArgumentException si l'enregistrement ne peut pas être rejoué
     */
    void charger(EnregistrementPartie enregistrement) {
        Rejeu nouveau = new Rejeu(enregistrement);
        arreter();
        rejeu = nouveau;
        Partie complete = enregistrement.restaurer(new Random(0));
        Joueur lecteur = complete.getJoueur1();
        miseAJour = true;
        coups.clear();
        List<Coup> historique = complete.getHistorique();
        for (int i = 0; i < historique.size(); i++) {
            coups.addElement((i + 1) + ". " + Annonces.decrire(historique.get(i), lecteur));
        }
        miseAJour = false;
        titre1.setTitle(Textes.flotte(complete.getJoueur1()));
        titre2.setTitle(Textes.flotte(complete.getJoueur2()));
        for (PanneauGrille grille : List.of(grille1, grille2)) {
            grille.arreterAnimations();
            grille.setInteractif(false);
        }
        afficher();
    }

    /**
     * Active ou désactive les animations.
     *
     * @param actives si les animations sont jouées
     */
    void setAnimations(boolean actives) {
        animations = actives;
    }

    /** Arrête la lecture automatique. */
    void arreter() {
        arreterLecture();
        grille1.arreterAnimations();
        grille2.arreterAnimations();
    }

    private void basculerLecture() {
        if (lecture.isRunning()) {
            arreterLecture();
        } else if (rejeu != null) {
            if (rejeu.getPosition() == rejeu.getNombreCoups()) {
                aller(0);
            }
            lecture.start();
            boutonLecture.setText("Pause ❚❚");
        }
    }

    private void arreterLecture() {
        lecture.stop();
        boutonLecture.setText("Lecture ▶");
    }

    private void lireUnCoup() {
        if (!avancer()) {
            arreterLecture();
        }
    }

    private boolean avancer() {
        if (rejeu == null || !rejeu.avancer()) {
            return false;
        }
        afficher();
        if (animations) {
            animer(rejeu.getDernierCoup().orElseThrow());
        }
        return true;
    }

    private void reculer() {
        arreterLecture();
        if (rejeu != null && rejeu.reculer()) {
            afficher();
        }
    }

    private void aller(int nouvellePosition) {
        if (rejeu == null) {
            return;
        }
        arreterLecture();
        rejeu.allerA(Math.max(0, Math.min(nouvellePosition, rejeu.getNombreCoups())));
        afficher();
    }

    private void afficher() {
        Partie partie = rejeu.getPartie();
        Plateau plateau1 = partie.getJoueur1().getPlateau();
        Plateau plateau2 = partie.getJoueur2().getPlateau();
        int taille = plateau1.getTaille();
        grille1.afficher(taille, plateau1::getEtatPourProprietaire, plateau1.getBateaux());
        grille1.setDetections(plateau1.getDetections());
        grille2.afficher(taille, plateau2::getEtatPourProprietaire, plateau2.getBateaux());
        grille2.setDetections(plateau2.getDetections());
        flotte1.afficher(PanneauFlotte.pourProprietaire(plateau1));
        flotte2.afficher(PanneauFlotte.pourProprietaire(plateau2));
        List<Coordonnee> marques = new ArrayList<>();
        rejeu.getDernierCoup().ifPresent(coup -> coup.tirs().forEach(tir -> marques.add(tir.cible())));
        boolean coupDuJoueur1 = rejeu.getDernierCoup().map(coup -> coup.auteur() == partie.getJoueur1()).orElse(false);
        grille1.setMarques(coupDuJoueur1 ? List.of() : marques);
        grille2.setMarques(coupDuJoueur1 ? marques : List.of());
        position.setText(rejeu.getPosition() == 0 ? "Début de la partie" : "Coup " + rejeu.getPosition() + " sur "
                + rejeu.getNombreCoups());
        String texte;
        if (rejeu.getPosition() == 0) {
            texte = "Les flottes sont en place. " + rejeu.getNombreCoups() + " coups à revoir.";
        } else {
            texte = Annonces.decrire(rejeu.getDernierCoup().orElseThrow(), partie.getJoueur1());
        }
        if (partie.estTerminee()) {
            Joueur gagnant = partie.getGagnant().orElseThrow();
            texte += "\nFin de la partie : " + (gagnant.estHumain() ? gagnant.getNom() + " gagne."
                    : "l'ordinateur gagne.");
        }
        description.setText(Textes.paragraphe(texte, LARGEUR_CENTRE - 20));
        miseAJour = true;
        if (rejeu.getPosition() == 0) {
            listeCoups.clearSelection();
        } else {
            listeCoups.setSelectedIndex(rejeu.getPosition() - 1);
            listeCoups.ensureIndexIsVisible(rejeu.getPosition() - 1);
        }
        miseAJour = false;
        repaint();
    }

    /** Rejoue les animations du coup qui vient d'être affiché. */
    private void animer(Coup coup) {
        Partie partie = rejeu.getPartie();
        boolean duJoueur1 = coup.auteur() == partie.getJoueur1();
        PanneauGrille grille = duJoueur1 ? grille2 : grille1;
        Plateau vise = duJoueur1 ? partie.getJoueur2().getPlateau() : partie.getJoueur1().getPlateau();
        if (coup.ordre().capacite() == Capacite.RADAR) {
            coup.detection().ifPresent(detection -> grille.lancerEffet(Effet.BALAYAGE, detection.cases()));
        }
        for (Tir tir : coup.tirs()) {
            grille.lancerEffet(tir.resultat().estAuBut() ? Effet.EXPLOSION : Effet.ECLABOUSSURE, List.of(tir.cible()));
            if (tir.bateauCoule().isPresent()) {
                vise.getBateau(tir.cible()).ifPresent(bateau -> grille.lancerEffet(Effet.NAUFRAGE, bateau.getCases()));
            }
        }
    }
}
