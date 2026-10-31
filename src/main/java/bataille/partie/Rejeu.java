package bataille.partie;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Le replay d'une partie enregistrée, coup par coup : avancer, reculer, aller à un coup donné.
 * <p>
 * Chaque position reconstruit la partie depuis le début et rejoue les coups jusque-là : c'est simple et sûr,
 * et une partie ne compte que quelques centaines de coups.
 */
public final class Rejeu {
    /** La partie enregistrée. */
    private final EnregistrementPartie enregistrement;
    /** Le nombre de coups rejoués. */
    private int position;
    /** La partie après ces coups. */
    private Partie partie;

    /**
     * Prépare le replay, positionné avant le premier coup.
     *
     * @param enregistrement la partie enregistrée
     */
    public Rejeu(EnregistrementPartie enregistrement) {
        this.enregistrement = Objects.requireNonNull(enregistrement, "enregistrement");
        allerA(0);
    }

    /**
     * Retourne la partie enregistrée.
     *
     * @return l'enregistrement
     */
    public EnregistrementPartie getEnregistrement() {
        return enregistrement;
    }

    /**
     * Retourne le nombre total de coups.
     *
     * @return le nombre de coups enregistrés
     */
    public int getNombreCoups() {
        return enregistrement.ordres().size();
    }

    /**
     * Retourne le nombre de coups rejoués.
     *
     * @return la position, entre 0 et le nombre de coups
     */
    public int getPosition() {
        return position;
    }

    /**
     * Retourne la partie dans l'état de la position courante.
     *
     * @return la partie reconstruite ; ne pas y jouer de coup
     */
    public Partie getPartie() {
        return partie;
    }

    /**
     * Retourne le dernier coup rejoué.
     *
     * @return le coup, ou une valeur vide au début du replay
     */
    public Optional<Coup> getDernierCoup() {
        return position == 0 ? Optional.empty() : Optional.of(partie.getHistorique().get(position - 1));
    }

    /**
     * Va à une position donnée.
     *
     * @param nouvellePosition le nombre de coups à rejouer, ramené entre 0 et le nombre de coups
     */
    public void allerA(int nouvellePosition) {
        position = Math.max(0, Math.min(nouvellePosition, getNombreCoups()));
        partie = enregistrement.restaurer(position, new Random(0));
    }

    /**
     * Rejoue le coup suivant.
     *
     * @return faux si le replay était déjà à la fin
     */
    public boolean avancer() {
        if (position == getNombreCoups()) {
            return false;
        }
        allerA(position + 1);
        return true;
    }

    /**
     * Revient au coup précédent.
     *
     * @return faux si le replay était déjà au début
     */
    public boolean reculer() {
        if (position == 0) {
            return false;
        }
        allerA(position - 1);
        return true;
    }
}
