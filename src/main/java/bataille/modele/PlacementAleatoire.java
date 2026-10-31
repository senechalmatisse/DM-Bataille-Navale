package bataille.modele;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Place des bateaux au hasard sur un plateau.
 * <p>
 * Chaque bateau est tiré parmi toutes ses positions libres, du plus long au plus court : toutes les cases
 * de la grille peuvent recevoir un bateau. Si un bateau ne trouve plus de place, les bateaux posés par
 * le tirage en cours sont retirés et le tirage recommence.
 */
public final class PlacementAleatoire {
    /** Le nombre de tirages tentés avant d'abandonner. */
    private static final int ESSAIS_MAX = 1000;

    private PlacementAleatoire() {
    }

    /**
     * Place au hasard des bateaux sur un plateau, en plus de ceux qui y sont déjà.
     *
     * @param plateau le plateau à compléter, qui n'a encore reçu aucun tir
     * @param types les types des bateaux à placer
     * @param random le générateur de hasard, à graine fixe pour un tirage reproductible
     * @return les bateaux placés
     * @throws IllegalStateException si les bateaux ne trouvent pas de place
     */
    public static List<Bateau> placer(Plateau plateau, List<TypeBateau> types, Random random) {
        List<TypeBateau> ordre = new ArrayList<>(types);
        ordre.sort(Comparator.comparingInt(TypeBateau::getLongueur).reversed());
        for (int essai = 0; essai < ESSAIS_MAX; essai++) {
            List<Bateau> poses = new ArrayList<>();
            if (essayer(plateau, ordre, random, poses)) {
                return List.copyOf(poses);
            }
            poses.forEach(plateau::retirer);
        }
        throw new IllegalStateException("Impossible de placer la flotte après " + ESSAIS_MAX + " essais.");
    }

    /**
     * Retourne toutes les positions où un bateau peut être placé.
     *
     * @param plateau le plateau
     * @param type le type du bateau
     * @return les positions libres
     */
    public static List<Position> positionsLibres(Plateau plateau, TypeBateau type) {
        List<Position> positions = new ArrayList<>();
        for (int ligne = 0; ligne < plateau.getTaille(); ligne++) {
            for (int colonne = 0; colonne < plateau.getTaille(); colonne++) {
                Coordonnee origine = new Coordonnee(ligne, colonne);
                for (Orientation orientation : Orientation.values()) {
                    if (plateau.peutPlacer(type, origine, orientation)) {
                        positions.add(new Position(origine, orientation));
                    }
                }
            }
        }
        return positions;
    }

    private static boolean essayer(Plateau plateau, List<TypeBateau> ordre, Random random, List<Bateau> poses) {
        for (TypeBateau type : ordre) {
            List<Position> libres = positionsLibres(plateau, type);
            if (libres.isEmpty()) {
                return false;
            }
            Position choisie = libres.get(random.nextInt(libres.size()));
            poses.add(plateau.placer(type, choisie.origine(), choisie.orientation()));
        }
        return true;
    }

    /**
     * Une position possible pour un bateau.
     *
     * @param origine la case d'origine
     * @param orientation l'orientation
     */
    public record Position(Coordonnee origine, Orientation orientation) {
    }
}
