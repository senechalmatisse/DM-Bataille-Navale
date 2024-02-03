package bataille.joueurs;

import java.util.*;
import java.lang.Character;
import bataille.jeu.bateaux.Bateau;
import bataille.jeu.*;

/**
 * Une classe représentant un joueur humain. Une instance représente un humain jouant à la bataille navale.
 * 
 * @author Matisse Senechal, Ali Azou, Rafik Halit, Souleymane Barry, Université de Caen Normandie, France
 */
public class Humain extends Joueur {
    /** 
     * Le nom du joueur humain
     * Un objet de Random
     */
    private String nom;
    private Random random;

    /** Construit une nouvelle instance. */
    public Humain() {
        this.nom = "JoueurHumain";
        random = new Random();
    }

    @Override
    public String getNom() {
        return this.nom;
    }

    /**
     * Retourne en fonction de la lettre en entrée le chiffre/nombre correspondant
     * @param lettre lettre à transformer
     * @return En fonction de la lettre en entrée le chiffre/nombre correspondant
     */
    private int transformeLettre(char lettre) {
        int index = Character.toUpperCase(lettre) - 'A';
        if (index >= 0 && index < 26)
            return index;
        return 27;
    }

    /**
     * Retourne selon le chiffre/nombre donné la ligne correspondante.
     * @param ligne ligne à transformer
     * @exception NumberFormatException indique que ligne n'a pas le format appoprié et que la conversion vers un type numérique est impossible
     * @return Selon le chiffre/nombre donné la ligne correspondante
     */
    private int renvoieLigne(String ligne) {
        try {
            int res = Integer.parseInt(ligne);
            return res;
        } catch (NumberFormatException e) {
            System.out.println("La ligne choisie n'est pas valide");
            return -1;
        }
    }

    /**
     * Retourne un entier si la chaine contient un entrée valide.
     * @param chaine chaine à transformer
     * @exception NumberFormatException indique que chaine n'a pas le format appoprié et que la conversion vers un type numérique est impossible
     * @return Un entier si la chaine contient un entrée valide.
     */
    private boolean stringEntierValide(String chaine) {
        try {
            Integer.parseInt(chaine);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }

    @Override
    public boolean ligneColonneValide(Jeu bataille, String choix) {
        if (choix.length() != 2) {
            System.out.println("Votre choix n'est pas valide");
            System.out.println("Où voulez-vous placer votre bateau ?");
            System.out.println("Choisissez un chiffre (ligne) et une lettre (colonne), exemple A1");
            return false;
        }
        char choixColonne = choix.charAt(0);
        String choixLigne = choix.substring(1);
        if (!Character.isLetter(choixColonne) || !stringEntierValide(choixLigne)) {
            System.out.println("Votre choix n'est pas valide");
            System.out.println("Où voulez-vous placer votre bateau ?");
            System.out.println("Choisissez un chiffre (ligne) et une lettre (colonne), exemple A1");
            return false;
        }
        int ligne = renvoieLigne(choixLigne) - 1;
        int colonne = transformeLettre(choixColonne);
        if (!bataille.estDansLaGrille(ligne, colonne)) {
            System.out.println("Votre choix n'est pas valide");
            System.out.println("Où voulez-vous placer votre bateau ?");
            System.out.println("Choisissez un chiffre (ligne) et une lettre (colonne), exemple A1");
            return false;
        }
        return true;
    }

    @Override
    public void tirer(Jeu bataille, Scanner scanner) {
        System.out.println("                ------------ C'EST AU TOUR DE " + nom + " DE JOUER ------------");
        System.out.println(bataille.situationToString());
        int ligne;
        int colonne;
        do {
            System.out.println("Où voulez-vous tirer ?");
            System.out.println("Choisissez un chiffre (ligne) et une lettre (colonne), exemple A1");
            String choix = scanner.next().trim();
            ligne = renvoieLigne(choix.substring(1)) - 1;
            colonne = transformeLettre(choix.charAt(0));
        } while(!bataille.estDansLaGrille(ligne, colonne) || bataille.caseTouche(ligne, colonne));
        bataille.setCaseGrilleTir(ligne, colonne);
        bataille.bateauTouche(ligne, colonne);
        if(!bataille.partieTerminee())
            bataille.changeJoueur();
    }
}
