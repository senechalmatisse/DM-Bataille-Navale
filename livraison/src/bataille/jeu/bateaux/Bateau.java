package bataille.jeu.bateaux;

/** Une classe abstraite pour représenter un bateau de n'importe quel type. */
public abstract class Bateau {
    /** 
     * La position de départ sur la ligne.
     * La position de départ sur la colonne.
     * Si le bateau est horizontal ou vertical.
    */
    private int posX, posY;
    private boolean horizontal;

    /**
     * Construit une nouvelle instance.
     * @param posX La position de départ sur la ligne
     * @param posY La position de départ sur la colonne
     * @param horizontal Si le bateau est horizontal ou vertical
     */
    public Bateau(int posX, int posY, boolean horizontal) {
        this.posX = posX;
        this.posY = posY;
        this.horizontal = horizontal;
    }

    /**
     * Retourne la position de départ sur la ligne.
     * @return La position de départ sur la ligne
     */
    public int getPosX() {
        return this.posX;
    }
    /**
     * Retourne la position de départ sur la colonne.
     * @return La position de départ sur la colonne
     */
    public int getPosY() {
        return this.posY;
    }
    /**
     * Retourne si le bateau est horizontal ou vertical.
     * @return Si le bateau est horizontal ou vertical
     */
    public boolean estHorizontal() {
        return this.horizontal;
    }
    /**
     * Retourne le nom du bateau.
     * @return Le nom du bateau
     */
    public abstract String getNom();
    /**
     * Retourne la longueur du bateau.
     * @return La longueur du bateau
     */
    public abstract int getLongueur();
}
