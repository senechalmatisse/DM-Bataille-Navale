package bataille.modele;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Une case de la grille, repérée par sa ligne et sa colonne, numérotées à partir de 0.
 * <p>
 * En notation de jeu, la colonne s'écrit avec une lettre et la ligne avec un nombre à partir de 1 :
 * la case de ligne 6 et de colonne 1 s'écrit « B7 ».
 *
 * @param ligne la ligne, à partir de 0
 * @param colonne la colonne, à partir de 0
 */
public record Coordonnee(int ligne, int colonne) {

    /** Une lettre suivie d'un nombre, avec d'éventuels espaces entre les deux. */
    private static final Pattern NOTATION = Pattern.compile("([A-Z])\\s*(\\d{1,3})");

    /**
     * Lit une case écrite en notation de jeu, par exemple « B7 », « b7 » ou « B 7 ».
     * La case lue peut se trouver hors de la grille : c'est au plateau de le vérifier.
     *
     * @param saisie le texte à lire
     * @return la case correspondante
     * @throws IllegalArgumentException si le texte n'est pas une lettre suivie d'un nombre
     */
    public static Coordonnee depuis(String saisie) {
        String texte = saisie == null ? "" : saisie.trim().toUpperCase(Locale.ROOT);
        if (texte.isEmpty()) {
            throw new IllegalArgumentException("Tapez une case : une lettre puis un nombre, par exemple B7.");
        }
        Matcher correspondance = NOTATION.matcher(texte);
        if (!correspondance.matches()) {
            throw new IllegalArgumentException(
                    "« " + texte + " » n'est pas une case : tapez une lettre puis un nombre, par exemple B7.");
        }
        int colonne = correspondance.group(1).charAt(0) - 'A';
        int ligne = Integer.parseInt(correspondance.group(2)) - 1;
        return new Coordonnee(ligne, colonne);
    }

    /**
     * Retourne la lettre qui désigne une colonne : 0 donne « A », 1 donne « B ».
     *
     * @param colonne la colonne, à partir de 0
     * @return la lettre de la colonne
     */
    public static String lettreColonne(int colonne) {
        return String.valueOf((char) ('A' + colonne));
    }

    /**
     * Retourne la case décalée du nombre de lignes et de colonnes donné.
     *
     * @param deltaLigne le décalage en lignes (négatif vers le haut)
     * @param deltaColonne le décalage en colonnes (négatif vers la gauche)
     * @return la case décalée, éventuellement hors de la grille
     */
    public Coordonnee decaler(int deltaLigne, int deltaColonne) {
        return new Coordonnee(ligne + deltaLigne, colonne + deltaColonne);
    }

    /**
     * Retourne la case en notation de jeu, par exemple « B7 ».
     *
     * @return la notation de la case
     */
    @Override
    public String toString() {
        return lettreColonne(colonne) + (ligne + 1);
    }
}
