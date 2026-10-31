package bataille.console;

import bataille.ia.Difficulte;
import bataille.modele.ConfigurationPartie;
import bataille.modele.Coordonnee;
import bataille.modele.EtatCase;
import bataille.modele.ModeleFlotte;
import bataille.modele.Orientation;
import bataille.modele.PlacementAleatoire;
import bataille.modele.Plateau;
import bataille.modele.TypeBateau;
import bataille.modele.VuePlateau;
import bataille.partie.Annonces;
import bataille.partie.Joueur;
import bataille.partie.ParametresPartie;
import bataille.partie.Partie;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * La partie en console : un joueur humain contre l'ordinateur.
 * <p>
 * Avant la partie, le joueur choisit son nom, le niveau de l'ordinateur, la grille, la flotte et la façon
 * de placer ses bateaux ; chaque question propose une réponse par défaut (touche Entrée). Pendant la partie,
 * ses deux grilles s'affichent côte à côte et chaque saisie refusée est expliquée. Q abandonne la partie ;
 * la fin de l'entrée (Ctrl+D, Ctrl+Z) arrête le programme proprement.
 */
public final class JeuConsole {
    /** Une case suivie d'une orientation, par exemple « B2 H ». */
    private static final Pattern PLACEMENT = Pattern.compile("([A-Za-z]\\s*\\d{1,3})\\s*([HhVv])");
    /** Le mot qui place au hasard les bateaux restants. */
    private static final String HASARD = "hasard";
    /** La touche qui abandonne la partie. */
    private static final String QUITTER = "q";

    /** L'entrée du joueur. */
    private final BufferedReader entree;
    /** La sortie où s'affiche le jeu. */
    private final PrintStream sortie;
    /** Le générateur de hasard des placements et de l'ordinateur. */
    private final Random random;

    /**
     * Construit le jeu.
     *
     * @param entree l'entrée du joueur
     * @param sortie la sortie où afficher le jeu
     * @param random le générateur de hasard, à graine fixe pour une partie reproductible
     */
    public JeuConsole(BufferedReader entree, PrintStream sortie, Random random) {
        this.entree = Objects.requireNonNull(entree, "entree");
        this.sortie = Objects.requireNonNull(sortie, "sortie");
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Lance le jeu en console, sur l'entrée et la sortie standard.
     *
     * @param args non utilisés
     */
    public static void main(String[] args) {
        Charset charset = System.console() != null ? System.console().charset() : Charset.defaultCharset();
        BufferedReader entree = new BufferedReader(new InputStreamReader(System.in, charset));
        new JeuConsole(entree, System.out, new Random()).lancer();
    }

    /** Pose les questions de départ, puis enchaîne les parties tant que le joueur veut rejouer. */
    public void lancer() {
        sortie.println("=== BATAILLE NAVALE ===");
        try {
            ParametresPartie parametres = demanderParametres();
            boolean rejouer = jouer(parametres);
            while (rejouer && demanderOuiNon("Rejouer avec les mêmes réglages ? (o/N) : ", false)) {
                rejouer = jouer(parametres);
            }
            sortie.println("Au revoir !");
        } catch (FinDeSaisie e) {
            sortie.println();
            sortie.println("Fin de l'entrée : au revoir !");
        }
    }

    private ParametresPartie demanderParametres() {
        String nom = lireLigne("Votre nom [" + ParametresPartie.NOM_PAR_DEFAUT + "] : ").strip();
        if (nom.isEmpty()) {
            nom = ParametresPartie.NOM_PAR_DEFAUT;
        }
        sortie.println("Niveau de l'ordinateur :");
        Difficulte[] niveaux = Difficulte.values();
        for (int i = 0; i < niveaux.length; i++) {
            sortie.println("  " + (i + 1) + ". " + niveaux[i]);
        }
        int niveauParDefaut = Difficulte.NORMALE.ordinal() + 1;
        Difficulte difficulte = niveaux[lireEntier("Votre choix [" + niveauParDefaut + "] : ", 1, niveaux.length,
                niveauParDefaut) - 1];
        ConfigurationPartie configuration = demanderConfiguration();
        boolean placementManuel = demanderOuiNon("Placer vos bateaux vous-même ? (o/N) : ", false);
        return new ParametresPartie(nom, difficulte, configuration, placementManuel);
    }

    private ConfigurationPartie demanderConfiguration() {
        while (true) {
            int taille = lireEntier("Taille de la grille, de " + ConfigurationPartie.TAILLE_MIN + " à "
                    + ConfigurationPartie.TAILLE_MAX + " [" + ConfigurationPartie.TAILLE_PAR_DEFAUT + "] : ",
                    ConfigurationPartie.TAILLE_MIN, ConfigurationPartie.TAILLE_MAX,
                    ConfigurationPartie.TAILLE_PAR_DEFAUT);
            sortie.println("Flotte :");
            ModeleFlotte[] modeles = ModeleFlotte.values();
            for (int i = 0; i < modeles.length; i++) {
                sortie.println("  " + (i + 1) + ". " + modeles[i]);
            }
            ModeleFlotte modele = modeles[lireEntier("Votre choix [1] : ", 1, modeles.length, 1) - 1];
            try {
                return ConfigurationPartie.de(taille, modele);
            } catch (IllegalArgumentException e) {
                sortie.println(e.getMessage() + " Choisissez une grille plus grande ou une flotte plus petite.");
            }
        }
    }

    /**
     * Joue une partie complète.
     *
     * @param parametres les choix du joueur
     * @return si la partie est allée à son terme, plutôt qu'abandonnée
     */
    private boolean jouer(ParametresPartie parametres) {
        ConfigurationPartie configuration = parametres.configuration();
        Plateau plateau = new Plateau(configuration.taille());
        if (parametres.placementManuel()) {
            if (!placerALaMain(plateau, configuration.flotte())) {
                sortie.println("Partie abandonnée.");
                return false;
            }
        } else {
            PlacementAleatoire.placer(plateau, configuration.flotte(), random);
        }
        Partie partie = parametres.creerPartie(plateau, random);
        Joueur joueur = partie.getJoueur1();
        Joueur ordinateur = partie.getJoueur2();
        sortie.println();
        sortie.println("La partie commence. " + joueur.getNom() + ", à vous de tirer ! (difficulté : "
                + parametres.difficulte().getNom().toLowerCase(Locale.ROOT) + ")");
        while (!partie.estTerminee()) {
            if (partie.getJoueurCourant().estHumain()) {
                afficherGrilles(partie);
                Optional<Coordonnee> cible = lireTir(ordinateur.getPlateau().getVueAdverse());
                if (cible.isEmpty()) {
                    sortie.println("Partie abandonnée.");
                    return false;
                }
                sortie.println(Annonces.tirDuJoueur(partie.tirer(cible.get())));
            } else {
                sortie.println(Annonces.tirDeLOrdinateur(partie.faireJouerOrdinateur()));
            }
        }
        afficherBilan(partie);
        return true;
    }

    private boolean placerALaMain(Plateau plateau, List<TypeBateau> flotte) {
        sortie.println();
        sortie.println("Placez vos bateaux : tapez la case de départ puis H (vers la droite) ou V (vers le bas).");
        for (int i = 0; i < flotte.size(); i++) {
            TypeBateau type = flotte.get(i);
            boolean place = false;
            while (!place) {
                sortie.println();
                sortie.print(RenduConsole.grille(plateau, "Votre flotte"));
                String saisie = lireLigne(type.getNom() + " (" + type.getLongueur() + " cases), par exemple B2 H ("
                        + HASARD + " pour placer le reste, Q pour quitter) : ").strip();
                if (saisie.equalsIgnoreCase(QUITTER)) {
                    return false;
                }
                if (saisie.equalsIgnoreCase(HASARD)) {
                    PlacementAleatoire.placer(plateau, flotte.subList(i, flotte.size()), random);
                    break;
                }
                place = essayerDePlacer(plateau, type, saisie);
            }
            if (plateau.getBateaux().size() == flotte.size()) {
                break;
            }
        }
        sortie.println();
        sortie.print(RenduConsole.grille(plateau, "Votre flotte est prête"));
        return true;
    }

    private boolean essayerDePlacer(Plateau plateau, TypeBateau type, String saisie) {
        Matcher correspondance = PLACEMENT.matcher(saisie);
        if (!correspondance.matches()) {
            sortie.println("Tapez une case puis H ou V, par exemple B2 H.");
            return false;
        }
        try {
            Coordonnee origine = Coordonnee.depuis(correspondance.group(1));
            plateau.placer(type, origine, Orientation.depuis(correspondance.group(2)));
            return true;
        } catch (IllegalArgumentException e) {
            sortie.println(e.getMessage());
            return false;
        }
    }

    private void afficherGrilles(Partie partie) {
        Joueur joueur = partie.getJoueur1();
        Joueur ordinateur = partie.getJoueur2();
        int nombreBateaux = partie.getConfiguration().flotte().size();
        sortie.println();
        sortie.print(RenduConsole.deuxGrilles(joueur.getPlateau(), "Votre flotte",
                ordinateur.getPlateau().getVueAdverse(), "Flotte ennemie"));
        sortie.println(RenduConsole.LEGENDE);
        sortie.println("Bateaux à flot : vous " + joueur.getPlateau().getNombreBateauxRestants() + "/" + nombreBateaux
                + ", ordinateur " + ordinateur.getPlateau().getNombreBateauxRestants() + "/" + nombreBateaux + ".");
    }

    private Optional<Coordonnee> lireTir(VuePlateau vue) {
        while (true) {
            String saisie = lireLigne("Votre tir (par exemple B7, Q pour quitter) : ").strip();
            if (saisie.equalsIgnoreCase(QUITTER)) {
                return Optional.empty();
            }
            try {
                Coordonnee cible = Coordonnee.depuis(saisie);
                if (!vue.contient(cible)) {
                    sortie.println(cible + " est hors de la grille (" + vue.decrireEtendue() + ").");
                } else if (vue.getEtat(cible) != EtatCase.INCONNUE) {
                    sortie.println("Vous avez déjà tiré en " + cible + ".");
                } else {
                    return Optional.of(cible);
                }
            } catch (IllegalArgumentException e) {
                sortie.println(e.getMessage());
            }
        }
    }

    private void afficherBilan(Partie partie) {
        Joueur joueur = partie.getJoueur1();
        Joueur ordinateur = partie.getJoueur2();
        sortie.println();
        sortie.print(RenduConsole.deuxGrilles(joueur.getPlateau(), "Votre flotte",
                ordinateur.getPlateau()::getEtatPourProprietaire, "Flotte ennemie dévoilée"));
        sortie.println(RenduConsole.LEGENDE);
        if (partie.getGagnant().orElseThrow() == joueur) {
            sortie.println("*** Victoire ! Vous avez coulé toute la flotte ennemie. ***");
        } else {
            sortie.println("*** Défaite : l'ordinateur a coulé toute votre flotte. ***");
        }
        sortie.println("Vous : " + Annonces.bilan(partie.getStatistiques(joueur)) + ". Ordinateur : "
                + Annonces.bilan(partie.getStatistiques(ordinateur)) + ".");
    }

    private int lireEntier(String invite, int minimum, int maximum, int parDefaut) {
        while (true) {
            String saisie = lireLigne(invite).strip();
            if (saisie.isEmpty()) {
                return parDefaut;
            }
            try {
                int valeur = Integer.parseInt(saisie);
                if (valeur >= minimum && valeur <= maximum) {
                    return valeur;
                }
            } catch (NumberFormatException e) {
                // la saisie n'est pas un nombre : le message ci-dessous l'explique
            }
            sortie.println("Tapez un nombre entre " + minimum + " et " + maximum + ".");
        }
    }

    private boolean demanderOuiNon(String invite, boolean parDefaut) {
        while (true) {
            String saisie = lireLigne(invite).strip().toLowerCase(Locale.ROOT);
            if (saisie.isEmpty()) {
                return parDefaut;
            }
            if (saisie.equals("o") || saisie.equals("oui")) {
                return true;
            }
            if (saisie.equals("n") || saisie.equals("non")) {
                return false;
            }
            sortie.println("Répondez o (oui) ou n (non).");
        }
    }

    private String lireLigne(String invite) {
        sortie.print(invite);
        sortie.flush();
        String ligne;
        try {
            ligne = entree.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (ligne == null) {
            throw new FinDeSaisie();
        }
        return ligne;
    }

    /** Signale que l'entrée du joueur est terminée. */
    private static final class FinDeSaisie extends RuntimeException {
        private static final long serialVersionUID = 1L;

        FinDeSaisie() {
            super("Fin de l'entrée");
        }
    }
}
