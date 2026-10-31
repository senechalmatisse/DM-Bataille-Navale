package bataille.partie;

import bataille.modele.ConfigurationPartie;
import bataille.modele.Coordonnee;
import bataille.modele.Plateau;
import bataille.modele.ResultatTir;
import bataille.modele.Tir;
import bataille.modele.TypeBateau;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Le moteur d'une partie : le seul endroit où vivent les règles.
 * <p>
 * Les deux joueurs tirent à tour de rôle sur le plateau adverse. Un tir sur une case déjà visée ne compte pas
 * et le joueur rejoue. La partie s'arrête au tir qui coule le dernier bateau adverse : son auteur gagne,
 * et plus aucun tir n'est accepté.
 * <p>
 * La console et la fenêtre Swing utilisent ce même moteur : elles ne font qu'afficher et transmettre les tirs.
 */
public final class Partie {
    /** Les règles de la partie. */
    private final ConfigurationPartie configuration;
    /** Le joueur qui commence. */
    private final Joueur joueur1;
    /** Le second joueur. */
    private final Joueur joueur2;
    /** Le joueur dont c'est le tour. */
    private Joueur joueurCourant;
    /** Le gagnant, ou {@code null} tant que la partie continue. */
    private Joueur gagnant;
    /** Les coups joués, dans l'ordre. */
    private final List<Coup> historique = new ArrayList<>();

    /**
     * Crée une partie entre deux joueurs dont les flottes sont déjà placées.
     *
     * @param configuration les règles de la partie
     * @param joueur1 le joueur qui commence
     * @param joueur2 le second joueur
     * @throws IllegalArgumentException si un plateau n'a pas la taille ou la flotte prévues
     */
    public Partie(ConfigurationPartie configuration, Joueur joueur1, Joueur joueur2) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.joueur1 = Objects.requireNonNull(joueur1, "joueur1");
        this.joueur2 = Objects.requireNonNull(joueur2, "joueur2");
        if (joueur1 == joueur2 || joueur1.getPlateau() == joueur2.getPlateau()) {
            throw new IllegalArgumentException("Les deux joueurs doivent avoir chacun leur plateau.");
        }
        verifierPlateau(joueur1);
        verifierPlateau(joueur2);
        this.joueurCourant = joueur1;
    }

    private void verifierPlateau(Joueur joueur) {
        Plateau plateau = joueur.getPlateau();
        if (plateau.getTaille() != configuration.taille()) {
            throw new IllegalArgumentException("Le plateau de " + joueur.getNom() + " fait " + plateau.getTaille()
                    + " cases de côté au lieu de " + configuration.taille() + ".");
        }
        if (plateau.getNombreTirsRecus() > 0) {
            throw new IllegalArgumentException("Le plateau de " + joueur.getNom() + " a déjà reçu des tirs.");
        }
        List<TypeBateau> attendus = new ArrayList<>(configuration.flotte());
        List<TypeBateau> poses = new ArrayList<>(plateau.getTypes());
        Collections.sort(attendus);
        Collections.sort(poses);
        if (!attendus.equals(poses)) {
            throw new IllegalArgumentException("La flotte de " + joueur.getNom() + " ne correspond pas aux règles : "
                    + poses + " au lieu de " + attendus + ".");
        }
    }

    /**
     * Retourne les règles de la partie.
     *
     * @return la configuration
     */
    public ConfigurationPartie getConfiguration() {
        return configuration;
    }

    /**
     * Retourne le joueur qui a commencé.
     *
     * @return le joueur 1
     */
    public Joueur getJoueur1() {
        return joueur1;
    }

    /**
     * Retourne le second joueur.
     *
     * @return le joueur 2
     */
    public Joueur getJoueur2() {
        return joueur2;
    }

    /**
     * Retourne le joueur dont c'est le tour. Une fois la partie terminée, c'est le gagnant.
     *
     * @return le joueur courant
     */
    public Joueur getJoueurCourant() {
        return joueurCourant;
    }

    /**
     * Retourne l'adversaire d'un joueur de la partie.
     *
     * @param joueur un des deux joueurs
     * @return l'autre joueur
     */
    public Joueur adversaireDe(Joueur joueur) {
        if (joueur == joueur1) {
            return joueur2;
        }
        if (joueur == joueur2) {
            return joueur1;
        }
        throw new IllegalArgumentException(joueur + " ne participe pas à cette partie.");
    }

    /**
     * Fait tirer le joueur courant sur une case du plateau adverse.
     * <p>
     * Si la case avait déjà été visée, le tir ne compte pas ({@link ResultatTir#DEJA_JOUE}) et le joueur rejoue.
     * Sinon, la main passe à l'adversaire, sauf si ce tir coule le dernier bateau adverse :
     * la partie est alors terminée et le tireur l'emporte.
     *
     * @param cible la case visée
     * @return le compte rendu du tir
     * @throws IllegalArgumentException si la case est hors de la grille
     * @throws IllegalStateException si la partie est terminée
     */
    public Tir tirer(Coordonnee cible) {
        if (gagnant != null) {
            throw new IllegalStateException("La partie est terminée : " + gagnant.getNom() + " a gagné.");
        }
        Joueur tireur = joueurCourant;
        Joueur vise = adversaireDe(tireur);
        Tir tir = vise.getPlateau().recevoirTir(cible);
        if (tir.resultat() == ResultatTir.DEJA_JOUE) {
            return tir;
        }
        historique.add(new Coup(tireur, tir));
        if (vise.getPlateau().flotteCoulee()) {
            gagnant = tireur;
        } else {
            joueurCourant = vise;
        }
        return tir;
    }

    /**
     * Fait jouer l'ordinateur dont c'est le tour, selon sa stratégie.
     *
     * @return le compte rendu du tir
     * @throws IllegalStateException si c'est au tour d'un joueur humain, si la partie est terminée,
     *         ou si la stratégie vise une case déjà jouée
     */
    public Tir faireJouerOrdinateur() {
        if (joueurCourant.estHumain()) {
            throw new IllegalStateException("C'est au tour de " + joueurCourant.getNom() + ", un joueur humain.");
        }
        Coordonnee cible = joueurCourant.choisirTir(adversaireDe(joueurCourant).getPlateau().getVueAdverse());
        Joueur tireur = joueurCourant;
        Tir tir = tirer(cible);
        if (tir.resultat() == ResultatTir.DEJA_JOUE) {
            throw new IllegalStateException("La stratégie de " + tireur.getNom() + " a visé " + cible
                    + ", une case déjà jouée.");
        }
        return tir;
    }

    /**
     * Indique si la partie est terminée.
     *
     * @return si une flotte a été entièrement coulée
     */
    public boolean estTerminee() {
        return gagnant != null;
    }

    /**
     * Retourne le gagnant.
     *
     * @return le gagnant, ou une valeur vide tant que la partie continue
     */
    public Optional<Joueur> getGagnant() {
        return Optional.ofNullable(gagnant);
    }

    /**
     * Retourne les coups joués.
     *
     * @return les coups, dans l'ordre, dans une liste non modifiable
     */
    public List<Coup> getHistorique() {
        return Collections.unmodifiableList(historique);
    }

    /**
     * Retourne le bilan des tirs d'un joueur.
     *
     * @param joueur un des deux joueurs
     * @return le nombre de tirs joués et de tirs au but
     */
    public Statistiques getStatistiques(Joueur joueur) {
        adversaireDe(joueur);
        int tirs = 0;
        int tirsAuBut = 0;
        for (Coup coup : historique) {
            if (coup.tireur() == joueur) {
                tirs++;
                if (coup.tir().resultat().estAuBut()) {
                    tirsAuBut++;
                }
            }
        }
        return new Statistiques(tirs, tirsAuBut);
    }
}
