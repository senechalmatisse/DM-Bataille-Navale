package bataille.modele;

import java.util.Locale;
import java.util.Objects;

/**
 * Ce qu'un joueur décide de faire à son tour : un tir simple ou une capacité spéciale.
 * <p>
 * Un ordre s'écrit aussi en texte, pour les sauvegardes : « TIR B7 », « RADAR E5 », « BOMBARDEMENT C3 »,
 * « TORPILLE A5 H » (la ligne 5, depuis le bord gauche), « TORPILLE E1 V » (la colonne E, depuis le haut)
 * et « DOUBLE_TIR ».
 *
 * @param capacite la capacité utilisée, ou {@code null} pour un tir simple
 * @param cible la case visée : la case du tir, le centre du radar, le coin haut gauche du bombardement,
 *              le départ de la torpille ; {@code null} pour le double tir
 * @param orientation le sens de la torpille, {@code null} pour les autres ordres
 */
public record Ordre(Capacite capacite, Coordonnee cible, Orientation orientation) {

    /**
     * Vérifie que l'ordre est complet.
     *
     * @param capacite la capacité utilisée, ou {@code null} pour un tir simple
     * @param cible la case visée, {@code null} pour le double tir
     * @param orientation le sens de la torpille, {@code null} pour les autres ordres
     */
    public Ordre {
        boolean ciblee = capacite == null || capacite.aUneCible();
        if (ciblee != (cible != null)) {
            throw new IllegalArgumentException("Un tir et chaque capacité, sauf le double tir, visent une case.");
        }
        if ((capacite == Capacite.TORPILLE) != (orientation != null)) {
            throw new IllegalArgumentException("Seule la torpille a une orientation.");
        }
    }

    /**
     * Crée un tir simple.
     *
     * @param cible la case visée
     * @return l'ordre
     */
    public static Ordre tir(Coordonnee cible) {
        return new Ordre(null, Objects.requireNonNull(cible, "cible"), null);
    }

    /**
     * Crée un radar centré sur une case.
     *
     * @param centre le centre du carré de 3 × 3
     * @return l'ordre
     */
    public static Ordre radar(Coordonnee centre) {
        return new Ordre(Capacite.RADAR, Objects.requireNonNull(centre, "centre"), null);
    }

    /**
     * Crée un bombardement.
     *
     * @param coinHautGauche le coin haut gauche du carré de 2 × 2
     * @return l'ordre
     */
    public static Ordre bombardement(Coordonnee coinHautGauche) {
        return new Ordre(Capacite.BOMBARDEMENT, Objects.requireNonNull(coinHautGauche, "coinHautGauche"), null);
    }

    /**
     * Crée une torpille qui suit une ligne depuis le bord gauche.
     *
     * @param ligne la ligne, à partir de 0
     * @return l'ordre
     */
    public static Ordre torpilleSurLigne(int ligne) {
        return new Ordre(Capacite.TORPILLE, new Coordonnee(ligne, 0), Orientation.HORIZONTALE);
    }

    /**
     * Crée une torpille qui suit une colonne depuis le bord haut.
     *
     * @param colonne la colonne, à partir de 0
     * @return l'ordre
     */
    public static Ordre torpilleSurColonne(int colonne) {
        return new Ordre(Capacite.TORPILLE, new Coordonnee(0, colonne), Orientation.VERTICALE);
    }

    /**
     * Crée un double tir.
     *
     * @return l'ordre
     */
    public static Ordre doubleTir() {
        return new Ordre(Capacite.DOUBLE_TIR, null, null);
    }

    /**
     * Indique si l'ordre est un tir simple.
     *
     * @return vrai si aucune capacité n'est utilisée
     */
    public boolean estUnTirSimple() {
        return capacite == null;
    }

    /**
     * Décrit l'ordre pour un message, par exemple « tir en B7 » ou « torpille sur la colonne E ».
     *
     * @return la description
     */
    public String decrire() {
        if (capacite == null) {
            return "tir en " + cible;
        }
        return switch (capacite) {
            case RADAR -> "radar en " + cible;
            case BOMBARDEMENT -> "bombardement en " + cible;
            case TORPILLE -> orientation == Orientation.HORIZONTALE
                    ? "torpille sur la ligne " + (cible.ligne() + 1)
                    : "torpille sur la colonne " + Coordonnee.lettreColonne(cible.colonne());
            case DOUBLE_TIR -> "double tir";
        };
    }

    /**
     * Écrit l'ordre en texte, pour une sauvegarde.
     *
     * @return le texte, par exemple « TIR B7 »
     */
    public String enTexte() {
        if (capacite == null) {
            return "TIR " + cible;
        }
        if (capacite == Capacite.DOUBLE_TIR) {
            return capacite.name();
        }
        String texte = capacite.name() + " " + cible;
        return orientation == null ? texte : texte + " " + orientation.getLettre();
    }

    /**
     * Lit un ordre écrit par {@link #enTexte()}.
     *
     * @param texte le texte à lire
     * @return l'ordre
     * @throws IllegalArgumentException si le texte n'est pas un ordre
     */
    public static Ordre depuisTexte(String texte) {
        String[] mots = texte.strip().toUpperCase(Locale.ROOT).split("\\s+");
        try {
            if (mots[0].equals("TIR") && mots.length == 2) {
                return tir(Coordonnee.depuis(mots[1]));
            }
            Capacite capacite = Capacite.valueOf(mots[0]);
            if (capacite == Capacite.DOUBLE_TIR && mots.length == 1) {
                return doubleTir();
            }
            if (capacite == Capacite.TORPILLE && mots.length == 3) {
                return new Ordre(capacite, Coordonnee.depuis(mots[1]), Orientation.depuis(mots[2]));
            }
            if (mots.length == 2) {
                return new Ordre(capacite, Coordonnee.depuis(mots[1]), null);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ordre illisible : « " + texte + " ».", e);
        }
        throw new IllegalArgumentException("Ordre illisible : « " + texte + " ».");
    }
}
