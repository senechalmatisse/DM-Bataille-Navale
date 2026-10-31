package bataille.partie;

import bataille.ia.Difficulte;
import bataille.modele.ConfigurationPartie;
import bataille.modele.PlacementAleatoire;
import bataille.modele.Plateau;
import java.util.Objects;
import java.util.Random;

/**
 * Les choix faits avant une partie contre l'ordinateur, communs à la console et à la fenêtre.
 *
 * @param nomJoueur le nom du joueur humain
 * @param difficulte le niveau de l'ordinateur
 * @param configuration la taille de la grille et la flotte
 * @param placementManuel si le joueur place ses bateaux lui-même
 */
public record ParametresPartie(String nomJoueur, Difficulte difficulte, ConfigurationPartie configuration,
        boolean placementManuel) {

    /** Le nom proposé quand le joueur n'en donne pas. */
    public static final String NOM_PAR_DEFAUT = "Joueur";
    /** Le nom de l'ordinateur. */
    public static final String NOM_ORDINATEUR = "Ordinateur";

    /**
     * Vérifie les paramètres.
     *
     * @param nomJoueur le nom du joueur humain
     * @param difficulte le niveau de l'ordinateur
     * @param configuration la taille de la grille et la flotte
     * @param placementManuel si le joueur place ses bateaux lui-même
     */
    public ParametresPartie {
        Objects.requireNonNull(nomJoueur, "nomJoueur");
        Objects.requireNonNull(difficulte, "difficulte");
        Objects.requireNonNull(configuration, "configuration");
    }

    /**
     * Retourne les paramètres proposés par défaut : niveau normal, grille 10×10, flotte classique,
     * placement automatique.
     *
     * @return les paramètres par défaut
     */
    public static ParametresPartie parDefaut() {
        return new ParametresPartie(NOM_PAR_DEFAUT, Difficulte.NORMALE, ConfigurationPartie.classique(), false);
    }

    /**
     * Crée la partie : le joueur humain commence avec le plateau fourni, l'ordinateur reçoit un plateau
     * placé au hasard.
     *
     * @param plateauJoueur le plateau du joueur humain, flotte placée
     * @param random le générateur de hasard du placement et de la stratégie de l'ordinateur
     * @return la partie prête à jouer
     */
    public Partie creerPartie(Plateau plateauJoueur, Random random) {
        Plateau plateauOrdinateur = new Plateau(configuration.taille());
        PlacementAleatoire.placer(plateauOrdinateur, configuration.flotte(), random);
        String nom = nomJoueur.isBlank() ? NOM_PAR_DEFAUT : nomJoueur;
        Joueur humain = Joueur.humain(nom, plateauJoueur);
        Joueur ordinateur = Joueur.ordinateur(NOM_ORDINATEUR, plateauOrdinateur, difficulte.creerStrategie(random));
        return new Partie(configuration, humain, ordinateur);
    }
}
