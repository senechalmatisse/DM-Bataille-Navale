package bataille.partie;

import bataille.ia.StrategieTir;
import bataille.ia.Tacticien;
import bataille.modele.Capacite;
import bataille.modele.Coordonnee;
import bataille.modele.Ordre;
import bataille.modele.Plateau;
import bataille.modele.VuePlateau;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Un joueur : son nom, son plateau et, pour l'ordinateur, sa stratégie de tir et son usage des capacités.
 * <p>
 * Les ordres d'un joueur humain viennent de l'interface (console ou fenêtre) ; ceux de l'ordinateur,
 * de sa stratégie et de son tacticien.
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
    /** Le choix des capacités de l'ordinateur, ou {@code null} s'il ne s'en sert pas. */
    private final Tacticien tacticien;

    private Joueur(String nom, Plateau plateau, StrategieTir strategie, Tacticien tacticien) {
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
        this.tacticien = tacticien;
    }

    /**
     * Crée un joueur humain.
     *
     * @param nom le nom du joueur, coupé à {@value #LONGUEUR_NOM_MAX} caractères
     * @param plateau le plateau du joueur
     * @return le joueur
     */
    public static Joueur humain(String nom, Plateau plateau) {
        return new Joueur(nom, plateau, null, null);
    }

    /**
     * Crée un joueur ordinateur qui n'utilise pas les capacités spéciales.
     *
     * @param nom le nom du joueur
     * @param plateau le plateau du joueur
     * @param strategie la manière dont il choisit ses tirs
     * @return le joueur
     */
    public static Joueur ordinateur(String nom, Plateau plateau, StrategieTir strategie) {
        return new Joueur(nom, plateau, Objects.requireNonNull(strategie, "strategie"), null);
    }

    /**
     * Crée un joueur ordinateur.
     *
     * @param nom le nom du joueur
     * @param plateau le plateau du joueur
     * @param strategie la manière dont il choisit ses tirs
     * @param tacticien la manière dont il utilise les capacités, ou {@code null} s'il ne s'en sert pas
     * @return le joueur
     */
    public static Joueur ordinateur(String nom, Plateau plateau, StrategieTir strategie, Tacticien tacticien) {
        return new Joueur(nom, plateau, Objects.requireNonNull(strategie, "strategie"), tacticien);
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
     * @return si ses ordres viennent de l'interface plutôt que d'une stratégie
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
     * Demande au tacticien de l'ordinateur s'il veut utiliser une capacité ce tour-ci.
     *
     * @param adversaire la vue du plateau adverse
     * @param disponibles les capacités utilisables maintenant
     * @param tour le numéro du tour de ce joueur, à partir de 1
     * @return l'ordre de la capacité choisie, ou une valeur vide pour un tir simple
     */
    public Optional<Ordre> choisirCapacite(VuePlateau adversaire, Set<Capacite> disponibles, int tour) {
        if (tacticien == null || disponibles.isEmpty()) {
            return Optional.empty();
        }
        return tacticien.choisir(adversaire, disponibles, tour);
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
