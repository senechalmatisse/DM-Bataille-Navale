package bataille.partie;

/**
 * Le bilan des tirs d'un joueur dans une partie.
 *
 * @param tirs le nombre de cases visées, capacités comprises, sans compter les cases déjà visées
 * @param tirsAuBut le nombre de cases visées qui ont touché ou coulé un bateau
 * @param capacites le nombre de capacités spéciales utilisées
 */
public record Statistiques(int tirs, int tirsAuBut, int capacites) {

    /**
     * Construit un bilan sans capacité utilisée.
     *
     * @param tirs le nombre de cases visées
     * @param tirsAuBut le nombre de tirs au but
     */
    public Statistiques(int tirs, int tirsAuBut) {
        this(tirs, tirsAuBut, 0);
    }

    /**
     * Retourne la part des tirs au but.
     *
     * @return la précision, en pourcentage arrondi, ou 0 si aucun tir n'a été joué
     */
    public int precisionEnPourcentage() {
        return tirs == 0 ? 0 : Math.round(100f * tirsAuBut / tirs);
    }
}
