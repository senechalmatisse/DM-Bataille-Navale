package bataille.ia;

import bataille.modele.Bateau;
import bataille.modele.Coordonnee;
import bataille.modele.EtatCase;
import bataille.modele.Orientation;
import bataille.modele.TypeBateau;
import bataille.modele.VuePlateau;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Niveau difficile : la carte de probabilités.
 * <p>
 * Pour chaque bateau encore à flot, la stratégie énumère toutes les positions compatibles avec ce qu'elle sait
 * (aucune case manquée ni coulée sous le bateau) et compte, case par case, combien de positions passent par là.
 * Elle vise la case la plus souvent couverte. Quand des cases sont touchées, seules comptent les positions
 * qui passent par ces touches, d'autant plus qu'elles en couvrent beaucoup.
 */
public final class CarteProbabilites implements StrategieTir {
    /** Le poids d'une position multiplié à chaque touche qu'elle couvre. */
    private static final long POIDS_PAR_TOUCHE = 20;

    /** Le générateur de hasard, pour départager les cases à égalité. */
    private final Random random;

    /**
     * Construit la stratégie.
     *
     * @param random le générateur de hasard, à graine fixe pour une partie reproductible
     */
    public CarteProbabilites(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public Coordonnee choisirTir(VuePlateau adversaire) {
        boolean modeCible = !Cases.dansLEtat(adversaire, EtatCase.TOUCHE).isEmpty();
        long[][] carte = calculerCarte(adversaire, modeCible);
        if (modeCible && estVide(carte)) {
            carte = calculerCarte(adversaire, false);
        }
        long meilleur = 0;
        List<Coordonnee> meilleures = new ArrayList<>();
        for (Coordonnee coordonnee : Cases.dansLEtat(adversaire, EtatCase.INCONNUE)) {
            long valeur = carte[coordonnee.ligne()][coordonnee.colonne()];
            if (valeur > meilleur) {
                meilleur = valeur;
                meilleures.clear();
            }
            if (valeur == meilleur) {
                meilleures.add(coordonnee);
            }
        }
        return Cases.auHasard(meilleures, random);
    }

    /**
     * Compte, pour chaque case inconnue, le poids des positions de bateau qui la couvrent.
     *
     * @param vue la vue adverse
     * @param modeCible si seules les positions qui couvrent une touche comptent
     * @return la carte des poids, indexée par ligne puis colonne
     */
    private static long[][] calculerCarte(VuePlateau vue, boolean modeCible) {
        int taille = vue.getTaille();
        long[][] carte = new long[taille][taille];
        for (TypeBateau type : vue.getTypesRestants()) {
            for (int ligne = 0; ligne < taille; ligne++) {
                for (int colonne = 0; colonne < taille; colonne++) {
                    for (Orientation orientation : Orientation.values()) {
                        Bateau position = new Bateau(type, new Coordonnee(ligne, colonne), orientation);
                        ajouterPosition(vue, carte, position.getCases(), modeCible);
                    }
                }
            }
        }
        return carte;
    }

    private static void ajouterPosition(VuePlateau vue, long[][] carte, List<Coordonnee> cases, boolean modeCible) {
        int touches = 0;
        for (Coordonnee coordonnee : cases) {
            if (!vue.contient(coordonnee)) {
                return;
            }
            EtatCase etat = vue.getEtat(coordonnee);
            if (etat == EtatCase.MANQUE || etat == EtatCase.COULE) {
                return;
            }
            if (etat == EtatCase.TOUCHE) {
                touches++;
            }
        }
        if (modeCible && touches == 0) {
            return;
        }
        long poids = 1;
        for (int i = 0; i < touches; i++) {
            poids *= POIDS_PAR_TOUCHE;
        }
        for (Coordonnee coordonnee : cases) {
            if (vue.getEtat(coordonnee) == EtatCase.INCONNUE) {
                carte[coordonnee.ligne()][coordonnee.colonne()] += poids;
            }
        }
    }

    private static boolean estVide(long[][] carte) {
        for (long[] ligne : carte) {
            for (long valeur : ligne) {
                if (valeur > 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
