package bataille.ia;

import bataille.modele.Coordonnee;
import bataille.modele.EtatCase;
import bataille.modele.Orientation;
import bataille.modele.TypeBateau;
import bataille.modele.VuePlateau;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Niveau normal : la stratégie « chasse et cible ».
 * <ul>
 *   <li>En chasse, elle tire en damier : un bateau de longueur n coupe forcément une case sur n.</li>
 *   <li>Dès qu'un bateau est touché, elle vise les cases voisines ; avec deux touches alignées,
 *       elle prolonge la ligne jusqu'à couler le bateau.</li>
 * </ul>
 */
public final class ChasseCible implements StrategieTir {
    /** Le générateur de hasard. */
    private final Random random;

    /**
     * Construit la stratégie.
     *
     * @param random le générateur de hasard, à graine fixe pour une partie reproductible
     */
    public ChasseCible(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public Coordonnee choisirTir(VuePlateau adversaire) {
        List<Coordonnee> touchees = Cases.dansLEtat(adversaire, EtatCase.TOUCHE);
        if (!touchees.isEmpty()) {
            List<Coordonnee> prolongements = prolongements(adversaire, touchees);
            if (!prolongements.isEmpty()) {
                return Cases.auHasard(prolongements, random);
            }
            Set<Coordonnee> voisines = new LinkedHashSet<>();
            for (Coordonnee touchee : touchees) {
                voisines.addAll(Cases.voisinesInconnues(adversaire, touchee));
            }
            if (!voisines.isEmpty()) {
                return Cases.auHasard(new ArrayList<>(voisines), random);
            }
        }
        return chasser(adversaire);
    }

    /**
     * Retourne les cases qui prolongent une ligne d'au moins deux touches alignées.
     *
     * @param vue la vue adverse
     * @param touchees les cases touchées d'un bateau pas encore coulé
     * @return les extrémités encore inconnues de ces lignes
     */
    private List<Coordonnee> prolongements(VuePlateau vue, List<Coordonnee> touchees) {
        Set<Coordonnee> extremites = new LinkedHashSet<>();
        for (Coordonnee touchee : touchees) {
            for (Orientation orientation : Orientation.values()) {
                int deltaLigne = orientation.getDeltaLigne();
                int deltaColonne = orientation.getDeltaColonne();
                if (!estTouchee(vue, touchee.decaler(deltaLigne, deltaColonne))) {
                    continue;
                }
                Coordonnee debut = touchee;
                while (estTouchee(vue, debut.decaler(-deltaLigne, -deltaColonne))) {
                    debut = debut.decaler(-deltaLigne, -deltaColonne);
                }
                Coordonnee fin = touchee;
                while (estTouchee(vue, fin.decaler(deltaLigne, deltaColonne))) {
                    fin = fin.decaler(deltaLigne, deltaColonne);
                }
                Coordonnee avant = debut.decaler(-deltaLigne, -deltaColonne);
                Coordonnee apres = fin.decaler(deltaLigne, deltaColonne);
                if (Cases.estInconnue(vue, avant)) {
                    extremites.add(avant);
                }
                if (Cases.estInconnue(vue, apres)) {
                    extremites.add(apres);
                }
            }
        }
        return new ArrayList<>(extremites);
    }

    /**
     * Choisit une case en chasse : en damier, avec un pas égal au plus petit bateau encore à flot.
     *
     * @param vue la vue adverse
     * @return la case visée
     */
    private Coordonnee chasser(VuePlateau vue) {
        int pas = vue.getTypesRestants().stream().mapToInt(TypeBateau::getLongueur).min().orElse(1);
        List<Coordonnee> inconnues = Cases.dansLEtat(vue, EtatCase.INCONNUE);
        List<Coordonnee> damier = inconnues.stream()
                .filter(coordonnee -> (coordonnee.ligne() + coordonnee.colonne()) % pas == 0)
                .toList();
        return Cases.auHasard(damier.isEmpty() ? inconnues : damier, random);
    }

    private static boolean estTouchee(VuePlateau vue, Coordonnee coordonnee) {
        return vue.contient(coordonnee) && vue.getEtat(coordonnee) == EtatCase.TOUCHE;
    }
}
