package bataille.joue;

import java.util.Scanner;
import bataille.joueurs.*;
import bataille.jeu.*;
import bataille.jeu.bateaux.*;

/**
 * Une classe éxécutable qui permet à deux joueurs de
 * lancer une partie d'un jeu et d'y jouer.
 * 
 * @author Matisse Senechal, Ali Azou, Rafik Halit, Souleymane Barry, Université de Caen Normandie, France
 */
public class Jouer {
    public static void main (String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        Joueur joueur1 = new Humain();
        Joueur joueur2 = new Ordinateur();
        Jeu bataille = new Mer(joueur1, joueur2);
        Orchestrateur orchestrateur = new Orchestrateur(bataille, 5);
        orchestrateur.joue(scanner);

        scanner.close();
    }
} 








