package bataille.partie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bataille.ia.Difficulte;
import bataille.modele.ConfigurationPartie;
import bataille.modele.Coordonnee;
import bataille.modele.EtatCase;
import bataille.modele.Ordre;
import bataille.modele.Orientation;
import bataille.modele.Plateau;
import bataille.modele.PositionBateau;
import bataille.modele.TypeBateau;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RejeuEtAnalyseTest {

    private static final ConfigurationPartie MINI =
            new ConfigurationPartie(5, List.of(TypeBateau.TORPILLEUR, TypeBateau.SOUS_MARIN), true);

    private static Coordonnee c(String notation) {
        return Coordonnee.depuis(notation);
    }

    private static Partie partieAdeux() {
        Plateau alice = new Plateau(5);
        alice.placer(TypeBateau.TORPILLEUR, c("A1"), Orientation.HORIZONTALE);
        alice.placer(TypeBateau.SOUS_MARIN, c("A3"), Orientation.HORIZONTALE);
        Plateau bob = new Plateau(5);
        bob.placer(TypeBateau.TORPILLEUR, c("A1"), Orientation.VERTICALE);
        bob.placer(TypeBateau.SOUS_MARIN, c("C3"), Orientation.HORIZONTALE);
        Partie partie = ParametresPartie.aDeux("Alice", "Bob", MINI, false).creerPartie(alice, bob, new Random(1));
        partie.tirer(c("E5"));
        partie.tirer(c("A1"));
        partie.tirer(c("A1"));
        partie.tirer(c("B1"));
        partie.tirer(c("A2"));
        partie.jouer(Ordre.radar(c("D4")));
        return partie;
    }

    @Test
    void leRejeuAvanceReculeEtVaAUnCoup() {
        Partie partie = partieAdeux();
        Rejeu rejeu = new Rejeu(EnregistrementPartie.de(partie, ParametresPartie.aDeux("Alice", "Bob", MINI, false)));
        assertEquals(6, rejeu.getNombreCoups());
        assertEquals(0, rejeu.getPosition());
        assertFalse(rejeu.getDernierCoup().isPresent());
        assertFalse(rejeu.reculer());
        assertTrue(rejeu.avancer());
        assertEquals(c("E5"), rejeu.getDernierCoup().orElseThrow().tirs().get(0).cible());
        rejeu.allerA(5);
        assertEquals(EtatCase.COULE, rejeu.getPartie().getJoueur1().getPlateau().getVueAdverse().getEtat(c("A1")));
        assertTrue(rejeu.avancer());
        assertFalse(rejeu.avancer());
        assertEquals(1, rejeu.getPartie().getJoueur1().getPlateau().getDetections().size());
        rejeu.allerA(99);
        assertEquals(6, rejeu.getPosition());
        assertTrue(rejeu.reculer());
        assertEquals(5, rejeu.getPosition());
    }

    @Test
    void unEnregistrementQuiNeRespectePasLesToursEstRefuse() {
        Partie partie = partieAdeux();
        EnregistrementPartie juste = EnregistrementPartie.de(partie, ParametresPartie.aDeux("A", "B", MINI, false));
        List<EnregistrementPartie.OrdreJoue> ordres =
                List.of(new EnregistrementPartie.OrdreJoue(2, Ordre.tir(c("C3"))));
        EnregistrementPartie faux =
                new EnregistrementPartie(juste.parametres(), juste.flotte1(), juste.flotte2(), ordres);
        assertThrows(IllegalArgumentException.class, () -> faux.restaurer(new Random(1)));
    }

    @Test
    void lAnalyseRetraceLaChasseDeChaqueBateau() {
        Partie partie = partieAdeux();
        Joueur bob = partie.getJoueur2();
        List<AnalysePartie.Traque> traques = AnalysePartie.traques(partie, bob);
        assertEquals(new AnalysePartie.Traque(TypeBateau.TORPILLEUR, 1, 2), traques.get(0));
        assertFalse(traques.get(1).estCoule());
        assertEquals(List.of("Vous avez trouvé le torpilleur au 1er tir et l'avez coulé en 2 tirs."),
                AnalysePartie.raconter(partie, bob, bob));
        assertEquals(List.of("Bob a trouvé votre torpilleur au 1er tir et l'a coulé en 2 tirs."),
                AnalysePartie.raconter(partie, bob, partie.getJoueur1()));
    }

    @Test
    void unePositionDeBateauSEcritEtSeRelit() {
        PositionBateau position = new PositionBateau(TypeBateau.CROISEUR, c("H4"), Orientation.VERTICALE);
        assertEquals("CROISEUR@H4:V", position.enTexte());
        assertEquals(position, PositionBateau.depuisTexte("croiseur@h4:v"));
        assertThrows(IllegalArgumentException.class, () -> PositionBateau.depuisTexte("CROISEUR-H4"));
    }

    @Test
    void lOrdinateurReprendUnePartieRestauree() {
        Plateau joueur = new Plateau(5);
        joueur.placer(TypeBateau.TORPILLEUR, c("A1"), Orientation.HORIZONTALE);
        joueur.placer(TypeBateau.SOUS_MARIN, c("A3"), Orientation.HORIZONTALE);
        ParametresPartie parametres = ParametresPartie.contreOrdinateur("Matisse", Difficulte.NORMALE, MINI, false);
        Partie partie = parametres.creerPartie(joueur, new Random(4));
        partie.tirer(c("E5"));
        partie.faireJouerOrdinateur();
        Partie reprise = EnregistrementPartie.de(partie, parametres).restaurer(new Random(8));
        reprise.tirer(c("E4"));
        while (!reprise.getJoueurCourant().estHumain() && !reprise.estTerminee()) {
            reprise.faireJouerOrdinateur();
        }
        assertTrue(reprise.getHistorique().size() >= 4);
    }
}
