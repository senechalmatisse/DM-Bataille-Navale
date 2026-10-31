package bataille.modele;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Le résultat d'un radar : le nombre de cases de bateaux trouvées parmi les cases de la zone
 * qui n'avaient pas encore été visées.
 *
 * @param centre le centre de la zone de 3 × 3
 * @param cases les cases de la zone, dans la grille et pas encore visées au moment du radar
 * @param nombre le nombre de ces cases occupées par un bateau
 */
public record Detection(Coordonnee centre, List<Coordonnee> cases, int nombre) {

    /**
     * Vérifie la cohérence de la détection.
     *
     * @param centre le centre de la zone
     * @param cases les cases sondées
     * @param nombre le nombre de cases de bateaux trouvées
     */
    public Detection {
        Objects.requireNonNull(centre, "centre");
        cases = List.copyOf(cases);
        if (nombre < 0 || nombre > cases.size()) {
            throw new IllegalArgumentException("Nombre de cases détectées incohérent : " + nombre);
        }
    }

    /**
     * Indique si une case fait partie des cases sondées.
     *
     * @param coordonnee la case à tester
     * @return si le radar a sondé cette case
     */
    public boolean couvre(Coordonnee coordonnee) {
        return cases.contains(coordonnee);
    }

    /**
     * Retourne le nombre de cases de bateaux détectées qui n'ont pas encore été touchées.
     *
     * @param etats l'état actuel de chaque case, vu par l'adversaire
     * @return le nombre détecté, moins les cases sondées touchées depuis
     */
    public int restantes(Function<Coordonnee, EtatCase> etats) {
        int touchees = 0;
        for (Coordonnee coordonnee : cases) {
            EtatCase etat = etats.apply(coordonnee);
            if (etat == EtatCase.TOUCHE || etat == EtatCase.COULE) {
                touchees++;
            }
        }
        return nombre - touchees;
    }
}
