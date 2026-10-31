package bataille.donnees;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Les fichiers du jeu, rangés dans un même dossier : par défaut {@code .bataille-navale} dans le dossier personnel.
 * La propriété système {@code bataille.dossier} permet d'en choisir un autre ; les tests passent un dossier
 * temporaire au constructeur.
 *
 * @param racine le dossier où sont rangés statistiques, préférences et parties
 */
public record Dossiers(Path racine) {

    /**
     * Vérifie le dossier.
     *
     * @param racine le dossier du jeu
     */
    public Dossiers {
        Objects.requireNonNull(racine, "racine");
    }

    /**
     * Retourne le dossier du jeu choisi par la propriété {@code bataille.dossier}, sinon celui du dossier personnel.
     *
     * @return les fichiers du jeu
     */
    public static Dossiers parDefaut() {
        String choisi = System.getProperty("bataille.dossier");
        return new Dossiers(choisi != null ? Path.of(choisi) : Path.of(System.getProperty("user.home"),
                ".bataille-navale"));
    }

    /**
     * Retourne le fichier des statistiques.
     *
     * @return le chemin du fichier
     */
    public Path statistiques() {
        return racine.resolve("statistiques.properties");
    }

    /**
     * Retourne le fichier des préférences.
     *
     * @return le chemin du fichier
     */
    public Path preferences() {
        return racine.resolve("preferences.properties");
    }

    /**
     * Retourne le fichier de la dernière partie jouée, enregistrée après chaque coup : on peut la reprendre
     * si elle a été interrompue, ou la revoir coup par coup.
     *
     * @return le chemin du fichier
     */
    public Path dernierePartie() {
        return racine.resolve("derniere-partie.txt");
    }

    /**
     * Retourne le dossier proposé pour enregistrer les parties sous un autre nom.
     *
     * @return le chemin du dossier
     */
    public Path parties() {
        return racine.resolve("parties");
    }
}
