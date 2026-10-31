package bataille.console;

import bataille.modele.Coordonnee;
import bataille.modele.Ordre;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lit les ordres tapés pendant la partie : une case pour tirer, ou une capacité spéciale
 * (« radar E5 », « bombe C3 », « torpille 5 », « torpille E », « double »).
 */
final class Commandes {
    /** Un mot de capacité, suivi ou non de sa cible. */
    private static final Pattern CAPACITE =
            Pattern.compile("(radar|bombe|bombardement|torpille|double)(?:\\s+(.*))?");
    /** L'aide affichée par la commande « aide ». */
    static final List<String> AIDE = List.of(
            "  B7           tirer en B7",
            "  radar E5     compter les cases de bateaux du carré de 3 × 3 centré en E5",
            "  bombe C3     tirer sur le carré de 2 × 2 dont C3 est le coin en haut à gauche",
            "  torpille 5   lancer une torpille sur la ligne 5, depuis la gauche",
            "  torpille E   lancer une torpille sur la colonne E, depuis le haut",
            "  double       tirer deux fois ce tour-ci",
            "  Q            interrompre la partie");

    private Commandes() {
    }

    /**
     * Lit un ordre.
     *
     * @param saisie le texte tapé par le joueur
     * @return l'ordre ; sa cible peut être hors de la grille, ce que la partie vérifiera
     * @throws IllegalArgumentException si le texte n'est ni une case ni une capacité, avec un message qui l'explique
     */
    static Ordre lire(String saisie) {
        String texte = saisie.strip().toLowerCase(Locale.ROOT);
        Matcher capacite = CAPACITE.matcher(texte);
        if (!capacite.matches()) {
            return Ordre.tir(Coordonnee.depuis(texte));
        }
        String cible = capacite.group(2) == null ? "" : capacite.group(2).strip();
        return switch (capacite.group(1)) {
            case "radar" -> Ordre.radar(caseVisee(cible, "radar E5"));
            case "bombe", "bombardement" -> Ordre.bombardement(caseVisee(cible, "bombe C3"));
            case "torpille" -> torpille(cible);
            default -> {
                if (!cible.isEmpty() && !cible.equals("tir")) {
                    throw new IllegalArgumentException("Le double tir n'a pas de cible : tapez simplement « double ».");
                }
                yield Ordre.doubleTir();
            }
        };
    }

    private static Coordonnee caseVisee(String cible, String exemple) {
        if (cible.isEmpty()) {
            throw new IllegalArgumentException("Précisez la case visée, par exemple « " + exemple + " ».");
        }
        return Coordonnee.depuis(cible);
    }

    private static Ordre torpille(String cible) {
        if (cible.matches("\\d{1,3}")) {
            return Ordre.torpilleSurLigne(Integer.parseInt(cible) - 1);
        }
        if (cible.matches("[a-z]")) {
            return Ordre.torpilleSurColonne(cible.charAt(0) - 'a');
        }
        throw new IllegalArgumentException(
                "Précisez une ligne ou une colonne, par exemple « torpille 5 » ou « torpille E ».");
    }
}
