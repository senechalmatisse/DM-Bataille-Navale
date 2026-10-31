package bataille.partie;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bataille.modele.Coordonnee;
import bataille.modele.ResultatTir;
import bataille.modele.Tir;
import bataille.modele.TypeBateau;
import org.junit.jupiter.api.Test;

class AnnoncesTest {

    private static final Coordonnee B7 = Coordonnee.depuis("B7");

    @Test
    void annonceLesTirsDuJoueur() {
        assertEquals("Vous tirez en B7 : manqué.", Annonces.tirDuJoueur(Tir.de(B7, ResultatTir.MANQUE)));
        assertEquals("Vous tirez en B7 : touché !", Annonces.tirDuJoueur(Tir.de(B7, ResultatTir.TOUCHE)));
        assertEquals("Vous tirez en B7 : coulé ! Vous avez coulé le porte-avions.",
                Annonces.tirDuJoueur(Tir.coule(B7, TypeBateau.PORTE_AVIONS)));
    }

    @Test
    void annonceLesTirsDeLOrdinateur() {
        assertEquals("L'ordinateur tire en B7 : manqué.", Annonces.tirDeLOrdinateur(Tir.de(B7, ResultatTir.MANQUE)));
        assertEquals("L'ordinateur tire en B7 : coulé ! Il a coulé votre sous-marin.",
                Annonces.tirDeLOrdinateur(Tir.coule(B7, TypeBateau.SOUS_MARIN)));
    }

    @Test
    void resumeLesTirsDUnJoueur() {
        assertEquals("47 tirs, précision 36 %", Annonces.bilan(new Statistiques(47, 17)));
    }
}
