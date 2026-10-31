package bataille.ia;

import bataille.modele.Coordonnee;
import bataille.modele.EtatCase;
import bataille.modele.VuePlateau;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Outils communs aux stratégies : parcourir la vue adverse et tirer une case au hasard. */
final class Cases {
    /** Les quatre directions : haut, bas, gauche, droite. */
    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private Cases() {
    }

    /**
     * Retourne les cases de la vue qui sont dans un état donné.
     *
     * @param vue la vue adverse
     * @param etat l'état recherché
     * @return les cases dans cet état, ligne par ligne
     */
    static List<Coordonnee> dansLEtat(VuePlateau vue, EtatCase etat) {
        List<Coordonnee> cases = new ArrayList<>();
        for (int ligne = 0; ligne < vue.getTaille(); ligne++) {
            for (int colonne = 0; colonne < vue.getTaille(); colonne++) {
                Coordonnee coordonnee = new Coordonnee(ligne, colonne);
                if (vue.getEtat(coordonnee) == etat) {
                    cases.add(coordonnee);
                }
            }
        }
        return cases;
    }

    /**
     * Retourne les voisines d'une case, dans la grille et pas encore visées.
     *
     * @param vue la vue adverse
     * @param centre la case dont on cherche les voisines
     * @return les voisines inconnues, parmi les quatre directions
     */
    static List<Coordonnee> voisinesInconnues(VuePlateau vue, Coordonnee centre) {
        List<Coordonnee> voisines = new ArrayList<>();
        for (int[] direction : DIRECTIONS) {
            Coordonnee voisine = centre.decaler(direction[0], direction[1]);
            if (estInconnue(vue, voisine)) {
                voisines.add(voisine);
            }
        }
        return voisines;
    }

    /**
     * Indique si une case est dans la grille et pas encore visée.
     *
     * @param vue la vue adverse
     * @param coordonnee la case à tester
     * @return si la case peut encore être visée
     */
    static boolean estInconnue(VuePlateau vue, Coordonnee coordonnee) {
        return vue.contient(coordonnee) && vue.getEtat(coordonnee) == EtatCase.INCONNUE;
    }

    /**
     * Tire une case au hasard dans une liste.
     *
     * @param cases les cases possibles, au moins une
     * @param random le générateur de hasard
     * @return une des cases
     * @throws IllegalStateException si la liste est vide
     */
    static Coordonnee auHasard(List<Coordonnee> cases, Random random) {
        if (cases.isEmpty()) {
            throw new IllegalStateException("Plus aucune case à viser : la partie aurait dû se terminer.");
        }
        return cases.get(random.nextInt(cases.size()));
    }
}
