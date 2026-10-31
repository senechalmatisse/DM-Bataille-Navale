package bataille.swing;

/** Les animations jouées sur une grille ; le menu Options permet de les désactiver. */
enum Effet {
    /** Des ronds dans l'eau : un tir manqué. */
    ECLABOUSSURE(650),
    /** Une boule de feu : un tir au but. */
    EXPLOSION(650),
    /** Le bateau qui vient de couler clignote. */
    NAUFRAGE(1100),
    /** Une onde balaie la zone sondée par le radar. */
    BALAYAGE(900);

    /** La durée de l'animation, en millisecondes. */
    private final int duree;

    Effet(int duree) {
        this.duree = duree;
    }

    /**
     * Retourne la durée de l'animation.
     *
     * @return la durée, en millisecondes
     */
    int getDuree() {
        return duree;
    }
}
