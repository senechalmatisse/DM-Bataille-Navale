package bataille.partie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bataille.ia.Difficulte;
import bataille.modele.ConfigurationPartie;
import bataille.modele.Coordonnee;
import bataille.modele.ModeleFlotte;
import bataille.modele.Orientation;
import bataille.modele.PlacementAleatoire;
import bataille.modele.Plateau;
import bataille.modele.ResultatTir;
import bataille.modele.TypeBateau;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PartieTest {

    /** Une grille 5×5 avec un seul torpilleur par joueur : deux tirs au but suffisent pour gagner. */
    private static final ConfigurationPartie MINI = new ConfigurationPartie(5, List.of(TypeBateau.TORPILLEUR));

    private Joueur alice;
    private Joueur bob;
    private Partie partie;

    private static Coordonnee c(String notation) {
        return Coordonnee.depuis(notation);
    }

    @BeforeEach
    void creerPartie() {
        Plateau plateauAlice = new Plateau(5);
        plateauAlice.placer(TypeBateau.TORPILLEUR, c("A1"), Orientation.HORIZONTALE);
        Plateau plateauBob = new Plateau(5);
        plateauBob.placer(TypeBateau.TORPILLEUR, c("A1"), Orientation.VERTICALE);
        alice = Joueur.humain("Alice", plateauAlice);
        bob = Joueur.humain("Bob", plateauBob);
        partie = new Partie(MINI, alice, bob);
    }

    @Test
    void lesJoueursTirentATourDeRole() {
        assertSame(alice, partie.getJoueurCourant());
        assertEquals(ResultatTir.MANQUE, partie.tirer(c("E5")).resultat());
        assertSame(bob, partie.getJoueurCourant());
        assertEquals(ResultatTir.TOUCHE, partie.tirer(c("A1")).resultat());
        assertSame(alice, partie.getJoueurCourant());
    }

    @Test
    void unTirSurUneCaseDejaViseeLaisseLaMainAuMemeJoueur() {
        partie.tirer(c("C3"));
        partie.tirer(c("C3"));
        assertEquals(ResultatTir.DEJA_JOUE, partie.tirer(c("C3")).resultat());
        assertSame(alice, partie.getJoueurCourant());
        assertEquals(2, partie.getHistorique().size());
    }

    @Test
    void unTirHorsDeLaGrilleEstRefuseSansChangerDeJoueur() {
        assertThrows(IllegalArgumentException.class, () -> partie.tirer(c("F1")));
        assertSame(alice, partie.getJoueurCourant());
        assertTrue(partie.getHistorique().isEmpty());
    }

    @Test
    @DisplayName("La victoire est détectée au tir qui coule le dernier bateau, sans tour supplémentaire")
    void laVictoireEstImmediate() {
        partie.tirer(c("A1"));
        partie.tirer(c("E5"));
        assertFalse(partie.estTerminee());
        assertEquals(ResultatTir.COULE, partie.tirer(c("A2")).resultat());
        assertTrue(partie.estTerminee());
        assertEquals(Optional.of(alice), partie.getGagnant());
        assertSame(alice, partie.getJoueurCourant());
        assertThrows(IllegalStateException.class, () -> partie.tirer(c("B1")));
        assertEquals(3, partie.getHistorique().size());
    }

    @Test
    void compteLesTirsEtLaPrecisionDeChaqueJoueur() {
        partie.tirer(c("A1"));
        partie.tirer(c("E5"));
        partie.tirer(c("D4"));
        partie.tirer(c("B1"));
        assertEquals(new Statistiques(2, 1), partie.getStatistiques(alice));
        assertEquals(new Statistiques(2, 1), partie.getStatistiques(bob));
        assertEquals(50, partie.getStatistiques(alice).precisionEnPourcentage());
        assertEquals(0, new Statistiques(0, 0).precisionEnPourcentage());
    }

    @Test
    void refuseUnPlateauSansSaFlotte() {
        Joueur sansFlotte = Joueur.humain("Chloé", new Plateau(5));
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> new Partie(MINI, alice, sansFlotte));
        assertTrue(erreur.getMessage().contains("Chloé"), erreur.getMessage());
    }

    @Test
    void refuseUnPlateauDeLaMauvaiseTaille() {
        Plateau grand = new Plateau(6);
        grand.placer(TypeBateau.TORPILLEUR, c("A1"), Orientation.HORIZONTALE);
        assertThrows(IllegalArgumentException.class, () -> new Partie(MINI, alice, Joueur.humain("David", grand)));
    }

    @Test
    void refuseDeFaireJouerUnHumainCommeUnOrdinateur() {
        assertThrows(IllegalStateException.class, () -> partie.faireJouerOrdinateur());
    }

    @Test
    void deuxOrdinateursFinissentToujoursLaPartie() {
        ConfigurationPartie configuration = ConfigurationPartie.classique();
        for (int graine = 0; graine < 30; graine++) {
            Random random = new Random(graine);
            Plateau premier = new Plateau(10);
            Plateau second = new Plateau(10);
            PlacementAleatoire.placer(premier, ModeleFlotte.CLASSIQUE.getTypes(), random);
            PlacementAleatoire.placer(second, ModeleFlotte.CLASSIQUE.getTypes(), random);
            Joueur facile = Joueur.ordinateur("Facile", premier, Difficulte.FACILE.creerStrategie(random));
            Joueur difficile = Joueur.ordinateur("Difficile", second, Difficulte.DIFFICILE.creerStrategie(random));
            Partie duel = new Partie(configuration, facile, difficile);
            while (!duel.estTerminee()) {
                duel.faireJouerOrdinateur();
            }
            Joueur gagnant = duel.getGagnant().orElseThrow();
            assertTrue(duel.adversaireDe(gagnant).getPlateau().flotteCoulee());
            assertFalse(gagnant.getPlateau().flotteCoulee());
            assertTrue(duel.getHistorique().size() <= 199);
        }
    }

    @Test
    void coupeUnNomTropLongEtRefuseUnNomVide() {
        Plateau plateau = new Plateau(5);
        assertEquals(Joueur.LONGUEUR_NOM_MAX, Joueur.humain("Un nom beaucoup trop long pour l'affichage", plateau)
                .getNom().length());
        assertThrows(IllegalArgumentException.class, () -> Joueur.humain("   ", plateau));
        assertEquals("Alice", Joueur.humain("  Alice ", plateau).getNom());
    }
}
