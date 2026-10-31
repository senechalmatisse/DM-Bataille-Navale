package bataille.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bataille.donnees.Dossiers;
import bataille.modele.Coordonnee;
import bataille.modele.Orientation;
import bataille.modele.Plateau;
import bataille.modele.TypeBateau;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JeuConsoleTest {

    /** Menu « contre l'ordinateur », nom par défaut, niveau facile, grille 5×5, flotte rapide. */
    private static final String CONTRE_ORDINATEUR_5 = "1\n\n1\n5\n2\n";
    /** Sans capacités spéciales, placement automatique. */
    private static final String SANS_CAPACITES = "n\nn\n";

    private static Path nouveauDossier() {
        try {
            return Files.createTempDirectory("bataille-console");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String jouer(String saisies, long graine, Path dossier) {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        PrintStream sortie = new PrintStream(tampon, true, StandardCharsets.UTF_8);
        new JeuConsole(new BufferedReader(new StringReader(saisies)), sortie, new Random(graine), new Dossiers(dossier))
                .lancer();
        return tampon.toString(StandardCharsets.UTF_8);
    }

    private static String jouer(String saisies, long graine) {
        return jouer(saisies, graine, nouveauDossier());
    }

    @Test
    @DisplayName("S'arrête proprement quand l'entrée se termine")
    void sArreteProprementEnFinDEntree() {
        assertTrue(jouer("", 1).contains("Fin de l'entrée : au revoir !"));
        assertTrue(jouer(CONTRE_ORDINATEUR_5 + SANS_CAPACITES + "A1\n", 1).contains("Fin de l'entrée : au revoir !"));
        assertTrue(jouer("4\n", 1).contains("Au revoir !"));
    }

    @Test
    @DisplayName("Explique chaque saisie refusée")
    void expliqueChaqueSaisieRefusee() {
        String sortie = jouer(CONTRE_ORDINATEUR_5 + SANS_CAPACITES + "Z9\nhello\nA0\nA1\nA1\nradar C3\nq\n", 3);
        assertTrue(sortie.contains("Z9 est hors de la grille (A1 à E5)."), sortie);
        assertTrue(sortie.contains("A0 est hors de la grille (A1 à E5)."), sortie);
        assertTrue(sortie.contains("« HELLO » n'est pas une case"), sortie);
        assertTrue(sortie.contains("Vous avez déjà tiré en A1."), sortie);
        assertTrue(sortie.contains("Les capacités spéciales ne sont pas activées dans cette partie."), sortie);
        assertTrue(sortie.contains("Partie interrompue : vous pourrez la reprendre depuis le menu."), sortie);
    }

    @Test
    void redemandeUneConfigurationInjouable() {
        String sortie = jouer("1\nAlice\n2\n5\n1\n6\n1\n\n\nq\n", 4);
        assertTrue(sortie.contains("La flotte occupe 17 cases"), sortie);
        assertTrue(sortie.contains("     A B C D E F"), sortie);
    }

    @Test
    void joueUnePartieCompleteJusquAuBilanPuisAfficheLesStatistiques() {
        StringBuilder saisies = new StringBuilder("1\nAlice\n1\n5\n2\nn\nn\n");
        for (int ligne = 1; ligne <= 5; ligne++) {
            for (char colonne = 'A'; colonne <= 'E'; colonne++) {
                saisies.append(colonne).append(ligne).append('\n');
            }
        }
        saisies.append("n\n3\n4\n");
        String sortie = jouer(saisies.toString(), 11);
        assertTrue(sortie.contains("*** Victoire") || sortie.contains("*** Défaite"), sortie);
        assertTrue(sortie.contains("Vous : ") && sortie.contains(" tirs, précision "), sortie);
        assertTrue(sortie.contains("Flotte ennemie dévoilée"), sortie);
        assertTrue(sortie.contains("Vos statistiques : 1 partie, "), sortie);
        assertTrue(sortie.contains("  Parties contre l'ordinateur : 1"), sortie);
        assertTrue(sortie.contains("  Difficulté facile "), sortie);
        assertTrue(sortie.contains("Au revoir !"), sortie);
    }

    @Test
    void placeLesBateauxALaMainEnExpliquantLesRefus() {
        String sortie = jouer(CONTRE_ORDINATEUR_5 + "n\no\nannuler\nA5 H\nannuler\nC1 H\nB1 H\nB1 V\nA3 V\nC3H\nq\n",
                5);
        assertTrue(sortie.contains("Aucun bateau à reprendre."), sortie);
        assertTrue(sortie.contains("Croiseur retiré : placez-le à nouveau."), sortie);
        assertTrue(sortie.contains("Le croiseur déborderait de la grille (A1 à E5)."), sortie);
        assertTrue(sortie.contains("Le sous-marin chevaucherait le croiseur."), sortie);
        assertTrue(sortie.contains("Votre flotte est prête"), sortie);
        assertTrue(sortie.contains("  1  ~ # # # #"), sortie);
        assertTrue(sortie.contains("  3  # ~ # # ~"), sortie);
        assertTrue(sortie.contains("Partie interrompue"), sortie);
    }

    @Test
    void placeLeResteDeLaFlotteAuHasard() {
        String sortie = jouer(CONTRE_ORDINATEUR_5 + "n\no\nB1 H\nhasard\nq\n", 6);
        assertTrue(sortie.contains("Votre flotte est prête"), sortie);
        assertFalse(sortie.contains("Tapez une case puis H ou V"), sortie);
    }

    @Test
    void utiliseLesCapacitesSpeciales() {
        String sortie = jouer(CONTRE_ORDINATEUR_5 + "o\nn\naide\nbombe\nradar C3\nradar C3\ntorpille 9\ndouble\nA1\n"
                + "bombe D4\nA2\nq\n", 7);
        assertTrue(sortie.contains("  torpille 5   lancer une torpille sur la ligne 5, depuis la gauche"), sortie);
        assertTrue(sortie.contains("Précisez la case visée, par exemple « bombe C3 »."), sortie);
        assertTrue(sortie.contains("Vous passez le radar en C3 : "), sortie);
        assertTrue(sortie.contains("Radar en C3 : "), sortie);
        assertTrue(sortie.contains("Capacité déjà utilisée : radar."), sortie);
        assertTrue(sortie.contains("A9 est hors de la grille (A1 à E5)."), sortie);
        assertTrue(sortie.contains("Vous activez le double tir : tirez deux fois ce tour-ci."), sortie);
        assertTrue(sortie.contains("Second tir (par exemple B7) : "), sortie);
        assertTrue(sortie.contains("Une seule capacité par tour, à utiliser avant de tirer."), sortie);
        assertTrue(sortie.contains("Capacités : bombardement, torpille (« aide » pour les utiliser)."), sortie);
    }

    @Test
    void aDeuxLEcranDefileEntreLesTours() {
        String sortie = jouer("2\nAlice\nalice\nBob\n5\n2\nn\nn\n\nA1\n\n\nB2\n\n\nq\n5\n", 8);
        assertTrue(sortie.contains("Les deux joueurs doivent avoir des noms différents."), sortie);
        assertTrue(sortie.contains("La partie commence : Alice contre Bob. Alice tire en premier."), sortie);
        assertTrue(sortie.contains("=== Au tour de Bob ==="), sortie);
        assertTrue(sortie.contains("Flotte d'Alice"), sortie);
        assertTrue(sortie.contains("Bob, appuyez sur Entrée pour afficher vos grilles. Alice tire en A1 : "), sortie);
        assertTrue(sortie.contains("Alice, appuyez sur Entrée pour afficher vos grilles. Bob tire en B2 : "), sortie);
        assertTrue(sortie.contains(System.lineSeparator().repeat(50) + "=== Au tour"), sortie);
        assertTrue(sortie.contains("3. Reprendre la partie interrompue (Alice contre Bob, 2 coups joués)"), sortie);
    }

    @Test
    void reprendLaPartieInterrompue() {
        Path dossier = nouveauDossier();
        jouer(CONTRE_ORDINATEUR_5 + SANS_CAPACITES + "A1\nq\n5\n", 9, dossier);
        String sortie = jouer("\nA1\nq\n5\n", 10, dossier);
        assertTrue(sortie.contains("Votre choix [3] : "), sortie);
        assertTrue(sortie.contains("Reprise de la partie : Joueur contre l'ordinateur, 2 coups joués."), sortie);
        assertTrue(sortie.contains("L'ordinateur tire en "), sortie);
        assertTrue(sortie.contains("Vous avez déjà tiré en A1."), sortie);
    }

    @Test
    void proposeLesReglagesDeLaPartiePrecedente() {
        Path dossier = nouveauDossier();
        jouer("1\nAlice\n3\n6\n2\no\nn\nq\n5\n", 12, dossier);
        String sortie = jouer("1\n", 13, dossier);
        assertTrue(sortie.contains("Votre nom [Alice] : "), sortie);
        sortie = jouer("1\n\n\n\n\n", 13, dossier);
        assertTrue(sortie.contains("Votre choix [3] : Taille de la grille, de 5 à 26 [6] : "), sortie);
        assertTrue(sortie.contains("Capacités spéciales (radar, bombardement, torpille, double tir) ? (O/n) : "),
                sortie);
    }

    @Test
    void afficheDeuxGrillesAligneesAvecLeurLegende() {
        Plateau plateau = new Plateau(5);
        plateau.placer(TypeBateau.TORPILLEUR, Coordonnee.depuis("A1"), Orientation.HORIZONTALE);
        plateau.recevoirTir(Coordonnee.depuis("A1"));
        plateau.recevoirTir(Coordonnee.depuis("C3"));
        plateau.sonder(Coordonnee.depuis("D5"));
        String grilles = RenduConsole.deuxGrilles(plateau, "Votre flotte", plateau.getVueAdverse(), "Vue adverse");
        String[] lignes = grilles.split(System.lineSeparator());
        assertEquals(7, lignes.length);
        assertTrue(lignes[2].startsWith("  1  X # ~ ~ ~"), lignes[2]);
        assertTrue(lignes[2].endsWith("  1  X ~ ~ ~ ~"), lignes[2]);
        assertTrue(lignes[4].endsWith("  3  ~ ~ o ~ ~"), lignes[4]);
        assertTrue(lignes[6].endsWith("  5  ~ ~ - - -"), lignes[6]);
        assertEquals(lignes[1].indexOf('A', 10), lignes[2].indexOf('X', 10));
    }

    @Test
    void resumeLEtatDesFlottesEtDesRadars() {
        List<TypeBateau> flotte = List.of(TypeBateau.CROISEUR, TypeBateau.SOUS_MARIN, TypeBateau.TORPILLEUR);
        assertEquals("Votre flotte : 3 bateaux à flot sur 3.",
                RenduConsole.suiviFlotte("Votre flotte", flotte, flotte));
        assertEquals("Flotte de Bob : 1 bateau à flot sur 3 (coulés : croiseur, torpilleur).",
                RenduConsole.suiviFlotte("Flotte de Bob", flotte, List.of(TypeBateau.SOUS_MARIN)));

        Plateau plateau = new Plateau(5);
        plateau.placer(TypeBateau.TORPILLEUR, Coordonnee.depuis("B2"), Orientation.HORIZONTALE);
        plateau.sonder(Coordonnee.depuis("B2"));
        assertEquals("Radar en B2 : 2 cases de bateaux, toutes encore à trouver.",
                RenduConsole.radar(plateau.getDetections().get(0), plateau.getVueAdverse()).orElseThrow());
        plateau.recevoirTir(Coordonnee.depuis("B2"));
        assertEquals("Radar en B2 : 2 cases de bateaux, 1 encore à trouver.",
                RenduConsole.radar(plateau.getDetections().get(0), plateau.getVueAdverse()).orElseThrow());
        plateau.sonder(Coordonnee.depuis("E5"));
        assertTrue(RenduConsole.radar(plateau.getDetections().get(1), plateau.getVueAdverse()).isEmpty());
    }
}
