package bataille.modele;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TirTest {

    @Test
    void unBateauCouleAccompagneUnTirCoule() {
        Coordonnee b7 = Coordonnee.depuis("B7");
        assertThrows(IllegalArgumentException.class, () -> new Tir(b7, ResultatTir.COULE, Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> new Tir(b7, ResultatTir.TOUCHE, Optional.of(TypeBateau.CROISEUR)));
    }

    @Test
    void seDecrit() {
        assertEquals("B7 : manqué", Tir.de(Coordonnee.depuis("B7"), ResultatTir.MANQUE).decrire());
        assertEquals("C3 : coulé (le croiseur)", Tir.coule(Coordonnee.depuis("C3"), TypeBateau.CROISEUR).decrire());
    }

    @Test
    void distingueLesTirsAuBut() {
        assertTrue(ResultatTir.TOUCHE.estAuBut());
        assertTrue(ResultatTir.COULE.estAuBut());
        assertFalse(ResultatTir.MANQUE.estAuBut());
        assertFalse(ResultatTir.DEJA_JOUE.estAuBut());
    }
}
