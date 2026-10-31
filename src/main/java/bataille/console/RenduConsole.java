package bataille.console;

import bataille.modele.Coordonnee;
import bataille.modele.EtatCase;
import bataille.modele.Plateau;
import bataille.modele.VuePlateau;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Dessine les grilles en texte, avec un symbole stable par état de case. */
final class RenduConsole {
    /** L'espace entre les deux grilles affichées côte à côte. */
    private static final String ECART = "      ";
    /** La légende des symboles. */
    static final String LEGENDE = "Légende : ~ eau   # bateau   o manqué   X touché   * coulé";

    private RenduConsole() {
    }

    /**
     * Retourne le symbole d'un état de case.
     *
     * @param etat l'état de la case
     * @return le symbole affiché
     */
    static char symbole(EtatCase etat) {
        return switch (etat) {
            case EAU, INCONNUE -> '~';
            case BATEAU -> '#';
            case MANQUE -> 'o';
            case TOUCHE -> 'X';
            case COULE -> '*';
        };
    }

    /**
     * Dessine une grille, vue par son propriétaire.
     *
     * @param plateau le plateau à dessiner
     * @param titre le titre affiché au-dessus
     * @return la grille, sur plusieurs lignes
     */
    static String grille(Plateau plateau, String titre) {
        List<String> lignes = lignes(plateau.getTaille(), plateau::getEtatPourProprietaire, titre);
        return String.join(System.lineSeparator(), lignes) + System.lineSeparator();
    }

    /**
     * Dessine deux grilles côte à côte : la flotte du joueur à gauche, ce qu'il sait de la flotte adverse à droite.
     *
     * @param gauche le plateau du joueur, vu par lui
     * @param titreGauche le titre de la grille de gauche
     * @param droite la fonction qui donne l'état des cases de la grille de droite
     * @param titreDroite le titre de la grille de droite
     * @return les deux grilles, sur plusieurs lignes
     */
    static String deuxGrilles(Plateau gauche, String titreGauche, Function<Coordonnee, EtatCase> droite,
            String titreDroite) {
        int taille = gauche.getTaille();
        List<String> lignesGauche = lignes(taille, gauche::getEtatPourProprietaire, titreGauche);
        List<String> lignesDroite = lignes(taille, droite, titreDroite);
        int largeur = lignesGauche.stream().mapToInt(String::length).max().orElse(0);
        StringBuilder texte = new StringBuilder();
        for (int i = 0; i < lignesGauche.size(); i++) {
            String ligneGauche = lignesGauche.get(i);
            texte.append(ligneGauche).append(" ".repeat(largeur - ligneGauche.length())).append(ECART)
                    .append(lignesDroite.get(i)).append(System.lineSeparator());
        }
        return texte.toString();
    }

    /**
     * Dessine la grille du joueur et la vue de la grille adverse.
     *
     * @param plateau le plateau du joueur
     * @param titreGauche le titre de la grille du joueur
     * @param vueAdverse ce que le joueur sait du plateau adverse
     * @param titreDroite le titre de la grille adverse
     * @return les deux grilles, sur plusieurs lignes
     */
    static String deuxGrilles(Plateau plateau, String titreGauche, VuePlateau vueAdverse, String titreDroite) {
        return deuxGrilles(plateau, titreGauche, vueAdverse::getEtat, titreDroite);
    }

    private static List<String> lignes(int taille, Function<Coordonnee, EtatCase> etat, String titre) {
        List<String> lignes = new ArrayList<>();
        lignes.add("    " + titre);
        StringBuilder entete = new StringBuilder("    ");
        for (int colonne = 0; colonne < taille; colonne++) {
            entete.append(' ').append(Coordonnee.lettreColonne(colonne));
        }
        lignes.add(entete.toString());
        for (int ligne = 0; ligne < taille; ligne++) {
            StringBuilder texte = new StringBuilder(String.format(Locale.ROOT, "%3d ", ligne + 1));
            for (int colonne = 0; colonne < taille; colonne++) {
                texte.append(' ').append(symbole(etat.apply(new Coordonnee(ligne, colonne))));
            }
            lignes.add(texte.toString());
        }
        return lignes;
    }
}
