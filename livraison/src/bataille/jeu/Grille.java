package bataille.jeu;

import bataille.joueurs.Joueur;

/**
 * Une classe abstraite qui représente un partie typique pour n'importe quel type de jeu.
 *
 * @author Matisse Senechal, Ali Azou, Rafik Halit, Souleymane Barry, Université de Caen Normandie, France
 */
public abstract class Grille implements Jeu { 
    /** Les noms des joueurs. */
    private Joueur joueur1, joueur2, joueurCourant;

    /**
     * Construits une nouvelle instance.
     * @param joueur1 Nom du joueur 1
     * @param joueur2 Nom du joueur 2
     * @param joueurCourant Nom du joueur courant
     */
    public Grille(Joueur joueur1, Joueur joueur2) {
        this.joueur1 = joueur1;
        this.joueur2 = joueur2;
        this.joueurCourant = joueur1;
    }

    @Override
    public Joueur getJoueur1() {
        return this.joueur1;
    }
    @Override
    public Joueur getJoueur2() {
        return this.joueur2;
    }
    @Override
    public Joueur getJoueurCourant() {
        return this.joueurCourant;
    }
    
    @Override
    public void changeJoueur() {
        if(joueurCourant.equals(joueur1))
            joueurCourant = joueur2;
        else
            joueurCourant = joueur1;
    }

    /**
     * Retourne une ligne avec un nombre de lettre définit en fonction de valeur.
     * @param valeur nombre de lettres à afficher
     * @return Une ligne avec un nombre de lettre définit en fonction de valeur.
     */
    protected String afficheColonne(int valeur) {
        StringBuilder colonneGrille = new StringBuilder(" ");
        if (valeur > 9)
            colonneGrille.append(" ");
        for (int i = 0; i < valeur; i++)
            colonneGrille.append(" ").append((char) ('A' + i));
        colonneGrille.append("\n");
        return colonneGrille.toString();
    }
}