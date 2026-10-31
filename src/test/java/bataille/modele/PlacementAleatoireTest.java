package bataille.modele;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlacementAleatoireTest {

    private static int casesOccupees(Plateau plateau) {
        int occupees = 0;
        for (int ligne = 0; ligne < plateau.getTaille(); ligne++) {
            for (int colonne = 0; colonne < plateau.getTaille(); colonne++) {
                if (plateau.getBateau(new Coordonnee(ligne, colonne)).isPresent()) {
                    occupees++;
                }
            }
        }
        return occupees;
    }

    @Test
    void placeToutLaFlotteSansChevauchement() {
        for (int graine = 0; graine < 200; graine++) {
            Plateau plateau = new Plateau(10);
            List<Bateau> poses = PlacementAleatoire.placer(plateau, ModeleFlotte.CLASSIQUE.getTypes(),
                    new Random(graine));
            assertEquals(5, poses.size());
            assertEquals(17, casesOccupees(plateau), "graine " + graine);
        }
    }

    @Test
    @DisplayName("Toutes les cases peuvent recevoir un bateau, y compris la ligne 1 et la colonne A")
    void toutesLesCasesPeuventRecevoirUnBateau() {
        boolean[][] dejaOccupee = new boolean[10][10];
        for (int graine = 0; graine < 500; graine++) {
            Plateau plateau = new Plateau(10);
            PlacementAleatoire.placer(plateau, ModeleFlotte.CLASSIQUE.getTypes(), new Random(graine));
            for (Bateau bateau : plateau.getBateaux()) {
                for (Coordonnee coordonnee : bateau.getCases()) {
                    dejaOccupee[coordonnee.ligne()][coordonnee.colonne()] = true;
                }
            }
        }
        for (int ligne = 0; ligne < 10; ligne++) {
            for (int colonne = 0; colonne < 10; colonne++) {
                assertTrue(dejaOccupee[ligne][colonne], "case jamais occupée : " + new Coordonnee(ligne, colonne));
            }
        }
    }

    @Test
    void laMemeGraineDonneLeMemePlacement() {
        Plateau premier = new Plateau(10);
        Plateau second = new Plateau(10);
        PlacementAleatoire.placer(premier, ModeleFlotte.CLASSIQUE.getTypes(), new Random(42));
        PlacementAleatoire.placer(second, ModeleFlotte.CLASSIQUE.getTypes(), new Random(42));
        assertEquals(premier.getBateaux().toString(), second.getBateaux().toString());
    }

    @Test
    void completeUnPlacementCommence() {
        Plateau plateau = new Plateau(10);
        plateau.placer(TypeBateau.PORTE_AVIONS, Coordonnee.depuis("A1"), Orientation.HORIZONTALE);
        List<TypeBateau> reste = ModeleFlotte.CLASSIQUE.getTypes().subList(1, 5);
        PlacementAleatoire.placer(plateau, reste, new Random(7));
        assertEquals(5, plateau.getBateaux().size());
        assertEquals(17, casesOccupees(plateau));
    }

    @Test
    void placeLaFlotteClassiqueSurLaPlusPetiteGrilleQuiLAccepte() {
        for (int graine = 0; graine < 100; graine++) {
            Plateau plateau = new Plateau(6);
            PlacementAleatoire.placer(plateau, ModeleFlotte.CLASSIQUE.getTypes(), new Random(graine));
            assertEquals(17, casesOccupees(plateau));
        }
    }

    @Test
    @DisplayName("Signale une flotte qui ne tient pas au lieu de boucler sans fin")
    void signaleUneFlotteQuiNeTientPas() {
        Plateau plateau = new Plateau(5);
        List<TypeBateau> tropGrande = Collections.nCopies(6, TypeBateau.PORTE_AVIONS);
        assertThrows(IllegalStateException.class, () -> PlacementAleatoire.placer(plateau, tropGrande, new Random(1)));
        assertTrue(plateau.getBateaux().isEmpty());
    }
}
