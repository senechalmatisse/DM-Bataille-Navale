package bataille.partie;

import bataille.modele.Tir;
import java.util.Locale;

/** Les phrases qui annoncent le résultat d'un tir, communes à la console et à la fenêtre. */
public final class Annonces {

    private Annonces() {
    }

    /**
     * Annonce le tir du joueur, par exemple « Vous tirez en B7 : touché ! ».
     *
     * @param tir le compte rendu du tir
     * @return la phrase à afficher
     */
    public static String tirDuJoueur(Tir tir) {
        String debut = "Vous tirez en " + tir.cible() + " : ";
        return switch (tir.resultat()) {
            case MANQUE -> debut + "manqué.";
            case TOUCHE -> debut + "touché !";
            case COULE -> debut + "coulé ! Vous avez coulé "
                    + tir.bateauCoule().orElseThrow().getNomAvecArticle() + ".";
            case DEJA_JOUE -> "Vous aviez déjà tiré en " + tir.cible() + ".";
        };
    }

    /**
     * Annonce le tir de l'ordinateur, par exemple « L'ordinateur tire en C3 : coulé ! Il a coulé votre torpilleur. ».
     *
     * @param tir le compte rendu du tir
     * @return la phrase à afficher
     */
    public static String tirDeLOrdinateur(Tir tir) {
        String debut = "L'ordinateur tire en " + tir.cible() + " : ";
        return switch (tir.resultat()) {
            case MANQUE -> debut + "manqué.";
            case TOUCHE -> debut + "touché !";
            case COULE -> debut + "coulé ! Il a coulé votre "
                    + tir.bateauCoule().orElseThrow().getNom().toLowerCase(Locale.ROOT) + ".";
            case DEJA_JOUE -> "L'ordinateur avait déjà tiré en " + tir.cible() + ".";
        };
    }

    /**
     * Résume les tirs d'un joueur, par exemple « 47 tirs, précision 36 % ».
     *
     * @param statistiques le bilan des tirs
     * @return le résumé
     */
    public static String bilan(Statistiques statistiques) {
        return statistiques.tirs() + " tirs, précision " + statistiques.precisionEnPourcentage() + " %";
    }
}
