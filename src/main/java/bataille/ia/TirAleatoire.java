package bataille.ia;

import bataille.modele.Coordonnee;
import bataille.modele.EtatCase;
import bataille.modele.VuePlateau;
import java.util.Objects;
import java.util.Random;

/** Niveau facile : tire au hasard parmi les cases pas encore visées. */
public final class TirAleatoire implements StrategieTir {
    /** Le générateur de hasard. */
    private final Random random;

    /**
     * Construit la stratégie.
     *
     * @param random le générateur de hasard, à graine fixe pour une partie reproductible
     */
    public TirAleatoire(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public Coordonnee choisirTir(VuePlateau adversaire) {
        return Cases.auHasard(Cases.dansLEtat(adversaire, EtatCase.INCONNUE), random);
    }
}
