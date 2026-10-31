package bataille.swing;

import bataille.donnees.Dossiers;
import bataille.donnees.FichierPartie;
import bataille.donnees.ResumeStatistiques;
import bataille.donnees.StatistiquesCumulees;
import bataille.modele.Bateau;
import bataille.modele.Capacite;
import bataille.modele.Coordonnee;
import bataille.modele.EtatCase;
import bataille.modele.Ordre;
import bataille.modele.Orientation;
import bataille.modele.PlacementAleatoire;
import bataille.modele.Plateau;
import bataille.modele.ResultatTir;
import bataille.modele.TypeBateau;
import bataille.modele.VuePlateau;
import bataille.partie.AnalysePartie;
import bataille.partie.Annonces;
import bataille.partie.Coup;
import bataille.partie.EcouteurPartie;
import bataille.partie.EnregistrementPartie;
import bataille.partie.EvenementPartie;
import bataille.partie.Joueur;
import bataille.partie.ParametresPartie;
import bataille.partie.Partie;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

/**
 * L'écran de jeu, en trois zones : la flotte du joueur à gauche, le déroulement de la partie au centre, la flotte
 * adverse à droite, chacune avec le suivi de ses bateaux.
 * <p>
 * Il conduit le placement des bateaux (poser, pivoter, déplacer un bateau posé, annuler), les tirs et les capacités
 * spéciales, le tour de l'ordinateur et, à deux, l'écran de passage qui cache les grilles entre les tours. Toutes les
 * règles passent par {@link Partie} ; la partie est enregistrée après chaque coup, pour la reprendre ou la revoir.
 */
final class EcranJeu extends JPanel {
    private static final long serialVersionUID = 1L;
    /** Le délai avant chaque coup de l'ordinateur, pour laisser voir le coup précédent. */
    private static final int DELAI_ORDINATEUR_MS = 700;
    /** Le délai entre la fin d'un tour et l'écran de passage, à deux. */
    private static final int DELAI_PASSAGE_MS = 1300;
    /** Le délai entre le dernier tir et le bilan, pour laisser voir l'explosion. */
    private static final int DELAI_FIN_MS = 900;
    /** La largeur de la zone centrale. */
    private static final int LARGEUR_CENTRE = 300;
    /** La carte des boutons du placement. */
    private static final String CARTE_PLACEMENT = "placement";
    /** La carte des capacités, pendant la partie. */
    private static final String CARTE_JEU = "jeu";
    /** La carte du passage de la souris, à deux. */
    private static final String CARTE_PASSAGE = "passage";
    /** La carte de la fin de partie. */
    private static final String CARTE_FIN = "fin";

    /** Les étapes de l'écran. */
    private enum Phase {
        /** Aucune partie. */
        INACTIVE,
        /** Un joueur place ses bateaux. */
        PLACEMENT,
        /** Les grilles sont cachées le temps que les joueurs se passent la souris. */
        PASSAGE,
        /** Les joueurs tirent. */
        JEU,
        /** La partie est terminée. */
        FIN
    }

    /** Ce que l'écran demande à la fenêtre. */
    interface Navigation {
        /** Revient à l'écran d'accueil. */
        void accueil();

        /**
         * Lance une nouvelle partie avec les mêmes réglages.
         *
         * @param parametres les réglages
         */
        void rejouer(ParametresPartie parametres);

        /**
         * Ouvre le rejeu d'une partie.
         *
         * @param enregistrement la partie à revoir
         */
        void revoir(EnregistrementPartie enregistrement);
    }

    /** Le générateur de hasard des placements et de l'ordinateur. */
    private final transient Random random;
    /** Les fichiers du jeu. */
    private final transient Dossiers dossiers;
    /** La fenêtre qui affiche cet écran. */
    private final transient Navigation navigation;
    /** Reçoit les événements de la partie pour lancer les animations. */
    private final transient EcouteurPartie ecouteur = this::surEvenement;
    /** Si les animations sont jouées. */
    private boolean animations = true;

    /** Les réglages de la partie. */
    private transient ParametresPartie parametres;
    /** La partie, ou {@code null} pendant le placement. */
    private transient Partie partie;
    /** Le plateau du joueur 1. */
    private transient Plateau plateau1;
    /** Le plateau du joueur 2, placé ici seulement à deux. */
    private transient Plateau plateau2;
    /** L'étape en cours. */
    private Phase phase = Phase.INACTIVE;

    /** Le plateau en cours de placement. */
    private transient Plateau plateauEnPlacement;
    /** Les bateaux qui restent à placer, le prochain en tête. */
    private final transient ArrayDeque<TypeBateau> aPlacer = new ArrayDeque<>();
    /** Les bateaux posés, le dernier en tête, pour « Annuler ». */
    private final transient ArrayDeque<Bateau> poses = new ArrayDeque<>();
    /** L'orientation du prochain bateau à placer. */
    private Orientation orientation = Orientation.HORIZONTALE;

    /** Le joueur dont l'écran montre le point de vue. */
    private transient Joueur spectateur;
    /** La capacité en cours de visée, ou {@code null} pour un tir simple. */
    private Capacite capaciteVisee;
    /** Le sens de la torpille en cours de visée. */
    private Orientation sensTorpille = Orientation.HORIZONTALE;
    /** Le début de la partie, pour sa durée. */
    private transient Instant debut;
    /** Si l'échec de la sauvegarde automatique a déjà été signalé. */
    private boolean echecSauvegardeSignale;
    /** Si la partie terminée a déjà été comptée dans les statistiques. */
    private boolean statistiquesEnregistrees;
    /** Ce qui suit l'écran de passage. */
    private transient Runnable apresPassage = () -> { };

    /** La grille du joueur. */
    private final PanneauGrille grilleJoueur = new PanneauGrille(10);
    /** La grille adverse. */
    private final PanneauGrille grilleAdverse = new PanneauGrille(10);
    /** Le suivi de la flotte du joueur. */
    private final PanneauFlotte flotteJoueur = new PanneauFlotte();
    /** Le suivi de la flotte adverse. */
    private final PanneauFlotte flotteAdverse = new PanneauFlotte();
    /** Le titre de la colonne du joueur. */
    private final TitledBorder titreJoueur = BorderFactory.createTitledBorder("Votre flotte");
    /** Le titre de la colonne adverse. */
    private final TitledBorder titreAdverse = BorderFactory.createTitledBorder("Flotte ennemie");
    /** À qui de jouer. */
    private final JLabel titre = new JLabel(" ");
    /** La dernière action et la consigne. */
    private final JLabel message = new JLabel(" ");
    /** Les boutons de l'étape en cours : une seule carte à la fois, à la hauteur de son contenu. */
    private final JPanel actions = new JPanel(new BorderLayout()) {
        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(LARGEUR_CENTRE, getPreferredSize().height);
        }
    };
    /** Les cartes de boutons, par nom. */
    private final HashMap<String, JComponent> cartes = new HashMap<>();
    /** Les coups joués. */
    private final JTextArea journal = new JTextArea(8, 20);
    /** Le bilan des tirs des deux joueurs. */
    private final JLabel indicateurs = new JLabel(" ");
    /** Pivote le prochain bateau. */
    private final JButton boutonPivoter = new JButton("Pivoter (R)");
    /** Reprend le dernier bateau posé. */
    private final JButton boutonAnnuler = new JButton("Annuler le dernier bateau");
    /** Place au hasard les bateaux restants. */
    private final JButton boutonHasard = new JButton("Placer le reste au hasard");
    /** Retire tous les bateaux. */
    private final JButton boutonRecommencer = new JButton("Tout reprendre");
    /** Valide la flotte placée. */
    private final JButton boutonValider = new JButton("Valider la flotte");
    /** Un bouton par capacité spéciale. */
    private final EnumMap<Capacite, JToggleButton> boutonsCapacites = new EnumMap<>(Capacite.class);
    /** Les boutons des capacités, cachés si la règle n'est pas activée. */
    private final JPanel panneauCapacites = new JPanel(new GridLayout(0, 2, 6, 6));
    /** Affiche les grilles du joueur suivant, à deux. */
    private final JButton boutonPret = new JButton("Afficher les grilles");
    /** Le minuteur du coup de l'ordinateur. */
    private final Timer minuteurOrdinateur;
    /** Le minuteur de l'écran de passage. */
    private final Timer minuteurPassage;
    /** Le minuteur du bilan de fin de partie. */
    private final Timer minuteurFin;

    /**
     * Construit l'écran, sans partie.
     *
     * @param random le générateur de hasard des placements et de l'ordinateur
     * @param dossiers les fichiers du jeu, pour la sauvegarde automatique et les statistiques
     * @param navigation la fenêtre qui affiche l'écran
     */
    EcranJeu(Random random, Dossiers dossiers, Navigation navigation) {
        super(new GridBagLayout());
        this.random = Objects.requireNonNull(random, "random");
        this.dossiers = Objects.requireNonNull(dossiers, "dossiers");
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        GridBagConstraints contraintes = new GridBagConstraints();
        contraintes.gridy = 0;
        contraintes.fill = GridBagConstraints.BOTH;
        contraintes.weighty = 1;
        contraintes.weightx = 0.5;
        add(new ColonneFlotte(grilleJoueur, flotteJoueur, titreJoueur), contraintes);
        contraintes.gridx = 1;
        contraintes.weightx = 0;
        contraintes.insets = new Insets(6, 14, 0, 14);
        add(creerCentre(), contraintes);
        contraintes.gridx = 2;
        contraintes.weightx = 0.5;
        contraintes.insets = new Insets(0, 0, 0, 0);
        add(new ColonneFlotte(grilleAdverse, flotteAdverse, titreAdverse), contraintes);

        grilleJoueur.surClic(this::placerSur);
        grilleJoueur.surSurvol(this::montrerApercu);
        grilleJoueur.surClicDroit(this::pivoter);
        grilleAdverse.surClic(this::viser);
        grilleAdverse.surSurvol(this::montrerVisee);
        grilleAdverse.surClicDroit(this::clicDroitAdverse);
        lier(KeyEvent.VK_R, "pivoter", this::pivoter);
        lier(KeyEvent.VK_ESCAPE, "annulerVisee", this::annulerVisee);

        minuteurOrdinateur = minuteur(DELAI_ORDINATEUR_MS, this::tourOrdinateur);
        minuteurPassage = minuteur(DELAI_PASSAGE_MS, this::commencerTour);
        minuteurFin = minuteur(DELAI_FIN_MS, this::terminer);
    }

    private static Timer minuteur(int delai, Runnable action) {
        Timer minuteur = new Timer(delai, evenement -> action.run());
        minuteur.setRepeats(false);
        return minuteur;
    }

    private void lier(int touche, String nom, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(touche, 0), nom);
        getActionMap().put(nom, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent evenement) {
                action.run();
            }
        });
    }

    private JPanel creerCentre() {
        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        titre.setFont(titre.getFont().deriveFont(Font.BOLD, 17f));
        JPanel placement = new JPanel(new GridLayout(0, 1, 0, 6));
        boutonValider.setFont(boutonValider.getFont().deriveFont(Font.BOLD));
        for (JButton bouton : List.of(boutonPivoter, boutonAnnuler, boutonHasard, boutonRecommencer, boutonValider)) {
            placement.add(bouton);
        }
        boutonPivoter.addActionListener(evenement -> pivoter());
        boutonAnnuler.addActionListener(evenement -> annulerPlacement());
        boutonHasard.addActionListener(evenement -> placerAuHasard());
        boutonRecommencer.addActionListener(evenement -> recommencerPlacement());
        boutonValider.addActionListener(evenement -> validerFlotte());

        for (Capacite capacite : Capacite.values()) {
            JToggleButton bouton = new JToggleButton(capacite.getNom());
            bouton.setToolTipText(capacite.getNom() + " : " + capacite.getDescription() + ".");
            bouton.addActionListener(evenement -> choisirCapacite(capacite));
            bouton.setMargin(new Insets(4, 4, 4, 4));
            boutonsCapacites.put(capacite, bouton);
            panneauCapacites.add(bouton);
        }
        JPanel jeu = new JPanel(new BorderLayout());
        jeu.add(panneauCapacites, BorderLayout.NORTH);

        JPanel passage = new JPanel(new BorderLayout());
        boutonPret.setFont(boutonPret.getFont().deriveFont(Font.BOLD, 14f));
        boutonPret.addActionListener(evenement -> {
            Runnable suite = apresPassage;
            apresPassage = () -> { };
            suite.run();
        });
        passage.add(boutonPret, BorderLayout.NORTH);

        JPanel fin = new JPanel(new GridLayout(0, 1, 0, 6));
        JButton rejouer = new JButton("Rejouer avec les mêmes réglages");
        rejouer.addActionListener(evenement -> navigation.rejouer(parametres));
        JButton revoir = new JButton("Revoir la partie");
        revoir.addActionListener(evenement -> enregistrement().ifPresent(navigation::revoir));
        JButton accueil = new JButton("Retour à l'accueil");
        accueil.addActionListener(evenement -> navigation.accueil());
        fin.add(rejouer);
        fin.add(revoir);
        fin.add(accueil);

        cartes.put(CARTE_PLACEMENT, placement);
        cartes.put(CARTE_JEU, jeu);
        cartes.put(CARTE_PASSAGE, passage);
        cartes.put(CARTE_FIN, fin);
        montrerCarte(CARTE_JEU);

        journal.setEditable(false);
        journal.setLineWrap(true);
        journal.setWrapStyleWord(true);
        journal.setFont(journal.getFont().deriveFont(12f));
        JScrollPane defilement = new JScrollPane(journal);
        defilement.setBorder(BorderFactory.createTitledBorder("Journal"));
        defilement.setPreferredSize(new Dimension(LARGEUR_CENTRE, 170));

        for (JComponent composant : List.of(titre, message, actions, defilement, indicateurs, creerLegende())) {
            composant.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        message.setVerticalAlignment(JLabel.TOP);
        centre.add(titre);
        centre.add(Box.createVerticalStrut(6));
        centre.add(message);
        centre.add(Box.createVerticalStrut(12));
        centre.add(actions);
        centre.add(Box.createVerticalStrut(12));
        centre.add(defilement);
        centre.add(Box.createVerticalStrut(8));
        centre.add(indicateurs);
        centre.add(Box.createVerticalStrut(8));
        centre.add(creerLegende());
        Dimension taille = new Dimension(LARGEUR_CENTRE, 10);
        centre.setPreferredSize(new Dimension(LARGEUR_CENTRE, centre.getPreferredSize().height));
        centre.setMinimumSize(taille);
        return centre;
    }

    private static JPanel creerLegende() {
        JPanel legende = new JPanel(new GridLayout(0, 3, 6, 2));
        legende.setAlignmentX(Component.LEFT_ALIGNMENT);
        EtatCase[] etats = {EtatCase.EAU, EtatCase.BATEAU, EtatCase.MANQUE, EtatCase.TOUCHE, EtatCase.COULE,
            EtatCase.VIDE};
        String[] libelles = {"Eau", "Bateau", "Manqué", "Touché", "Coulé", "Vide (radar)"};
        for (int i = 0; i < etats.length; i++) {
            JLabel libelle = new JLabel(libelles[i], new IconeEtat(etats[i], 14), JLabel.LEFT);
            libelle.setFont(libelle.getFont().deriveFont(11f));
            legende.add(libelle);
        }
        legende.setMaximumSize(new Dimension(LARGEUR_CENTRE, legende.getPreferredSize().height));
        return legende;
    }

    /**
     * Active ou désactive les animations.
     *
     * @param actives si les animations sont jouées
     */
    void setAnimations(boolean actives) {
        animations = actives;
        if (!actives) {
            grilleJoueur.arreterAnimations();
            grilleAdverse.arreterAnimations();
        }
    }

    /**
     * Lance une nouvelle partie : placement des bateaux, à la main ou au hasard, puis premier tour.
     *
     * @param choisis les réglages de la partie
     */
    void demarrer(ParametresPartie choisis) {
        arreter();
        parametres = Objects.requireNonNull(choisis, "choisis");
        partie = null;
        int taille = choisis.configuration().taille();
        plateau1 = new Plateau(taille);
        plateau2 = new Plateau(taille);
        preparerGrilles(taille);
        if (choisis.placementManuel()) {
            commencerPlacement(plateau1, choisis.nomJoueur1());
            return;
        }
        PlacementAleatoire.placer(plateau1, choisis.configuration().flotte(), random);
        if (!choisis.contreLOrdinateur()) {
            PlacementAleatoire.placer(plateau2, choisis.configuration().flotte(), random);
        }
        lancerPartie();
    }

    /**
     * Reprend une partie enregistrée là où elle s'était arrêtée.
     *
     * @param enregistrement la partie
     * @throws IllegalArgumentException si l'enregistrement ne peut pas être rejoué
     */
    void reprendre(EnregistrementPartie enregistrement) {
        Partie restauree = enregistrement.restaurer(random);
        arreter();
        parametres = enregistrement.parametres();
        partie = restauree;
        plateau1 = restauree.getJoueur1().getPlateau();
        plateau2 = restauree.getJoueur2().getPlateau();
        preparerGrilles(plateau1.getTaille());
        partie.ajouterEcouteur(ecouteur);
        debut = Instant.now();
        statistiquesEnregistrees = partie.estTerminee();
        commencerTour();
    }

    /** Arrête les minuteurs et les animations quand le joueur quitte l'écran ; la partie reste enregistrée. */
    void arreter() {
        minuteurOrdinateur.stop();
        minuteurPassage.stop();
        minuteurFin.stop();
        if (partie != null) {
            partie.retirerEcouteur(ecouteur);
        }
        capaciteVisee = null;
        grilleJoueur.arreterAnimations();
        grilleAdverse.arreterAnimations();
        phase = Phase.INACTIVE;
    }

    /**
     * Retourne la partie en cours, pour l'enregistrer sous un autre nom.
     *
     * @return la partie, ou une valeur vide pendant le placement ou sans partie
     */
    Optional<EnregistrementPartie> enregistrement() {
        return partie == null ? Optional.empty() : Optional.of(EnregistrementPartie.de(partie, parametres));
    }

    /**
     * Retourne les réglages de la dernière partie lancée.
     *
     * @return les réglages, ou une valeur vide si aucune partie n'a été lancée
     */
    Optional<ParametresPartie> getParametres() {
        return Optional.ofNullable(parametres);
    }

    private void preparerGrilles(int taille) {
        for (PanneauGrille grille : List.of(grilleJoueur, grilleAdverse)) {
            grille.afficher(taille, coordonnee -> EtatCase.INCONNUE, List.of());
            grille.setDetections(List.of());
            grille.setMarques(List.of());
            grille.setVisee(List.of());
            grille.setApercu(List.of(), true);
            grille.setInteractif(false);
            grille.setCache(null);
            grille.arreterAnimations();
        }
        panneauCapacites.setVisible(parametres.configuration().capacites());
        spectateur = null;
        journal.setText("");
        indicateurs.setText(" ");
        revalidate();
    }

    // ----- Placement -----

    private void commencerPlacement(Plateau plateau, String nom) {
        Runnable placer = () -> afficherPlacement(plateau, nom);
        if (parametres.contreLOrdinateur()) {
            placer.run();
        } else {
            passage(nom, "placer ses bateaux", placer);
        }
    }

    private void afficherPlacement(Plateau plateau, String nom) {
        phase = Phase.PLACEMENT;
        plateauEnPlacement = plateau;
        aPlacer.clear();
        aPlacer.addAll(parametres.configuration().flotte());
        poses.clear();
        orientation = Orientation.HORIZONTALE;
        grilleJoueur.setCache(null);
        grilleJoueur.setInteractif(true);
        grilleAdverse.setCache("Placez votre flotte\nsur la grille de gauche.");
        grilleAdverse.setInteractif(false);
        flotteAdverse.afficher(List.of());
        titreJoueur.setTitle(parametres.contreLOrdinateur() ? "Votre flotte" : "Votre flotte (" + nom + ")");
        titreAdverse.setTitle(parametres.contreLOrdinateur() ? "Flotte ennemie" : "Flotte adverse");
        journal.setText("");
        indicateurs.setText(" ");
        montrerCarte(CARTE_PLACEMENT);
        rafraichirPlacement();
    }

    private void rafraichirPlacement() {
        grilleJoueur.afficher(plateauEnPlacement.getTaille(), plateauEnPlacement::getEtatPourProprietaire,
                plateauEnPlacement.getBateaux());
        flotteJoueur.afficher(PanneauFlotte.pourPlacement(parametres.configuration().flotte(), plateauEnPlacement));
        boolean reste = !aPlacer.isEmpty();
        boutonPivoter.setEnabled(reste);
        boutonHasard.setEnabled(reste);
        boutonAnnuler.setEnabled(!poses.isEmpty());
        boutonRecommencer.setEnabled(!plateauEnPlacement.getBateaux().isEmpty());
        boutonValider.setEnabled(!reste);
        if (reste) {
            TypeBateau type = aPlacer.getFirst();
            annoncer("Placez " + type.getNomAvecArticle(), type.getLongueur() + " cases, "
                    + (orientation == Orientation.HORIZONTALE ? "à l'horizontale" : "à la verticale")
                    + " : cliquez sur votre grille.\nClic droit ou R pour pivoter. Cliquez sur un bateau posé pour "
                    + "le déplacer.");
        } else {
            annoncer("Votre flotte est prête", "Cliquez sur un bateau pour le déplacer, ou validez la flotte.");
        }
        montrerApercu(grilleJoueur.getSurvol());
        repaint();
    }

    private void montrerApercu(Coordonnee survol) {
        if (phase != Phase.PLACEMENT || survol == null || aPlacer.isEmpty() || !plateauEnPlacement.contient(survol)
                || plateauEnPlacement.getBateau(survol).isPresent()) {
            grilleJoueur.setApercu(List.of(), true);
            return;
        }
        TypeBateau type = aPlacer.getFirst();
        grilleJoueur.setApercu(new Bateau(type, survol, orientation).getCases(),
                plateauEnPlacement.peutPlacer(type, survol, orientation));
    }

    private void placerSur(Coordonnee coordonnee) {
        if (phase != Phase.PLACEMENT || !plateauEnPlacement.contient(coordonnee)) {
            return;
        }
        Optional<Bateau> existant = plateauEnPlacement.getBateau(coordonnee);
        if (existant.isPresent()) {
            Bateau bateau = existant.get();
            plateauEnPlacement.retirer(bateau);
            poses.remove(bateau);
            aPlacer.addFirst(bateau.getType());
            orientation = bateau.getOrientation();
            rafraichirPlacement();
            return;
        }
        if (aPlacer.isEmpty()) {
            return;
        }
        TypeBateau type = aPlacer.getFirst();
        Optional<String> refus = plateauEnPlacement.raisonRefusPlacement(type, coordonnee, orientation);
        if (refus.isPresent()) {
            rafraichirPlacement();
            annoncer("Placez " + type.getNomAvecArticle(), refus.get() + " Choisissez une autre case.");
            return;
        }
        poses.push(plateauEnPlacement.placer(type, coordonnee, orientation));
        aPlacer.removeFirst();
        rafraichirPlacement();
    }

    private void pivoter() {
        if (phase == Phase.PLACEMENT && !aPlacer.isEmpty()) {
            orientation = orientation.inverse();
            rafraichirPlacement();
        } else if (phase == Phase.JEU && capaciteVisee == Capacite.TORPILLE) {
            sensTorpille = sensTorpille.inverse();
            montrerVisee(grilleAdverse.getSurvol());
        }
    }

    private void annulerPlacement() {
        if (phase == Phase.PLACEMENT && !poses.isEmpty()) {
            Bateau dernier = poses.pop();
            plateauEnPlacement.retirer(dernier);
            aPlacer.addFirst(dernier.getType());
            orientation = dernier.getOrientation();
            rafraichirPlacement();
        }
    }

    private void placerAuHasard() {
        if (phase != Phase.PLACEMENT || aPlacer.isEmpty()) {
            return;
        }
        try {
            List<Bateau> places = PlacementAleatoire.placer(plateauEnPlacement, List.copyOf(aPlacer), random);
            places.forEach(poses::push);
            aPlacer.clear();
            rafraichirPlacement();
        } catch (IllegalStateException e) {
            annoncer("Plus assez de place", "Les bateaux restants ne trouvent pas de place : déplacez ou retirez un "
                    + "bateau posé.");
        }
    }

    private void recommencerPlacement() {
        if (phase == Phase.PLACEMENT) {
            plateauEnPlacement.retirerTout();
            poses.clear();
            aPlacer.clear();
            aPlacer.addAll(parametres.configuration().flotte());
            orientation = Orientation.HORIZONTALE;
            rafraichirPlacement();
        }
    }

    private void validerFlotte() {
        if (phase != Phase.PLACEMENT || !aPlacer.isEmpty()) {
            return;
        }
        grilleJoueur.setApercu(List.of(), true);
        grilleJoueur.setInteractif(false);
        if (!parametres.contreLOrdinateur() && plateauEnPlacement == plateau1) {
            commencerPlacement(plateau2, parametres.nomJoueur2());
        } else {
            lancerPartie();
        }
    }

    // ----- Partie -----

    private void lancerPartie() {
        partie = parametres.contreLOrdinateur() ? parametres.creerPartie(plateau1, random)
                : parametres.creerPartie(plateau1, plateau2, random);
        partie.ajouterEcouteur(ecouteur);
        debut = Instant.now();
        statistiquesEnregistrees = false;
        echecSauvegardeSignale = false;
        sauvegarderAuto();
        commencerTour();
    }

    /** Montre le tour qui commence : celui de l'ordinateur, ou celui d'un joueur, après l'écran de passage à deux. */
    private void commencerTour() {
        if (partie == null) {
            return;
        }
        if (partie.estTerminee()) {
            terminer();
            return;
        }
        Joueur courant = partie.getJoueurCourant();
        if (!courant.estHumain()) {
            spectateur = partie.getJoueur1();
            afficherJeu();
            annoncer("L'ordinateur joue…", "");
            minuteurOrdinateur.restart();
        } else if (parametres.contreLOrdinateur()) {
            spectateur = courant;
            afficherJeu();
        } else {
            passage(courant.getNom(), "jouer", () -> {
                spectateur = courant;
                afficherJeu();
            });
        }
    }

    private void passage(String nom, String objet, Runnable ensuite) {
        phase = Phase.PASSAGE;
        capaciteVisee = null;
        for (PanneauGrille grille : List.of(grilleJoueur, grilleAdverse)) {
            grille.setCache("Grille cachée");
            grille.setInteractif(false);
            grille.setApercu(List.of(), true);
            grille.setVisee(List.of());
        }
        flotteJoueur.afficher(List.of());
        flotteAdverse.afficher(List.of());
        titreJoueur.setTitle("Flotte cachée");
        titreAdverse.setTitle("Flotte cachée");
        journal.setText("");
        indicateurs.setText(" ");
        annoncer("Au tour " + Annonces.de(nom), "Passez la souris à " + nom + " pour " + objet
                + ". Les grilles restent cachées jusqu'au clic sur le bouton ci-dessous.");
        boutonPret.setText("Afficher les grilles " + Annonces.de(nom));
        apresPassage = ensuite;
        montrerCarte(CARTE_PASSAGE);
        repaint();
    }

    private void afficherJeu() {
        phase = Phase.JEU;
        grilleJoueur.setCache(null);
        grilleAdverse.setCache(null);
        grilleJoueur.setInteractif(false);
        grilleJoueur.setApercu(List.of(), true);
        montrerCarte(CARTE_JEU);
        rafraichirJeu();
        if (monTour()) {
            String recit = coupsAdverses();
            annoncer(titreTour(), (recit.isEmpty() ? "" : recit + "\n") + consigne());
        }
    }

    private boolean monTour() {
        return phase == Phase.JEU && partie != null && !partie.estTerminee() && spectateur != null
                && partie.getJoueurCourant() == spectateur && spectateur.estHumain();
    }

    private String titreTour() {
        int manche = (partie.getNumeroTour() + 1) / 2;
        return "Tour " + manche + " · " + (parametres.contreLOrdinateur() ? "à vous de jouer" : spectateur.getNom());
    }

    private String consigne() {
        return partie.capacitesDisponibles().isEmpty() ? "Cliquez sur une case de la grille ennemie pour tirer."
                : "Cliquez sur la grille ennemie pour tirer, ou choisissez une capacité.";
    }

    /** Raconte, du point de vue du spectateur, les coups adverses joués depuis son dernier coup. */
    private String coupsAdverses() {
        List<Coup> historique = partie.getHistorique();
        int debutRecit = historique.size();
        while (debutRecit > 0 && historique.get(debutRecit - 1).auteur() != spectateur) {
            debutRecit--;
        }
        return historique.subList(debutRecit, historique.size()).stream()
                .map(coup -> Annonces.decrire(coup, spectateur)).collect(Collectors.joining("\n"));
    }

    /** Retourne les cases visées par le dernier tour d'un joueur. */
    private List<Coordonnee> casesDuDernierTour(Joueur auteur) {
        List<Coup> historique = partie.getHistorique();
        int indice = historique.size() - 1;
        while (indice >= 0 && historique.get(indice).auteur() != auteur) {
            indice--;
        }
        List<Coordonnee> cases = new ArrayList<>();
        while (indice >= 0 && historique.get(indice).auteur() == auteur) {
            historique.get(indice).tirs().forEach(tir -> cases.add(tir.cible()));
            indice--;
        }
        return cases;
    }

    private void rafraichirJeu() {
        Joueur adversaire = partie.adversaireDe(spectateur);
        Plateau mien = spectateur.getPlateau();
        VuePlateau vue = adversaire.getPlateau().getVueAdverse();
        int taille = mien.getTaille();
        grilleJoueur.afficher(taille, mien::getEtatPourProprietaire, mien.getBateaux());
        grilleJoueur.setDetections(mien.getDetections());
        grilleJoueur.setMarques(casesDuDernierTour(adversaire));
        grilleAdverse.afficher(taille, vue::getEtat, vue.getBateauxCoules());
        grilleAdverse.setDetections(vue.getDetections());
        grilleAdverse.setMarques(casesDuDernierTour(spectateur));
        flotteJoueur.afficher(PanneauFlotte.pourProprietaire(mien));
        flotteAdverse.afficher(PanneauFlotte.pourAdversaire(parametres.configuration().flotte(), vue));
        boolean contreOrdinateur = parametres.contreLOrdinateur();
        titreJoueur.setTitle(contreOrdinateur ? "Votre flotte" : "Votre flotte (" + spectateur.getNom() + ")");
        titreAdverse.setTitle(contreOrdinateur ? "Flotte ennemie (difficulté "
                + parametres.difficulte().getNom().toLowerCase(Locale.ROOT) + ")" : Textes.flotte(adversaire));
        remplirJournal(spectateur);
        indicateurs.setText(Textes.paragraphe("Vous : " + Annonces.bilan(partie.getStatistiques(spectateur)) + "\n"
                + Annonces.sujet(adversaire) + " : " + Annonces.bilan(partie.getStatistiques(adversaire)),
                LARGEUR_CENTRE - 20));
        mettreAJourCapacites();
        grilleAdverse.setInteractif(monTour());
        repaint();
    }

    private void remplirJournal(Joueur lecteur) {
        journal.setText(partie.getHistorique().stream().map(coup -> "• " + Annonces.decrire(coup, lecteur))
                .collect(Collectors.joining("\n")));
        journal.setCaretPosition(journal.getDocument().getLength());
    }

    private void mettreAJourCapacites() {
        Set<Capacite> disponibles = monTour() ? partie.capacitesDisponibles() : Set.of();
        if (capaciteVisee != null && !disponibles.contains(capaciteVisee)) {
            capaciteVisee = null;
            grilleAdverse.setVisee(List.of());
        }
        for (Capacite capacite : Capacite.values()) {
            JToggleButton bouton = boutonsCapacites.get(capacite);
            int charges = partie == null || spectateur == null ? 0 : partie.getCharges(spectateur, capacite);
            bouton.setText(capacite.getNom() + " (" + charges + ")");
            bouton.setEnabled(disponibles.contains(capacite));
            bouton.setSelected(capacite == capaciteVisee);
        }
    }

    private void choisirCapacite(Capacite capacite) {
        if (!monTour() || !partie.capacitesDisponibles().contains(capacite)) {
            mettreAJourCapacites();
            return;
        }
        if (capacite == Capacite.DOUBLE_TIR) {
            capaciteVisee = null;
            jouer(Ordre.doubleTir());
            return;
        }
        capaciteVisee = capaciteVisee == capacite ? null : capacite;
        mettreAJourCapacites();
        montrerVisee(grilleAdverse.getSurvol());
        if (capaciteVisee == null) {
            annoncer(titreTour(), consigne());
        } else {
            annoncer(titreTour(), capacite.getNom() + " : " + capacite.getDescription()
                    + ".\nCliquez sur la grille ennemie pour viser"
                    + (capacite == Capacite.TORPILLE ? " ; clic droit ou R pour passer de la ligne à la colonne" : "")
                    + ". Échap pour annuler.");
        }
    }

    private void annulerVisee() {
        if (capaciteVisee != null) {
            capaciteVisee = null;
            grilleAdverse.setVisee(List.of());
            mettreAJourCapacites();
            if (monTour()) {
                annoncer(titreTour(), consigne());
            }
        }
    }

    private void clicDroitAdverse() {
        if (capaciteVisee == Capacite.TORPILLE) {
            pivoter();
        } else {
            annulerVisee();
        }
    }

    private void montrerVisee(Coordonnee survol) {
        if (!monTour() || capaciteVisee == null || survol == null) {
            grilleAdverse.setVisee(List.of());
            return;
        }
        grilleAdverse.setVisee(casesVisees(survol));
    }

    private List<Coordonnee> casesVisees(Coordonnee survol) {
        int taille = partie.getConfiguration().taille();
        List<Coordonnee> cases = new ArrayList<>();
        switch (capaciteVisee) {
            case RADAR -> {
                for (int deltaLigne = -1; deltaLigne <= 1; deltaLigne++) {
                    for (int deltaColonne = -1; deltaColonne <= 1; deltaColonne++) {
                        cases.add(survol.decaler(deltaLigne, deltaColonne));
                    }
                }
            }
            case BOMBARDEMENT -> {
                Coordonnee coin = coinBombardement(survol, taille);
                cases.addAll(List.of(coin, coin.decaler(0, 1), coin.decaler(1, 0), coin.decaler(1, 1)));
            }
            case TORPILLE -> {
                for (int i = 0; i < taille; i++) {
                    cases.add(sensTorpille == Orientation.HORIZONTALE ? new Coordonnee(survol.ligne(), i)
                            : new Coordonnee(i, survol.colonne()));
                }
            }
            default -> cases.add(survol);
        }
        return cases;
    }

    /** Recale le carré du bombardement pour qu'il reste dans la grille quand la souris est au bord. */
    private static Coordonnee coinBombardement(Coordonnee survol, int taille) {
        return new Coordonnee(Math.min(survol.ligne(), taille - 2), Math.min(survol.colonne(), taille - 2));
    }

    private void viser(Coordonnee cible) {
        if (!monTour()) {
            return;
        }
        Ordre ordre;
        if (capaciteVisee == null) {
            ordre = Ordre.tir(cible);
        } else {
            ordre = switch (capaciteVisee) {
                case RADAR -> Ordre.radar(cible);
                case BOMBARDEMENT -> Ordre.bombardement(coinBombardement(cible, partie.getConfiguration().taille()));
                case TORPILLE -> sensTorpille == Orientation.HORIZONTALE ? Ordre.torpilleSurLigne(cible.ligne())
                        : Ordre.torpilleSurColonne(cible.colonne());
                default -> Ordre.tir(cible);
            };
        }
        jouer(ordre);
    }

    private void jouer(Ordre ordre) {
        Coup coup;
        try {
            coup = partie.jouer(ordre);
        } catch (IllegalArgumentException | IllegalStateException e) {
            annoncer(titreTour(), e.getMessage());
            return;
        }
        if (ordre.estUnTirSimple() && coup.tirs().get(0).resultat() == ResultatTir.DEJA_JOUE) {
            annoncer(titreTour(), "Vous avez déjà tiré en " + ordre.cible() + " : choisissez une autre case.");
            return;
        }
        capaciteVisee = null;
        grilleAdverse.setVisee(List.of());
        String recit = Annonces.decrire(coup, spectateur);
        sauvegarderAuto();
        rafraichirJeu();
        if (partie.estTerminee()) {
            annoncer(titreTour(), recit);
            minuteurFin.setInitialDelay(animations ? DELAI_FIN_MS : 200);
            minuteurFin.restart();
        } else if (partie.getJoueurCourant() == spectateur) {
            annoncer(titreTour(), ordre.capacite() == Capacite.DOUBLE_TIR ? recit
                    : recit + "\nTirez une seconde fois.");
        } else if (parametres.contreLOrdinateur()) {
            annoncer("L'ordinateur joue…", recit);
            minuteurOrdinateur.restart();
        } else {
            annoncer("Tour terminé", recit + "\nLa main passe à " + partie.getJoueurCourant().getNom() + "…");
            minuteurPassage.restart();
        }
    }

    private void tourOrdinateur() {
        if (phase != Phase.JEU || partie == null || partie.estTerminee() || partie.getJoueurCourant().estHumain()) {
            return;
        }
        Coup coup = partie.faireJouerOrdinateur();
        sauvegarderAuto();
        rafraichirJeu();
        if (partie.estTerminee()) {
            annoncer("L'ordinateur joue…", Annonces.decrire(coup, spectateur));
            minuteurFin.setInitialDelay(animations ? DELAI_FIN_MS : 200);
            minuteurFin.restart();
        } else if (!partie.getJoueurCourant().estHumain()) {
            annoncer("L'ordinateur joue…", Annonces.decrire(coup, spectateur));
            minuteurOrdinateur.restart();
        } else {
            annoncer(titreTour(), coupsAdverses() + "\n" + consigne());
        }
    }

    private void surEvenement(EvenementPartie evenement) {
        if (!animations || spectateur == null) {
            return;
        }
        if (evenement instanceof EvenementPartie.TirEffectue tir) {
            grilleVisee(tir.tireur()).lancerEffet(tir.tir().resultat().estAuBut() ? Effet.EXPLOSION
                    : Effet.ECLABOUSSURE, List.of(tir.tir().cible()));
        } else if (evenement instanceof EvenementPartie.BateauCoule coule) {
            grilleVisee(coule.tireur()).lancerEffet(Effet.NAUFRAGE, coule.bateau().getCases());
        } else if (evenement instanceof EvenementPartie.CapaciteUtilisee capacite
                && capacite.detection().isPresent()) {
            grilleVisee(capacite.joueur()).lancerEffet(Effet.BALAYAGE, capacite.detection().get().cases());
        }
    }

    private PanneauGrille grilleVisee(Joueur tireur) {
        return tireur == spectateur ? grilleAdverse : grilleJoueur;
    }

    private void sauvegarderAuto() {
        try {
            FichierPartie.ecrire(EnregistrementPartie.de(partie, parametres), dossiers.dernierePartie());
        } catch (IOException e) {
            if (!echecSauvegardeSignale) {
                echecSauvegardeSignale = true;
                JOptionPane.showMessageDialog(this, "La partie ne peut pas être enregistrée automatiquement :\n"
                        + e.getMessage() + "\nElle ne pourra pas être reprise.", "Enregistrement impossible",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // ----- Fin de partie -----

    private void terminer() {
        if (partie == null || !partie.estTerminee() || phase == Phase.FIN) {
            return;
        }
        minuteurOrdinateur.stop();
        minuteurPassage.stop();
        phase = Phase.FIN;
        capaciteVisee = null;
        Joueur joueur1 = partie.getJoueur1();
        Joueur joueur2 = partie.getJoueur2();
        Joueur gagnant = partie.getGagnant().orElseThrow();
        boolean contreOrdinateur = parametres.contreLOrdinateur();
        spectateur = joueur1;
        int taille = plateau1.getTaille();
        grilleJoueur.setCache(null);
        grilleAdverse.setCache(null);
        grilleJoueur.afficher(taille, joueur1.getPlateau()::getEtatPourProprietaire, joueur1.getPlateau().getBateaux());
        grilleJoueur.setDetections(joueur1.getPlateau().getDetections());
        grilleAdverse.afficher(taille, joueur2.getPlateau()::getEtatPourProprietaire,
                joueur2.getPlateau().getBateaux());
        grilleAdverse.setDetections(joueur2.getPlateau().getDetections());
        for (PanneauGrille grille : List.of(grilleJoueur, grilleAdverse)) {
            grille.setMarques(List.of());
            grille.setVisee(List.of());
            grille.setInteractif(false);
        }
        flotteJoueur.afficher(PanneauFlotte.pourProprietaire(joueur1.getPlateau()));
        flotteAdverse.afficher(PanneauFlotte.pourProprietaire(joueur2.getPlateau()));
        titreJoueur.setTitle(contreOrdinateur ? "Votre flotte" : Textes.flotte(joueur1));
        titreAdverse.setTitle(contreOrdinateur ? "Flotte ennemie dévoilée" : Textes.flotte(joueur2));
        remplirJournal(joueur1);
        mettreAJourCapacites();
        boolean victoire = gagnant == joueur1;
        String titreFin;
        String phrase;
        if (contreOrdinateur) {
            titreFin = victoire ? "Victoire !" : "Défaite";
            phrase = victoire ? "Vous avez coulé toute la flotte ennemie." : "L'ordinateur a coulé toute votre flotte.";
        } else {
            titreFin = gagnant.getNom() + " gagne !";
            phrase = "Toute la flotte " + Annonces.de(partie.adversaireDe(gagnant).getNom()) + " est coulée.";
        }
        annoncer(titreFin, phrase);
        montrerCarte(CARTE_FIN);
        String statistiques = enregistrerStatistiques();
        repaint();
        SwingUtilities.invokeLater(() -> afficherBilan(titreFin, phrase, statistiques, victoire || !contreOrdinateur));
    }

    /**
     * Compte la partie dans les statistiques du joueur.
     *
     * @return le résumé des statistiques contre l'ordinateur, ou {@code null} à deux ou si elle était déjà comptée
     */
    private String enregistrerStatistiques() {
        if (statistiquesEnregistrees) {
            return null;
        }
        statistiquesEnregistrees = true;
        try {
            StatistiquesCumulees statistiques = StatistiquesCumulees.charger(dossiers.statistiques());
            if (parametres.contreLOrdinateur()) {
                statistiques.ajouterPartieContreOrdinateur(partie, parametres.difficulte(),
                        Duration.between(debut, Instant.now()));
            } else {
                statistiques.ajouterPartieADeux();
            }
            statistiques.sauvegarder();
            return parametres.contreLOrdinateur() ? ResumeStatistiques.enUneLigne(statistiques) : null;
        } catch (IOException | IllegalArgumentException e) {
            return "non enregistrées (" + e.getMessage() + ")";
        }
    }

    private void afficherBilan(String titreFin, String phrase, String statistiques, boolean heureux) {
        if (phase != Phase.FIN) {
            return;
        }
        Joueur joueur1 = partie.getJoueur1();
        Joueur joueur2 = partie.getJoueur2();
        boolean contreOrdinateur = parametres.contreLOrdinateur();
        StringBuilder texte = new StringBuilder(phrase).append("\n\n");
        texte.append(contreOrdinateur ? "Vous" : joueur1.getNom()).append(" : ")
                .append(Annonces.bilan(partie.getStatistiques(joueur1))).append('\n');
        texte.append(contreOrdinateur ? "Ordinateur" : joueur2.getNom()).append(" : ")
                .append(Annonces.bilan(partie.getStatistiques(joueur2))).append('\n');
        List<String> analyse = new ArrayList<>();
        if (contreOrdinateur) {
            analyse.addAll(AnalysePartie.raconter(partie, joueur1, joueur1));
            analyse.addAll(AnalysePartie.raconter(partie, joueur2, joueur1));
        } else {
            analyse.addAll(AnalysePartie.raconter(partie, joueur1));
            analyse.addAll(AnalysePartie.raconter(partie, joueur2));
        }
        if (!analyse.isEmpty()) {
            texte.append('\n').append(String.join("\n", analyse)).append('\n');
        }
        if (statistiques != null) {
            texte.append("\nVos statistiques : ").append(statistiques).append('.');
        }
        Object[] options = {"Rejouer", "Revoir la partie", "Fermer"};
        int choix = JOptionPane.showOptionDialog(this, new JLabel(Textes.paragraphe(texte.toString(), 430)),
                titreFin, JOptionPane.DEFAULT_OPTION,
                heureux ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choix == 0) {
            navigation.rejouer(parametres);
        } else if (choix == 1) {
            enregistrement().ifPresent(navigation::revoir);
        }
    }

    private void annoncer(String nouveauTitre, String nouveauMessage) {
        titre.setText(nouveauTitre);
        message.setText(nouveauMessage.isEmpty() ? " " : Textes.paragraphe(nouveauMessage, LARGEUR_CENTRE - 20));
    }

    private void montrerCarte(String carte) {
        actions.removeAll();
        actions.add(cartes.get(carte), BorderLayout.CENTER);
        actions.revalidate();
        actions.repaint();
    }
}
