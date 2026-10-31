package bataille.swing;

import bataille.modele.Bateau;
import bataille.modele.ConfigurationPartie;
import bataille.modele.Coordonnee;
import bataille.modele.EtatCase;
import bataille.modele.Orientation;
import bataille.modele.PlacementAleatoire;
import bataille.modele.Plateau;
import bataille.modele.Tir;
import bataille.modele.TypeBateau;
import bataille.modele.VuePlateau;
import bataille.partie.Annonces;
import bataille.partie.Joueur;
import bataille.partie.ParametresPartie;
import bataille.partie.Partie;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

/**
 * La fenêtre du jeu : la flotte du joueur à gauche, la flotte ennemie à droite, une barre d'état en haut.
 * <p>
 * Le joueur tire en cliquant sur la grille de droite ; l'ordinateur répond tout seul après un court délai,
 * pendant lequel les clics sont ignorés. Toutes les règles passent par {@link Partie}.
 */
public final class FenetreJeu extends JFrame {
    private static final long serialVersionUID = 1L;
    /** Le délai avant le tir de l'ordinateur, pour laisser voir le résultat du tir du joueur. */
    private static final int DELAI_ORDINATEUR_MS = 600;
    /** La taille des icônes de la légende. */
    private static final int COTE_ICONE = 16;

    /** Le générateur de hasard des placements et de l'ordinateur. */
    private final transient Random random;
    /** Les derniers paramètres choisis. */
    private transient ParametresPartie parametres = ParametresPartie.parDefaut();
    /** La partie en cours, ou {@code null} pendant le placement. */
    private transient Partie partie;
    /** Le plateau du joueur, en cours de placement ou de jeu. */
    private transient Plateau plateauJoueur;
    /** Les bateaux à placer à la main, vide hors placement. */
    private transient List<TypeBateau> aPlacer = List.of();
    /** L'indice du prochain bateau à placer. */
    private int indexAPlacer;
    /** L'orientation du prochain bateau à placer. */
    private Orientation orientation = Orientation.HORIZONTALE;

    /** La grille du joueur. */
    private final PanneauGrille grilleJoueur = new PanneauGrille(ConfigurationPartie.TAILLE_PAR_DEFAUT);
    /** La grille de la flotte ennemie. */
    private final PanneauGrille grilleAdverse = new PanneauGrille(ConfigurationPartie.TAILLE_PAR_DEFAUT);
    /** Le titre au-dessus de la grille du joueur. */
    private final TitledBorder titreJoueur = BorderFactory.createTitledBorder("Votre flotte");
    /** Le titre au-dessus de la grille ennemie. */
    private final TitledBorder titreAdverse = BorderFactory.createTitledBorder("Flotte ennemie");
    /** La barre d'état : à qui de jouer, et le résultat du dernier tir. */
    private final JLabel etat = new JLabel(" ");
    /** Le nombre de bateaux à flot de chaque côté. */
    private final JLabel bilan = new JLabel(" ");
    /** Les boutons du placement manuel. */
    private final JPanel boutonsPlacement = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
    /** Le minuteur qui déclenche le tir de l'ordinateur. */
    private final Timer minuteurOrdinateur;

    /**
     * Construit la fenêtre, sans partie en cours : appeler ensuite {@link #nouvellePartie()}.
     *
     * @param random le générateur de hasard des placements et de l'ordinateur
     */
    public FenetreJeu(Random random) {
        super("Bataille navale");
        this.random = Objects.requireNonNull(random, "random");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setJMenuBar(creerMenu());

        JPanel contenu = new JPanel(new BorderLayout(0, 10));
        contenu.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        etat.setFont(etat.getFont().deriveFont(Font.BOLD, 15f));
        contenu.add(etat, BorderLayout.NORTH);

        JPanel grilles = new JPanel(new GridLayout(1, 2, 24, 0));
        grilles.add(encadrer(grilleJoueur, titreJoueur));
        grilles.add(encadrer(grilleAdverse, titreAdverse));
        contenu.add(grilles, BorderLayout.CENTER);

        JButton pivoter = new JButton("Pivoter (R)");
        pivoter.addActionListener(evenement -> pivoter());
        JButton hasard = new JButton("Placer le reste au hasard");
        hasard.addActionListener(evenement -> placerLeResteAuHasard());
        JButton recommencer = new JButton("Recommencer le placement");
        recommencer.addActionListener(evenement -> recommencerPlacement());
        boutonsPlacement.add(pivoter);
        boutonsPlacement.add(hasard);
        boutonsPlacement.add(recommencer);
        boutonsPlacement.setVisible(false);

        JPanel bas = new JPanel(new BorderLayout(0, 8));
        bas.add(boutonsPlacement, BorderLayout.NORTH);
        bas.add(creerLegende(), BorderLayout.WEST);
        bas.add(bilan, BorderLayout.EAST);
        contenu.add(bas, BorderLayout.SOUTH);
        setContentPane(contenu);

        grilleAdverse.surClic(this::tirerSur);
        grilleJoueur.surClic(this::placerSur);
        grilleJoueur.surSurvol(this::montrerApercu);
        grilleJoueur.surClicDroit(this::pivoter);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "pivoter");
        getRootPane().getActionMap().put("pivoter", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent evenement) {
                pivoter();
            }
        });

        minuteurOrdinateur = new Timer(DELAI_ORDINATEUR_MS, evenement -> faireJouerOrdinateur());
        minuteurOrdinateur.setRepeats(false);
        pack();
        setLocationRelativeTo(null);
    }

    private static JPanel encadrer(PanneauGrille grille, TitledBorder titre) {
        Font police = UIManager.getFont("TitledBorder.font");
        if (police != null) {
            titre.setTitleFont(police.deriveFont(Font.BOLD, 14f));
        }
        JPanel cadre = new JPanel(new BorderLayout());
        cadre.setBorder(BorderFactory.createCompoundBorder(titre, BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        cadre.add(grille, BorderLayout.CENTER);
        return cadre;
    }

    private JMenuBar creerMenu() {
        JMenu menu = new JMenu("Partie");
        menu.setMnemonic(KeyEvent.VK_P);
        JMenuItem nouvelle = new JMenuItem("Nouvelle partie…");
        nouvelle.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        nouvelle.addActionListener(evenement -> nouvellePartie());
        JMenuItem rejouer = new JMenuItem("Rejouer avec les mêmes réglages");
        rejouer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        rejouer.addActionListener(evenement -> demarrer(parametres));
        JMenuItem quitter = new JMenuItem("Quitter");
        quitter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        quitter.addActionListener(evenement -> dispose());
        menu.add(nouvelle);
        menu.add(rejouer);
        menu.addSeparator();
        menu.add(quitter);
        JMenuBar barre = new JMenuBar();
        barre.add(menu);
        return barre;
    }

    private static JPanel creerLegende() {
        JPanel legende = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        legende.add(new JLabel("Eau", new IconeEtat(EtatCase.EAU, COTE_ICONE), JLabel.LEFT));
        legende.add(new JLabel("Bateau", new IconeEtat(EtatCase.BATEAU, COTE_ICONE), JLabel.LEFT));
        legende.add(new JLabel("Manqué", new IconeEtat(EtatCase.MANQUE, COTE_ICONE), JLabel.LEFT));
        legende.add(new JLabel("Touché", new IconeEtat(EtatCase.TOUCHE, COTE_ICONE), JLabel.LEFT));
        legende.add(new JLabel("Coulé", new IconeEtat(EtatCase.COULE, COTE_ICONE), JLabel.LEFT));
        return legende;
    }

    /**
     * Demande les paramètres d'une nouvelle partie, puis la lance. Si le joueur annule alors qu'aucune partie
     * n'est en cours, une partie est lancée avec les paramètres par défaut.
     */
    public void nouvellePartie() {
        Optional<ParametresPartie> choix = DialogueParametres.demander(this, parametres);
        if (choix.isPresent()) {
            demarrer(choix.get());
        } else if (partie == null && !enPlacement()) {
            demarrer(parametres);
        }
    }

    /**
     * Lance une partie : placement manuel ou aléatoire de la flotte du joueur, puis premier tir.
     *
     * @param choisis les paramètres de la partie
     */
    void demarrer(ParametresPartie choisis) {
        minuteurOrdinateur.stop();
        parametres = choisis;
        partie = null;
        ConfigurationPartie configuration = choisis.configuration();
        plateauJoueur = new Plateau(configuration.taille());
        titreJoueur.setTitle("Votre flotte (" + choisis.nomJoueur() + ")");
        String difficulte = choisis.difficulte().getNom().toLowerCase(Locale.ROOT);
        titreAdverse.setTitle("Flotte ennemie (difficulté " + difficulte + ")");
        grilleAdverse.afficher(configuration.taille(), coordonnee -> EtatCase.INCONNUE, List.of());
        grilleAdverse.setInteractif(false);
        grilleJoueur.setDernierTir(null);
        if (choisis.placementManuel()) {
            aPlacer = configuration.flotte();
            indexAPlacer = 0;
            orientation = Orientation.HORIZONTALE;
            boutonsPlacement.setVisible(true);
            grilleJoueur.setInteractif(true);
            afficherPlacement();
        } else {
            PlacementAleatoire.placer(plateauJoueur, configuration.flotte(), random);
            commencerPartie();
        }
        pack();
    }

    private boolean enPlacement() {
        return partie == null && indexAPlacer < aPlacer.size();
    }

    private void afficherPlacement() {
        int taille = plateauJoueur.getTaille();
        grilleJoueur.afficher(taille, plateauJoueur::getEtatPourProprietaire, plateauJoueur.getBateaux());
        TypeBateau type = aPlacer.get(indexAPlacer);
        etat.setText("Placez " + type.getNomAvecArticle() + " (" + type.getLongueur() + " cases, "
                + (orientation == Orientation.HORIZONTALE ? "horizontal" : "vertical")
                + ") : cliquez sur votre grille. Clic droit ou R pour pivoter.");
        bilan.setText("Bateau " + (indexAPlacer + 1) + " sur " + aPlacer.size());
    }

    private void montrerApercu(Coordonnee survol) {
        if (!enPlacement() || survol == null) {
            grilleJoueur.setApercu(List.of(), true);
            return;
        }
        TypeBateau type = aPlacer.get(indexAPlacer);
        Bateau candidat = new Bateau(type, survol, orientation);
        grilleJoueur.setApercu(candidat.getCases(), plateauJoueur.peutPlacer(type, survol, orientation));
    }

    private void placerSur(Coordonnee coordonnee) {
        if (!enPlacement()) {
            return;
        }
        TypeBateau type = aPlacer.get(indexAPlacer);
        Optional<String> refus = plateauJoueur.raisonRefusPlacement(type, coordonnee, orientation);
        if (refus.isPresent()) {
            etat.setText(refus.get() + " Choisissez une autre case.");
            return;
        }
        plateauJoueur.placer(type, coordonnee, orientation);
        indexAPlacer++;
        if (indexAPlacer == aPlacer.size()) {
            terminerPlacement();
        } else {
            afficherPlacement();
            montrerApercu(coordonnee);
        }
    }

    private void pivoter() {
        if (enPlacement()) {
            orientation = orientation.inverse();
            afficherPlacement();
            montrerApercu(grilleJoueur.getSurvol());
        }
    }

    private void placerLeResteAuHasard() {
        if (enPlacement()) {
            PlacementAleatoire.placer(plateauJoueur, aPlacer.subList(indexAPlacer, aPlacer.size()), random);
            terminerPlacement();
        }
    }

    private void recommencerPlacement() {
        if (enPlacement()) {
            plateauJoueur.retirerTout();
            indexAPlacer = 0;
            afficherPlacement();
        }
    }

    private void terminerPlacement() {
        aPlacer = List.of();
        indexAPlacer = 0;
        boutonsPlacement.setVisible(false);
        grilleJoueur.setApercu(List.of(), true);
        grilleJoueur.setInteractif(false);
        commencerPartie();
    }

    private void commencerPartie() {
        partie = parametres.creerPartie(plateauJoueur, random);
        rafraichirGrilles();
        grilleAdverse.setInteractif(true);
        etat.setText("À vous de tirer : cliquez sur une case de la flotte ennemie.");
    }

    private void rafraichirGrilles() {
        Joueur joueur = partie.getJoueur1();
        Plateau plateauAdverse = partie.getJoueur2().getPlateau();
        VuePlateau vue = plateauAdverse.getVueAdverse();
        int taille = plateauAdverse.getTaille();
        grilleJoueur.afficher(taille, joueur.getPlateau()::getEtatPourProprietaire, joueur.getPlateau().getBateaux());
        grilleAdverse.afficher(taille, vue::getEtat, vue.getBateauxCoules());
        int nombre = partie.getConfiguration().flotte().size();
        bilan.setText("Bateaux à flot : vous " + joueur.getPlateau().getNombreBateauxRestants() + "/" + nombre
                + "  ·  ordinateur " + plateauAdverse.getNombreBateauxRestants() + "/" + nombre);
    }

    private void tirerSur(Coordonnee cible) {
        if (partie == null || partie.estTerminee() || !partie.getJoueurCourant().estHumain()) {
            return;
        }
        if (partie.getJoueur2().getPlateau().getVueAdverse().getEtat(cible) != EtatCase.INCONNUE) {
            etat.setText("Vous avez déjà tiré en " + cible + " : choisissez une autre case.");
            return;
        }
        Tir tir = partie.tirer(cible);
        rafraichirGrilles();
        if (partie.estTerminee()) {
            terminerPartie();
            return;
        }
        grilleAdverse.setInteractif(false);
        etat.setText(Annonces.tirDuJoueur(tir) + " L'ordinateur vise…");
        minuteurOrdinateur.restart();
    }

    private void faireJouerOrdinateur() {
        if (partie == null || partie.estTerminee() || partie.getJoueurCourant().estHumain()) {
            return;
        }
        Tir tir = partie.faireJouerOrdinateur();
        grilleJoueur.setDernierTir(tir.cible());
        rafraichirGrilles();
        if (partie.estTerminee()) {
            terminerPartie();
            return;
        }
        grilleAdverse.setInteractif(true);
        etat.setText(Annonces.tirDeLOrdinateur(tir) + " À vous de tirer.");
    }

    private void terminerPartie() {
        grilleAdverse.setInteractif(false);
        Joueur joueur = partie.getJoueur1();
        Joueur ordinateur = partie.getJoueur2();
        Plateau plateauAdverse = ordinateur.getPlateau();
        grilleAdverse.afficher(plateauAdverse.getTaille(), plateauAdverse::getEtatPourProprietaire,
                plateauAdverse.getBateaux());
        titreAdverse.setTitle("Flotte ennemie dévoilée");
        repaint();
        boolean victoire = partie.getGagnant().orElseThrow() == joueur;
        String titre = victoire ? "Victoire !" : "Défaite";
        String phrase = victoire ? "Vous avez coulé toute la flotte ennemie."
                : "L'ordinateur a coulé toute votre flotte.";
        etat.setText(titre + " " + phrase);
        String message = phrase + "\n\nVous : " + Annonces.bilan(partie.getStatistiques(joueur))
                + "\nOrdinateur : " + Annonces.bilan(partie.getStatistiques(ordinateur));
        Object[] options = {"Rejouer", "Nouvelle partie…", "Fermer"};
        int choix = JOptionPane.showOptionDialog(this, message, titre, JOptionPane.DEFAULT_OPTION,
                victoire ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choix == 0) {
            demarrer(parametres);
        } else if (choix == 1) {
            nouvellePartie();
        }
    }
}
