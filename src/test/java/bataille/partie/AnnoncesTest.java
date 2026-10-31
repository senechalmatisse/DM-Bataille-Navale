package bataille.partie;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bataille.ia.Difficulte;
import bataille.modele.ConfigurationPartie;
import bataille.modele.Coordonnee;
import bataille.modele.Detection;
import bataille.modele.Ordre;
import bataille.modele.Orientation;
import bataille.modele.Plateau;
import bataille.modele.ResultatTir;
import bataille.modele.Tir;
import bataille.modele.TypeBateau;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class AnnoncesTest {

    private static final Coordonnee B7 = Coordonnee.depuis("B7");

    private static Plateau plateau() {
        Plateau plateau = new Plateau(10);
        plateau.placer(TypeBateau.TORPILLEUR, Coordonnee.depuis("A1"), Orientation.HORIZONTALE);
        return plateau;
    }

    private final Joueur alice = Joueur.humain("Alice", plateau());
    private final Joueur bob = Joueur.humain("Bob", plateau());
    private final Joueur ordinateur = Joueur.ordinateur("Ordinateur", plateau(),
            Difficulte.FACILE.creerStrategie(new Random(1)));

    private static Coup tir(Joueur auteur, Tir tir) {
        return new Coup(auteur, Ordre.tir(tir.cible()), List.of(tir), Optional.empty());
    }

    @Test
    void raconteUnTirAuJoueurQuiLAJoue() {
        assertEquals("Vous tirez en B7 : manqué.", Annonces.decrire(tir(alice, Tir.de(B7, ResultatTir.MANQUE)), alice));
        assertEquals("Vous tirez en B7 : coulé ! Vous avez coulé le porte-avions.",
                Annonces.decrire(tir(alice, Tir.coule(B7, TypeBateau.PORTE_AVIONS)), alice));
    }

    @Test
    void raconteUnTirAdverse() {
        assertEquals("L'ordinateur tire en B7 : coulé ! Il a coulé votre sous-marin.",
                Annonces.decrire(tir(ordinateur, Tir.coule(B7, TypeBateau.SOUS_MARIN)), alice));
        assertEquals("Bob tire en B7 : touché !", Annonces.decrire(tir(bob, Tir.de(B7, ResultatTir.TOUCHE)), alice));
    }

    @Test
    void raconteLesCapacites() {
        Coup radar = new Coup(alice, Ordre.radar(B7), List.of(),
                Optional.of(new Detection(B7, List.of(B7, Coordonnee.depuis("B8")), 2)));
        assertEquals("Vous passez le radar en B7 : 2 cases de bateaux détectées.", Annonces.decrire(radar, alice));
        Coup bombe = new Coup(bob, Ordre.bombardement(B7), List.of(Tir.de(B7, ResultatTir.TOUCHE),
                Tir.de(Coordonnee.depuis("C7"), ResultatTir.MANQUE)), Optional.empty());
        assertEquals("Bob bombarde la zone B7 : 1 touché, 1 manqué.", Annonces.decrire(bombe, alice));
        Coup torpille = new Coup(ordinateur, Ordre.torpilleSurLigne(6), List.of(Tir.de(Coordonnee.depuis("A7"),
                ResultatTir.MANQUE)), Optional.empty());
        assertEquals("L'ordinateur lance une torpille sur la ligne 7 : elle se perd sans rien toucher.",
                Annonces.decrire(torpille, alice));
        Coup doubleTir = new Coup(alice, Ordre.doubleTir(), List.of(), Optional.empty());
        assertEquals("Vous activez le double tir : tirez deux fois ce tour-ci.", Annonces.decrire(doubleTir, alice));
    }

    @Test
    void resumeLesTirsDUnJoueur() {
        assertEquals("47 tirs, précision 36 %", Annonces.bilan(new Statistiques(47, 17)));
        assertEquals("12 tirs, précision 50 %, 2 capacités", Annonces.bilan(new Statistiques(12, 6, 2)));
    }

    @Test
    void lesParametresRemplacentLesNomsVides() {
        ParametresPartie parametres = ParametresPartie.aDeux(" ", null, ConfigurationPartie.classique(), false);
        assertEquals(ParametresPartie.NOM_PAR_DEFAUT, parametres.nomJoueur1());
        assertEquals(ParametresPartie.NOM_PAR_DEFAUT_2, parametres.nomJoueur2());
    }
}
