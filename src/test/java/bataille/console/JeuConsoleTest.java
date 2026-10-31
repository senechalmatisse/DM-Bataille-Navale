package bataille.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bataille.modele.Coordonnee;
import bataille.modele.Orientation;
import bataille.modele.Plateau;
import bataille.modele.TypeBateau;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JeuConsoleTest {

    /** Réponses aux questions de départ : nom par défaut, niveau facile, grille 5×5, flotte rapide. */
    private static final String DEBUT_GRILLE_5 = "\n1\n5\n2\n";

    private static String jouer(String saisies, long graine) {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        PrintStream sortie = new PrintStream(tampon, true, StandardCharsets.UTF_8);
        new JeuConsole(new BufferedReader(new StringReader(saisies)), sortie, new Random(graine)).lancer();
        return tampon.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("S'arrête proprement quand l'entrée se termine")
    void sArreteProprementEnFinDEntree() {
        assertTrue(jouer("", 1).contains("Fin de l'entrée : au revoir !"));
        assertTrue(jouer(DEBUT_GRILLE_5 + "\nA1\n", 1).contains("Fin de l'entrée : au revoir !"));
    }

    @Test
    @DisplayName("Explique chaque saisie refusée")
    void expliqueChaqueSaisieRefusee() {
        String sortie = jouer(DEBUT_GRILLE_5 + "\nZ9\nhello\nA0\nA1\nA1\nq\n", 3);
        assertTrue(sortie.contains("Z9 est hors de la grille (A1 à E5)."), sortie);
        assertTrue(sortie.contains("A0 est hors de la grille (A1 à E5)."), sortie);
        assertTrue(sortie.contains("« HELLO » n'est pas une case"), sortie);
        assertTrue(sortie.contains("Vous avez déjà tiré en A1."), sortie);
        assertTrue(sortie.contains("Partie abandonnée."), sortie);
    }

    @Test
    void redemandeUneConfigurationInjouable() {
        String sortie = jouer("Alice\n2\n5\n1\n6\n1\n\nq\n", 4);
        assertTrue(sortie.contains("La flotte occupe 17 cases"), sortie);
        assertTrue(sortie.contains("A1 à F6") || sortie.contains("     A B C D E F"), sortie);
    }

    @Test
    void joueUnePartieCompleteJusquAuBilan() {
        StringBuilder saisies = new StringBuilder("Alice\n1\n5\n2\nn\n");
        for (int ligne = 1; ligne <= 5; ligne++) {
            for (char colonne = 'A'; colonne <= 'E'; colonne++) {
                saisies.append(colonne).append(ligne).append('\n');
            }
        }
        saisies.append("n\n");
        String sortie = jouer(saisies.toString(), 11);
        assertTrue(sortie.contains("*** Victoire") || sortie.contains("*** Défaite"), sortie);
        assertTrue(sortie.contains("Vous : ") && sortie.contains(" tirs, précision "), sortie);
        assertTrue(sortie.contains("Flotte ennemie dévoilée"), sortie);
        assertTrue(sortie.contains("Au revoir !"), sortie);
    }

    @Test
    void placeLesBateauxALaMainEnExpliquantLesRefus() {
        String sortie = jouer(DEBUT_GRILLE_5 + "o\nC1 H\nB1 H\nB1 V\nA3 V\nC3H\nq\n", 5);
        assertTrue(sortie.contains("Le croiseur déborderait de la grille (A1 à E5)."), sortie);
        assertTrue(sortie.contains("Le sous-marin chevaucherait le croiseur."), sortie);
        assertTrue(sortie.contains("Votre flotte est prête"), sortie);
        assertTrue(sortie.contains("  1  ~ # # # #"), sortie);
        assertTrue(sortie.contains("  3  # ~ # # ~"), sortie);
        assertTrue(sortie.contains("Partie abandonnée."), sortie);
    }

    @Test
    void placeLeResteDeLaFlotteAuHasard() {
        String sortie = jouer(DEBUT_GRILLE_5 + "o\nB1 H\nhasard\nq\n", 6);
        assertTrue(sortie.contains("Votre flotte est prête"), sortie);
        assertFalse(sortie.contains("Tapez une case puis H ou V"), sortie);
    }

    @Test
    void afficheDeuxGrillesAligneesAvecLeurLegende() {
        Plateau plateau = new Plateau(5);
        plateau.placer(TypeBateau.TORPILLEUR, Coordonnee.depuis("A1"), Orientation.HORIZONTALE);
        plateau.recevoirTir(Coordonnee.depuis("A1"));
        plateau.recevoirTir(Coordonnee.depuis("C3"));
        String grilles = RenduConsole.deuxGrilles(plateau, "Votre flotte", plateau.getVueAdverse(), "Vue adverse");
        String[] lignes = grilles.split(System.lineSeparator());
        assertEquals(7, lignes.length);
        assertTrue(lignes[2].startsWith("  1  X # ~ ~ ~"), lignes[2]);
        assertTrue(lignes[2].endsWith("  1  X ~ ~ ~ ~"), lignes[2]);
        assertTrue(lignes[4].endsWith("  3  ~ ~ o ~ ~"), lignes[4]);
        assertEquals(lignes[1].indexOf('A', 10), lignes[2].indexOf('X', 10));
    }
}
