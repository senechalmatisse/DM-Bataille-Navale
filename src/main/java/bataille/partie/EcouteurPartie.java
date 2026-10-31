package bataille.partie;

/** Reçoit les événements d'une partie, dans l'ordre où ils se produisent. */
@FunctionalInterface
public interface EcouteurPartie {

    /**
     * Traite un événement de la partie.
     *
     * @param evenement ce qui vient de se passer
     */
    void surEvenement(EvenementPartie evenement);
}
