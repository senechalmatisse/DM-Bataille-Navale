package bataille.partie;

import bataille.ia.Difficulte;
import bataille.modele.ConfigurationPartie;
import bataille.modele.PlacementAleatoire;
import bataille.modele.Plateau;
import java.util.Objects;
import java.util.Random;

/**
 * Les choix faits avant une partie, communs à la console et à la fenêtre.
 *
 * @param mode contre l'ordinateur, ou à deux sur le même écran
 * @param nomJoueur1 le nom du joueur qui commence
 * @param nomJoueur2 le nom du second joueur humain ; ignoré contre l'ordinateur
 * @param difficulte le niveau de l'ordinateur ; ignoré à deux
 * @param configuration la taille de la grille, la flotte et les capacités spéciales
 * @param placementManuel si les joueurs humains placent leurs bateaux eux-mêmes
 */
public record ParametresPartie(ModeJeu mode, String nomJoueur1, String nomJoueur2, Difficulte difficulte,
        ConfigurationPartie configuration, boolean placementManuel) {

    /** Le nom proposé quand le premier joueur n'en donne pas. */
    public static final String NOM_PAR_DEFAUT = "Joueur";
    /** Le nom proposé quand le second joueur n'en donne pas. */
    public static final String NOM_PAR_DEFAUT_2 = "Joueur 2";
    /** Le nom de l'ordinateur. */
    public static final String NOM_ORDINATEUR = "Ordinateur";

    /**
     * Vérifie les paramètres et remplace les noms vides par les noms par défaut.
     *
     * @param mode contre l'ordinateur, ou à deux
     * @param nomJoueur1 le nom du joueur qui commence
     * @param nomJoueur2 le nom du second joueur humain
     * @param difficulte le niveau de l'ordinateur
     * @param configuration la grille, la flotte et les capacités
     * @param placementManuel si les joueurs placent leurs bateaux eux-mêmes
     */
    public ParametresPartie {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(difficulte, "difficulte");
        Objects.requireNonNull(configuration, "configuration");
        nomJoueur1 = nomJoueur1 == null || nomJoueur1.isBlank() ? NOM_PAR_DEFAUT : nomJoueur1.strip();
        nomJoueur2 = nomJoueur2 == null || nomJoueur2.isBlank() ? NOM_PAR_DEFAUT_2 : nomJoueur2.strip();
    }

    /**
     * Construit les paramètres d'une partie contre l'ordinateur.
     *
     * @param nomJoueur le nom du joueur humain
     * @param difficulte le niveau de l'ordinateur
     * @param configuration la grille, la flotte et les capacités
     * @param placementManuel si le joueur place ses bateaux lui-même
     * @return les paramètres
     */
    public static ParametresPartie contreOrdinateur(String nomJoueur, Difficulte difficulte,
            ConfigurationPartie configuration, boolean placementManuel) {
        return new ParametresPartie(ModeJeu.CONTRE_ORDINATEUR, nomJoueur, NOM_PAR_DEFAUT_2, difficulte, configuration,
                placementManuel);
    }

    /**
     * Construit les paramètres d'une partie à deux sur le même écran.
     *
     * @param nomJoueur1 le nom du joueur qui commence
     * @param nomJoueur2 le nom du second joueur
     * @param configuration la grille, la flotte et les capacités
     * @param placementManuel si les joueurs placent leurs bateaux eux-mêmes
     * @return les paramètres
     */
    public static ParametresPartie aDeux(String nomJoueur1, String nomJoueur2, ConfigurationPartie configuration,
            boolean placementManuel) {
        return new ParametresPartie(ModeJeu.DEUX_JOUEURS, nomJoueur1, nomJoueur2, Difficulte.NORMALE, configuration,
                placementManuel);
    }

    /**
     * Retourne les paramètres proposés par défaut : contre l'ordinateur, niveau normal, grille 10×10,
     * flotte classique, sans capacités, placement automatique.
     *
     * @return les paramètres par défaut
     */
    public static ParametresPartie parDefaut() {
        return contreOrdinateur(NOM_PAR_DEFAUT, Difficulte.NORMALE, ConfigurationPartie.classique(), false);
    }

    /**
     * Indique si le second joueur est l'ordinateur.
     *
     * @return vrai contre l'ordinateur
     */
    public boolean contreLOrdinateur() {
        return mode == ModeJeu.CONTRE_ORDINATEUR;
    }

    /**
     * Retourne les mêmes paramètres avec un autre mode.
     *
     * @param nouveauMode le mode voulu
     * @return les nouveaux paramètres
     */
    public ParametresPartie avecMode(ModeJeu nouveauMode) {
        return new ParametresPartie(nouveauMode, nomJoueur1, nomJoueur2, difficulte, configuration, placementManuel);
    }

    /**
     * Crée une partie contre l'ordinateur : le joueur humain commence avec le plateau fourni, l'ordinateur
     * reçoit un plateau placé au hasard.
     *
     * @param plateauJoueur le plateau du joueur humain, flotte placée
     * @param random le générateur de hasard du placement et de l'ordinateur
     * @return la partie prête à jouer
     */
    public Partie creerPartie(Plateau plateauJoueur, Random random) {
        Plateau plateauOrdinateur = new Plateau(configuration.taille());
        PlacementAleatoire.placer(plateauOrdinateur, configuration.flotte(), random);
        return creerPartie(plateauJoueur, plateauOrdinateur, random);
    }

    /**
     * Crée la partie avec deux plateaux déjà placés : celui du joueur 1, puis celui du joueur 2 (l'ordinateur,
     * ou le second joueur humain).
     *
     * @param plateau1 le plateau du joueur qui commence
     * @param plateau2 le plateau du second joueur
     * @param random le générateur de hasard de l'ordinateur ; inutilisé à deux
     * @return la partie prête à jouer
     */
    public Partie creerPartie(Plateau plateau1, Plateau plateau2, Random random) {
        Joueur premier = Joueur.humain(nomJoueur1, plateau1);
        Joueur second = contreLOrdinateur()
                ? Joueur.ordinateur(NOM_ORDINATEUR, plateau2, difficulte.creerStrategie(random),
                        difficulte.creerTacticien(random).orElse(null))
                : Joueur.humain(nomJoueur2, plateau2);
        return new Partie(configuration, premier, second);
    }
}
