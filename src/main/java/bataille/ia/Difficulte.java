package bataille.ia;

import java.util.Random;

/** Les niveaux de l'ordinateur, chacun associé à une stratégie de tir. */
public enum Difficulte {
    /** L'ordinateur tire au hasard. */
    FACILE("Facile", "tire au hasard"),
    /** L'ordinateur chasse en damier puis cible autour de ses touches. */
    NORMALE("Normale", "chasse en damier puis cible autour des touches"),
    /** L'ordinateur calcule où les bateaux peuvent encore se trouver. */
    DIFFICILE("Difficile", "calcule où les bateaux peuvent encore se trouver");

    /** Le nom affiché du niveau. */
    private final String nom;
    /** La description du comportement de l'ordinateur. */
    private final String description;

    Difficulte(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }

    /**
     * Retourne le nom du niveau, par exemple « Normale ».
     *
     * @return le nom du niveau
     */
    public String getNom() {
        return nom;
    }

    /**
     * Crée la stratégie de tir de ce niveau.
     *
     * @param random le générateur de hasard de la stratégie
     * @return une nouvelle stratégie
     */
    public StrategieTir creerStrategie(Random random) {
        return switch (this) {
            case FACILE -> new TirAleatoire(random);
            case NORMALE -> new ChasseCible(random);
            case DIFFICILE -> new CarteProbabilites(random);
        };
    }

    /**
     * Décrit le niveau, par exemple « Normale : chasse en damier puis cible autour des touches ».
     *
     * @return la description du niveau
     */
    @Override
    public String toString() {
        return nom + " : " + description;
    }
}
