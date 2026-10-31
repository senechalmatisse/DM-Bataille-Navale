package bataille.modele;

import java.util.List;
import java.util.Objects;

/**
 * Les règles d'une partie : la taille de la grille et la flotte de chaque joueur.
 * <p>
 * Une configuration est toujours jouable : la construction refuse une grille trop petite ou trop grande,
 * et une flotte qui occuperait plus de la moitié de la grille.
 *
 * @param taille le nombre de lignes et de colonnes de la grille
 * @param flotte les types de bateaux de chaque joueur
 */
public record ConfigurationPartie(int taille, List<TypeBateau> flotte) {
    /** La plus petite grille acceptée. */
    public static final int TAILLE_MIN = 5;
    /** La plus grande grille acceptée : une lettre par colonne, de A à Z. */
    public static final int TAILLE_MAX = 26;
    /** La taille de la grille réglementaire. */
    public static final int TAILLE_PAR_DEFAUT = 10;

    /**
     * Vérifie que la configuration est jouable.
     *
     * @param taille le nombre de lignes et de colonnes de la grille
     * @param flotte les types de bateaux de chaque joueur
     * @throws IllegalArgumentException si la grille ou la flotte ne conviennent pas
     */
    public ConfigurationPartie {
        Objects.requireNonNull(flotte, "flotte");
        if (taille < TAILLE_MIN || taille > TAILLE_MAX) {
            throw new IllegalArgumentException(
                    "La grille doit faire entre " + TAILLE_MIN + " et " + TAILLE_MAX + " cases de côté (et non "
                    + taille + ").");
        }
        flotte = List.copyOf(flotte);
        if (flotte.isEmpty()) {
            throw new IllegalArgumentException("La flotte doit compter au moins un bateau.");
        }
        int nombreCases = 0;
        for (TypeBateau type : flotte) {
            if (type.getLongueur() > taille) {
                throw new IllegalArgumentException(
                        "Le bateau " + type.getNom() + " est plus long que la grille (" + taille + " cases).");
            }
            nombreCases += type.getLongueur();
        }
        int maximum = taille * taille / 2;
        if (nombreCases > maximum) {
            throw new IllegalArgumentException("La flotte occupe " + nombreCases + " cases : une grille "
                    + taille + "×" + taille + " en accepte " + maximum + " au plus.");
        }
    }

    /**
     * Retourne la configuration réglementaire : grille 10×10 et flotte classique.
     *
     * @return la configuration classique
     */
    public static ConfigurationPartie classique() {
        return de(TAILLE_PAR_DEFAUT, ModeleFlotte.CLASSIQUE);
    }

    /**
     * Construit une configuration à partir d'une taille et d'un modèle de flotte.
     *
     * @param taille le nombre de lignes et de colonnes de la grille
     * @param modele le modèle de flotte
     * @return la configuration
     * @throws IllegalArgumentException si la flotte ne tient pas dans la grille
     */
    public static ConfigurationPartie de(int taille, ModeleFlotte modele) {
        return new ConfigurationPartie(taille, modele.getTypes());
    }

    /**
     * Retourne le nombre de cases occupées par la flotte d'un joueur.
     *
     * @return la somme des longueurs des bateaux
     */
    public int nombreCasesFlotte() {
        return flotte.stream().mapToInt(TypeBateau::getLongueur).sum();
    }
}
