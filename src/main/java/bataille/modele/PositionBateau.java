package bataille.modele;

import java.util.Locale;
import java.util.Objects;

/**
 * La place d'un bateau sur un plateau, telle qu'on l'enregistre : son type, sa case d'origine et son orientation.
 * En texte, elle s'écrit « PORTE_AVIONS@B2:H ».
 *
 * @param type le type du bateau
 * @param origine la case la plus à gauche ou la plus haute
 * @param orientation le sens dans lequel le bateau s'étend
 */
public record PositionBateau(TypeBateau type, Coordonnee origine, Orientation orientation) {

    /**
     * Vérifie que la position est complète.
     *
     * @param type le type du bateau
     * @param origine la case d'origine
     * @param orientation l'orientation
     */
    public PositionBateau {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(origine, "origine");
        Objects.requireNonNull(orientation, "orientation");
    }

    /**
     * Retourne la position d'un bateau posé.
     *
     * @param bateau le bateau
     * @return sa position
     */
    public static PositionBateau de(Bateau bateau) {
        return new PositionBateau(bateau.getType(), bateau.getOrigine(), bateau.getOrientation());
    }

    /**
     * Pose le bateau sur un plateau.
     *
     * @param plateau le plateau qui n'a encore reçu aucun tir
     * @return le bateau posé
     */
    public Bateau placerSur(Plateau plateau) {
        return plateau.placer(type, origine, orientation);
    }

    /**
     * Écrit la position en texte.
     *
     * @return le texte, par exemple « PORTE_AVIONS@B2:H »
     */
    public String enTexte() {
        return type.name() + "@" + origine + ":" + orientation.getLettre();
    }

    /**
     * Lit une position écrite par {@link #enTexte()}.
     *
     * @param texte le texte à lire
     * @return la position
     * @throws IllegalArgumentException si le texte n'est pas une position
     */
    public static PositionBateau depuisTexte(String texte) {
        String[] parties = texte.strip().toUpperCase(Locale.ROOT).split("[@:]");
        if (parties.length != 3) {
            throw new IllegalArgumentException("Position de bateau illisible : « " + texte + " ».");
        }
        try {
            return new PositionBateau(TypeBateau.valueOf(parties[0]), Coordonnee.depuis(parties[1]),
                    Orientation.depuis(parties[2]));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Position de bateau illisible : « " + texte + " ».", e);
        }
    }
}
