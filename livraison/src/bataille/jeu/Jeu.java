package bataille.jeu;

import java.util.ArrayList;
import bataille.joueurs.Joueur;
import bataille.jeu.bateaux.*;

/**
 * Une interface représentant une partie typique d'un jeu de bataille navale en demandant
 * le joueur courant, ainsi que le joueur1 et joueur2. Puis en demandant les paramètres à
 * remplir pour la partie. Montre au joueur sa grille de départ et ensuite selon si son tir
 * est valide ou non lui montre le résultat de son action. Un fois que le coup a été éxécuté
 * on change de joueur jusqu'à ce que la partie soit terminée. Une fois la partie terminée
 * le vainqueur est affiché.  
 * 
 * @author Matisse Senechal, Ali Azou, Rafik Halit, Souleymane Barry, Université de Caen Normandie, France
 */
public interface Jeu {
    /**
     * Retourne le joueur courant.
     * @return Le joueur courant
     */
    public Joueur getJoueurCourant();
    /**
     * Retourne le joueur1.
     * @return Le joueur1
     */
    public Joueur getJoueur1();
    /**
     * Retourne le joueur2.
     * @return Le joueur2
     */
    public Joueur getJoueur2();
    /**
     * Retourne la taille de la grille.
     * @return La taille de la grille
     */
    public int getTaille();
    /**
     * Chnage le joueur courant.
     */
    public void changeJoueur();
    /**
     * Retourne le tir à la case correspondante.
     * @param ligne ligne de la grille
     * @param colonne colonne de la grille
     * @return Le tir à la case correspondante
     */
    public boolean recupCaseGrilleTir(int ligne, int colonne);
    /**
     * Retourne le bateau de l'adversaire à la case correspondante.
     * @param ligne ligne de la grille
     * @param colonne colonne de la grille
     * @return Le bateau de l'adversaire à la case correspondante
     */
    public Bateau recupCaseGrilleInitAdversaire(int ligne, int colonne);
    /**
     * Retourne la liste des bateaux à placer.
     * @return La liste des bateaux à placer
     */
    public ArrayList<Bateau> getListeBateauxJeu();
    /**
     * Retourne la liste des longueurs des bateaux à placer.
     * @return La liste des longueurs des bateaux à placer
     */
    public ArrayList<Integer> getlisteLongueursFlotte();
    /**
     * Retourne si la case aux coordonnées données en entrée se trouve dans la grille.
     * @param ligne ligne de la grille
     * @param colonne colonne de la grille
     * @return Si la case aux coordonnées données en entrée se trouve dans la grille
     */
    public boolean estDansLaGrille(int ligne, int colonne);
    /**
     * Retourne si la partie est terminée.
     * @return Si la partie est terminée
     */
    public boolean partieTerminee();
    /**
     * Retourne une représentation du cours de la partie (sous la forme d'une chaîne de caractère de plusieurs lignes).
     * @return Une représentation du cours de la partie (sous la forme d'une chaîne de caractère de plusieurs lignes)
     */
    public String situationToString();
    /**
     * Retourne une représentation du placement des bateaux (sous la forme d'une chaîne de caractère de plusieurs lignes).
     * @return Une représentation du placement des bateaux (sous la forme d'une chaîne de caractère de plusieurs lignes)
     */
    public String initSituationString();
    /**
     * Retourne si la case a déjà été touchée.
     * @param ligne ligne de la grille
     * @param colonne colonne de la grille
     * @return Si la case a déjà été touchée
     */
    public boolean caseTouche(int ligne, int colonne);
    /**
     * Retourne si un bateau est coulé ou pas.
     * @param ligne ligne de la grille
     * @param colonne colonne du bateau
     * @return Si un bateau est coulé ou pas
     */
    public boolean bateauCoule(int ligne, int colonne);
    /**
     * Retourne le bateau à la case correspondante.
     * @param ligne ligne de la grille
     * @param colonne colonne de la grille
     * @return Le bateau à la case correspondante
     */
    public Bateau recupCaseGrilleInit(int ligne, int colonne);
    /**
     * Affiche si le bateau a été touché et ajoute un point au joueur courant
     * @param ligne ligne de la grille
     * @param colonne colonne de la grille
     */
    public void bateauTouche(int ligne, int colonne);
    /**
     * Place le tir dans la grille.
     * @param ligne ligne de la grille
     * @param colonne colonne de la grille
     */
    public void setCaseGrilleTir(int ligne, int colonne);
    /**
     * Rempli les cases d'une grille par un bateau selon sa direction.
     * @param bateau bateau à placer
     */
    public void rempliSelonDirection(Bateau bateau);
    /**
     * Retourne si un bateau avec cette longueeur est dajà présent dans la grille.
     * @param ligne ligne de la grille
     * @param colonne colonne de la grille
     * @param longueur longueur du bateau
     * @param horizontal direction du bateau
     * @return Si un bateau avec cette longueeur est dajà présent dans la grille
     */
    public boolean bateauDejaInit(int ligne, int colonne, int longueur, boolean horizontal);
    /**
     * Retourne le nombre total de point à avoir pour gagner.
     * @return Le nombre total de point à avoir pour gagner.
     */
    public int scorePourGagner();
    /**
     * Retourne le nom du gagnant.
     * @throws IllegalStateException indique que la méthode a été invoquée à un moment inapproprié
     * @return Le nom du gagnant
     */
    public Joueur getGagnant();
    /**
     * Retourne si la case de la grille renvoie vers l'adresse d'un bateau ou pas.
     * @param ligne ligne de la grille
     * @param colonne colonne du bateau
     * @return Si la case de la grille renvoie vers l'adresse d'un bateau ou pas
     */
    public boolean estUnBateau(int ligne, int colonne);
}
