package bataille.partie;

import bataille.modele.Ordre;
import bataille.modele.Plateau;
import bataille.modele.PositionBateau;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Tout ce qu'il faut pour reconstruire une partie : ses paramètres, la place des bateaux au départ et les ordres
 * joués, dans l'ordre. Sert à la sauvegarde, à la reprise et au replay.
 *
 * @param parametres les paramètres de la partie
 * @param flotte1 la place des bateaux du joueur 1 au départ
 * @param flotte2 la place des bateaux du joueur 2 au départ
 * @param ordres les ordres joués, dans l'ordre, chacun avec le numéro (1 ou 2) de son auteur
 */
public record EnregistrementPartie(ParametresPartie parametres, List<PositionBateau> flotte1,
        List<PositionBateau> flotte2, List<OrdreJoue> ordres) {

    /**
     * Vérifie que l'enregistrement est complet.
     *
     * @param parametres les paramètres de la partie
     * @param flotte1 la flotte du joueur 1
     * @param flotte2 la flotte du joueur 2
     * @param ordres les ordres joués
     */
    public EnregistrementPartie {
        Objects.requireNonNull(parametres, "parametres");
        flotte1 = List.copyOf(flotte1);
        flotte2 = List.copyOf(flotte2);
        ordres = List.copyOf(ordres);
    }

    /**
     * Enregistre l'état d'une partie : la place de départ des bateaux et tous les coups joués.
     *
     * @param partie la partie
     * @param parametres les paramètres avec lesquels elle a été créée
     * @return l'enregistrement
     */
    public static EnregistrementPartie de(Partie partie, ParametresPartie parametres) {
        List<OrdreJoue> ordres = partie.getHistorique().stream()
                .map(coup -> new OrdreJoue(coup.auteur() == partie.getJoueur1() ? 1 : 2, coup.ordre()))
                .toList();
        return new EnregistrementPartie(parametres, positions(partie.getJoueur1()), positions(partie.getJoueur2()),
                ordres);
    }

    private static List<PositionBateau> positions(Joueur joueur) {
        return joueur.getPlateau().getBateaux().stream().map(PositionBateau::de).toList();
    }

    /**
     * Reconstruit la partie et rejoue tous ses ordres.
     *
     * @param random le générateur de hasard de l'ordinateur, pour la suite de la partie
     * @return la partie, dans l'état où elle a été enregistrée
     * @throws IllegalArgumentException si l'enregistrement ne respecte pas les règles
     */
    public Partie restaurer(Random random) {
        return restaurer(ordres.size(), random);
    }

    /**
     * Reconstruit la partie et rejoue ses premiers ordres seulement.
     *
     * @param nombreOrdres le nombre d'ordres à rejouer, entre 0 et le nombre d'ordres enregistrés
     * @param random le générateur de hasard de l'ordinateur
     * @return la partie après ces ordres
     * @throws IllegalArgumentException si l'enregistrement ne respecte pas les règles
     */
    public Partie restaurer(int nombreOrdres, Random random) {
        if (nombreOrdres < 0 || nombreOrdres > ordres.size()) {
            throw new IllegalArgumentException("Nombre d'ordres hors limites : " + nombreOrdres);
        }
        Plateau plateau1 = new Plateau(parametres.configuration().taille());
        Plateau plateau2 = new Plateau(parametres.configuration().taille());
        flotte1.forEach(position -> position.placerSur(plateau1));
        flotte2.forEach(position -> position.placerSur(plateau2));
        Partie partie = parametres.creerPartie(plateau1, plateau2, random);
        for (int i = 0; i < nombreOrdres; i++) {
            OrdreJoue ordreJoue = ordres.get(i);
            Joueur attendu = ordreJoue.joueur() == 1 ? partie.getJoueur1() : partie.getJoueur2();
            if (partie.getJoueurCourant() != attendu || partie.estTerminee()) {
                throw new IllegalArgumentException("Ordre n° " + (i + 1) + " joué hors de son tour : "
                        + ordreJoue.ordre().enTexte());
            }
            try {
                partie.jouer(ordreJoue.ordre());
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException("Ordre n° " + (i + 1) + " impossible : " + e.getMessage(), e);
            }
        }
        return partie;
    }

    /**
     * Indique si la partie enregistrée est terminée, en la rejouant.
     *
     * @return vrai si une flotte a été entièrement coulée
     * @throws IllegalArgumentException si l'enregistrement ne respecte pas les règles
     */
    public boolean estTerminee() {
        return restaurer(new Random(0)).estTerminee();
    }

    /**
     * Décrit la partie en quelques mots, par exemple « Alice contre l'ordinateur, 12 coups joués ».
     *
     * @return la description
     */
    public String decrire() {
        int coups = ordres.size();
        return parametres.nomJoueur1() + " contre " + (parametres.contreLOrdinateur() ? "l'ordinateur"
                : parametres.nomJoueur2()) + ", " + coups + (coups > 1 ? " coups joués" : " coup joué");
    }

    /**
     * Un ordre enregistré, avec le numéro de son auteur.
     *
     * @param joueur 1 pour le joueur qui commence, 2 pour l'autre
     * @param ordre l'ordre joué
     */
    public record OrdreJoue(int joueur, Ordre ordre) {

        /**
         * Vérifie l'ordre enregistré.
         *
         * @param joueur 1 ou 2
         * @param ordre l'ordre joué
         */
        public OrdreJoue {
            if (joueur != 1 && joueur != 2) {
                throw new IllegalArgumentException("Le joueur d'un ordre est 1 ou 2, et non " + joueur + ".");
            }
            Objects.requireNonNull(ordre, "ordre");
        }
    }
}
