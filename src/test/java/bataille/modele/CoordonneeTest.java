package bataille.modele;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CoordonneeTest {

    @Test
    @DisplayName("Lit la notation de jeu, quelles que soient la casse et les espaces")
    void litLaNotationDeJeu() {
        assertEquals(new Coordonnee(0, 0), Coordonnee.depuis("A1"));
        assertEquals(new Coordonnee(6, 1), Coordonnee.depuis("B7"));
        assertEquals(new Coordonnee(6, 1), Coordonnee.depuis(" b7 "));
        assertEquals(new Coordonnee(6, 1), Coordonnee.depuis("B 7"));
        assertEquals(new Coordonnee(9, 9), Coordonnee.depuis("J10"));
        assertEquals(new Coordonnee(25, 25), Coordonnee.depuis("Z26"));
    }

    @Test
    @DisplayName("Lit une case hors de la grille : c'est au plateau de la refuser")
    void litUneCaseHorsDeLaGrille() {
        assertEquals(new Coordonnee(-1, 0), Coordonnee.depuis("A0"));
        assertEquals(new Coordonnee(10, 10), Coordonnee.depuis("K11"));
    }

    @Test
    @DisplayName("Refuse ce qui n'est pas une lettre suivie d'un nombre")
    void refuseCeQuiNestPasUneCase() {
        String[] saisies = {"", "   ", "1A", "A", "7", "AA1", "A1B", "B1000", "é5", null};
        for (String saisie : saisies) {
            assertThrows(IllegalArgumentException.class, () -> Coordonnee.depuis(saisie), "saisie : " + saisie);
        }
    }

    @Test
    void ecritLaNotationDeJeu() {
        assertEquals("B7", new Coordonnee(6, 1).toString());
        assertEquals("J10", new Coordonnee(9, 9).toString());
        assertEquals("A1", Coordonnee.depuis("a1").toString());
    }

    @Test
    void decaleUneCase() {
        assertEquals(new Coordonnee(7, 3), new Coordonnee(6, 1).decaler(1, 2));
        assertEquals(new Coordonnee(5, 0), new Coordonnee(6, 1).decaler(-1, -1));
    }

    @Test
    void donneLaLettreDUneColonne() {
        assertEquals("A", Coordonnee.lettreColonne(0));
        assertEquals("Z", Coordonnee.lettreColonne(25));
    }
}
