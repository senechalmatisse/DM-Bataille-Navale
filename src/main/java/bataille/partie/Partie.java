package bataille.partie;

import bataille.modele.Capacite;
import bataille.modele.ConfigurationPartie;
import bataille.modele.Coordonnee;
import bataille.modele.Detection;
import bataille.modele.Ordre;
import bataille.modele.Orientation;
import bataille.modele.Plateau;
import bataille.modele.ResultatTir;
import bataille.modele.Tir;
import bataille.modele.TypeBateau;
import bataille.modele.VuePlateau;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Le moteur d'une partie : le seul endroit où vivent les règles.
 * <p>
 * Les deux joueurs jouent à tour de rôle sur le plateau adverse. À son tour, un joueur tire une fois ; si la règle
 * est activée, il peut d'abord utiliser une capacité spéciale ({@link Capacite}), une seule par tour. Un tir sur une
 * case déjà visée ne compte pas et le joueur rejoue. La partie s'arrête dès que le dernier bateau adverse est coulé :
 * son auteur gagne, et plus aucun ordre n'est accepté.
 * <p>
 * Le moteur émet des {@link EvenementPartie} après chaque changement ; la console et la fenêtre Swing s'en servent
 * pour afficher la partie, sans connaître les règles.
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
    /** Les écouteurs des événements. */
    private final List<EcouteurPartie> ecouteurs = new ArrayList<>();
    /** Les utilisations restantes de chaque capacité, par joueur. */
    private final Map<Joueur, Map<Capacite, Integer>> charges = new IdentityHashMap<>();
    /** Le numéro du tour en cours, à partir de 1 ; chaque joueur a ses propres tours. */
    private int numeroTour = 1;
    /** Le nombre de tirs simples encore permis ce tour-ci : 1, ou 2 après un double tir. */
    private int tirsRestantsCeTour = 1;
    /** Le nombre de tirs simples déjà joués ce tour-ci. */
    private int tirsCeTour;
    /** Si une capacité a déjà été utilisée ce tour-ci. */
    private boolean capaciteUtiliseeCeTour;

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
        int utilisations = configuration.capacites() ? Capacite.UTILISATIONS_PAR_PARTIE : 0;
        for (Joueur joueur : List.of(joueur1, joueur2)) {
            Map<Capacite, Integer> parCapacite = new EnumMap<>(Capacite.class);
            for (Capacite capacite : Capacite.values()) {
                parCapacite.put(capacite, utilisations);
            }
            charges.put(joueur, parCapacite);
        }
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
     * Abonne un écouteur aux événements de la partie.
     *
     * @param ecouteur l'écouteur à prévenir
     */
    public void ajouterEcouteur(EcouteurPartie ecouteur) {
        ecouteurs.add(Objects.requireNonNull(ecouteur, "ecouteur"));
    }

    /**
     * Désabonne un écouteur.
     *
     * @param ecouteur l'écouteur à ne plus prévenir
     */
    public void retirerEcouteur(EcouteurPartie ecouteur) {
        ecouteurs.remove(ecouteur);
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
     * Retourne le numéro du tour en cours. Chaque joueur a ses propres tours : le joueur 1 joue les tours impairs.
     *
     * @return le numéro du tour, à partir de 1
     */
    public int getNumeroTour() {
        return numeroTour;
    }

    /**
     * Retourne le nombre de tirs simples encore permis au joueur courant ce tour-ci.
     *
     * @return 1, ou 2 après un double tir
     */
    public int getTirsRestantsCeTour() {
        return tirsRestantsCeTour;
    }

    /**
     * Retourne le nombre d'utilisations restantes d'une capacité pour un joueur.
     *
     * @param joueur un des deux joueurs
     * @param capacite la capacité
     * @return le nombre d'utilisations restantes, 0 si les capacités ne sont pas activées
     */
    public int getCharges(Joueur joueur, Capacite capacite) {
        adversaireDe(joueur);
        return charges.get(joueur).get(capacite);
    }

    /**
     * Retourne les capacités que le joueur courant peut utiliser maintenant : règle activée, utilisation restante,
     * début de tour et aucune autre capacité ce tour-ci.
     *
     * @return les capacités utilisables, vide si aucune
     */
    public Set<Capacite> capacitesDisponibles() {
        Set<Capacite> disponibles = EnumSet.noneOf(Capacite.class);
        if (gagnant != null || capaciteUtiliseeCeTour || tirsCeTour > 0) {
            return disponibles;
        }
        for (Capacite capacite : Capacite.values()) {
            if (charges.get(joueurCourant).get(capacite) > 0) {
                disponibles.add(capacite);
            }
        }
        return disponibles;
    }

    /**
     * Fait tirer le joueur courant sur une case du plateau adverse.
     *
     * @param cible la case visée
     * @return le compte rendu du tir ; {@link ResultatTir#DEJA_JOUE} si la case avait déjà été visée
     * @throws IllegalArgumentException si la case est hors de la grille
     * @throws IllegalStateException si la partie est terminée
     */
    public Tir tirer(Coordonnee cible) {
        return jouer(Ordre.tir(cible)).tirs().get(0);
    }

    /**
     * Joue un ordre du joueur courant : un tir simple ou une capacité spéciale.
     * <p>
     * Un tir sur une case déjà visée ne compte pas ({@link ResultatTir#DEJA_JOUE}) et n'est pas enregistré :
     * le joueur rejoue. Après le dernier tir permis du tour, ou après un radar, un bombardement ou une torpille,
     * la main passe à l'adversaire, sauf si la flotte adverse vient d'être coulée.
     *
     * @param ordre l'ordre à jouer
     * @return le coup joué
     * @throws IllegalArgumentException si la cible est hors de la grille, ou si la zone visée a déjà été entièrement
     *         visée
     * @throws IllegalStateException si la partie est terminée ou si la capacité n'est pas utilisable maintenant
     */
    public Coup jouer(Ordre ordre) {
        Objects.requireNonNull(ordre, "ordre");
        if (gagnant != null) {
            throw new IllegalStateException("La partie est terminée : " + gagnant.getNom() + " a gagné.");
        }
        Joueur auteur = joueurCourant;
        Plateau plateauVise = adversaireDe(auteur).getPlateau();
        if (ordre.estUnTirSimple()) {
            Tir tir = plateauVise.recevoirTir(ordre.cible());
            Coup coup = new Coup(auteur, ordre, List.of(tir), Optional.empty());
            if (tir.resultat() == ResultatTir.DEJA_JOUE) {
                return coup;
            }
            historique.add(coup);
            annoncerTir(auteur, plateauVise, tir);
            tirsCeTour++;
            tirsRestantsCeTour--;
            conclure(auteur, plateauVise, tirsRestantsCeTour == 0);
            return coup;
        }
        Capacite capacite = ordre.capacite();
        verifierCapacite(capacite);
        List<Coordonnee> trajet = trajet(ordre, plateauVise);
        List<Tir> tirs = new ArrayList<>();
        Optional<Detection> detection = Optional.empty();
        switch (capacite) {
            case RADAR -> detection = Optional.of(plateauVise.sonder(ordre.cible()));
            case BOMBARDEMENT -> {
                for (Coordonnee coordonnee : trajet) {
                    if (!plateauVise.flotteCoulee()) {
                        tirs.add(plateauVise.recevoirTir(coordonnee));
                    }
                }
            }
            case TORPILLE -> {
                for (Coordonnee coordonnee : trajet) {
                    Tir tir = plateauVise.recevoirTir(coordonnee);
                    tirs.add(tir);
                    if (tir.resultat().estAuBut()) {
                        break;
                    }
                }
            }
            case DOUBLE_TIR -> tirsRestantsCeTour = 2;
            default -> throw new IllegalStateException("Capacité inconnue : " + capacite);
        }
        charges.get(auteur).merge(capacite, -1, Integer::sum);
        capaciteUtiliseeCeTour = true;
        Coup coup = new Coup(auteur, ordre, tirs, detection);
        historique.add(coup);
        emettre(new EvenementPartie.CapaciteUtilisee(auteur, ordre, detection));
        for (Tir tir : tirs) {
            annoncerTir(auteur, plateauVise, tir);
        }
        if (capacite != Capacite.DOUBLE_TIR) {
            conclure(auteur, plateauVise, true);
        }
        return coup;
    }

    private void verifierCapacite(Capacite capacite) {
        if (!configuration.capacites()) {
            throw new IllegalStateException("Les capacités spéciales ne sont pas activées dans cette partie.");
        }
        if (charges.get(joueurCourant).get(capacite) <= 0) {
            throw new IllegalStateException("Capacité déjà utilisée : "
                    + capacite.getNom().toLowerCase(Locale.ROOT) + ".");
        }
        if (capaciteUtiliseeCeTour || tirsCeTour > 0) {
            throw new IllegalStateException("Une seule capacité par tour, à utiliser avant de tirer.");
        }
    }

    /**
     * Retourne les cases que visera une capacité, en vérifiant la cible sans rien modifier.
     *
     * @param ordre l'ordre de la capacité
     * @param plateau le plateau visé
     * @return les cases encore jamais visées, dans l'ordre où elles seront visées ; vide pour un radar
     *         ou un double tir
     */
    private static List<Coordonnee> trajet(Ordre ordre, Plateau plateau) {
        List<Coordonnee> cases = new ArrayList<>();
        Coordonnee cible = ordre.cible();
        switch (ordre.capacite()) {
            case RADAR -> verifierDansLaGrille(plateau, cible);
            case BOMBARDEMENT -> {
                verifierDansLaGrille(plateau, cible);
                verifierDansLaGrille(plateau, cible.decaler(1, 1));
                for (Coordonnee coordonnee : List.of(cible, cible.decaler(0, 1), cible.decaler(1, 0),
                        cible.decaler(1, 1))) {
                    if (!plateau.estVise(coordonnee)) {
                        cases.add(coordonnee);
                    }
                }
            }
            case TORPILLE -> {
                Orientation sens = ordre.orientation();
                Coordonnee depart = sens == Orientation.HORIZONTALE ? new Coordonnee(cible.ligne(), 0)
                        : new Coordonnee(0, cible.colonne());
                verifierDansLaGrille(plateau, depart);
                for (int i = 0; i < plateau.getTaille(); i++) {
                    Coordonnee coordonnee = depart.decaler(i * sens.getDeltaLigne(), i * sens.getDeltaColonne());
                    if (!plateau.estVise(coordonnee)) {
                        cases.add(coordonnee);
                    }
                }
            }
            case DOUBLE_TIR -> {
                return cases;
            }
            default -> throw new IllegalStateException("Capacité inconnue : " + ordre.capacite());
        }
        if (ordre.capacite() != Capacite.RADAR && cases.isEmpty()) {
            throw new IllegalArgumentException("Toutes ces cases ont déjà été visées (" + ordre.decrire() + ").");
        }
        return cases;
    }

    private static void verifierDansLaGrille(Plateau plateau, Coordonnee coordonnee) {
        if (!plateau.contient(coordonnee)) {
            throw new IllegalArgumentException(coordonnee + " est hors de la grille (" + plateau.decrireEtendue()
                    + ").");
        }
    }

    private void annoncerTir(Joueur tireur, Plateau plateauVise, Tir tir) {
        emettre(new EvenementPartie.TirEffectue(tireur, tir));
        if (tir.resultat() == ResultatTir.COULE) {
            emettre(new EvenementPartie.BateauCoule(tireur, plateauVise.getBateau(tir.cible()).orElseThrow()));
        }
    }

    /**
     * Termine une action : déclare le gagnant si la flotte visée est coulée, sinon passe la main si le tour est fini.
     */
    private void conclure(Joueur auteur, Plateau plateauVise, boolean finDuTour) {
        if (plateauVise.flotteCoulee()) {
            gagnant = auteur;
            emettre(new EvenementPartie.PartieTerminee(auteur));
        } else if (finDuTour) {
            joueurCourant = adversaireDe(joueurCourant);
            numeroTour++;
            tirsRestantsCeTour = 1;
            tirsCeTour = 0;
            capaciteUtiliseeCeTour = false;
            emettre(new EvenementPartie.TourChange(joueurCourant, numeroTour));
        }
    }

    private void emettre(EvenementPartie evenement) {
        for (EcouteurPartie ecouteur : List.copyOf(ecouteurs)) {
            ecouteur.surEvenement(evenement);
        }
    }

    /**
     * Fait jouer l'ordinateur dont c'est le tour : une capacité si son tacticien le décide, sinon un tir choisi
     * par sa stratégie. Un double tir laisse la main à l'ordinateur : appeler à nouveau cette méthode.
     *
     * @return le coup joué
     * @throws IllegalStateException si c'est au tour d'un joueur humain, si la partie est terminée,
     *         ou si la stratégie vise une case déjà jouée
     */
    public Coup faireJouerOrdinateur() {
        if (joueurCourant.estHumain()) {
            throw new IllegalStateException("C'est au tour de " + joueurCourant.getNom() + ", un joueur humain.");
        }
        Joueur ordinateur = joueurCourant;
        VuePlateau vue = adversaireDe(ordinateur).getPlateau().getVueAdverse();
        int tourDuJoueur = (numeroTour + (ordinateur == joueur1 ? 1 : 0)) / 2;
        Optional<Ordre> capacite = ordinateur.choisirCapacite(vue, capacitesDisponibles(), tourDuJoueur);
        if (capacite.isPresent()) {
            return jouer(capacite.get());
        }
        Coordonnee cible = ordinateur.choisirTir(vue);
        Coup coup = jouer(Ordre.tir(cible));
        if (coup.tirs().get(0).resultat() == ResultatTir.DEJA_JOUE) {
            throw new IllegalStateException("La stratégie de " + ordinateur.getNom() + " a visé " + cible
                    + ", une case déjà jouée.");
        }
        return coup;
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
     * @return le nombre de cases visées, de tirs au but et de capacités utilisées
     */
    public Statistiques getStatistiques(Joueur joueur) {
        adversaireDe(joueur);
        int tirs = 0;
        int tirsAuBut = 0;
        int capacites = 0;
        for (Coup coup : historique) {
            if (coup.auteur() != joueur) {
                continue;
            }
            if (!coup.ordre().estUnTirSimple()) {
                capacites++;
            }
            for (Tir tir : coup.tirs()) {
                tirs++;
                if (tir.resultat().estAuBut()) {
                    tirsAuBut++;
                }
            }
        }
        return new Statistiques(tirs, tirsAuBut, capacites);
    }
}
