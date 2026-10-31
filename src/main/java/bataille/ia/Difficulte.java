package bataille.ia;

import bataille.modele.Capacite;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Random;

/** Les niveaux de l'ordinateur : chacun associe une stratégie de tir et un usage des capacités spéciales. */
public enum Difficulte {
    /** L'ordinateur tire au hasard et n'utilise pas les capacités. */
    FACILE("Facile", "tire au hasard"),
    /** L'ordinateur chasse en damier puis cible autour de ses touches ; il bombarde et tire deux fois. */
    NORMALE("Normale", "chasse en damier puis cible autour des touches"),
    /** L'ordinateur calcule où les bateaux peuvent encore se trouver et utilise toutes les capacités. */
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
     * Crée le tacticien de ce niveau, qui décide de l'usage des capacités spéciales.
     *
     * @param random le générateur de hasard du tacticien
     * @return le tacticien, ou une valeur vide si ce niveau n'utilise pas les capacités
     */
    public Optional<Tacticien> creerTacticien(Random random) {
        return switch (this) {
            case FACILE -> Optional.empty();
            case NORMALE -> Optional.of(new Tacticien(EnumSet.of(Capacite.DOUBLE_TIR, Capacite.BOMBARDEMENT), random));
            case DIFFICILE -> Optional.of(new Tacticien(EnumSet.allOf(Capacite.class), random));
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
