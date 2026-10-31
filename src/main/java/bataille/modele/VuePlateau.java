package bataille.modele;

import java.util.List;

/**
 * Ce qu'un joueur sait du plateau adverse : le résultat de ses propres tirs, les bateaux coulés
 * et les types de bateaux encore à flot, mais jamais la position des bateaux intacts.
 * <p>
 * C'est tout ce que reçoit une intelligence artificielle pour choisir son tir.
 */
public interface VuePlateau {

    /**
     * Retourne la taille du plateau.
     *
     * @return le nombre de lignes, égal au nombre de colonnes
     */
    int getTaille();

    /**
     * Indique si une case est dans la grille.
     *
     * @param coordonnee la case à tester
     * @return si la case est dans la grille
     */
    boolean contient(Coordonnee coordonnee);

    /**
     * Décrit l'étendue de la grille, par exemple « A1 à J10 ».
     *
     * @return la première et la dernière case de la grille
     */
    default String decrireEtendue() {
        return new Coordonnee(0, 0) + " à " + new Coordonnee(getTaille() - 1, getTaille() - 1);
    }

    /**
     * Retourne l'état connu d'une case.
     *
     * @param coordonnee une case de la grille
     * @return {@link EtatCase#INCONNUE}, {@link EtatCase#MANQUE}, {@link EtatCase#TOUCHE} ou {@link EtatCase#COULE}
     */
    EtatCase getEtat(Coordonnee coordonnee);

    /**
     * Retourne les bateaux déjà coulés, dont la position est désormais connue.
     *
     * @return les bateaux coulés, dans une liste non modifiable
     */
    List<Bateau> getBateauxCoules();

    /**
     * Retourne les types des bateaux encore à flot. La composition de la flotte est connue des deux joueurs.
     *
     * @return les types des bateaux pas encore coulés
     */
    List<TypeBateau> getTypesRestants();
}
