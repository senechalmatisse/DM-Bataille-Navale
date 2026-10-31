package bataille.partie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bataille.ia.Difficulte;
import bataille.modele.Capacite;
import bataille.modele.ConfigurationPartie;
import bataille.modele.Coordonnee;
import bataille.modele.Detection;
import bataille.modele.EtatCase;
import bataille.modele.ModeleFlotte;
import bataille.modele.Ordre;
import bataille.modele.Orientation;
import bataille.modele.PlacementAleatoire;
import bataille.modele.Plateau;
import bataille.modele.ResultatTir;
import bataille.modele.Tir;
import bataille.modele.TypeBateau;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CapacitesTest {

    /** Grille 5×5, un torpilleur et un sous-marin par joueur, capacités activées. */
    private static final ConfigurationPartie REGLES =
            new ConfigurationPartie(5, List.of(TypeBateau.TORPILLEUR, TypeBateau.SOUS_MARIN), true);

    private Joueur alice;
    private Joueur bob;
    private Partie partie;
    private final List<EvenementPartie> evenements = new ArrayList<>();

    private static Coordonnee c(String notation) {
        return Coordonnee.depuis(notation);
    }

    private static List<ResultatTir> resultats(Coup coup) {
        return coup.tirs().stream().map(Tir::resultat).toList();
    }

    @BeforeEach
    void creerPartie() {
        Plateau plateauAlice = new Plateau(5);
        plateauAlice.placer(TypeBateau.TORPILLEUR, c("A1"), Orientation.HORIZONTALE);
        plateauAlice.placer(TypeBateau.SOUS_MARIN, c("A3"), Orientation.HORIZONTALE);
        Plateau plateauBob = new Plateau(5);
        plateauBob.placer(TypeBateau.TORPILLEUR, c("A1"), Orientation.VERTICALE);
        plateauBob.placer(TypeBateau.SOUS_MARIN, c("C3"), Orientation.HORIZONTALE);
        alice = Joueur.humain("Alice", plateauAlice);
        bob = Joueur.humain("Bob", plateauBob);
        partie = new Partie(REGLES, alice, bob);
        partie.ajouterEcouteur(evenements::add);
    }

    @Test
    void leRadarCompteLesCasesDeBateauxEtPasseLaMain() {
        Coup coup = partie.jouer(Ordre.radar(c("B2")));
        Detection detection = coup.detection().orElseThrow();
        assertEquals(3, detection.nombre());
        assertEquals(9, detection.cases().size());
        assertTrue(coup.tirs().isEmpty());
        assertSame(bob, partie.getJoueurCourant());
        assertEquals(0, partie.getCharges(alice, Capacite.RADAR));
        assertEquals(1, partie.getCharges(bob, Capacite.RADAR));
    }

    @Test
    void unRadarSansBateauMarqueLesCasesCommeVides() {
        Detection detection = partie.jouer(Ordre.radar(c("E1"))).detection().orElseThrow();
        assertEquals(0, detection.nombre());
        assertEquals(4, detection.cases().size());
        assertEquals(EtatCase.VIDE, bob.getPlateau().getVueAdverse().getEtat(c("D2")));
        assertEquals(EtatCase.INCONNUE, bob.getPlateau().getVueAdverse().getEtat(c("D3")));
    }

    @Test
    void leBombardementTireSurLesQuatreCasesDuCarre() {
        Coup coup = partie.jouer(Ordre.bombardement(c("A1")));
        assertEquals(List.of(c("A1"), c("B1"), c("A2"), c("B2")), coup.tirs().stream().map(Tir::cible).toList());
        assertEquals(List.of(ResultatTir.TOUCHE, ResultatTir.MANQUE, ResultatTir.COULE, ResultatTir.MANQUE),
                resultats(coup));
        assertSame(bob, partie.getJoueurCourant());
        assertTrue(evenements.get(0) instanceof EvenementPartie.CapaciteUtilisee);
        assertEquals(1, evenements.stream().filter(e -> e instanceof EvenementPartie.BateauCoule).count());
    }

    @Test
    void unBombardementQuiDepasseDeLaGrilleEstRefuseSansRienConsommer() {
        assertThrows(IllegalArgumentException.class, () -> partie.jouer(Ordre.bombardement(c("E5"))));
        assertEquals(1, partie.getCharges(alice, Capacite.BOMBARDEMENT));
        assertSame(alice, partie.getJoueurCourant());
        assertTrue(partie.getHistorique().isEmpty());
    }

    @Test
    void laTorpilleTraverseLEauEtExploseSurLePremierBateau() {
        Coup surLigne = partie.jouer(Ordre.torpilleSurLigne(2));
        assertEquals(List.of(ResultatTir.MANQUE, ResultatTir.MANQUE, ResultatTir.TOUCHE), resultats(surLigne));
        assertEquals(c("C3"), surLigne.tirs().get(2).cible());
        Coup surColonne = partie.jouer(Ordre.torpilleSurColonne(2));
        assertSame(bob, surColonne.auteur());
        assertEquals(List.of(c("C1"), c("C2"), c("C3")), surColonne.tirs().stream().map(Tir::cible).toList());
        assertEquals(ResultatTir.TOUCHE, surColonne.tirs().get(2).resultat());
    }

    @Test
    void laTorpillePasseLesCasesDejaViseesEtPeutSePerdre() {
        partie.tirer(c("A5"));
        partie.tirer(c("E5"));
        Coup coup = partie.jouer(Ordre.torpilleSurLigne(4));
        assertEquals(c("B5"), coup.tirs().get(0).cible());
        assertEquals(4, coup.tirs().size());
        assertFalse(coup.tirs().get(3).resultat().estAuBut());
    }

    @Test
    void leDoubleTirDonneDeuxTirsSimples() {
        partie.jouer(Ordre.doubleTir());
        assertSame(alice, partie.getJoueurCourant());
        assertEquals(2, partie.getTirsRestantsCeTour());
        partie.tirer(c("E5"));
        assertSame(alice, partie.getJoueurCourant());
        partie.tirer(c("D5"));
        assertSame(bob, partie.getJoueurCourant());
        assertEquals(1, partie.getTirsRestantsCeTour());
    }

    @Test
    void uneSeuleCapaciteParTourEtAvantDeTirer() {
        partie.jouer(Ordre.doubleTir());
        assertTrue(partie.capacitesDisponibles().isEmpty());
        assertThrows(IllegalStateException.class, () -> partie.jouer(Ordre.radar(c("C3"))));
        partie.tirer(c("E5"));
        assertThrows(IllegalStateException.class, () -> partie.jouer(Ordre.bombardement(c("A1"))));
    }

    @Test
    void chaqueCapaciteNeSertQuUneFois() {
        partie.jouer(Ordre.radar(c("C3")));
        partie.tirer(c("E5"));
        assertFalse(partie.capacitesDisponibles().contains(Capacite.RADAR));
        assertThrows(IllegalStateException.class, () -> partie.jouer(Ordre.radar(c("C3"))));
    }

    @Test
    void lesCapacitesSontRefuseesQuandLaRegleEstDesactivee() {
        Plateau plateauAlice = new Plateau(5);
        plateauAlice.placer(TypeBateau.TORPILLEUR, c("A1"), Orientation.HORIZONTALE);
        Plateau plateauBob = new Plateau(5);
        plateauBob.placer(TypeBateau.TORPILLEUR, c("A1"), Orientation.HORIZONTALE);
        Partie classique = new Partie(new ConfigurationPartie(5, List.of(TypeBateau.TORPILLEUR)),
                Joueur.humain("Alice", plateauAlice), Joueur.humain("Bob", plateauBob));
        assertTrue(classique.capacitesDisponibles().isEmpty());
        assertThrows(IllegalStateException.class, () -> classique.jouer(Ordre.doubleTir()));
    }

    @Test
    void uneCapacitePeutGagnerLaPartie() {
        partie.jouer(Ordre.bombardement(c("A1")));
        partie.tirer(c("E5"));
        partie.tirer(c("C3"));
        partie.tirer(c("D5"));
        partie.tirer(c("D3"));
        partie.tirer(c("E4"));
        Coup coup = partie.jouer(Ordre.torpilleSurLigne(2));
        assertEquals(List.of(c("A3"), c("B3"), c("E3")), coup.tirs().stream().map(Tir::cible).toList());
        assertEquals(ResultatTir.COULE, coup.tirs().get(coup.tirs().size() - 1).resultat());
        assertTrue(partie.estTerminee());
        assertSame(alice, partie.getGagnant().orElseThrow());
        assertTrue(evenements.get(evenements.size() - 1) instanceof EvenementPartie.PartieTerminee);
    }

    @Test
    void lesEvenementsSuiventLOrdreDuJeu() {
        partie.tirer(c("A1"));
        partie.tirer(c("E5"));
        assertEquals(List.of(new EvenementPartie.TirEffectue(alice, Tir.de(c("A1"), ResultatTir.TOUCHE)),
                new EvenementPartie.TourChange(bob, 2),
                new EvenementPartie.TirEffectue(bob, Tir.de(c("E5"), ResultatTir.MANQUE)),
                new EvenementPartie.TourChange(alice, 3)), evenements);
    }

    @Test
    void lesStatistiquesComptentLesCasesEtLesCapacites() {
        partie.jouer(Ordre.bombardement(c("A1")));
        assertEquals(new Statistiques(4, 2, 1), partie.getStatistiques(alice));
    }

    @Test
    void lOrdinateurUtiliseSesCapacitesEtFinitLaPartie() {
        ConfigurationPartie configuration = ConfigurationPartie.classique().avecCapacites(true);
        for (int graine = 0; graine < 20; graine++) {
            Random random = new Random(graine);
            Plateau premier = new Plateau(10);
            Plateau second = new Plateau(10);
            PlacementAleatoire.placer(premier, ModeleFlotte.CLASSIQUE.getTypes(), random);
            PlacementAleatoire.placer(second, ModeleFlotte.CLASSIQUE.getTypes(), random);
            Joueur normale = Joueur.ordinateur("Normale", premier, Difficulte.NORMALE.creerStrategie(random),
                    Difficulte.NORMALE.creerTacticien(random).orElseThrow());
            Joueur difficile = Joueur.ordinateur("Difficile", second, Difficulte.DIFFICILE.creerStrategie(random),
                    Difficulte.DIFFICILE.creerTacticien(random).orElseThrow());
            Partie duel = new Partie(configuration, normale, difficile);
            while (!duel.estTerminee()) {
                duel.faireJouerOrdinateur();
            }
            Joueur gagnant = duel.getGagnant().orElseThrow();
            assertTrue(duel.adversaireDe(gagnant).getPlateau().flotteCoulee());
            assertTrue(duel.getStatistiques(difficile).capacites() >= 1, "graine " + graine);
        }
    }
}
