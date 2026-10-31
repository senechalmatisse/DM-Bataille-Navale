package bataille.donnees;

import bataille.ia.Difficulte;
import bataille.modele.TypeBateau;
import bataille.partie.AnalysePartie;
import bataille.partie.Joueur;
import bataille.partie.Partie;
import bataille.partie.Statistiques;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Properties;

/**
 * Les statistiques du joueur, d'une partie à l'autre, enregistrées dans un fichier de propriétés.
 * <p>
 * Elles portent sur les parties contre l'ordinateur, vues du joueur humain : victoires, séries, précision, durée,
 * capacités utilisées, résultats par niveau et nombre de tirs pour couler chaque type de bateau une fois trouvé.
 * Les parties à deux sont seulement comptées.
 */
public final class StatistiquesCumulees {
    /** Le fichier où les statistiques sont enregistrées. */
    private final Path fichier;
    /** Les compteurs, par nom. */
    private final Properties compteurs;

    private StatistiquesCumulees(Path fichier, Properties compteurs) {
        this.fichier = fichier;
        this.compteurs = compteurs;
    }

    /**
     * Charge les statistiques ; un fichier absent donne des statistiques vides.
     *
     * @param fichier le fichier des statistiques
     * @return les statistiques
     * @throws IOException si le fichier existe mais ne peut pas être lu
     */
    public static StatistiquesCumulees charger(Path fichier) throws IOException {
        Properties compteurs = new Properties();
        if (Files.exists(fichier)) {
            try (Reader lecteur = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
                compteurs.load(lecteur);
            }
        }
        return new StatistiquesCumulees(Objects.requireNonNull(fichier, "fichier"), compteurs);
    }

    /**
     * Enregistre les statistiques dans leur fichier ; crée le dossier si besoin.
     *
     * @throws IOException si l'écriture échoue
     */
    public void sauvegarder() throws IOException {
        Path dossier = fichier.toAbsolutePath().getParent();
        if (dossier != null) {
            Files.createDirectories(dossier);
        }
        try (Writer redacteur = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
            compteurs.store(redacteur, "Bataille navale : statistiques");
        }
    }

    /**
     * Ajoute une partie terminée contre l'ordinateur ; le joueur humain est le joueur 1.
     *
     * @param partie la partie terminée
     * @param difficulte le niveau de l'ordinateur
     * @param duree la durée de la partie
     */
    public void ajouterPartieContreOrdinateur(Partie partie, Difficulte difficulte, Duration duree) {
        if (!partie.estTerminee()) {
            throw new IllegalArgumentException("Seule une partie terminée compte dans les statistiques.");
        }
        Joueur humain = partie.getJoueur1();
        boolean victoire = partie.getGagnant().orElseThrow() == humain;
        ajouter("parties", 1);
        ajouter("parties." + difficulte.name(), 1);
        if (victoire) {
            ajouter("victoires", 1);
            ajouter("victoires." + difficulte.name(), 1);
            int serie = lire("serie.actuelle") + 1;
            ecrire("serie.actuelle", serie);
            ecrire("serie.meilleure", Math.max(serie, lire("serie.meilleure")));
        } else {
            ecrire("serie.actuelle", 0);
        }
        Statistiques statistiques = partie.getStatistiques(humain);
        ajouter("tirs", statistiques.tirs());
        ajouter("tirs.au.but", statistiques.tirsAuBut());
        ajouter("capacites", statistiques.capacites());
        ajouter("duree.secondes", (int) Math.min(Integer.MAX_VALUE, duree.toSeconds()));
        for (AnalysePartie.Traque traque : AnalysePartie.traques(partie, humain)) {
            if (traque.estCoule()) {
                ajouter("coules." + traque.type().name(), 1);
                ajouter("tirs.pour.couler." + traque.type().name(), traque.tirsPourCouler());
            }
        }
    }

    /** Compte une partie à deux. */
    public void ajouterPartieADeux() {
        ajouter("parties.a.deux", 1);
    }

    /** Efface toutes les statistiques ; appeler ensuite {@link #sauvegarder()}. */
    public void reinitialiser() {
        compteurs.clear();
    }

    /**
     * Retourne le nombre de parties terminées contre l'ordinateur.
     *
     * @return le nombre de parties
     */
    public int getParties() {
        return lire("parties");
    }

    /**
     * Retourne le nombre de victoires contre l'ordinateur.
     *
     * @return le nombre de victoires
     */
    public int getVictoires() {
        return lire("victoires");
    }

    /**
     * Retourne la part des victoires.
     *
     * @return le pourcentage arrondi, 0 sans partie
     */
    public int getPourcentageVictoires() {
        return pourcentage(getVictoires(), getParties());
    }

    /**
     * Retourne le nombre de victoires d'affilée en cours.
     *
     * @return la série en cours
     */
    public int getSerieActuelle() {
        return lire("serie.actuelle");
    }

    /**
     * Retourne la plus longue série de victoires.
     *
     * @return la meilleure série
     */
    public int getMeilleureSerie() {
        return lire("serie.meilleure");
    }

    /**
     * Retourne la précision sur toutes les parties.
     *
     * @return la part des tirs au but, en pourcentage arrondi
     */
    public int getPrecision() {
        return pourcentage(lire("tirs.au.but"), lire("tirs"));
    }

    /**
     * Retourne le nombre moyen de tirs par partie.
     *
     * @return la moyenne, 0 sans partie
     */
    public double getTirsMoyensParPartie() {
        return getParties() == 0 ? 0 : (double) lire("tirs") / getParties();
    }

    /**
     * Retourne la durée moyenne d'une partie.
     *
     * @return la durée moyenne, nulle sans partie
     */
    public Duration getDureeMoyenne() {
        return getParties() == 0 ? Duration.ZERO : Duration.ofSeconds(lire("duree.secondes") / getParties());
    }

    /**
     * Retourne le nombre de capacités spéciales utilisées.
     *
     * @return le total sur toutes les parties
     */
    public int getCapacites() {
        return lire("capacites");
    }

    /**
     * Retourne le nombre de parties jouées contre un niveau.
     *
     * @param difficulte le niveau
     * @return le nombre de parties
     */
    public int getParties(Difficulte difficulte) {
        return lire("parties." + difficulte.name());
    }

    /**
     * Retourne le nombre de victoires contre un niveau.
     *
     * @param difficulte le niveau
     * @return le nombre de victoires
     */
    public int getVictoires(Difficulte difficulte) {
        return lire("victoires." + difficulte.name());
    }

    /**
     * Retourne le nombre moyen de tirs pour couler un type de bateau, de la première touche au tir qui le coule.
     *
     * @param type le type de bateau
     * @return la moyenne, ou une valeur vide si ce type n'a jamais été coulé
     */
    public OptionalDouble getTirsMoyensPourCouler(TypeBateau type) {
        int coules = lire("coules." + type.name());
        return coules == 0 ? OptionalDouble.empty()
                : OptionalDouble.of((double) lire("tirs.pour.couler." + type.name()) / coules);
    }

    /**
     * Retourne le nombre de parties jouées à deux.
     *
     * @return le nombre de parties à deux
     */
    public int getPartiesADeux() {
        return lire("parties.a.deux");
    }

    private static int pourcentage(int partie, int total) {
        return total == 0 ? 0 : Math.round(100f * partie / total);
    }

    private int lire(String cle) {
        try {
            return Integer.parseInt(compteurs.getProperty(cle, "0").strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void ecrire(String cle, int valeur) {
        compteurs.setProperty(cle, Integer.toString(valeur));
    }

    private void ajouter(String cle, int valeur) {
        ecrire(cle, lire(cle) + valeur);
    }
}
