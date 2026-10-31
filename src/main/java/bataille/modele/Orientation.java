package bataille.modele;

import java.util.Locale;

/** Le sens dans lequel un bateau s'étend à partir de sa case d'origine. */
public enum Orientation {
    /** Le bateau s'étend vers la droite. */
    HORIZONTALE(0, 1, "H"),
    /** Le bateau s'étend vers le bas. */
    VERTICALE(1, 0, "V");

    /** Le décalage en lignes d'une case à la suivante. */
    private final int deltaLigne;
    /** Le décalage en colonnes d'une case à la suivante. */
    private final int deltaColonne;
    /** La lettre qui désigne l'orientation dans les saisies. */
    private final String lettre;

    Orientation(int deltaLigne, int deltaColonne, String lettre) {
        this.deltaLigne = deltaLigne;
        this.deltaColonne = deltaColonne;
        this.lettre = lettre;
    }

    /**
     * Retourne le décalage en lignes d'une case du bateau à la suivante.
     *
     * @return 0 pour un bateau horizontal, 1 pour un bateau vertical
     */
    public int getDeltaLigne() {
        return deltaLigne;
    }

    /**
     * Retourne le décalage en colonnes d'une case du bateau à la suivante.
     *
     * @return 1 pour un bateau horizontal, 0 pour un bateau vertical
     */
    public int getDeltaColonne() {
        return deltaColonne;
    }

    /**
     * Retourne la lettre qui désigne l'orientation : « H » ou « V ».
     *
     * @return la lettre de l'orientation
     */
    public String getLettre() {
        return lettre;
    }

    /**
     * Retourne l'autre orientation.
     *
     * @return VERTICALE pour HORIZONTALE, et inversement
     */
    public Orientation inverse() {
        return this == HORIZONTALE ? VERTICALE : HORIZONTALE;
    }

    /**
     * Lit une orientation saisie : « H » ou « V », en majuscule ou en minuscule.
     *
     * @param saisie le texte à lire
     * @return l'orientation correspondante
     * @throws IllegalArgumentException si le texte n'est ni « H » ni « V »
     */
    public static Orientation depuis(String saisie) {
        String texte = saisie == null ? "" : saisie.trim().toUpperCase(Locale.ROOT);
        for (Orientation orientation : values()) {
            if (orientation.lettre.equals(texte)) {
                return orientation;
            }
        }
        throw new IllegalArgumentException("Orientation inconnue : tapez H (horizontale) ou V (verticale).");
    }
}
