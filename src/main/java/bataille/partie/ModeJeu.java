package bataille.partie;

/** Les deux façons de jouer : contre l'ordinateur, ou à deux sur le même écran. */
public enum ModeJeu {
    /** Un joueur humain contre l'ordinateur. */
    CONTRE_ORDINATEUR("Contre l'ordinateur"),
    /** Deux joueurs humains sur le même écran, qui se le passent entre les tours. */
    DEUX_JOUEURS("À deux sur le même écran");

    /** Le nom affiché du mode. */
    private final String nom;

    ModeJeu(String nom) {
        this.nom = nom;
    }

    /**
     * Retourne le nom du mode.
     *
     * @return le nom affiché
     */
    @Override
    public String toString() {
        return nom;
    }
}
