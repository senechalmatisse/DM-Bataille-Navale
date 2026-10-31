package bataille.modele;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class OrdreTest {

    @Test
    void seRelitApresAvoirEteEcrit() {
        List<Ordre> ordres = List.of(Ordre.tir(Coordonnee.depuis("B7")), Ordre.radar(Coordonnee.depuis("E5")),
                Ordre.bombardement(Coordonnee.depuis("C3")), Ordre.torpilleSurLigne(4),
                Ordre.torpilleSurColonne(2), Ordre.doubleTir());
        for (Ordre ordre : ordres) {
            assertEquals(ordre, Ordre.depuisTexte(ordre.enTexte()), ordre.enTexte());
        }
        assertEquals("TORPILLE A5 H", Ordre.torpilleSurLigne(4).enTexte());
    }

    @Test
    void refuseUnTexteIllisible() {
        for (String texte : new String[] {"", "TIR", "TIR Z99Z", "RADAR", "LASER B2", "TORPILLE B2"}) {
            assertThrows(IllegalArgumentException.class, () -> Ordre.depuisTexte(texte), texte);
        }
    }

    @Test
    void refuseUnOrdreIncomplet() {
        assertThrows(IllegalArgumentException.class, () -> new Ordre(Capacite.RADAR, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Ordre(Capacite.TORPILLE, Coordonnee.depuis("A1"), null));
        assertThrows(IllegalArgumentException.class,
                () -> new Ordre(Capacite.DOUBLE_TIR, Coordonnee.depuis("A1"), null));
    }

    @Test
    void seDecrit() {
        assertEquals("torpille sur la ligne 5", Ordre.torpilleSurLigne(4).decrire());
        assertEquals("torpille sur la colonne C", Ordre.torpilleSurColonne(2).decrire());
        assertEquals("bombardement en C3", Ordre.bombardement(Coordonnee.depuis("C3")).decrire());
    }
}
