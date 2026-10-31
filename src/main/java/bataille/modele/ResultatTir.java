package bataille.modele;

/** Le résultat d'un tir sur une case. */
public enum ResultatTir {
    /** Aucun bateau sur la case. */
    MANQUE("manqué"),
    /** Un bateau est touché mais flotte encore. */
    TOUCHE("touché"),
    /** Le tir touche la dernière case intacte d'un bateau. */
    COULE("coulé"),
    /** La case avait déjà été visée : le tir ne compte pas. */
    DEJA_JOUE("déjà joué");

    /** Le libellé affiché du résultat. */
    private final String libelle;

    ResultatTir(String libelle) {
        this.libelle = libelle;
    }

    /**
     * Retourne le libellé du résultat, par exemple « touché ».
     *
     * @return le libellé du résultat
     */
    public String getLibelle() {
        return libelle;
    }

    /**
     * Indique si le tir a atteint un bateau.
     *
     * @return si le résultat est {@link #TOUCHE} ou {@link #COULE}
     */
    public boolean estAuBut() {
        return this == TOUCHE || this == COULE;
    }
}
