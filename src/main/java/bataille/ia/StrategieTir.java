package bataille.ia;

import bataille.modele.Coordonnee;
import bataille.modele.VuePlateau;

/**
 * La manière dont l'ordinateur choisit ses tirs.
 * <p>
 * Une stratégie ne voit que la vue adverse : le résultat de ses tirs précédents, les bateaux coulés
 * et les types encore à flot. Elle ne connaît jamais la position des bateaux intacts.
 */
@FunctionalInterface
public interface StrategieTir {

    /**
     * Choisit la prochaine case à viser.
     *
     * @param adversaire ce que l'ordinateur sait du plateau adverse
     * @return une case de la grille qui n'a pas encore été visée
     */
    Coordonnee choisirTir(VuePlateau adversaire);
}
