package bataille.joueurs;

import java.util.*;
import bataille.jeu.Jeu;
import bataille.jeu.bateaux.Bateau;

/**
 * Une classe représentant un joueur aléatoire. Une instance représentant un joueur aléatoire jouant de aléatoirement.
 * 
 * @author Matisse Senechal, Ali Azou, Rafik Halit, Souleymane Barry, Université de Caen Normandie, France
 */
public class Ordinateur extends Joueur {
    /** Un objet de Random */
    private Random random;

    /** Construit une nouvelle instance. */
    public Ordinateur() {
        random = new Random();
    }

    @Override
    public String getNom() {
        return "joueur aléatoire n°" + this.hashCode();
    }

    @Override
    protected boolean ligneColonneValide(Jeu bataille, String choix) {
        if (choix.length() != 2)
            return false;
        int ligne = choix.charAt(1) - '1';
        int colonne = choix.charAt(0) - 'A';
        return ligne >= 0 && ligne < bataille.getTaille() && colonne >= 0 && colonne < bataille.getTaille() && !bataille.estDansLaGrille(ligne, colonne);
    }

    /**
     * Retourne selon le chiffre donné l'orientation correspondante.
     * @param direction direction à transformer
     * @return Selon le chiffre donné l'orientation correspondante
     */
    private boolean renvoieHorizontal(int direction) {
        return direction == 1;
    }

    @Override
    public void tirer(Jeu bataille, Scanner scanner) {
        System.out.println("                ------------ C'EST AU TOUR DE " + getNom() + " DE JOUER ------------");
        int tailleGrille = bataille.getTaille();
        int ligne;
        int colonne;
        do {
            ligne = (int)(Math.random() * tailleGrille);
            colonne = (int)(Math.random() * tailleGrille);
        } while(bataille.caseTouche(ligne, colonne));
        bataille.setCaseGrilleTir(ligne, colonne);
        System.out.println(bataille.situationToString());
        bataille.bateauTouche(ligne, colonne);
        if (!bataille.partieTerminee())
            bataille.changeJoueur();
    }
}
