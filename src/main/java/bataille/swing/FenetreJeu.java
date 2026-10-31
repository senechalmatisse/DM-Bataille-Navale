package bataille.swing;

import bataille.donnees.Dossiers;
import bataille.donnees.FichierPartie;
import bataille.donnees.PreferencesJeu;
import bataille.donnees.ResumeStatistiques;
import bataille.donnees.StatistiquesCumulees;
import bataille.modele.Capacite;
import bataille.partie.EnregistrementPartie;
import bataille.partie.ModeJeu;
import bataille.partie.ParametresPartie;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * La fenêtre du jeu. Elle passe d'un écran à l'autre (accueil, partie, rejeu, statistiques), porte les menus et
 * s'occupe des fichiers : préférences, parties enregistrées et statistiques.
 */
public final class FenetreJeu extends JFrame implements EcranJeu.Navigation, EcranAccueil.Actions {
    private static final long serialVersionUID = 1L;
    /** L'écran d'accueil. */
    private static final String ACCUEIL = "accueil";
    /** L'écran de jeu. */
    private static final String JEU = "jeu";
    /** L'écran de rejeu. */
    private static final String REJEU = "rejeu";
    /** L'écran des statistiques. */
    private static final String STATISTIQUES = "statistiques";

    /** Les fichiers du jeu. */
    private final transient Dossiers dossiers;
    /** Les préférences : animations et derniers réglages. */
    private final transient PreferencesJeu preferences;
    /** Les écrans, un seul visible à la fois. */
    private final JPanel ecrans = new JPanel(new CardLayout());
    /** L'écran d'accueil. */
    private final EcranAccueil accueil;
    /** L'écran de jeu. */
    private final EcranJeu jeu;
    /** L'écran de rejeu. */
    private final EcranRejeu rejeu;
    /** L'écran des statistiques. */
    private final EcranStatistiques statistiques;
    /** L'écran affiché. */
    private String ecranCourant = ACCUEIL;
    /** Relance une partie avec les mêmes réglages. */
    private final JMenuItem menuRejouer = new JMenuItem("Rejouer avec les mêmes réglages");
    /** Enregistre la partie en cours sous un autre nom. */
    private final JMenuItem menuEnregistrer = new JMenuItem("Enregistrer la partie sous…");
    /** Active ou désactive les animations. */
    private final JCheckBoxMenuItem menuAnimations = new JCheckBoxMenuItem("Animations");

    /**
     * Construit la fenêtre sur l'écran d'accueil.
     *
     * @param random le générateur de hasard des placements et de l'ordinateur
     * @param dossiers les fichiers du jeu
     */
    public FenetreJeu(Random random, Dossiers dossiers) {
        super("Bataille navale");
        Objects.requireNonNull(random, "random");
        this.dossiers = Objects.requireNonNull(dossiers, "dossiers");
        this.preferences = PreferencesJeu.charger(dossiers.preferences());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        accueil = new EcranAccueil(this);
        jeu = new EcranJeu(random, dossiers, this);
        rejeu = new EcranRejeu(this::accueil);
        statistiques = new EcranStatistiques(this::accueil, this::reinitialiserStatistiques);
        ecrans.add(accueil, ACCUEIL);
        ecrans.add(jeu, JEU);
        ecrans.add(rejeu, REJEU);
        ecrans.add(statistiques, STATISTIQUES);
        setContentPane(ecrans);
        setJMenuBar(creerMenus());
        appliquerAnimations(preferences.animations());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent evenement) {
                jeu.arreter();
                rejeu.arreter();
            }
        });
        actualiserAccueil();
        pack();
        setMinimumSize(new Dimension(Math.min(getWidth(), 980), Math.min(getHeight(), 620)));
        setLocationRelativeTo(null);
    }

    private JMenuBar creerMenus() {
        JMenu partie = new JMenu("Partie");
        partie.setMnemonic(KeyEvent.VK_P);
        partie.add(element("Nouvelle partie…", KeyEvent.VK_N, () -> nouvellePartie(null)));
        menuRejouer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        menuRejouer.addActionListener(evenement -> jeu.getParametres().ifPresent(this::demarrer));
        partie.add(menuRejouer);
        partie.addSeparator();
        partie.add(element("Ouvrir une partie…", KeyEvent.VK_O, this::ouvrir));
        menuEnregistrer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        menuEnregistrer.addActionListener(evenement -> enregistrerSous());
        partie.add(menuEnregistrer);
        partie.addSeparator();
        partie.add(element("Revoir la dernière partie", 0, this::revoirDernierePartie));
        partie.add(element("Statistiques", 0, this::statistiques));
        partie.add(element("Accueil", 0, this::accueil));
        partie.addSeparator();
        partie.add(element("Quitter", KeyEvent.VK_Q, this::quitter));
        partie.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent evenement) {
                menuRejouer.setEnabled(jeu.getParametres().isPresent());
                menuEnregistrer.setEnabled(JEU.equals(ecranCourant) && jeu.enregistrement().isPresent());
            }

            @Override
            public void menuDeselected(MenuEvent evenement) {
                // rien à faire quand le menu se referme
            }

            @Override
            public void menuCanceled(MenuEvent evenement) {
                // rien à faire quand le menu est abandonné
            }
        });

        JMenu options = new JMenu("Options");
        options.setMnemonic(KeyEvent.VK_O);
        menuAnimations.addActionListener(evenement -> {
            appliquerAnimations(menuAnimations.isSelected());
            preferences.setAnimations(menuAnimations.isSelected());
            sauvegarderPreferences();
        });
        options.add(menuAnimations);

        JMenu aide = new JMenu("Aide");
        aide.setMnemonic(KeyEvent.VK_A);
        aide.add(element("Règles et commandes", KeyEvent.VK_F1, this::afficherRegles));

        JMenuBar barre = new JMenuBar();
        barre.add(partie);
        barre.add(options);
        barre.add(aide);
        return barre;
    }

    private static JMenuItem element(String texte, int touche, Runnable action) {
        JMenuItem element = new JMenuItem(texte);
        if (touche == KeyEvent.VK_F1) {
            element.setAccelerator(KeyStroke.getKeyStroke(touche, 0));
        } else if (touche != 0) {
            element.setAccelerator(KeyStroke.getKeyStroke(touche, InputEvent.CTRL_DOWN_MASK));
        }
        element.addActionListener(evenement -> action.run());
        return element;
    }

    private void appliquerAnimations(boolean actives) {
        menuAnimations.setSelected(actives);
        jeu.setAnimations(actives);
        rejeu.setAnimations(actives);
    }

    private void montrer(String ecran) {
        ecranCourant = ecran;
        ((CardLayout) ecrans.getLayout()).show(ecrans, ecran);
    }

    // ----- Navigation -----

    @Override
    public void accueil() {
        jeu.arreter();
        rejeu.arreter();
        actualiserAccueil();
        montrer(ACCUEIL);
    }

    @Override
    public void rejouer(ParametresPartie parametres) {
        demarrer(parametres);
    }

    @Override
    public void revoir(EnregistrementPartie enregistrement) {
        try {
            rejeu.charger(enregistrement);
        } catch (IllegalArgumentException e) {
            erreur("Cette partie ne peut pas être revue", e.getMessage());
            return;
        }
        jeu.arreter();
        montrer(REJEU);
    }

    @Override
    public void jouerContreOrdinateur() {
        nouvellePartie(ModeJeu.CONTRE_ORDINATEUR);
    }

    @Override
    public void jouerADeux() {
        nouvellePartie(ModeJeu.DEUX_JOUEURS);
    }

    @Override
    public void reprendre() {
        lireDernierePartie().ifPresent(this::reprendre);
    }

    @Override
    public void revoirDernierePartie() {
        lireDernierePartie().ifPresent(this::revoir);
    }

    @Override
    public void ouvrir() {
        JFileChooser selecteur = selecteur();
        if (selecteur.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        EnregistrementPartie enregistrement;
        boolean terminee;
        try {
            enregistrement = FichierPartie.lire(selecteur.getSelectedFile().toPath());
            terminee = enregistrement.estTerminee();
        } catch (IOException | IllegalArgumentException e) {
            erreur("Ouverture impossible", e.getMessage());
            return;
        }
        if (terminee) {
            revoir(enregistrement);
        } else {
            reprendre(enregistrement);
        }
    }

    @Override
    public void statistiques() {
        jeu.arreter();
        rejeu.arreter();
        try {
            statistiques.afficher(ResumeStatistiques.lignes(StatistiquesCumulees.charger(dossiers.statistiques())),
                    null);
        } catch (IOException | IllegalArgumentException e) {
            statistiques.afficher(List.of(), e.getMessage());
        }
        montrer(STATISTIQUES);
    }

    @Override
    public void quitter() {
        dispose();
    }

    /**
     * Demande les réglages d'une nouvelle partie, puis la lance.
     *
     * @param mode le mode proposé, ou {@code null} pour celui de la partie précédente
     */
    void nouvellePartie(ModeJeu mode) {
        ParametresPartie precedents = preferences.derniersParametres();
        Optional<ParametresPartie> choix = DialogueParametres.demander(this,
                mode == null ? precedents : precedents.avecMode(mode));
        choix.ifPresent(this::demarrer);
    }

    private void demarrer(ParametresPartie parametres) {
        preferences.setDerniersParametres(parametres);
        sauvegarderPreferences();
        rejeu.arreter();
        montrer(JEU);
        jeu.demarrer(parametres);
    }

    private void reprendre(EnregistrementPartie enregistrement) {
        rejeu.arreter();
        try {
            jeu.reprendre(enregistrement);
        } catch (IllegalArgumentException e) {
            erreur("Reprise impossible", e.getMessage());
            return;
        }
        montrer(JEU);
    }

    private Optional<EnregistrementPartie> lireDernierePartie() {
        try {
            return Optional.of(FichierPartie.lire(dossiers.dernierePartie()));
        } catch (IOException e) {
            erreur("Lecture impossible", e.getMessage());
            actualiserAccueil();
            return Optional.empty();
        }
    }

    private void enregistrerSous() {
        Optional<EnregistrementPartie> enregistrement = jeu.enregistrement();
        if (!JEU.equals(ecranCourant) || enregistrement.isEmpty()) {
            return;
        }
        JFileChooser selecteur = selecteur();
        String nom = "partie-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")) + ".txt";
        selecteur.setSelectedFile(new File(selecteur.getCurrentDirectory(), nom));
        if (selecteur.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path fichier = selecteur.getSelectedFile().toPath();
        if (!fichier.getFileName().toString().contains(".")) {
            fichier = fichier.resolveSibling(fichier.getFileName() + ".txt");
        }
        if (Files.exists(fichier) && JOptionPane.showConfirmDialog(this, "Remplacer " + fichier.getFileName() + " ?",
                "Le fichier existe déjà", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            FichierPartie.ecrire(enregistrement.get(), fichier);
        } catch (IOException e) {
            erreur("Enregistrement impossible", e.getMessage());
        }
    }

    private JFileChooser selecteur() {
        Path dossier = dossiers.parties();
        try {
            Files.createDirectories(dossier);
        } catch (IOException e) {
            dossier = Path.of(System.getProperty("user.home"));
        }
        JFileChooser selecteur = new JFileChooser(dossier.toFile());
        selecteur.setFileFilter(new FileNameExtensionFilter("Parties de bataille navale (*.txt)", "txt"));
        return selecteur;
    }

    private void reinitialiserStatistiques() {
        int reponse = JOptionPane.showConfirmDialog(this, "Effacer toutes vos statistiques ? Cette action est "
                + "définitive.", "Réinitialiser les statistiques", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (reponse != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            StatistiquesCumulees cumul = StatistiquesCumulees.charger(dossiers.statistiques());
            cumul.reinitialiser();
            cumul.sauvegarder();
        } catch (IOException | IllegalArgumentException e) {
            erreur("Réinitialisation impossible", e.getMessage());
        }
        statistiques();
    }

    /** Met à jour l'accueil d'après les fichiers : partie à reprendre, dernière partie, statistiques. */
    private void actualiserAccueil() {
        Optional<String> interrompue = Optional.empty();
        boolean derniere = false;
        if (Files.isRegularFile(dossiers.dernierePartie())) {
            try {
                EnregistrementPartie enregistrement = FichierPartie.lire(dossiers.dernierePartie());
                derniere = true;
                if (!enregistrement.estTerminee()) {
                    interrompue = Optional.of(enregistrement.decrire());
                }
            } catch (IOException | IllegalArgumentException e) {
                derniere = false;
            }
        }
        Optional<String> resume = Optional.empty();
        try {
            StatistiquesCumulees cumul = StatistiquesCumulees.charger(dossiers.statistiques());
            if (cumul.getParties() > 0) {
                resume = Optional.of(ResumeStatistiques.enUneLigne(cumul));
            }
        } catch (IOException | IllegalArgumentException e) {
            resume = Optional.empty();
        }
        accueil.actualiser(interrompue, derniere, resume);
    }

    private void sauvegarderPreferences() {
        try {
            preferences.sauvegarder();
        } catch (IOException e) {
            // les préférences ne seront simplement pas retenues pour la prochaine fois
        }
    }

    private void erreur(String titre, String message) {
        JOptionPane.showMessageDialog(this, new JLabel(Textes.paragraphe(message, 380)), titre,
                JOptionPane.ERROR_MESSAGE);
    }

    private void afficherRegles() {
        StringBuilder texte = new StringBuilder();
        texte.append("Chaque joueur cache sa flotte sur sa grille. À tour de rôle, on tire sur une case de la grille ")
                .append("adverse : manqué, touché, ou coulé quand toutes les cases d'un bateau sont touchées. ")
                .append("Le premier qui coule toute la flotte adverse gagne.\n\n")
                .append("Capacités spéciales (si elles sont activées) : chacune sert une fois par partie, une seule ")
                .append("par tour, avant de tirer.\n");
        for (Capacite capacite : Capacite.values()) {
            texte.append("• ").append(capacite.getNom()).append(" : ").append(capacite.getDescription()).append(".\n");
        }
        texte.append("\nCommandes : clic gauche pour tirer ou poser un bateau ; clic droit ou R pour pivoter un ")
                .append("bateau ou la torpille ; Échap pour annuler une capacité ; cliquer sur un bateau posé le ")
                .append("reprend pour le déplacer.\n\n")
                .append("À deux, les grilles se cachent entre les tours : passez la souris à l'autre joueur.\n")
                .append("La partie est enregistrée après chaque coup : vous pouvez la reprendre ou la revoir depuis ")
                .append("l'accueil.");
        JOptionPane.showMessageDialog(this, new JLabel(Textes.paragraphe(texte.toString(), 440)),
                "Règles et commandes", JOptionPane.INFORMATION_MESSAGE);
    }
}
