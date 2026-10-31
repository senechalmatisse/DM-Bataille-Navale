package bataille.modele;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Un bateau posé sur un plateau : son type, sa position et les cases déjà touchées.
 * <p>
 * Seul le plateau qui le porte peut enregistrer une touche ; les autres classes le consultent.
 */
public final class Bateau {
    /** Le type du bateau, qui fixe son nom et sa longueur. */
    private final TypeBateau type;
    /** La case d'origine : la plus à gauche ou la plus haute. */
    private final Coordonnee origine;
    /** Le sens dans lequel le bateau s'étend depuis son origine. */
    private final Orientation orientation;
    /** Les cases occupées, de l'origine à l'extrémité. */
    private final List<Coordonnee> cases;
    /** Les cases déjà touchées par l'adversaire. */
    private final Set<Coordonnee> casesTouchees = new HashSet<>();

    /**
     * Construit un bateau intact.
     *
     * @param type le type du bateau
     * @param origine la case la plus à gauche (bateau horizontal) ou la plus haute (bateau vertical)
     * @param orientation le sens dans lequel le bateau s'étend
     */
    public Bateau(TypeBateau type, Coordonnee origine, Orientation orientation) {
        this.type = Objects.requireNonNull(type, "type");
        this.origine = Objects.requireNonNull(origine, "origine");
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        List<Coordonnee> liste = new ArrayList<>(type.getLongueur());
        for (int i = 0; i < type.getLongueur(); i++) {
            liste.add(origine.decaler(i * orientation.getDeltaLigne(), i * orientation.getDeltaColonne()));
        }
        this.cases = List.copyOf(liste);
    }

    /**
     * Retourne le type du bateau.
     *
     * @return le type du bateau
     */
    public TypeBateau getType() {
        return type;
    }

    /**
     * Retourne la case d'origine du bateau.
     *
     * @return la case la plus à gauche ou la plus haute
     */
    public Coordonnee getOrigine() {
        return origine;
    }

    /**
     * Retourne l'orientation du bateau.
     *
     * @return l'orientation du bateau
     */
    public Orientation getOrientation() {
        return orientation;
    }

    /**
     * Retourne les cases occupées par le bateau.
     *
     * @return les cases, de l'origine à l'extrémité, dans une liste non modifiable
     */
    public List<Coordonnee> getCases() {
        return cases;
    }

    /**
     * Indique si le bateau occupe une case.
     *
     * @param coordonnee la case à tester
     * @return si la case fait partie du bateau
     */
    public boolean occupe(Coordonnee coordonnee) {
        return cases.contains(coordonnee);
    }

    /**
     * Enregistre un tir reçu sur une case du bateau.
     *
     * @param coordonnee la case touchée, qui doit appartenir au bateau
     */
    void toucher(Coordonnee coordonnee) {
        if (!occupe(coordonnee)) {
            throw new IllegalArgumentException(coordonnee + " ne fait pas partie du bateau " + this);
        }
        casesTouchees.add(coordonnee);
    }

    /**
     * Indique si une case du bateau a été touchée.
     *
     * @param coordonnee la case à tester
     * @return si la case appartient au bateau et a été touchée
     */
    public boolean estTouche(Coordonnee coordonnee) {
        return casesTouchees.contains(coordonnee);
    }

    /**
     * Indique si toutes les cases du bateau ont été touchées.
     *
     * @return si le bateau est coulé
     */
    public boolean estCoule() {
        return casesTouchees.size() == cases.size();
    }

    /**
     * Décrit le bateau, par exemple « Croiseur en B7 (H) ».
     *
     * @return la description du bateau
     */
    @Override
    public String toString() {
        return type.getNom() + " en " + origine + " (" + orientation.getLettre() + ")";
    }
}
