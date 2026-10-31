package bataille.ia;

import bataille.modele.Capacite;
import bataille.modele.Coordonnee;
import bataille.modele.EtatCase;
import bataille.modele.Ordre;
import bataille.modele.VuePlateau;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Décide quand l'ordinateur utilise ses capacités spéciales, et où.
 * <ul>
 *   <li>Dès qu'un bateau est touché, il active le double tir pour l'achever plus vite.</li>
 *   <li>En chasse, il bombarde le carré de 2 × 2 le plus probable (à partir de son 3e tour), lance la torpille
 *       sur la ligne ou la colonne la plus probable (5e tour), puis passe le radar sur la zone la plus probable
 *       (7e tour).</li>
 * </ul>
 * Les probabilités viennent de la carte calculée par {@link CarteProbabilites}.
 */
public final class Tacticien {
    /** Le premier tour où l'ordinateur bombarde. */
    private static final int TOUR_BOMBARDEMENT = 3;
    /** Le premier tour où l'ordinateur lance la torpille. */
    private static final int TOUR_TORPILLE = 5;
    /** Le premier tour où l'ordinateur passe le radar. */
    private static final int TOUR_RADAR = 7;

    /** Les capacités que ce tacticien sait utiliser. */
    private final Set<Capacite> autorisees;
    /** Le générateur de hasard, pour départager les zones à égalité. */
    private final Random random;

    /**
     * Construit un tacticien.
     *
     * @param autorisees les capacités qu'il sait utiliser
     * @param random le générateur de hasard
     */
    public Tacticien(Set<Capacite> autorisees, Random random) {
        this.autorisees = autorisees.isEmpty() ? EnumSet.noneOf(Capacite.class) : EnumSet.copyOf(autorisees);
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Décide s'il faut utiliser une capacité ce tour-ci.
     *
     * @param vue la vue du plateau adverse
     * @param disponibles les capacités utilisables maintenant
     * @param tour le numéro du tour de l'ordinateur, à partir de 1
     * @return l'ordre de la capacité, ou une valeur vide pour un tir simple
     */
    public Optional<Ordre> choisir(VuePlateau vue, Set<Capacite> disponibles, int tour) {
        Set<Capacite> possibles = EnumSet.noneOf(Capacite.class);
        possibles.addAll(disponibles);
        possibles.retainAll(autorisees);
        if (possibles.isEmpty()) {
            return Optional.empty();
        }
        if (!Cases.dansLEtat(vue, EtatCase.TOUCHE).isEmpty()) {
            return possibles.contains(Capacite.DOUBLE_TIR) ? Optional.of(Ordre.doubleTir()) : Optional.empty();
        }
        long[][] carte = CarteProbabilites.calculerCarte(vue, false);
        if (possibles.contains(Capacite.BOMBARDEMENT) && tour >= TOUR_BOMBARDEMENT) {
            return meilleurCarre(vue, carte, 2, 0).map(Ordre::bombardement);
        }
        if (possibles.contains(Capacite.TORPILLE) && tour >= TOUR_TORPILLE) {
            return meilleureLigne(vue, carte);
        }
        if (possibles.contains(Capacite.RADAR) && tour >= TOUR_RADAR) {
            return meilleurCarre(vue, carte, 3, -1).map(Ordre::radar);
        }
        return Optional.empty();
    }

    /**
     * Cherche le carré entièrement dans la grille dont les cases inconnues sont les plus probables.
     *
     * @param vue la vue adverse
     * @param carte la carte des probabilités
     * @param cote la largeur du carré
     * @param decalage le décalage entre la case renvoyée et le coin haut gauche (−1 pour un centre de 3 × 3)
     * @return la case de référence du meilleur carré, ou une valeur vide si aucun carré n'a de case inconnue
     */
    private Optional<Coordonnee> meilleurCarre(VuePlateau vue, long[][] carte, int cote, int decalage) {
        long meilleur = 0;
        Coordonnee choisie = null;
        int taille = vue.getTaille();
        for (int ligne = 0; ligne + cote <= taille; ligne++) {
            for (int colonne = 0; colonne + cote <= taille; colonne++) {
                long somme = 0;
                for (int i = 0; i < cote; i++) {
                    for (int j = 0; j < cote; j++) {
                        somme += valeur(vue, carte, new Coordonnee(ligne + i, colonne + j));
                    }
                }
                if (somme > meilleur || (somme == meilleur && somme > 0 && random.nextBoolean())) {
                    meilleur = somme;
                    choisie = new Coordonnee(ligne - decalage, colonne - decalage);
                }
            }
        }
        return Optional.ofNullable(choisie);
    }

    /**
     * Cherche la ligne ou la colonne dont les cases inconnues sont les plus probables.
     *
     * @param vue la vue adverse
     * @param carte la carte des probabilités
     * @return l'ordre de torpille, ou une valeur vide si aucune ligne n'a de case inconnue
     */
    private Optional<Ordre> meilleureLigne(VuePlateau vue, long[][] carte) {
        long meilleur = 0;
        Ordre choisi = null;
        int taille = vue.getTaille();
        for (int i = 0; i < taille; i++) {
            long ligne = 0;
            long colonne = 0;
            for (int j = 0; j < taille; j++) {
                ligne += valeur(vue, carte, new Coordonnee(i, j));
                colonne += valeur(vue, carte, new Coordonnee(j, i));
            }
            if (ligne > meilleur) {
                meilleur = ligne;
                choisi = Ordre.torpilleSurLigne(i);
            }
            if (colonne > meilleur) {
                meilleur = colonne;
                choisi = Ordre.torpilleSurColonne(i);
            }
        }
        return Optional.ofNullable(choisi);
    }

    private static long valeur(VuePlateau vue, long[][] carte, Coordonnee coordonnee) {
        return vue.getEtat(coordonnee) == EtatCase.INCONNUE ? carte[coordonnee.ligne()][coordonnee.colonne()] : 0;
    }
}
