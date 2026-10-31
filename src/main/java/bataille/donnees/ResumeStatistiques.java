package bataille.donnees;

import bataille.ia.Difficulte;
import bataille.modele.TypeBateau;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/** Met en mots les statistiques du joueur, pour la console comme pour la fenêtre. */
public final class ResumeStatistiques {

    private ResumeStatistiques() {
    }

    /**
     * Résume les statistiques en une phrase, par exemple
     * « 5 parties, 3 victoires (60 %), série en cours : 2, meilleure série : 3 ».
     *
     * @param statistiques les statistiques
     * @return le résumé
     */
    public static String enUneLigne(StatistiquesCumulees statistiques) {
        return nombre(statistiques.getParties(), "partie", "parties") + ", "
                + nombre(statistiques.getVictoires(), "victoire", "victoires") + " ("
                + statistiques.getPourcentageVictoires() + " %), série en cours : " + statistiques.getSerieActuelle()
                + ", meilleure série : " + statistiques.getMeilleureSerie();
    }

    /**
     * Détaille les statistiques, une ligne par donnée connue.
     *
     * @param statistiques les statistiques
     * @return les lignes, vides si aucune partie n'a été jouée
     */
    public static List<Ligne> lignes(StatistiquesCumulees statistiques) {
        List<Ligne> lignes = new ArrayList<>();
        int parties = statistiques.getParties();
        if (parties > 0) {
            lignes.add(new Ligne("Parties contre l'ordinateur", Integer.toString(parties)));
            lignes.add(new Ligne("Victoires",
                    statistiques.getVictoires() + " (" + statistiques.getPourcentageVictoires() + " %)"));
            lignes.add(new Ligne("Série en cours", nombre(statistiques.getSerieActuelle(), "victoire", "victoires")));
            lignes.add(new Ligne("Meilleure série", nombre(statistiques.getMeilleureSerie(), "victoire", "victoires")));
            lignes.add(new Ligne("Précision", statistiques.getPrecision() + " %"));
            lignes.add(new Ligne("Tirs par partie", decimal(statistiques.getTirsMoyensParPartie())));
            lignes.add(new Ligne("Durée moyenne", duree(statistiques.getDureeMoyenne())));
            lignes.add(new Ligne("Capacités utilisées", Integer.toString(statistiques.getCapacites())));
            for (Difficulte difficulte : Difficulte.values()) {
                int partiesNiveau = statistiques.getParties(difficulte);
                if (partiesNiveau > 0) {
                    lignes.add(new Ligne("Difficulté " + difficulte.getNom().toLowerCase(Locale.ROOT),
                            nombre(statistiques.getVictoires(difficulte), "victoire", "victoires") + " sur "
                                    + nombre(partiesNiveau, "partie", "parties")));
                }
            }
            for (TypeBateau type : TypeBateau.values()) {
                OptionalDouble tirs = statistiques.getTirsMoyensPourCouler(type);
                if (tirs.isPresent()) {
                    lignes.add(new Ligne("Pour couler " + type.getNomAvecArticle(), decimal(tirs.getAsDouble())
                            + " tirs après la première touche"));
                }
            }
        }
        if (statistiques.getPartiesADeux() > 0) {
            lignes.add(new Ligne("Parties à deux", Integer.toString(statistiques.getPartiesADeux())));
        }
        return lignes;
    }

    /**
     * Écrit une durée en minutes et secondes, par exemple « 4 min 12 s ».
     *
     * @param duree la durée
     * @return la durée en mots
     */
    public static String duree(Duration duree) {
        long minutes = duree.toMinutes();
        long secondes = duree.toSecondsPart();
        return minutes == 0 ? secondes + " s" : minutes + " min " + secondes + " s";
    }

    private static String decimal(double valeur) {
        return String.format(Locale.FRANCE, "%.1f", valeur);
    }

    private static String nombre(int valeur, String singulier, String pluriel) {
        return valeur + " " + (valeur > 1 ? pluriel : singulier);
    }

    /**
     * Une ligne du détail des statistiques.
     *
     * @param libelle ce que mesure la ligne, par exemple « Précision »
     * @param valeur la valeur, par exemple « 36 % »
     */
    public record Ligne(String libelle, String valeur) {
    }
}
