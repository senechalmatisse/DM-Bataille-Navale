package bataille.modele;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlateauTest {

    private Plateau plateau;

    private static Coordonnee c(String notation) {
        return Coordonnee.depuis(notation);
    }

    @BeforeEach
    void creerPlateau() {
        plateau = new Plateau(10);
    }

    @Test
    void placeUnBateauSurLesCasesAttendues() {
        Bateau bateau = plateau.placer(TypeBateau.CROISEUR, c("B2"), Orientation.HORIZONTALE);
        assertEquals(List.of(c("B2"), c("C2"), c("D2"), c("E2")), bateau.getCases());
        assertEquals(Optional.of(bateau), plateau.getBateau(c("E2")));
        assertEquals(Optional.empty(), plateau.getBateau(c("F2")));
    }

    @Test
    @DisplayName("Accepte un bateau en ligne 1 et en colonne A")
    void accepteLaLigne1EtLaColonneA() {
        plateau.placer(TypeBateau.PORTE_AVIONS, c("A1"), Orientation.HORIZONTALE);
        plateau.placer(TypeBateau.CROISEUR, c("A2"), Orientation.VERTICALE);
        assertEquals(2, plateau.getBateaux().size());
    }

    @Test
    void refuseUnBateauQuiDeborde() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> plateau.placer(TypeBateau.PORTE_AVIONS, c("G1"), Orientation.HORIZONTALE));
        assertEquals("Le porte-avions déborderait de la grille (A1 à J10).", erreur.getMessage());
        assertTrue(plateau.peutPlacer(TypeBateau.PORTE_AVIONS, c("F1"), Orientation.HORIZONTALE));
        assertFalse(plateau.peutPlacer(TypeBateau.PORTE_AVIONS, c("A7"), Orientation.VERTICALE));
    }

    @Test
    void refuseUnBateauQuiEnChevaucheUnAutre() {
        plateau.placer(TypeBateau.CROISEUR, c("B2"), Orientation.HORIZONTALE);
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> plateau.placer(TypeBateau.TORPILLEUR, c("C1"), Orientation.VERTICALE));
        assertEquals("Le torpilleur chevaucherait le croiseur.", erreur.getMessage());
    }

    @Test
    void annonceManqueToucheEtCoule() {
        plateau.placer(TypeBateau.TORPILLEUR, c("C3"), Orientation.HORIZONTALE);
        assertEquals(ResultatTir.MANQUE, plateau.recevoirTir(c("A1")).resultat());
        assertEquals(ResultatTir.TOUCHE, plateau.recevoirTir(c("C3")).resultat());
        Tir dernier = plateau.recevoirTir(c("D3"));
        assertEquals(ResultatTir.COULE, dernier.resultat());
        assertEquals(Optional.of(TypeBateau.TORPILLEUR), dernier.bateauCoule());
        assertTrue(plateau.flotteCoulee());
    }

    @Test
    void unTirSurUneCaseDejaViseeNeComptePas() {
        plateau.placer(TypeBateau.TORPILLEUR, c("C3"), Orientation.HORIZONTALE);
        plateau.recevoirTir(c("C3"));
        assertEquals(ResultatTir.DEJA_JOUE, plateau.recevoirTir(c("C3")).resultat());
        assertEquals(1, plateau.getNombreTirsRecus());
        assertFalse(plateau.flotteCoulee());
    }

    @Test
    void refuseUnTirHorsDeLaGrille() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> plateau.recevoirTir(c("K1")));
        assertEquals("K1 est hors de la grille (A1 à J10).", erreur.getMessage());
        assertThrows(IllegalArgumentException.class, () -> plateau.recevoirTir(c("A0")));
    }

    @Test
    void laFlotteNEstPasCouleeTantQuUnBateauFlotte() {
        plateau.placer(TypeBateau.TORPILLEUR, c("A1"), Orientation.HORIZONTALE);
        plateau.placer(TypeBateau.TORPILLEUR, c("A3"), Orientation.HORIZONTALE);
        plateau.recevoirTir(c("A1"));
        plateau.recevoirTir(c("B1"));
        assertFalse(plateau.flotteCoulee());
        assertEquals(1, plateau.getNombreBateauxRestants());
    }

    @Test
    void unPlateauSansBateauNEstPasUneFlotteCoulee() {
        assertFalse(new Plateau(5).flotteCoulee());
    }

    @Test
    void laVueAdverseCacheLesBateauxIntacts() {
        plateau.placer(TypeBateau.TORPILLEUR, c("C3"), Orientation.HORIZONTALE);
        plateau.placer(TypeBateau.SOUS_MARIN, c("A5"), Orientation.VERTICALE);
        VuePlateau vue = plateau.getVueAdverse();
        assertEquals(EtatCase.BATEAU, plateau.getEtatPourProprietaire(c("C3")));
        assertEquals(EtatCase.EAU, plateau.getEtatPourProprietaire(c("B3")));
        assertEquals(EtatCase.INCONNUE, vue.getEtat(c("C3")));
        assertEquals(EtatCase.INCONNUE, vue.getEtat(c("B3")));

        plateau.recevoirTir(c("C3"));
        plateau.recevoirTir(c("B3"));
        assertEquals(EtatCase.TOUCHE, vue.getEtat(c("C3")));
        assertEquals(EtatCase.MANQUE, vue.getEtat(c("B3")));

        plateau.recevoirTir(c("D3"));
        assertEquals(EtatCase.COULE, vue.getEtat(c("C3")));
        assertEquals(EtatCase.COULE, vue.getEtat(c("D3")));
        assertEquals(List.of(TypeBateau.SOUS_MARIN), vue.getTypesRestants());
        assertEquals(1, vue.getBateauxCoules().size());
        assertEquals(TypeBateau.TORPILLEUR, vue.getBateauxCoules().get(0).getType());
    }

    @Test
    void lesBateauxNeBougentPlusApresLePremierTir() {
        Bateau bateau = plateau.placer(TypeBateau.TORPILLEUR, c("C3"), Orientation.HORIZONTALE);
        plateau.recevoirTir(c("A1"));
        assertThrows(IllegalStateException.class,
                () -> plateau.placer(TypeBateau.SOUS_MARIN, c("A5"), Orientation.VERTICALE));
        assertThrows(IllegalStateException.class, () -> plateau.retirer(bateau));
    }

    @Test
    void retirerUnBateauLibereSesCases() {
        Bateau bateau = plateau.placer(TypeBateau.CROISEUR, c("B2"), Orientation.HORIZONTALE);
        plateau.retirer(bateau);
        assertTrue(plateau.getBateaux().isEmpty());
        assertTrue(plateau.peutPlacer(TypeBateau.CROISEUR, c("B2"), Orientation.VERTICALE));
    }
}
