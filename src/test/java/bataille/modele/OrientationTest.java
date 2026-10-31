package bataille.modele;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OrientationTest {

    @Test
    void litHEtV() {
        assertEquals(Orientation.HORIZONTALE, Orientation.depuis("H"));
        assertEquals(Orientation.HORIZONTALE, Orientation.depuis(" h "));
        assertEquals(Orientation.VERTICALE, Orientation.depuis("v"));
    }

    @Test
    void refuseUneAutreLettre() {
        assertThrows(IllegalArgumentException.class, () -> Orientation.depuis("X"));
        assertThrows(IllegalArgumentException.class, () -> Orientation.depuis(""));
    }

    @Test
    void inverseLOrientation() {
        assertEquals(Orientation.VERTICALE, Orientation.HORIZONTALE.inverse());
        assertEquals(Orientation.HORIZONTALE, Orientation.VERTICALE.inverse());
    }
}
