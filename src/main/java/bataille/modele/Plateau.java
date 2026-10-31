package bataille.modele;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Le plateau d'un joueur : sa grille, ses bateaux et les tirs qu'il a reçus.
 * <p>
 * Les bateaux se placent avant le premier tir. Ensuite, le plateau enregistre les tirs de l'adversaire
 * et indique le résultat de chacun. L'adversaire n'accède au plateau qu'à travers {@link #getVueAdverse()},
 * qui cache les bateaux intacts.
 */
public final class Plateau {
    /** Le nombre de lignes, égal au nombre de colonnes. */
    private final int taille;
    /** Les bateaux posés, dans l'ordre de placement. */
    private final List<Bateau> bateaux = new ArrayList<>();
    /** Le bateau présent sur chaque case, ou {@code null}. */
    private final Bateau[][] occupation;
    /** Les cases déjà visées par l'adversaire. */
    private final boolean[][] vise;
    /** Le nombre de tirs reçus, sans compter les tirs sur une case déjà visée. */
    private int nombreTirsRecus;
    /** Les radars passés par l'adversaire sur ce plateau. */
    private final List<Detection> detections = new ArrayList<>();
    /** La vue du plateau donnée à l'adversaire. */
    private final VuePlateau vueAdverse = new VueAdverse();

    /**
     * Construit un plateau vide.
     *
     * @param taille le nombre de lignes et de colonnes
     */
    public Plateau(int taille) {
        if (taille < 1) {
            throw new IllegalArgumentException("La taille du plateau doit être positive : " + taille);
        }
        this.taille = taille;
        this.occupation = new Bateau[taille][taille];
        this.vise = new boolean[taille][taille];
    }

    /**
     * Retourne la taille du plateau.
     *
     * @return le nombre de lignes, égal au nombre de colonnes
     */
    public int getTaille() {
        return taille;
    }

    /**
     * Indique si une case est dans la grille.
     *
     * @param coordonnee la case à tester
     * @return si la case est dans la grille
     */
    public boolean contient(Coordonnee coordonnee) {
        return coordonnee.ligne() >= 0 && coordonnee.ligne() < taille
                && coordonnee.colonne() >= 0 && coordonnee.colonne() < taille;
    }

    /**
     * Décrit l'étendue de la grille, par exemple « A1 à J10 ».
     *
     * @return la première et la dernière case de la grille
     */
    public String decrireEtendue() {
        return new Coordonnee(0, 0) + " à " + new Coordonnee(taille - 1, taille - 1);
    }

    /**
     * Explique pourquoi un bateau ne peut pas être placé à une position.
     *
     * @param type le type du bateau
     * @param origine la case d'origine envisagée
     * @param orientation l'orientation envisagée
     * @return la raison du refus, ou une valeur vide si le placement est possible
     */
    public Optional<String> raisonRefusPlacement(TypeBateau type, Coordonnee origine, Orientation orientation) {
        Bateau candidat = new Bateau(type, origine, orientation);
        String sujet = "L" + type.getNomAvecArticle().substring(1);
        for (Coordonnee coordonnee : candidat.getCases()) {
            if (!contient(coordonnee)) {
                return Optional.of(sujet + " déborderait de la grille (" + decrireEtendue() + ").");
            }
        }
        for (Coordonnee coordonnee : candidat.getCases()) {
            Bateau present = occupation[coordonnee.ligne()][coordonnee.colonne()];
            if (present != null) {
                return Optional.of(sujet + " chevaucherait " + present.getType().getNomAvecArticle() + ".");
            }
        }
        return Optional.empty();
    }

    /**
     * Indique si un bateau peut être placé à une position : dans la grille et sans chevaucher un autre bateau.
     *
     * @param type le type du bateau
     * @param origine la case d'origine envisagée
     * @param orientation l'orientation envisagée
     * @return si le placement est possible
     */
    public boolean peutPlacer(TypeBateau type, Coordonnee origine, Orientation orientation) {
        return raisonRefusPlacement(type, origine, orientation).isEmpty();
    }

    /**
     * Place un bateau sur le plateau.
     *
     * @param type le type du bateau
     * @param origine la case la plus à gauche ou la plus haute
     * @param orientation le sens dans lequel le bateau s'étend
     * @return le bateau placé
     * @throws IllegalArgumentException si le bateau déborde ou chevauche un autre bateau
     * @throws IllegalStateException si le plateau a déjà reçu un tir
     */
    public Bateau placer(TypeBateau type, Coordonnee origine, Orientation orientation) {
        verifierAucunTir();
        Optional<String> raison = raisonRefusPlacement(type, origine, orientation);
        if (raison.isPresent()) {
            throw new IllegalArgumentException(raison.get());
        }
        Bateau bateau = new Bateau(type, origine, orientation);
        for (Coordonnee coordonnee : bateau.getCases()) {
            occupation[coordonnee.ligne()][coordonnee.colonne()] = bateau;
        }
        bateaux.add(bateau);
        return bateau;
    }

    /**
     * Retire un bateau du plateau, par exemple pour le replacer.
     *
     * @param bateau un bateau posé sur ce plateau
     * @throws IllegalStateException si le plateau a déjà reçu un tir
     */
    public void retirer(Bateau bateau) {
        verifierAucunTir();
        if (!bateaux.remove(bateau)) {
            throw new IllegalArgumentException("Ce bateau n'est pas sur le plateau : " + bateau);
        }
        for (Coordonnee coordonnee : bateau.getCases()) {
            occupation[coordonnee.ligne()][coordonnee.colonne()] = null;
        }
    }

    /**
     * Retire tous les bateaux du plateau.
     *
     * @throws IllegalStateException si le plateau a déjà reçu un tir
     */
    public void retirerTout() {
        for (Bateau bateau : List.copyOf(bateaux)) {
            retirer(bateau);
        }
    }

    /**
     * Retourne les bateaux posés.
     *
     * @return les bateaux, dans l'ordre de placement, dans une liste non modifiable
     */
    public List<Bateau> getBateaux() {
        return Collections.unmodifiableList(bateaux);
    }

    /**
     * Retourne les types des bateaux posés.
     *
     * @return les types, dans l'ordre de placement
     */
    public List<TypeBateau> getTypes() {
        return bateaux.stream().map(Bateau::getType).toList();
    }

    /**
     * Retourne le bateau présent sur une case.
     *
     * @param coordonnee une case de la grille
     * @return le bateau présent, ou une valeur vide si la case est libre
     */
    public Optional<Bateau> getBateau(Coordonnee coordonnee) {
        verifierDansLaGrille(coordonnee);
        return Optional.ofNullable(occupation[coordonnee.ligne()][coordonnee.colonne()]);
    }

    /**
     * Indique si une case a déjà été visée.
     *
     * @param coordonnee une case de la grille
     * @return si l'adversaire a déjà tiré sur la case
     */
    public boolean estVise(Coordonnee coordonnee) {
        verifierDansLaGrille(coordonnee);
        return vise[coordonnee.ligne()][coordonnee.colonne()];
    }

    /**
     * Reçoit un tir de l'adversaire. Un tir sur une case déjà visée ne change rien et renvoie
     * {@link ResultatTir#DEJA_JOUE}.
     *
     * @param coordonnee la case visée
     * @return le compte rendu du tir
     * @throws IllegalArgumentException si la case est hors de la grille
     */
    public Tir recevoirTir(Coordonnee coordonnee) {
        verifierDansLaGrille(coordonnee);
        int ligne = coordonnee.ligne();
        int colonne = coordonnee.colonne();
        if (vise[ligne][colonne]) {
            return Tir.de(coordonnee, ResultatTir.DEJA_JOUE);
        }
        vise[ligne][colonne] = true;
        nombreTirsRecus++;
        Bateau bateau = occupation[ligne][colonne];
        if (bateau == null) {
            return Tir.de(coordonnee, ResultatTir.MANQUE);
        }
        bateau.toucher(coordonnee);
        return bateau.estCoule() ? Tir.coule(coordonnee, bateau.getType()) : Tir.de(coordonnee, ResultatTir.TOUCHE);
    }

    /**
     * Passe un radar de l'adversaire : compte les cases de bateaux parmi les cases pas encore visées
     * d'un carré de 3 × 3, coupé aux bords de la grille. Le résultat reste visible dans la vue adverse.
     *
     * @param centre le centre du carré
     * @return la détection
     * @throws IllegalArgumentException si le centre est hors de la grille
     */
    public Detection sonder(Coordonnee centre) {
        verifierDansLaGrille(centre);
        List<Coordonnee> cases = new ArrayList<>();
        int nombre = 0;
        for (int deltaLigne = -1; deltaLigne <= 1; deltaLigne++) {
            for (int deltaColonne = -1; deltaColonne <= 1; deltaColonne++) {
                Coordonnee coordonnee = centre.decaler(deltaLigne, deltaColonne);
                if (contient(coordonnee) && !vise[coordonnee.ligne()][coordonnee.colonne()]) {
                    cases.add(coordonnee);
                    if (occupation[coordonnee.ligne()][coordonnee.colonne()] != null) {
                        nombre++;
                    }
                }
            }
        }
        Detection detection = new Detection(centre, cases, nombre);
        detections.add(detection);
        return detection;
    }

    /**
     * Retourne les radars passés sur ce plateau.
     *
     * @return les détections, dans l'ordre, dans une liste non modifiable
     */
    public List<Detection> getDetections() {
        return Collections.unmodifiableList(detections);
    }

    /**
     * Retourne le nombre de tirs reçus, sans compter les tirs sur une case déjà visée.
     *
     * @return le nombre de tirs reçus
     */
    public int getNombreTirsRecus() {
        return nombreTirsRecus;
    }

    /**
     * Indique si tous les bateaux du plateau sont coulés.
     *
     * @return si le plateau porte au moins un bateau et qu'ils sont tous coulés
     */
    public boolean flotteCoulee() {
        return !bateaux.isEmpty() && bateaux.stream().allMatch(Bateau::estCoule);
    }

    /**
     * Retourne le nombre de bateaux encore à flot.
     *
     * @return le nombre de bateaux pas encore coulés
     */
    public int getNombreBateauxRestants() {
        return (int) bateaux.stream().filter(bateau -> !bateau.estCoule()).count();
    }

    /**
     * Retourne l'état d'une case vu par le propriétaire du plateau, qui connaît ses bateaux.
     *
     * @param coordonnee une case de la grille
     * @return {@link EtatCase#EAU}, {@link EtatCase#BATEAU}, {@link EtatCase#MANQUE}, {@link EtatCase#TOUCHE}
     *         ou {@link EtatCase#COULE}
     */
    public EtatCase getEtatPourProprietaire(Coordonnee coordonnee) {
        verifierDansLaGrille(coordonnee);
        Bateau bateau = occupation[coordonnee.ligne()][coordonnee.colonne()];
        if (!vise[coordonnee.ligne()][coordonnee.colonne()]) {
            return bateau == null ? EtatCase.EAU : EtatCase.BATEAU;
        }
        if (bateau == null) {
            return EtatCase.MANQUE;
        }
        return bateau.estCoule() ? EtatCase.COULE : EtatCase.TOUCHE;
    }

    /**
     * Retourne la vue du plateau donnée à l'adversaire : elle cache les bateaux intacts.
     *
     * @return la vue adverse du plateau
     */
    public VuePlateau getVueAdverse() {
        return vueAdverse;
    }

    private void verifierDansLaGrille(Coordonnee coordonnee) {
        Objects.requireNonNull(coordonnee, "coordonnee");
        if (!contient(coordonnee)) {
            throw new IllegalArgumentException(coordonnee + " est hors de la grille (" + decrireEtendue() + ").");
        }
    }

    private void verifierAucunTir() {
        if (nombreTirsRecus > 0) {
            throw new IllegalStateException("Les bateaux ne peuvent plus bouger une fois la partie commencée.");
        }
    }

    /** La vue adverse : le résultat des tirs, sans la position des bateaux intacts. */
    private final class VueAdverse implements VuePlateau {

        @Override
        public int getTaille() {
            return taille;
        }

        @Override
        public boolean contient(Coordonnee coordonnee) {
            return Plateau.this.contient(coordonnee);
        }

        @Override
        public EtatCase getEtat(Coordonnee coordonnee) {
            EtatCase etat = getEtatPourProprietaire(coordonnee);
            if (etat == EtatCase.EAU || etat == EtatCase.BATEAU) {
                boolean videDetecte = detections.stream()
                        .anyMatch(detection -> detection.nombre() == 0 && detection.couvre(coordonnee));
                return videDetecte ? EtatCase.VIDE : EtatCase.INCONNUE;
            }
            return etat;
        }

        @Override
        public List<Detection> getDetections() {
            return Collections.unmodifiableList(detections);
        }

        @Override
        public List<Bateau> getBateauxCoules() {
            return bateaux.stream().filter(Bateau::estCoule).toList();
        }

        @Override
        public List<TypeBateau> getTypesRestants() {
            return bateaux.stream().filter(bateau -> !bateau.estCoule()).map(Bateau::getType).toList();
        }
    }
}
