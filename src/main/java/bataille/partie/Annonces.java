package bataille.partie;

import bataille.modele.Detection;
import bataille.modele.Ordre;
import bataille.modele.ResultatTir;
import bataille.modele.Tir;
import bataille.modele.TypeBateau;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Les phrases qui racontent la partie, communes à la console et à la fenêtre.
 * <p>
 * Chaque phrase s'adresse à un lecteur : ses propres coups commencent par « Vous », ceux de l'adversaire par son
 * nom, ou par « L'ordinateur ».
 */
public final class Annonces {

    private Annonces() {
    }

    /**
     * Raconte un coup, par exemple « Vous tirez en B7 : touché ! » ou
     * « L'ordinateur bombarde la zone C3 : 1 touché, 3 manqués. ».
     *
     * @param coup le coup joué
     * @param lecteur le joueur à qui la phrase s'adresse
     * @return la phrase à afficher
     */
    public static String decrire(Coup coup, Joueur lecteur) {
        Joueur auteur = coup.auteur();
        boolean vous = auteur == lecteur;
        String sujet = vous ? "Vous" : sujet(auteur);
        Ordre ordre = coup.ordre();
        if (ordre.estUnTirSimple()) {
            Tir tir = coup.tirs().get(0);
            if (tir.resultat() == ResultatTir.DEJA_JOUE) {
                return sujet + (vous ? " aviez" : " avait") + " déjà tiré en " + tir.cible() + ".";
            }
            return sujet + verbe(vous, " tirez", " tire") + " en " + tir.cible() + " : " + resultat(tir)
                    + bateauxCoules(coup, vous);
        }
        return switch (ordre.capacite()) {
            case RADAR -> sujet + verbe(vous, " passez", " passe") + " le radar en " + ordre.cible() + " : "
                    + detection(coup.detection().orElseThrow()) + ".";
            case BOMBARDEMENT -> sujet + verbe(vous, " bombardez", " bombarde") + " la zone " + ordre.cible() + " : "
                    + bilanDesTirs(coup.tirs()) + "." + bateauxCoules(coup, vous);
            case TORPILLE -> sujet + verbe(vous, " lancez", " lance") + " une " + ordre.decrire() + " : "
                    + impact(coup.tirs()) + bateauxCoules(coup, vous);
            case DOUBLE_TIR -> sujet + verbe(vous, " activez le double tir : tirez deux fois ce tour-ci.",
                    " active le double tir : il tire deux fois ce tour-ci.");
        };
    }

    /**
     * Résume les tirs d'un joueur, par exemple « 47 tirs, précision 36 % ».
     *
     * @param statistiques le bilan des tirs
     * @return le résumé
     */
    public static String bilan(Statistiques statistiques) {
        String resume = statistiques.tirs() + " tirs, précision " + statistiques.precisionEnPourcentage() + " %";
        if (statistiques.capacites() > 0) {
            resume += ", " + statistiques.capacites() + (statistiques.capacites() > 1 ? " capacités" : " capacité");
        }
        return resume;
    }

    /**
     * Retourne la façon de nommer un joueur en début de phrase : son nom, ou « L'ordinateur ».
     *
     * @param joueur le joueur
     * @return le sujet de la phrase
     */
    public static String sujet(Joueur joueur) {
        return joueur.estHumain() ? joueur.getNom() : "L'ordinateur";
    }

    /**
     * Retourne « de » suivi d'un nom, élidé devant une voyelle : « de Bob », mais « d'Alice ».
     *
     * @param nom le nom
     * @return le complément
     */
    public static String de(String nom) {
        String sansAccent = Normalizer.normalize(nom, Normalizer.Form.NFD);
        boolean voyelle = !sansAccent.isEmpty() && "aeiouy".indexOf(Character.toLowerCase(sansAccent.charAt(0))) >= 0;
        return (voyelle ? "d'" : "de ") + nom;
    }

    private static String verbe(boolean vous, String formeVous, String formeIl) {
        return vous ? formeVous : formeIl;
    }

    private static String resultat(Tir tir) {
        return switch (tir.resultat()) {
            case MANQUE -> "manqué.";
            case TOUCHE -> "touché !";
            case COULE -> "coulé !";
            case DEJA_JOUE -> "déjà joué.";
        };
    }

    private static String bateauxCoules(Coup coup, boolean vous) {
        StringBuilder phrase = new StringBuilder();
        for (Tir tir : coup.tirs()) {
            if (tir.bateauCoule().isEmpty()) {
                continue;
            }
            TypeBateau type = tir.bateauCoule().get();
            if (vous) {
                phrase.append(" Vous avez coulé ").append(type.getNomAvecArticle()).append('.');
            } else {
                String auteur = coup.auteur().estHumain() ? coup.auteur().getNom() : "Il";
                phrase.append(' ').append(auteur).append(" a coulé votre ")
                        .append(type.getNom().toLowerCase(Locale.ROOT)).append('.');
            }
        }
        return phrase.toString();
    }

    private static String detection(Detection detection) {
        return switch (detection.nombre()) {
            case 0 -> "aucun bateau dans la zone";
            case 1 -> "1 case de bateau détectée";
            default -> detection.nombre() + " cases de bateaux détectées";
        };
    }

    private static String bilanDesTirs(List<Tir> tirs) {
        long auBut = tirs.stream().filter(tir -> tir.resultat().estAuBut()).count();
        long manques = tirs.size() - auBut;
        return auBut + (auBut > 1 ? " touchés, " : " touché, ") + manques + (manques > 1 ? " manqués" : " manqué");
    }

    private static String impact(List<Tir> tirs) {
        Tir dernier = tirs.get(tirs.size() - 1);
        if (!dernier.resultat().estAuBut()) {
            return "elle se perd sans rien toucher.";
        }
        return (dernier.resultat() == ResultatTir.COULE ? "coulé en " : "touché en ") + dernier.cible() + " !";
    }
}
