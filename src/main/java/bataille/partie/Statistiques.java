package bataille.partie;

/**
 * Le bilan des tirs d'un joueur.
 *
 * @param tirs le nombre de tirs joués, sans compter les cases déjà visées
 * @param tirsAuBut le nombre de tirs qui ont touché ou coulé un bateau
 */
public record Statistiques(int tirs, int tirsAuBut) {

    /**
     * Retourne la part des tirs au but.
     *
     * @return la précision, en pourcentage arrondi, ou 0 si aucun tir n'a été joué
     */
    public int precisionEnPourcentage() {
        return tirs == 0 ? 0 : Math.round(100f * tirsAuBut / tirs);
    }
}
