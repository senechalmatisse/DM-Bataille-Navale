package bataille.modele;

/**
 * Les capacités spéciales, utilisables une fois par partie chacune quand la règle est activée.
 * <p>
 * Une capacité se joue au début du tour, à la place du tir, et une seule par tour : le radar, le bombardement
 * et la torpille terminent le tour ; le double tir donne deux tirs simples ce tour-ci.
 */
public enum Capacite {
    /** Compte les cases de bateaux intactes dans un carré de 3 × 3, sans dire où elles sont. */
    RADAR("Radar", "compte les cases de bateaux d'un carré de 3 × 3"),
    /** Tire sur les quatre cases d'un carré de 2 × 2. */
    BOMBARDEMENT("Bombardement", "tire sur les 4 cases d'un carré de 2 × 2"),
    /** Part du bord d'une ligne ou d'une colonne et explose sur le premier bateau rencontré. */
    TORPILLE("Torpille", "suit une ligne ou une colonne jusqu'au premier bateau"),
    /** Donne deux tirs simples ce tour-ci. */
    DOUBLE_TIR("Double tir", "deux tirs ce tour-ci");

    /** Le nombre d'utilisations de chaque capacité dans une partie. */
    public static final int UTILISATIONS_PAR_PARTIE = 1;

    /** Le nom affiché de la capacité. */
    private final String nom;
    /** Ce que fait la capacité, en quelques mots. */
    private final String description;

    Capacite(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }

    /**
     * Retourne le nom de la capacité, par exemple « Torpille ».
     *
     * @return le nom affiché
     */
    public String getNom() {
        return nom;
    }

    /**
     * Retourne ce que fait la capacité, par exemple « tire sur les 4 cases d'un carré de 2 × 2 ».
     *
     * @return la description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Indique si la capacité vise une case de la grille adverse.
     *
     * @return faux pour le double tir, vrai pour les autres
     */
    public boolean aUneCible() {
        return this != DOUBLE_TIR;
    }

    /**
     * Retourne le nom de la capacité.
     *
     * @return le nom affiché
     */
    @Override
    public String toString() {
        return nom;
    }
}
