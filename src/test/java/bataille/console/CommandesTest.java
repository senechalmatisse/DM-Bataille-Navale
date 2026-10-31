package bataille.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bataille.modele.Coordonnee;
import bataille.modele.Ordre;
import org.junit.jupiter.api.Test;

class CommandesTest {

    @Test
    void lisUneCaseOuUneCapacite() {
        assertEquals(Ordre.tir(Coordonnee.depuis("B7")), Commandes.lire(" b7 "));
        assertEquals(Ordre.radar(Coordonnee.depuis("E5")), Commandes.lire("Radar e5"));
        assertEquals(Ordre.bombardement(Coordonnee.depuis("C3")), Commandes.lire("bombe C3"));
        assertEquals(Ordre.bombardement(Coordonnee.depuis("C3")), Commandes.lire("bombardement C 3"));
        assertEquals(Ordre.torpilleSurLigne(4), Commandes.lire("torpille 5"));
        assertEquals(Ordre.torpilleSurColonne(4), Commandes.lire("TORPILLE E"));
        assertEquals(Ordre.doubleTir(), Commandes.lire("double"));
        assertEquals(Ordre.doubleTir(), Commandes.lire("double tir"));
    }

    @Test
    void expliqueUneCommandeIncomplete() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Commandes.lire("radar")).getMessage()
                .contains("« radar E5 »"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Commandes.lire("torpille E5")).getMessage()
                .contains("« torpille 5 » ou « torpille E »"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Commandes.lire("double B4")).getMessage()
                .contains("tapez simplement « double »"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Commandes.lire("radarE5")).getMessage()
                .contains("n'est pas une case"));
    }
}
