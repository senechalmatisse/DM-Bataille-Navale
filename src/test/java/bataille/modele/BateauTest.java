package bataille.modele;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BateauTest {

    @Test
    void sEtendVersLaDroiteQuandIlEstHorizontal() {
        Bateau bateau = new Bateau(TypeBateau.CONTRE_TORPILLEUR, Coordonnee.depuis("B2"), Orientation.HORIZONTALE);
        assertEquals(List.of(Coordonnee.depuis("B2"), Coordonnee.depuis("C2"), Coordonnee.depuis("D2")),
                bateau.getCases());
    }

    @Test
    void sEtendVersLeBasQuandIlEstVertical() {
        Bateau bateau = new Bateau(TypeBateau.TORPILLEUR, Coordonnee.depuis("J9"), Orientation.VERTICALE);
        assertEquals(List.of(Coordonnee.depuis("J9"), Coordonnee.depuis("J10")), bateau.getCases());
    }

    @Test
    void estCouleQuandToutesSesCasesSontTouchees() {
        Bateau bateau = new Bateau(TypeBateau.TORPILLEUR, Coordonnee.depuis("A1"), Orientation.HORIZONTALE);
        bateau.toucher(Coordonnee.depuis("A1"));
        assertTrue(bateau.estTouche(Coordonnee.depuis("A1")));
        assertFalse(bateau.estCoule());
        bateau.toucher(Coordonnee.depuis("B1"));
        assertTrue(bateau.estCoule());
    }

    @Test
    void refuseUneToucheHorsDeSesCases() {
        Bateau bateau = new Bateau(TypeBateau.TORPILLEUR, Coordonnee.depuis("A1"), Orientation.HORIZONTALE);
        assertThrows(IllegalArgumentException.class, () -> bateau.toucher(Coordonnee.depuis("A2")));
    }

    @Test
    void seDecrit() {
        Bateau bateau = new Bateau(TypeBateau.CROISEUR, Coordonnee.depuis("B7"), Orientation.HORIZONTALE);
        assertEquals("Croiseur en B7 (H)", bateau.toString());
    }
}
