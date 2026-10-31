package bataille.modele;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigurationPartieTest {

    @Test
    void laConfigurationClassiqueEstLaConfigurationReglementaire() {
        ConfigurationPartie configuration = ConfigurationPartie.classique();
        assertEquals(10, configuration.taille());
        assertEquals(5, configuration.flotte().size());
        assertEquals(17, configuration.nombreCasesFlotte());
    }

    @Test
    void refuseUneGrilleHorsLimites() {
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPartie.de(4, ModeleFlotte.RAPIDE));
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPartie.de(27, ModeleFlotte.RAPIDE));
    }

    @Test
    void refuseUneFlotteVide() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigurationPartie(10, List.of()));
    }

    @Test
    void refuseUneFlotteQuiOccuperaitPlusDeLaMoitieDeLaGrille() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> ConfigurationPartie.de(5, ModeleFlotte.CLASSIQUE));
        assertTrue(erreur.getMessage().contains("17 cases"), erreur.getMessage());
    }

    @Test
    void accepteLaFlotteClassiqueSurUneGrilleDeSix() {
        assertEquals(6, ConfigurationPartie.de(6, ModeleFlotte.CLASSIQUE).taille());
    }

    @Test
    void copieLaFlotteRecue() {
        List<TypeBateau> flotte = new ArrayList<>(List.of(TypeBateau.TORPILLEUR));
        ConfigurationPartie configuration = new ConfigurationPartie(5, flotte);
        flotte.add(TypeBateau.CROISEUR);
        assertEquals(1, configuration.flotte().size());
    }

    @Test
    void decritLesModelesDeFlotte() {
        assertEquals("Classique : 5 bateaux, 17 cases", ModeleFlotte.CLASSIQUE.toString());
        assertEquals("Rapide : 3 bateaux, 9 cases", ModeleFlotte.RAPIDE.toString());
    }
}
