package bataille.modele;

import java.util.Locale;

/** Les types de bateaux de la flotte classique, avec leur longueur en cases. */
public enum TypeBateau {
    /** Le porte-avions, 5 cases. */
    PORTE_AVIONS("Porte-avions", 5),
    /** Le croiseur, 4 cases. */
    CROISEUR("Croiseur", 4),
    /** Le contre-torpilleur, 3 cases. */
    CONTRE_TORPILLEUR("Contre-torpilleur", 3),
    /** Le sous-marin, 3 cases. */
    SOUS_MARIN("Sous-marin", 3),
    /** Le torpilleur, 2 cases. */
    TORPILLEUR("Torpilleur", 2);

    /** Le nom affiché du bateau. */
    private final String nom;
    /** La longueur du bateau, en cases. */
    private final int longueur;

    TypeBateau(String nom, int longueur) {
        this.nom = nom;
        this.longueur = longueur;
    }

    /**
     * Retourne le nom du bateau, par exemple « Porte-avions ».
     *
     * @return le nom du bateau
     */
    public String getNom() {
        return nom;
    }

    /**
     * Retourne le nom précédé de l'article, pour une phrase : « le porte-avions ».
     *
     * @return le nom avec son article, en minuscules
     */
    public String getNomAvecArticle() {
        return "le " + nom.toLowerCase(Locale.ROOT);
    }

    /**
     * Retourne la longueur du bateau.
     *
     * @return le nombre de cases occupées par le bateau
     */
    public int getLongueur() {
        return longueur;
    }

    /**
     * Retourne le nom du bateau.
     *
     * @return le nom du bateau
     */
    @Override
    public String toString() {
        return nom;
    }
}
