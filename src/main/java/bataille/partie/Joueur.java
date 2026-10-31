package bataille.partie;

import bataille.ia.StrategieTir;
import bataille.modele.Coordonnee;
import bataille.modele.Plateau;
import bataille.modele.VuePlateau;
import java.util.Objects;

/**
 * Un joueur : son nom, son plateau et, pour l'ordinateur, sa stratégie de tir.
 * <p>
 * Les tirs d'un joueur humain viennent de l'interface (console ou fenêtre) ; ceux de l'ordinateur,
 * de sa stratégie.
 */
public final class Joueur {
    /** La longueur maximale d'un nom, pour l'affichage. */
    public static final int LONGUEUR_NOM_MAX = 20;

    /** Le nom affiché du joueur. */
    private final String nom;
    /** Le plateau qui porte la flotte du joueur. */
    private final Plateau plateau;
    /** La stratégie de l'ordinateur, ou {@code null} pour un joueur humain. */
    private final StrategieTir strategie;

    private Joueur(String nom, Plateau plateau, StrategieTir strategie) {
        Objects.requireNonNull(nom, "nom");
        String nomNettoye = nom.strip();
        if (nomNettoye.isEmpty()) {
            throw new IllegalArgumentException("Le nom du joueur ne peut pas être vide.");
        }
        if (nomNettoye.length() > LONGUEUR_NOM_MAX) {
            nomNettoye = nomNettoye.substring(0, LONGUEUR_NOM_MAX);
        }
        this.nom = nomNettoye;
        this.plateau = Objects.requireNonNull(plateau, "plateau");
        this.strategie = strategie;
    }

    /**
     * Crée un joueur humain.
     *
     * @param nom le nom du joueur, coupé à {@value #LONGUEUR_NOM_MAX} caractères
     * @param plateau le plateau du joueur
     * @return le joueur
     */
    public static Joueur humain(String nom, Plateau plateau) {
        return new Joueur(nom, plateau, null);
    }

    /**
     * Crée un joueur ordinateur.
     *
     * @param nom le nom du joueur
     * @param plateau le plateau du joueur
     * @param strategie la manière dont il choisit ses tirs
     * @return le joueur
     */
    public static Joueur ordinateur(String nom, Plateau plateau, StrategieTir strategie) {
        return new Joueur(nom, plateau, Objects.requireNonNull(strategie, "strategie"));
    }

    /**
     * Retourne le nom du joueur.
     *
     * @return le nom du joueur
     */
    public String getNom() {
        return nom;
    }

    /**
     * Retourne le plateau du joueur.
     *
     * @return le plateau qui porte sa flotte
     */
    public Plateau getPlateau() {
        return plateau;
    }

    /**
     * Indique si le joueur est humain.
     *
     * @return si ses tirs viennent de l'interface plutôt que d'une stratégie
     */
    public boolean estHumain() {
        return strategie == null;
    }

    /**
     * Demande à la stratégie de l'ordinateur la prochaine case à viser.
     *
     * @param adversaire la vue du plateau adverse
     * @return la case choisie
     * @throws IllegalStateException si le joueur est humain
     */
    public Coordonnee choisirTir(VuePlateau adversaire) {
        if (strategie == null) {
            throw new IllegalStateException(nom + " est un joueur humain : ses tirs viennent de l'interface.");
        }
        return strategie.choisirTir(adversaire);
    }

    /**
     * Retourne le nom du joueur.
     *
     * @return le nom du joueur
     */
    @Override
    public String toString() {
        return nom;
    }
}
