package bataille.donnees;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bataille.ia.Difficulte;
import bataille.modele.ConfigurationPartie;
import bataille.modele.Coordonnee;
import bataille.modele.ModeleFlotte;
import bataille.modele.Ordre;
import bataille.modele.PlacementAleatoire;
import bataille.modele.Plateau;
import bataille.partie.EnregistrementPartie;
import bataille.partie.ParametresPartie;
import bataille.partie.Partie;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FichierPartieTest {

    private static Partie partieEnCours(ParametresPartie parametres) {
        Random random = new Random(3);
        Plateau plateau = new Plateau(10);
        PlacementAleatoire.placer(plateau, ModeleFlotte.CLASSIQUE.getTypes(), random);
        Partie partie = parametres.creerPartie(plateau, random);
        partie.jouer(Ordre.radar(Coordonnee.depuis("E5")));
        partie.faireJouerOrdinateur();
        while (partie.getJoueurCourant() != partie.getJoueur1()) {
            partie.faireJouerOrdinateur();
        }
        partie.jouer(Ordre.doubleTir());
        partie.tirer(Coordonnee.depuis("A1"));
        partie.tirer(Coordonnee.depuis("J10"));
        while (partie.getJoueurCourant() != partie.getJoueur1()) {
            partie.faireJouerOrdinateur();
        }
        partie.jouer(Ordre.torpilleSurColonne(3));
        return partie;
    }

    private static void assertMemeEtat(Partie attendue, Partie obtenue) {
        assertEquals(attendue.getHistorique().size(), obtenue.getHistorique().size());
        assertEquals(attendue.getNumeroTour(), obtenue.getNumeroTour());
        assertEquals(attendue.getJoueurCourant() == attendue.getJoueur1(),
                obtenue.getJoueurCourant() == obtenue.getJoueur1());
        int taille = attendue.getConfiguration().taille();
        for (int ligne = 0; ligne < taille; ligne++) {
            for (int colonne = 0; colonne < taille; colonne++) {
                Coordonnee coordonnee = new Coordonnee(ligne, colonne);
                assertEquals(attendue.getJoueur1().getPlateau().getEtatPourProprietaire(coordonnee),
                        obtenue.getJoueur1().getPlateau().getEtatPourProprietaire(coordonnee));
                assertEquals(attendue.getJoueur2().getPlateau().getVueAdverse().getEtat(coordonnee),
                        obtenue.getJoueur2().getPlateau().getVueAdverse().getEtat(coordonnee));
            }
        }
    }

    @Test
    void uneParteEnregistreeSeRelitEtReprendDansLeMemeEtat() throws IOException {
        ParametresPartie parametres = ParametresPartie.contreOrdinateur("Matisse", Difficulte.DIFFICILE,
                ConfigurationPartie.classique().avecCapacites(true), false);
        Partie partie = partieEnCours(parametres);
        Path dossier = Files.createTempDirectory("bataille");
        Path fichier = dossier.resolve("sous-dossier/partie.txt");
        FichierPartie.ecrire(EnregistrementPartie.de(partie, parametres), fichier);
        EnregistrementPartie relu = FichierPartie.lire(fichier);
        assertEquals("Matisse", relu.parametres().nomJoueur1());
        assertEquals(Difficulte.DIFFICILE, relu.parametres().difficulte());
        assertTrue(relu.parametres().configuration().capacites());
        Partie reprise = relu.restaurer(new Random(9));
        assertMemeEtat(partie, reprise);
        assertEquals(1, reprise.getJoueur1().getPlateau().getDetections().size() + reprise.getJoueur2().getPlateau()
                .getDetections().size() - partie.getJoueur1().getPlateau().getDetections().size());
        while (!reprise.estTerminee()) {
            if (reprise.getJoueurCourant().estHumain()) {
                reprise.jouer(Ordre.tir(premiereCaseInconnue(reprise)));
            } else {
                reprise.faireJouerOrdinateur();
            }
        }
    }

    private static Coordonnee premiereCaseInconnue(Partie partie) {
        var vue = partie.getJoueur2().getPlateau().getVueAdverse();
        for (int ligne = 0; ligne < 10; ligne++) {
            for (int colonne = 0; colonne < 10; colonne++) {
                Coordonnee coordonnee = new Coordonnee(ligne, colonne);
                if (!partie.getJoueur2().getPlateau().estVise(coordonnee)) {
                    return coordonnee;
                }
            }
        }
        throw new IllegalStateException("plus de case : " + vue.getTaille());
    }

    @Test
    void unePartieADeuxGardeSesDeuxNoms() throws IOException {
        Random random = new Random(5);
        ParametresPartie parametres = ParametresPartie.aDeux("Alice", "Bob", ConfigurationPartie.classique(), true);
        Plateau premier = new Plateau(10);
        Plateau second = new Plateau(10);
        PlacementAleatoire.placer(premier, ModeleFlotte.CLASSIQUE.getTypes(), random);
        PlacementAleatoire.placer(second, ModeleFlotte.CLASSIQUE.getTypes(), random);
        Partie partie = parametres.creerPartie(premier, second, random);
        partie.tirer(Coordonnee.depuis("C3"));
        Path fichier = Files.createTempDirectory("bataille").resolve("a-deux.txt");
        FichierPartie.ecrire(EnregistrementPartie.de(partie, parametres), fichier);
        Partie reprise = FichierPartie.lire(fichier).restaurer(new Random(1));
        assertEquals("Bob", reprise.getJoueur2().getNom());
        assertTrue(reprise.getJoueur2().estHumain());
        assertSame(reprise.getJoueur2(), reprise.getJoueurCourant());
    }

    @Test
    void unFichierQuiNEstPasUnePartieEstRefuseAvecUnMessageClair() throws IOException {
        Path fichier = Files.createTempFile("bataille", ".txt");
        Files.write(fichier, List.of("bonjour", "format=1"), StandardCharsets.UTF_8);
        IOException erreur = assertThrows(IOException.class, () -> FichierPartie.lire(fichier));
        assertTrue(erreur.getMessage().contains("partie enregistrée valide"), erreur.getMessage());
        Files.write(fichier, List.of("format=1", "mode=CONTRE_ORDINATEUR"), StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> FichierPartie.lire(fichier));
    }
}
