package bataille.joue;

import java.util.Scanner;
import bataille.jeu.Jeu;

/**
 * Une classe qui gère une partie de bataille navale. Une instance qui gère une partie de bataille navale.
 * 
 * @author Matisse Senechal, Ali Azou, Rafik Halit, Souleymane Barry, Université de Caen Normandie, France
 */
public class Orchestrateur {
    /** 
     * La situation actuelle de la partie.
     * Le nombre de bateaux.
     */
    private Jeu jeu;
    private int nombreBateaux;

    /**
     * Construit une nouvelle instance.
     * @param jeu La situation actuelle de la partie
     * @param nombreBateaux Le nombre de bateaux
     */
    public Orchestrateur(Jeu jeu, int nombreBateaux) {
        this.jeu = jeu;
        this.nombreBateaux = nombreBateaux;
    }
    public Orchestrateur(Jeu jeu) {
        this(jeu, 4);
    }

    /** 
     * Gère une partie en faisant initialiser la grille des deux joueurs et qui ensuite
     * fait une boucle durant laquelle elle affiche la situation actuelle de la partie
     * et retourne le coup choisit par le joueur courant. 
     * Une fois la partie terminée, elle affiche le résultat. 
     * @param scanner choix du joueur
     */
    protected void joue(Scanner scanner) {
        jeu.getJoueurCourant().initListeBateaux(jeu, nombreBateaux);
        jeu.getJoueurCourant().initListeBateaux(jeu, nombreBateaux);
        while(!jeu.partieTerminee()) {
            jeu.getJoueurCourant().tirer(jeu, scanner);
        }
        System.out.println("              ---------- LE GAGNANT EST " + jeu.getJoueurCourant().getNom() + " ----------");
    }
}