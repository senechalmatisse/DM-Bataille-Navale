package bataille.modele;

import java.util.List;

/** Les compositions de flotte proposées au joueur. */
public enum ModeleFlotte {
    /** La flotte réglementaire : 5 bateaux, 17 cases. */
    CLASSIQUE("Classique", List.of(TypeBateau.PORTE_AVIONS, TypeBateau.CROISEUR, TypeBateau.CONTRE_TORPILLEUR,
            TypeBateau.SOUS_MARIN, TypeBateau.TORPILLEUR)),
    /** Une flotte réduite pour une partie courte : 3 bateaux, 9 cases. */
    RAPIDE("Rapide", List.of(TypeBateau.CROISEUR, TypeBateau.SOUS_MARIN, TypeBateau.TORPILLEUR));

    /** Le nom affiché du modèle. */
    private final String nom;
    /** Les types de bateaux de la flotte. */
    private final List<TypeBateau> types;

    ModeleFlotte(String nom, List<TypeBateau> types) {
        this.nom = nom;
        this.types = types;
    }

    /**
     * Retourne les types de bateaux de la flotte.
     *
     * @return les types, du plus long au plus court, dans une liste non modifiable
     */
    public List<TypeBateau> getTypes() {
        return types;
    }

    /**
     * Retourne le nombre de cases occupées par la flotte.
     *
     * @return la somme des longueurs des bateaux
     */
    public int getNombreCases() {
        return types.stream().mapToInt(TypeBateau::getLongueur).sum();
    }

    /**
     * Décrit le modèle, par exemple « Classique : 5 bateaux, 17 cases ».
     *
     * @return la description du modèle
     */
    @Override
    public String toString() {
        return nom + " : " + types.size() + " bateaux, " + getNombreCases() + " cases";
    }
}
