package bataille.jeu;

import java.util.ArrayList;

import bataille.jeu.bateaux.Bateau;
import bataille.joueurs.Joueur;

/**
 * Une classe servant à représenter une partie de Bataille Navale. Une instance représente une partie de bataille navale.
 * 
 * @author Matisse Senechal, Ali Azou, Rafik Halit, Souleymane Barry, Université de Caen Normandie, France
 */
public class Mer extends Grille implements Jeu {
    /** 
	 * Deux listes pour récupérer les bateaux.
	 * La taille de la grille.
	 * Les grilles stockant les adresses des bateaux.
     * Les grilles stockant les tires sur les bateaux;
	 */
    private ArrayList<Bateau> listeBateauxJeu;
    private ArrayList<Integer> listeLongueursFlotte;
    private int tailleGrille;
    private Bateau[][] grilleInitJ1, grilleInitJ2;
    private boolean[][] grilleTirJ1, grilleTirJ2;

    /**
     * Builds a new instance.	 
	 * @param joueur1 Le nom du joueur1 dans cette instance
	 * @param joueur2 Le nom du joueur2 dans cette instance
     * @param tailleGrille La taille maximale de la grille dans cette instance
     */
    public Mer(Joueur joueur1, Joueur joueur2, int tailleGrille) {
        super(joueur1, joueur2);
        this.tailleGrille = tailleGrille;
        this.grilleInitJ1 = new Bateau[tailleGrille][tailleGrille];
        this.grilleInitJ2 = new Bateau[tailleGrille][tailleGrille];
        this.grilleTirJ1 = new boolean[tailleGrille][tailleGrille];
        this.grilleTirJ2 = new boolean[tailleGrille][tailleGrille];
        this.listeBateauxJeu = new ArrayList<Bateau>();
        this.listeLongueursFlotte = new ArrayList<Integer>();
    }
    public Mer(Joueur joueur1, Joueur joueur2) {
        super(joueur1, joueur2);
        this.tailleGrille = 10;
        this.grilleInitJ1 = new Bateau[10][10];
        this.grilleInitJ2 = new Bateau[10][10];
        this.grilleTirJ1 = new boolean[10][10];
        this.grilleTirJ2 = new boolean[10][10];
        this.listeBateauxJeu = new ArrayList<Bateau>();
        this.listeLongueursFlotte = new ArrayList<Integer>();
    }

    @Override
    public int getTaille() {
        return this.tailleGrille;
    }

    @Override
    public ArrayList<Bateau> getListeBateauxJeu() {
        return listeBateauxJeu;
    }

    @Override
    public ArrayList<Integer> getlisteLongueursFlotte() {
        return listeLongueursFlotte;
    }

    @Override
    public Bateau recupCaseGrilleInit(int ligne, int colonne) {
        if(getJoueurCourant().equals(getJoueur1()))
            return grilleInitJ1[ligne][colonne];
        else
            return grilleInitJ2[ligne][colonne];
    }

    @Override
    public Bateau recupCaseGrilleInitAdversaire(int ligne, int colonne) {
        if(getJoueurCourant().equals(getJoueur1()))
            return grilleInitJ2[ligne][colonne];
        else
            return grilleInitJ1[ligne][colonne];
    }

    /**
     * Place le bateau dans la grille.
     * @param ligne ligne de la grille
     * @param colonne colonne du bateau
     * @param bateau bateau à placer
     */
    private void setCaseGrilleInit(int ligne, int colonne, Bateau bateau) {
        if(getJoueurCourant().equals(getJoueur1()))
            grilleInitJ1[ligne][colonne] = bateau;
        else
            grilleInitJ2[ligne][colonne] = bateau;
    }

    @Override
    public boolean recupCaseGrilleTir(int ligne, int colonne) {
        if(getJoueurCourant().equals(getJoueur1()))
            return grilleTirJ2[ligne][colonne];
        else
            return grilleTirJ1[ligne][colonne];
    }

    @Override
    public void setCaseGrilleTir(int ligne, int colonne) {
        boolean[][] grilleTirJoueur = (getJoueurCourant().equals(getJoueur1())) ? grilleTirJ2 : grilleTirJ1;
        grilleTirJoueur[ligne][colonne] = true;
    }

    @Override
    public boolean bateauDejaInit(int ligne, int colonne, int longueur, boolean horizontal) {
        for(int j = 0; j < longueur; j++) {
            int ligneGrille = ligne + (horizontal ? 0 : j);
            int colonneGrille = colonne + (horizontal ? j : 0);
            if(!estDansLaGrille(ligneGrille, colonneGrille) || recupCaseGrilleInit(ligneGrille, colonneGrille) != null)
                return true;
        }
        return false;
    }

    @Override
    public boolean estUnBateau(int ligne, int colonne) {
        return (estDansLaGrille(ligne, colonne)) && (recupCaseGrilleInitAdversaire(ligne, colonne) != null);
    }

    @Override
    public void rempliSelonDirection(Bateau bateau) {
        int ligne = bateau.getPosX();
        int colonne = bateau.getPosY();
        int longueur = bateau.getLongueur();
        int ligneSuivante = bateau.estHorizontal() ? 0 : 1;
        int colonneSuivante = bateau.estHorizontal() ? 1 : 0;
        for(int i=0; i < longueur; i++) {
            setCaseGrilleInit(ligne, colonne, bateau);
            ligne += ligneSuivante;
            colonne += colonneSuivante;
        }
    }

    @Override
    public boolean estDansLaGrille(int ligne, int colonne) {
        return (ligne >= 0 && ligne < getTaille() && colonne >= 0 && colonne < getTaille());
    }

    @Override
    public boolean caseTouche(int ligne, int colonne) {
        return recupCaseGrilleTir(ligne, colonne);
    }

    @Override
    public boolean bateauCoule(int ligne, int colonne) {
        Bateau bateau = recupCaseGrilleInitAdversaire(ligne, colonne);
        boolean estHorizontal = bateau.estHorizontal();
        int longueur = bateau.getLongueur();
        int posX = bateau.getPosX();
        int posY = bateau.getPosY();
        for(int i=0; i < longueur; i++) {
            int x = estHorizontal ? posX : posX + i;
            int y = estHorizontal ? posY + i : posY;
            if(!caseTouche(x, y) || !estUnBateau(x, y))
                return false;
        }
        return true;
    }

    @Override
    public void bateauTouche(int ligne, int colonne) {
        if(!estUnBateau(ligne, colonne)) {
            System.out.println("Manqué");
            return;
        }
        if(bateauCoule(ligne, colonne)) {
            String nomBateau = recupCaseGrilleInitAdversaire(ligne, colonne).getNom();
            System.out.println(nomBateau + " coulé");
        }
        else
            System.out.println("Bateau touché");
        getJoueurCourant().ajoutPoint();
    }


    @Override
    public int scorePourGagner() {
        int scoreTotal = 0;
        ArrayList<Bateau> listeDesBateaux = getListeBateauxJeu(); 
        for(Bateau bateau : listeDesBateaux)
            scoreTotal += bateau.getLongueur();
        return scoreTotal / 2;
    }

    @Override
    public boolean partieTerminee() {
        return getJoueurCourant().getScore() == scorePourGagner();
    }

    @Override
    public Joueur getGagnant() throws IllegalStateException {
        if (partieTerminee()) {
            if (getJoueurCourant().equals(getJoueur1()))
                return getJoueur1();
            else
                return getJoueur2();
        }
        return null;
    }

    @Override
    public String initSituationString() {
        StringBuilder grillePlacementBateaux = new StringBuilder();
        grillePlacementBateaux.append("                    ------------ Voici votre grille ------------\n");
        grillePlacementBateaux.append(afficheColonne(getTaille()));
        for (int ligne = 0; ligne < getTaille(); ligne++) {
            if (ligne < 9)
                grillePlacementBateaux.append(ligne + 1).append(" ");
            else
                grillePlacementBateaux.append(ligne + 1);
            for (int colonne = 0; colonne < getTaille(); colonne++) {
                if (recupCaseGrilleInit(ligne, colonne) == null)
                    grillePlacementBateaux.append(" ~");
                else
                    grillePlacementBateaux.append(" !");
            }
            grillePlacementBateaux.append("\n");
        }
        return grillePlacementBateaux.toString();
    }

    @Override
    public String situationToString() {
        StringBuilder grilleAffichee = new StringBuilder(afficheColonne(tailleGrille));
        for (int ligne = 0; ligne < tailleGrille; ligne++) {
            grilleAffichee.append(ligne < 9 ? (ligne + 1) + " " : (ligne + 1) + "");
            for (int colonne = 0; colonne < tailleGrille; colonne++) {
                if (!caseTouche(ligne, colonne))
                    grilleAffichee.append(" ~");
                else {
                    if(estUnBateau(ligne, colonne))
                        grilleAffichee.append(bateauCoule(ligne, colonne) ? " O" : " !");
                    else
                        grilleAffichee.append(" X");
                }
            }
            grilleAffichee.append("\n");
        }
        return grilleAffichee.toString();
    }
}
