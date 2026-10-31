package bataille.ia;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import bataille.modele.Coordonnee;
import bataille.modele.ModeleFlotte;
import bataille.modele.Orientation;
import bataille.modele.PlacementAleatoire;
import bataille.modele.Plateau;
import bataille.modele.ResultatTir;
import bataille.modele.Tir;
import bataille.modele.TypeBateau;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StrategiesTest {

    private static Coordonnee c(String notation) {
        return Coordonnee.depuis(notation);
    }

    /** Fait tirer une stratégie jusqu'à couler une flotte classique placée au hasard ; renvoie le nombre de tirs. */
    private static int tirsPourCouler(StrategieTir strategie, long graine) {
        Plateau plateau = new Plateau(10);
        PlacementAleatoire.placer(plateau, ModeleFlotte.CLASSIQUE.getTypes(), new Random(graine));
        int tirs = 0;
        while (!plateau.flotteCoulee()) {
            Coordonnee cible = strategie.choisirTir(plateau.getVueAdverse());
            Tir tir = plateau.recevoirTir(cible);
            if (tir.resultat() == ResultatTir.DEJA_JOUE) {
                fail(strategie.getClass().getSimpleName() + " a visé deux fois " + cible);
            }
            tirs++;
        }
        return tirs;
    }

    private static double moyenne(Difficulte difficulte, int parties) {
        StrategieTir strategie = difficulte.creerStrategie(new Random(2026));
        long total = 0;
        for (int graine = 0; graine < parties; graine++) {
            total += tirsPourCouler(strategie, graine);
        }
        return (double) total / parties;
    }

    @Test
    @DisplayName("Chaque niveau coule toute la flotte sans viser deux fois la même case")
    void chaqueNiveauCouleLaFlotte() {
        for (Difficulte difficulte : Difficulte.values()) {
            StrategieTir strategie = difficulte.creerStrategie(new Random(1));
            for (int graine = 0; graine < 20; graine++) {
                assertTrue(tirsPourCouler(strategie, graine) <= 100);
            }
        }
    }

    @Test
    void apresUneToucheLaChasseCibleViseUneCaseVoisine() {
        Plateau plateau = new Plateau(10);
        plateau.placer(TypeBateau.CROISEUR, c("E5"), Orientation.HORIZONTALE);
        plateau.recevoirTir(c("F5"));
        List<Coordonnee> voisines = List.of(c("F4"), c("F6"), c("E5"), c("G5"));
        for (int graine = 0; graine < 20; graine++) {
            Coordonnee cible = new ChasseCible(new Random(graine)).choisirTir(plateau.getVueAdverse());
            assertTrue(voisines.contains(cible), "tir en " + cible);
        }
    }

    @Test
    void avecDeuxTouchesAligneesLesStrategiesProlongentLaLigne() {
        Plateau plateau = new Plateau(10);
        plateau.placer(TypeBateau.CROISEUR, c("E5"), Orientation.HORIZONTALE);
        plateau.recevoirTir(c("F5"));
        plateau.recevoirTir(c("G5"));
        List<Coordonnee> prolongements = List.of(c("E5"), c("H5"));
        for (int graine = 0; graine < 20; graine++) {
            Coordonnee chasse = new ChasseCible(new Random(graine)).choisirTir(plateau.getVueAdverse());
            Coordonnee carte = new CarteProbabilites(new Random(graine)).choisirTir(plateau.getVueAdverse());
            assertTrue(prolongements.contains(chasse), "chasse/cible en " + chasse);
            assertTrue(prolongements.contains(carte), "carte de probabilités en " + carte);
        }
    }

    @Test
    @DisplayName("Plus le niveau est élevé, moins il faut de tirs pour couler la flotte")
    void lesNiveauxSontDeDifficulteCroissante() {
        double facile = moyenne(Difficulte.FACILE, 150);
        double normale = moyenne(Difficulte.NORMALE, 150);
        double difficile = moyenne(Difficulte.DIFFICILE, 150);
        assertTrue(normale < facile - 30, "facile " + facile + ", normale " + normale);
        assertTrue(difficile < normale - 3, "normale " + normale + ", difficile " + difficile);
    }
}
