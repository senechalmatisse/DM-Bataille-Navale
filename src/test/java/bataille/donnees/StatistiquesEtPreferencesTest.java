package bataille.donnees;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bataille.ia.Difficulte;
import bataille.modele.ConfigurationPartie;
import bataille.modele.Coordonnee;
import bataille.modele.ModeleFlotte;
import bataille.modele.Orientation;
import bataille.modele.Plateau;
import bataille.modele.TypeBateau;
import bataille.partie.ModeJeu;
import bataille.partie.ParametresPartie;
import bataille.partie.Partie;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class StatistiquesEtPreferencesTest {

    private static final ConfigurationPartie MINI = new ConfigurationPartie(5, List.of(TypeBateau.TORPILLEUR));

    /**
     * Joue une partie 5×5 à un torpilleur : le joueur humain gagne s'il vise juste, sinon il ne vise que l'eau.
     * Pour qu'il perde à coup sûr avant de manquer de cases, l'ordinateur doit alors chasser vraiment (difficile).
     */
    private static Partie partieTerminee(boolean victoire, Difficulte difficulte) {
        Plateau joueur = new Plateau(5);
        joueur.placer(TypeBateau.TORPILLEUR, Coordonnee.depuis("A1"), Orientation.HORIZONTALE);
        ParametresPartie parametres = ParametresPartie.contreOrdinateur("Matisse", difficulte, MINI, false);
        Partie partie = parametres.creerPartie(joueur, new Random(1));
        Plateau ordinateur = partie.getJoueur2().getPlateau();
        List<Coordonnee> cibles = ordinateur.getBateaux().get(0).getCases();
        int indice = 0;
        while (!partie.estTerminee()) {
            if (partie.getJoueurCourant().estHumain()) {
                if (victoire) {
                    partie.tirer(cibles.get(indice++));
                } else {
                    partie.tirer(caseVide(ordinateur));
                }
            } else {
                partie.faireJouerOrdinateur();
            }
        }
        return partie;
    }

    private static Coordonnee caseVide(Plateau plateau) {
        for (int ligne = 0; ligne < 5; ligne++) {
            for (int colonne = 0; colonne < 5; colonne++) {
                Coordonnee coordonnee = new Coordonnee(ligne, colonne);
                if (!plateau.estVise(coordonnee) && plateau.getBateau(coordonnee).isEmpty()) {
                    return coordonnee;
                }
            }
        }
        throw new IllegalStateException("plus de case vide");
    }

    @Test
    void comptentVictoiresSeriesEtPrecision() throws IOException {
        Path fichier = Files.createTempDirectory("bataille").resolve("statistiques.properties");
        StatistiquesCumulees statistiques = StatistiquesCumulees.charger(fichier);
        assertEquals(0, statistiques.getParties());
        statistiques.ajouterPartieContreOrdinateur(partieTerminee(true, Difficulte.FACILE), Difficulte.FACILE,
                Duration.ofSeconds(60));
        statistiques.ajouterPartieContreOrdinateur(partieTerminee(true, Difficulte.FACILE), Difficulte.FACILE,
                Duration.ofSeconds(120));
        statistiques.ajouterPartieContreOrdinateur(partieTerminee(false, Difficulte.DIFFICILE), Difficulte.DIFFICILE,
                Duration.ofSeconds(30));
        statistiques.ajouterPartieADeux();
        statistiques.sauvegarder();

        StatistiquesCumulees relues = StatistiquesCumulees.charger(fichier);
        assertEquals(3, relues.getParties());
        assertEquals(2, relues.getVictoires());
        assertEquals(67, relues.getPourcentageVictoires());
        assertEquals(0, relues.getSerieActuelle());
        assertEquals(2, relues.getMeilleureSerie());
        assertEquals(Duration.ofSeconds(70), relues.getDureeMoyenne());
        assertEquals(2, relues.getParties(Difficulte.FACILE));
        assertEquals(2, relues.getVictoires(Difficulte.FACILE));
        assertEquals(1, relues.getParties(Difficulte.DIFFICILE));
        assertEquals(0, relues.getVictoires(Difficulte.DIFFICILE));
        assertEquals(0, relues.getParties(Difficulte.NORMALE));
        assertEquals(2.0, relues.getTirsMoyensPourCouler(TypeBateau.TORPILLEUR).orElseThrow());
        assertFalse(relues.getTirsMoyensPourCouler(TypeBateau.PORTE_AVIONS).isPresent());
        assertEquals(1, relues.getPartiesADeux());
        relues.reinitialiser();
        assertEquals(0, relues.getParties());
    }

    @Test
    void lesPreferencesSeRelisentEtResistentAUnFichierAbime() throws IOException {
        Path fichier = Files.createTempDirectory("bataille").resolve("preferences.properties");
        PreferencesJeu preferences = PreferencesJeu.charger(fichier);
        assertTrue(preferences.animations());
        assertEquals(ParametresPartie.parDefaut(), preferences.derniersParametres());

        ParametresPartie choisis = ParametresPartie.aDeux("Alice", "Bob",
                ConfigurationPartie.de(8, ModeleFlotte.RAPIDE).avecCapacites(true), true);
        preferences.setDerniersParametres(choisis);
        preferences.setAnimations(false);
        preferences.sauvegarder();
        PreferencesJeu relues = PreferencesJeu.charger(fichier);
        assertFalse(relues.animations());
        assertEquals(choisis, relues.derniersParametres());
        assertEquals(ModeJeu.DEUX_JOUEURS, relues.derniersParametres().mode());

        Files.write(fichier, List.of("taille=quarante", "flotte=GEANTE"), StandardCharsets.UTF_8);
        assertEquals(ParametresPartie.parDefaut(), PreferencesJeu.charger(fichier).derniersParametres());
    }
}
