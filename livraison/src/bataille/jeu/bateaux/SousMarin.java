package bataille.jeu.bateaux;

/**
 * Une classe représentant un bateau qui est un sous-marin. Une instance représente un bateau qui est de type sous-marin.
 * 
 * @author Matisse Senechal, Ali Azou, Rafik Halit, Souleymane Barry, Université de Caen Normandie, France
 */
public class SousMarin extends Bateau {
    /** 
     * Le nom du bateau
     * La longueur du bateau 
     */
    private String nom;
    private int longueur;

    /**
     * Construit une nouvelle instance.
     * @param posX La position de départ sur la ligne
     * @param posY La position de départ sur la colonne
     * @param horizontal Si le bateau est horizontal ou vertical
     */
	public SousMarin(int posX, int posY, boolean horizontal) {
		super(posX, posY, horizontal);
        this.nom = "Sous-marin";
        this.longueur = 1;
	}

    @Override
    public String getNom() {
        return this.nom;
    }
    @Override
    public int getLongueur() {
        return this.longueur;
    }
}