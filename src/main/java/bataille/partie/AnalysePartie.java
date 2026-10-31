package bataille.partie;

import bataille.modele.Bateau;
import bataille.modele.Plateau;
import bataille.modele.ResultatTir;
import bataille.modele.Tir;
import bataille.modele.TypeBateau;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * L'analyse d'une partie : pour chaque bateau adverse, au combientième tir il a été trouvé et en combien de tirs
 * il a ensuite été coulé.
 */
public final class AnalysePartie {

    private AnalysePartie() {
    }

    /**
     * Retrace la chasse de chaque bateau adverse par un joueur.
     *
     * @param partie la partie
     * @param tireur le joueur dont on analyse les tirs
     * @return une traque par bateau adverse, dans l'ordre de placement
     */
    public static List<Traque> traques(Partie partie, Joueur tireur) {
        Plateau plateauVise = partie.adversaireDe(tireur).getPlateau();
        Map<Bateau, Integer> premiereTouche = new IdentityHashMap<>();
        Map<Bateau, Integer> tirCoulant = new IdentityHashMap<>();
        int numero = 0;
        for (Coup coup : partie.getHistorique()) {
            if (coup.auteur() != tireur) {
                continue;
            }
            for (Tir tir : coup.tirs()) {
                numero++;
                if (!tir.resultat().estAuBut()) {
                    continue;
                }
                Bateau bateau = plateauVise.getBateau(tir.cible()).orElseThrow();
                premiereTouche.putIfAbsent(bateau, numero);
                if (tir.resultat() == ResultatTir.COULE) {
                    tirCoulant.put(bateau, numero);
                }
            }
        }
        List<Traque> traques = new ArrayList<>();
        for (Bateau bateau : plateauVise.getBateaux()) {
            int trouve = premiereTouche.getOrDefault(bateau, 0);
            int coule = tirCoulant.getOrDefault(bateau, 0);
            traques.add(new Traque(bateau.getType(), trouve, coule == 0 ? 0 : coule - trouve + 1));
        }
        return traques;
    }

    /**
     * Raconte la chasse de chaque bateau coulé, par exemple
     * « L'ordinateur a trouvé votre porte-avions au 12e tir et l'a coulé en 4 tirs. ».
     *
     * @param partie la partie
     * @param tireur le joueur dont on raconte les tirs
     * @param lecteur le joueur à qui les phrases s'adressent
     * @return une phrase par bateau coulé
     */
    public static List<String> raconter(Partie partie, Joueur tireur, Joueur lecteur) {
        return raconter(partie, tireur, lecteur == tireur ? Point.VOUS : Point.ADVERSAIRE);
    }

    /**
     * Raconte la chasse de chaque bateau coulé, pour un lecteur qui n'est aucun des deux joueurs, par exemple
     * « Alice a trouvé le porte-avions au 12e tir et l'a coulé en 4 tirs. ».
     *
     * @param partie la partie
     * @param tireur le joueur dont on raconte les tirs
     * @return une phrase par bateau coulé
     */
    public static List<String> raconter(Partie partie, Joueur tireur) {
        return raconter(partie, tireur, Point.NEUTRE);
    }

    private static List<String> raconter(Partie partie, Joueur tireur, Point point) {
        List<String> phrases = new ArrayList<>();
        for (Traque traque : traques(partie, tireur)) {
            if (!traque.estCoule()) {
                continue;
            }
            String trouve = ordinal(traque.trouveAuTir()) + " tir";
            String tirs = traque.tirsPourCouler() + (traque.tirsPourCouler() > 1 ? " tirs" : " tir");
            String phrase = switch (point) {
                case VOUS -> "Vous avez trouvé " + traque.type().getNomAvecArticle() + " au " + trouve
                        + " et l'avez coulé en " + tirs + ".";
                case ADVERSAIRE -> Annonces.sujet(tireur) + " a trouvé votre "
                        + traque.type().getNom().toLowerCase(Locale.ROOT) + " au " + trouve + " et l'a coulé en "
                        + tirs + ".";
                case NEUTRE -> Annonces.sujet(tireur) + " a trouvé " + traque.type().getNomAvecArticle() + " au "
                        + trouve + " et l'a coulé en " + tirs + ".";
            };
            phrases.add(phrase);
        }
        return phrases;
    }

    /** Le point de vue du lecteur. */
    private enum Point {
        /** Le lecteur est le tireur. */
        VOUS,
        /** Le lecteur est l'adversaire du tireur. */
        ADVERSAIRE,
        /** Le lecteur n'est aucun des deux joueurs. */
        NEUTRE
    }

    private static String ordinal(int numero) {
        return numero == 1 ? "1er" : numero + "e";
    }

    /**
     * La chasse d'un bateau.
     *
     * @param type le type du bateau
     * @param trouveAuTir le numéro du tir du chasseur qui l'a touché la première fois, 0 s'il n'a jamais été touché
     * @param tirsPourCouler le nombre de tirs entre cette première touche et le tir qui l'a coulé, inclus ;
     *                       0 s'il n'a pas été coulé
     */
    public record Traque(TypeBateau type, int trouveAuTir, int tirsPourCouler) {

        /**
         * Indique si le bateau a été coulé.
         *
         * @return vrai si le bateau a été coulé
         */
        public boolean estCoule() {
            return tirsPourCouler > 0;
        }
    }
}
