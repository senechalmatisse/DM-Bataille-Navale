package bataille.donnees;

import bataille.ia.Difficulte;
import bataille.modele.ConfigurationPartie;
import bataille.modele.ModeleFlotte;
import bataille.partie.ModeJeu;
import bataille.partie.ParametresPartie;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * Les préférences du joueur : les animations, et les derniers réglages de partie, proposés à la partie suivante.
 * Une préférence absente ou illisible reprend sa valeur par défaut.
 */
public final class PreferencesJeu {
    /** Le fichier des préférences. */
    private final Path fichier;
    /** Les valeurs, par nom. */
    private final Properties valeurs;

    private PreferencesJeu(Path fichier, Properties valeurs) {
        this.fichier = fichier;
        this.valeurs = valeurs;
    }

    /**
     * Charge les préférences ; un fichier absent ou illisible donne les préférences par défaut.
     *
     * @param fichier le fichier des préférences
     * @return les préférences
     */
    public static PreferencesJeu charger(Path fichier) {
        Properties valeurs = new Properties();
        if (Files.exists(fichier)) {
            try (Reader lecteur = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
                valeurs.load(lecteur);
            } catch (IOException e) {
                valeurs.clear();
            }
        }
        return new PreferencesJeu(Objects.requireNonNull(fichier, "fichier"), valeurs);
    }

    /**
     * Enregistre les préférences ; crée le dossier si besoin.
     *
     * @throws IOException si l'écriture échoue
     */
    public void sauvegarder() throws IOException {
        Path dossier = fichier.toAbsolutePath().getParent();
        if (dossier != null) {
            Files.createDirectories(dossier);
        }
        try (Writer redacteur = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
            valeurs.store(redacteur, "Bataille navale : préférences");
        }
    }

    /**
     * Indique si les animations sont activées.
     *
     * @return vrai par défaut
     */
    public boolean animations() {
        return Boolean.parseBoolean(valeurs.getProperty("animations", "true"));
    }

    /**
     * Active ou désactive les animations.
     *
     * @param actives si les animations sont jouées
     */
    public void setAnimations(boolean actives) {
        valeurs.setProperty("animations", Boolean.toString(actives));
    }

    /**
     * Retourne les derniers réglages de partie.
     *
     * @return les réglages enregistrés, ou les réglages par défaut s'ils sont absents ou illisibles
     */
    public ParametresPartie derniersParametres() {
        try {
            ModeleFlotte modele = ModeleFlotte.valueOf(valeurs.getProperty("flotte", ModeleFlotte.CLASSIQUE.name()));
            int taille = Integer.parseInt(valeurs.getProperty("taille",
                    Integer.toString(ConfigurationPartie.TAILLE_PAR_DEFAUT)));
            ConfigurationPartie configuration = ConfigurationPartie.de(taille, modele)
                    .avecCapacites(Boolean.parseBoolean(valeurs.getProperty("capacites", "false")));
            return new ParametresPartie(ModeJeu.valueOf(valeurs.getProperty("mode", ModeJeu.CONTRE_ORDINATEUR.name())),
                    valeurs.getProperty("joueur1"), valeurs.getProperty("joueur2"),
                    Difficulte.valueOf(valeurs.getProperty("difficulte", Difficulte.NORMALE.name())), configuration,
                    Boolean.parseBoolean(valeurs.getProperty("placementManuel", "false")));
        } catch (IllegalArgumentException e) {
            return ParametresPartie.parDefaut();
        }
    }

    /**
     * Retient les réglages d'une partie, pour les proposer à la suivante.
     *
     * @param parametres les réglages
     */
    public void setDerniersParametres(ParametresPartie parametres) {
        ConfigurationPartie configuration = parametres.configuration();
        valeurs.setProperty("mode", parametres.mode().name());
        valeurs.setProperty("joueur1", parametres.nomJoueur1());
        valeurs.setProperty("joueur2", parametres.nomJoueur2());
        valeurs.setProperty("difficulte", parametres.difficulte().name());
        valeurs.setProperty("taille", Integer.toString(configuration.taille()));
        for (ModeleFlotte modele : ModeleFlotte.values()) {
            if (modele.getTypes().equals(configuration.flotte())) {
                valeurs.setProperty("flotte", modele.name());
            }
        }
        valeurs.setProperty("capacites", Boolean.toString(configuration.capacites()));
        valeurs.setProperty("placementManuel", Boolean.toString(parametres.placementManuel()));
    }
}
