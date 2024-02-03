package bataille.joueurs;

import java.lang.Character;
import java.util.*;
import bataille.jeu.bateaux.*;
import bataille.jeu.Jeu;

/**
 * Une classe abstraite représentant un joueur typique pour un partie de bataille navale.
 *
 * @author Matisse Senechal, Ali Azou, Rafik Halit, Souleymane Barry, Université de Caen Normandie, France
 */
public abstract class Joueur {
    /** Le score du joueur. */
    private int score;
    /** Un nombre aléatoire. */
    private Random random;

    /** Builds a new instance. */
    public Joueur() {
        this.score = 0;
        random = new Random();
    }

    /**
     * Retourne le nom du joueur humain.
     * @return Le nom du joueur humain
     */
    public abstract String getNom();

    /**
     * Retourne le score actuelle du joueur.
     * @return Le score actuelle du joueur
     */
    public int getScore() {
        return score;
    }
    /**
     * Ajoute un point au score du joueur
     */
    public void ajoutPoint() {
        score++;
    }

    /**
     * Retourne un entier compris entre deux valeurs en entrée.
     * @return Un entier compris entre deux valeurs en entrée
     */
    public int nombreAlea(int debutCompris, int finNonComprise) {
        return debutCompris + random.nextInt(finNonComprise - debutCompris);
    }


    /**
     * Retourne une liste contenant des longueurs choises au hasard.
     * @return Une liste contenant des longueurs choises au hasard
     */
    private ArrayList<Integer> listeLongueursFlotte(Jeu bataille, int nbBateaux) {
        ArrayList<Integer> longueursFlotte = bataille.getlisteLongueursFlotte();
        for (int bateau=0; bateau < nbBateaux; bateau++) {
            int longueurAlea = random.nextInt(4) + 1;
            longueursFlotte.add(longueurAlea);
        }
        return longueursFlotte;
    }

    /**
     * Place les bateaux du joueur dans sa grille  
     */
    public void initListeBateaux(Jeu bataille, int nbBateaux) {
        ArrayList<Integer> listeDesLongueurs = listeLongueursFlotte(bataille, nbBateaux);
        ArrayList<Bateau> listeDesBateaux = bataille.getListeBateauxJeu();
        int tailleGrille = bataille.getTaille();
        for (int bateau = 0; bateau < nbBateaux; bateau++) {
            int longueur = listeDesLongueurs.get(bateau);
            do {
                boolean horizontal = random.nextBoolean();
                int ligne = random.nextInt(tailleGrille) + 1;
                int colonne = random.nextInt(tailleGrille) + 1;
                if (positionOk(bataille, ligne, colonne, longueur, horizontal)) {
                    creerBateau(bataille, ligne, colonne, horizontal, longueur);
                    int indexDernierBateauAjoute = listeDesBateaux.size()-1;
                    bataille.rempliSelonDirection(listeDesBateaux.get(indexDernierBateauAjoute));
                    break;
                }
            } while(true);
        }
        Joueur joueur = bataille.getJoueurCourant();
        if(joueur instanceof Humain)
            System.out.println(bataille.initSituationString());
        bataille.changeJoueur();
    }

    /**
     * Crée un nouveau bateau et l'ajoute aux listes des bateaux
     */
    public void creerBateau(Jeu bataille, int ligne, int colonne, boolean horizontal, int longueur) {
        Joueur joueur = bataille.getJoueurCourant();
        Bateau bateau = null;
        switch(longueur) {
            case 1:
                bateau = new SousMarin(ligne, colonne, horizontal);
                break;
            case 2:
                bateau = new Torpilleur(ligne, colonne, horizontal);
                break;
            case 3:
                bateau = new Croiseur(ligne, colonne, horizontal);
                break;
            case 4:
                bateau = new PorteAvions(ligne, colonne, horizontal);
                break;
        }
        bataille.getListeBateauxJeu().add(bateau);
    }

    /**
     * Retourne si les coordonnées choisit se trouve dans la grille.
     * @return Si les coordonnées choisit se trouve dans la grille
     */
    protected abstract boolean ligneColonneValide(Jeu bataille, String choix);

    /**
     * Retourne si les coordonnees rentrées en fonction de l'orientation du bateau sont dans la grille.
     * @return Si les coordonnees rentrées en fonction de l'orientation du bateau sont dans la grille.
     */
    public boolean positionOk(Jeu bataille, int ligne, int colonne, int longueur, boolean horizontal) {
        int tailleGrille = bataille.getTaille();
        if (ligne < 0 || colonne < 0 || ligne >= tailleGrille || colonne >= tailleGrille)
            return false;
        if (horizontal) {
            if (colonne + longueur > tailleGrille)
                return false;
            for (int i = 0; i < longueur; i++) {
                if (bataille.recupCaseGrilleInit(ligne, colonne + i) != null)
                    return false;
            }
        } else {
            if (ligne + longueur > tailleGrille)
                return false;
            for (int i = 0; i < longueur; i++) {
                if (bataille.recupCaseGrilleInit(ligne + i, colonne) != null)
                    return false;
            }
        }
        return true;
    }

    /**
     * Le joueur choisit où il veut tirer  
     */
    public abstract void tirer(Jeu bataille, Scanner scanner);

    /**
     * Retourne selon la taille d'une grille une case de la grille n'ayant pas été touchée.
     * @return Selon la taille d'une grille une case de la grille n'ayant pas été touchée
     */
    public int[] coordonneesAlea(int tailleGrille, Jeu bataille) {
        int[] coords = new int[2];
        do {
            coords[0] = nombreAlea(0, tailleGrille);
            coords[1] = nombreAlea(0, tailleGrille);
        } while (!bataille.estDansLaGrille(coords[0], coords[1]) || bataille.caseTouche(coords[0], coords[1]));
        return coords;
    }
}
