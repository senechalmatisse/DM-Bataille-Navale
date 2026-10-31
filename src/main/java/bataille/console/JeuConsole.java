package bataille.console;

import bataille.donnees.Dossiers;
import bataille.donnees.FichierPartie;
import bataille.donnees.PreferencesJeu;
import bataille.donnees.ResumeStatistiques;
import bataille.donnees.StatistiquesCumulees;
import bataille.ia.Difficulte;
import bataille.modele.Bateau;
import bataille.modele.Capacite;
import bataille.modele.ConfigurationPartie;
import bataille.modele.Coordonnee;
import bataille.modele.Detection;
import bataille.modele.ModeleFlotte;
import bataille.modele.Orientation;
import bataille.modele.PlacementAleatoire;
import bataille.modele.Plateau;
import bataille.modele.ResultatTir;
import bataille.modele.TypeBateau;
import bataille.modele.VuePlateau;
import bataille.partie.AnalysePartie;
import bataille.partie.Annonces;
import bataille.partie.Coup;
import bataille.partie.EnregistrementPartie;
import bataille.partie.Joueur;
import bataille.partie.ModeJeu;
import bataille.partie.ParametresPartie;
import bataille.partie.Partie;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * La partie en console : contre l'ordinateur, ou à deux sur le même clavier.
 * <p>
 * Un menu propose de jouer, de reprendre la partie interrompue et de consulter ses statistiques. Avant une partie,
 * chaque question propose la réponse donnée la fois précédente (touche Entrée). Pendant la partie, les deux grilles
 * s'affichent côte à côte avec l'état des deux flottes, et chaque saisie refusée est expliquée. La partie est
 * enregistrée après chaque coup : Q l'interrompt et le menu permet de la reprendre. À deux, l'écran défile entre
 * les tours pour que chacun garde sa flotte secrète. La fin de l'entrée (Ctrl+D, Ctrl+Z) arrête le programme
 * proprement.
 */
public final class JeuConsole {
    /** Une case suivie d'une orientation, par exemple « B2 H ». */
    private static final Pattern PLACEMENT = Pattern.compile("([A-Za-z]\\s*\\d{1,3})\\s*([HhVv])");
    /** Le mot qui place au hasard les bateaux restants. */
    private static final String HASARD = "hasard";
    /** Le mot qui reprend le dernier bateau placé. */
    private static final String ANNULER = "annuler";
    /** Le mot qui affiche les commandes pendant la partie. */
    private static final String AIDE = "aide";
    /** La touche qui interrompt la partie. */
    private static final String QUITTER = "q";
    /** La question posée après une partie terminée. */
    private static final String REJOUER = "Rejouer avec les mêmes réglages ? (o/N) : ";
    /** Le nombre de lignes vides qui font défiler les grilles hors de l'écran quand les joueurs se relaient. */
    private static final int LIGNES_POUR_CACHER = 60;

    /** L'entrée du joueur. */
    private final BufferedReader entree;
    /** La sortie où s'affiche le jeu. */
    private final PrintStream sortie;
    /** Le générateur de hasard des placements et de l'ordinateur. */
    private final Random random;
    /** Les fichiers du jeu : préférences, statistiques et dernière partie. */
    private final Dossiers dossiers;
    /** Les réglages de la partie précédente, proposés par défaut. */
    private final PreferencesJeu preferences;
    /** Si la partie en cours a pu être enregistrée après le dernier coup. */
    private boolean partieEnregistree;
    /** Si un échec d'enregistrement a déjà été signalé, pour ne pas le répéter à chaque coup. */
    private boolean echecEnregistrementSignale;

    /**
     * Construit le jeu.
     *
     * @param entree l'entrée du joueur
     * @param sortie la sortie où afficher le jeu
     * @param random le générateur de hasard, à graine fixe pour une partie reproductible
     * @param dossiers les fichiers du jeu
     */
    public JeuConsole(BufferedReader entree, PrintStream sortie, Random random, Dossiers dossiers) {
        this.entree = Objects.requireNonNull(entree, "entree");
        this.sortie = Objects.requireNonNull(sortie, "sortie");
        this.random = Objects.requireNonNull(random, "random");
        this.dossiers = Objects.requireNonNull(dossiers, "dossiers");
        this.preferences = PreferencesJeu.charger(dossiers.preferences());
    }

    /**
     * Lance le jeu en console, sur l'entrée et la sortie standard.
     *
     * @param args non utilisés
     */
    public static void main(String[] args) {
        Charset charset = System.console() != null ? System.console().charset() : Charset.defaultCharset();
        BufferedReader entree = new BufferedReader(new InputStreamReader(System.in, charset));
        new JeuConsole(entree, System.out, new Random(), Dossiers.parDefaut()).lancer();
    }

    /** Affiche le menu jusqu'à ce que le joueur quitte. */
    public void lancer() {
        sortie.println("=== BATAILLE NAVALE ===");
        try {
            boolean continuer = true;
            while (continuer) {
                continuer = menu();
            }
            sortie.println("Au revoir !");
        } catch (FinDeSaisie e) {
            sortie.println();
            sortie.println("Fin de l'entrée : au revoir !");
        }
    }

    /**
     * Affiche le menu et exécute le choix du joueur.
     *
     * @return faux quand le joueur veut quitter
     */
    private boolean menu() {
        Optional<EnregistrementPartie> interrompue = partieInterrompue();
        List<Choix> choix = new ArrayList<>(List.of(Choix.values()));
        if (interrompue.isEmpty()) {
            choix.remove(Choix.REPRENDRE);
        }
        sortie.println();
        sortie.println("Que voulez-vous faire ?");
        for (int i = 0; i < choix.size(); i++) {
            String libelle = choix.get(i).libelle;
            if (choix.get(i) == Choix.REPRENDRE) {
                libelle += " (" + interrompue.orElseThrow().decrire() + ")";
            }
            sortie.println("  " + (i + 1) + ". " + libelle);
        }
        int parDefaut = interrompue.isPresent() ? choix.indexOf(Choix.REPRENDRE) + 1 : 1;
        Choix choisi = choix.get(lireEntier("Votre choix [" + parDefaut + "] : ", 1, choix.size(), parDefaut) - 1);
        switch (choisi) {
            case CONTRE_ORDINATEUR -> jouerPuisRejouer(demanderParametres(ModeJeu.CONTRE_ORDINATEUR));
            case A_DEUX -> jouerPuisRejouer(demanderParametres(ModeJeu.DEUX_JOUEURS));
            case REPRENDRE -> reprendre(interrompue.orElseThrow());
            case STATISTIQUES -> afficherStatistiques();
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Joue une partie, puis les suivantes tant que le joueur veut rejouer avec les mêmes réglages. */
    private void jouerPuisRejouer(ParametresPartie parametres) {
        proposerDeRejouer(parametres, jouerNouvellePartie(parametres));
    }

    /**
     * Propose de rejouer après une partie terminée, autant de fois que le joueur le souhaite.
     *
     * @param parametres les réglages des parties
     * @param terminee si la partie précédente est allée à son terme ; rien n'est proposé après une interruption
     */
    private void proposerDeRejouer(ParametresPartie parametres, boolean terminee) {
        boolean continuer = terminee;
        while (continuer && demanderOuiNon(REJOUER, false)) {
            continuer = jouerNouvellePartie(parametres);
        }
    }

    private void reprendre(EnregistrementPartie enregistrement) {
        Partie partie;
        try {
            partie = enregistrement.restaurer(random);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sortie.println("La partie interrompue ne peut pas être reprise : " + e.getMessage());
            return;
        }
        sortie.println();
        sortie.println("Reprise de la partie : " + enregistrement.decrire() + ".");
        proposerDeRejouer(enregistrement.parametres(), derouler(partie, enregistrement.parametres(), true));
    }

    private ParametresPartie demanderParametres(ModeJeu mode) {
        ParametresPartie precedents = preferences.derniersParametres();
        boolean aDeux = mode == ModeJeu.DEUX_JOUEURS;
        String nom1 = demanderNom(aDeux ? "Nom du joueur 1" : "Votre nom", precedents.nomJoueur1());
        String nom2 = precedents.nomJoueur2();
        Difficulte difficulte = precedents.difficulte();
        if (aDeux) {
            String propose = nom2.equalsIgnoreCase(nom1) ? ParametresPartie.NOM_PAR_DEFAUT_2 : nom2;
            if (propose.equalsIgnoreCase(nom1)) {
                propose = ParametresPartie.NOM_PAR_DEFAUT;
            }
            nom2 = demanderNom("Nom du joueur 2", propose);
            while (nom2.equalsIgnoreCase(nom1)) {
                sortie.println("Les deux joueurs doivent avoir des noms différents.");
                nom2 = demanderNom("Nom du joueur 2", propose);
            }
        } else {
            difficulte = demanderDifficulte(difficulte);
        }
        ConfigurationPartie configuration = demanderConfiguration(precedents.configuration());
        boolean placementManuel = demanderOuiNon((aDeux ? "Placer les bateaux à la main ? " : "Placer vos bateaux "
                + "vous-même ? ") + ouiNon(precedents.placementManuel()), precedents.placementManuel());
        ParametresPartie parametres = new ParametresPartie(mode, nom1, nom2, difficulte, configuration,
                placementManuel);
        preferences.setDerniersParametres(parametres);
        try {
            preferences.sauvegarder();
        } catch (IOException e) {
            // les réglages ne seront simplement pas proposés la prochaine fois
        }
        return parametres;
    }

    private String demanderNom(String question, String parDefaut) {
        String nom = lireLigne(question + " [" + parDefaut + "] : ").strip();
        if (nom.isEmpty()) {
            return parDefaut;
        }
        return nom.length() > Joueur.LONGUEUR_NOM_MAX ? nom.substring(0, Joueur.LONGUEUR_NOM_MAX) : nom;
    }

    private Difficulte demanderDifficulte(Difficulte parDefaut) {
        sortie.println("Niveau de l'ordinateur :");
        Difficulte[] niveaux = Difficulte.values();
        for (int i = 0; i < niveaux.length; i++) {
            sortie.println("  " + (i + 1) + ". " + niveaux[i]);
        }
        int numero = parDefaut.ordinal() + 1;
        return niveaux[lireEntier("Votre choix [" + numero + "] : ", 1, niveaux.length, numero) - 1];
    }

    private ConfigurationPartie demanderConfiguration(ConfigurationPartie precedente) {
        while (true) {
            int taille = lireEntier("Taille de la grille, de " + ConfigurationPartie.TAILLE_MIN + " à "
                    + ConfigurationPartie.TAILLE_MAX + " [" + precedente.taille() + "] : ",
                    ConfigurationPartie.TAILLE_MIN, ConfigurationPartie.TAILLE_MAX, precedente.taille());
            sortie.println("Flotte :");
            ModeleFlotte[] modeles = ModeleFlotte.values();
            int modeleParDefaut = 1;
            for (int i = 0; i < modeles.length; i++) {
                sortie.println("  " + (i + 1) + ". " + modeles[i]);
                if (modeles[i].getTypes().equals(precedente.flotte())) {
                    modeleParDefaut = i + 1;
                }
            }
            ModeleFlotte modele = modeles[lireEntier("Votre choix [" + modeleParDefaut + "] : ", 1, modeles.length,
                    modeleParDefaut) - 1];
            try {
                ConfigurationPartie configuration = ConfigurationPartie.de(taille, modele);
                boolean capacites = demanderOuiNon("Capacités spéciales (radar, bombardement, torpille, double tir) ? "
                        + ouiNon(precedente.capacites()), precedente.capacites());
                return configuration.avecCapacites(capacites);
            } catch (IllegalArgumentException e) {
                sortie.println(e.getMessage() + " Choisissez une grille plus grande ou une flotte plus petite.");
            }
        }
    }

    /**
     * Fait placer les flottes, puis joue la partie.
     *
     * @param parametres les réglages
     * @return si la partie est allée à son terme, plutôt qu'interrompue
     */
    private boolean jouerNouvellePartie(ParametresPartie parametres) {
        int taille = parametres.configuration().taille();
        Plateau plateau1 = new Plateau(taille);
        if (!preparerFlotte(plateau1, parametres, parametres.nomJoueur1())) {
            sortie.println("Partie abandonnée.");
            return false;
        }
        Partie partie;
        if (parametres.contreLOrdinateur()) {
            partie = parametres.creerPartie(plateau1, random);
        } else {
            Plateau plateau2 = new Plateau(taille);
            if (!preparerFlotte(plateau2, parametres, parametres.nomJoueur2())) {
                sortie.println("Partie abandonnée.");
                return false;
            }
            partie = parametres.creerPartie(plateau1, plateau2, random);
        }
        return derouler(partie, parametres, false);
    }

    /**
     * Place la flotte d'un joueur, au hasard ou à la main selon les réglages.
     *
     * @return faux si le joueur abandonne pendant le placement
     */
    private boolean preparerFlotte(Plateau plateau, ParametresPartie parametres, String nom) {
        List<TypeBateau> flotte = parametres.configuration().flotte();
        if (!parametres.placementManuel()) {
            PlacementAleatoire.placer(plateau, flotte, random);
            return true;
        }
        boolean aDeux = !parametres.contreLOrdinateur();
        if (aDeux) {
            passerLaMain(nom, "placer vos bateaux");
        }
        boolean place = placerALaMain(plateau, flotte);
        if (place && aDeux) {
            lireLigne("Appuyez sur Entrée pour cacher votre flotte. ");
        }
        return place;
    }

    private boolean placerALaMain(Plateau plateau, List<TypeBateau> flotte) {
        sortie.println();
        sortie.println("Placez vos bateaux : tapez la case de départ puis H (vers la droite) ou V (vers le bas).");
        sortie.println("« " + ANNULER + " » reprend le bateau précédent, « " + HASARD
                + " » place les suivants au hasard, Q abandonne.");
        while (plateau.getBateaux().size() < flotte.size()) {
            int rang = plateau.getBateaux().size();
            TypeBateau type = flotte.get(rang);
            sortie.println();
            sortie.print(RenduConsole.grille(plateau, "Votre flotte"));
            String saisie = lireLigne(type.getNom() + " (" + type.getLongueur() + " cases), par exemple B2 H : ")
                    .strip();
            if (saisie.equalsIgnoreCase(QUITTER)) {
                return false;
            } else if (saisie.equalsIgnoreCase(HASARD)) {
                placerAuHasard(plateau, flotte.subList(rang, flotte.size()));
            } else if (saisie.equalsIgnoreCase(ANNULER)) {
                annulerDernierPlacement(plateau);
            } else {
                essayerDePlacer(plateau, type, saisie);
            }
        }
        sortie.println();
        sortie.print(RenduConsole.grille(plateau, "Votre flotte est prête"));
        return true;
    }

    private void placerAuHasard(Plateau plateau, List<TypeBateau> reste) {
        try {
            PlacementAleatoire.placer(plateau, reste, random);
        } catch (IllegalStateException e) {
            sortie.println("Les bateaux suivants ne trouvent plus de place : tapez « " + ANNULER
                    + " » pour déplacer les derniers bateaux posés.");
        }
    }

    private void annulerDernierPlacement(Plateau plateau) {
        List<Bateau> poses = plateau.getBateaux();
        if (poses.isEmpty()) {
            sortie.println("Aucun bateau à reprendre.");
            return;
        }
        Bateau dernier = poses.get(poses.size() - 1);
        plateau.retirer(dernier);
        sortie.println(dernier.getType().getNom() + " retiré : placez-le à nouveau.");
    }

    private void essayerDePlacer(Plateau plateau, TypeBateau type, String saisie) {
        Matcher correspondance = PLACEMENT.matcher(saisie);
        if (!correspondance.matches()) {
            sortie.println("Tapez une case puis H ou V, par exemple B2 H.");
            return;
        }
        try {
            Coordonnee origine = Coordonnee.depuis(correspondance.group(1));
            plateau.placer(type, origine, Orientation.depuis(correspondance.group(2)));
        } catch (IllegalArgumentException e) {
            sortie.println(e.getMessage());
        }
    }

    /**
     * Joue la partie jusqu'à la victoire d'un joueur, ou jusqu'à ce qu'un joueur l'interrompe.
     *
     * @param partie la partie, nouvelle ou reprise
     * @param parametres ses réglages
     * @param reprise si la partie reprend après une interruption
     * @return si la partie est allée à son terme
     */
    private boolean derouler(Partie partie, ParametresPartie parametres, boolean reprise) {
        Instant debut = Instant.now();
        boolean aDeux = !parametres.contreLOrdinateur();
        echecEnregistrementSignale = false;
        enregistrer(partie, parametres);
        if (!reprise) {
            sortie.println();
            sortie.println(aDeux ? "La partie commence : " + parametres.nomJoueur1() + " contre "
                    + parametres.nomJoueur2() + ". " + parametres.nomJoueur1() + " tire en premier."
                    : "La partie commence. " + parametres.nomJoueur1() + ", à vous de tirer ! (difficulté : "
                    + parametres.difficulte().getNom().toLowerCase(Locale.ROOT) + ")");
        }
        boolean rappeler = reprise;
        while (!partie.estTerminee()) {
            Joueur courant = partie.getJoueurCourant();
            if (!courant.estHumain()) {
                sortie.println(Annonces.decrire(partie.faireJouerOrdinateur(), partie.getJoueur1()));
                enregistrer(partie, parametres);
                rappeler = false;
                continue;
            }
            if (aDeux) {
                passerLaMain(courant.getNom(), "afficher vos grilles");
            }
            if (aDeux || rappeler) {
                raconterCoupsAdverses(partie, courant);
            }
            rappeler = false;
            if (!jouerTour(partie, courant, parametres)) {
                sortie.println(partieEnregistree ? "Partie interrompue : vous pourrez la reprendre depuis le menu."
                        : "Partie abandonnée.");
                return false;
            }
            if (aDeux && !partie.estTerminee()) {
                lireLigne("Appuyez sur Entrée pour cacher vos grilles et passer la main. ");
            }
        }
        afficherBilan(partie, parametres);
        enregistrerStatistiques(partie, parametres, Duration.between(debut, Instant.now()));
        return true;
    }

    /**
     * Fait jouer son tour à un joueur humain : un tir ou une capacité, puis un second tir après un double tir.
     *
     * @return faux si le joueur interrompt la partie
     */
    private boolean jouerTour(Partie partie, Joueur joueur, ParametresPartie parametres) {
        boolean doubleTir = false;
        boolean afficher = true;
        while (partie.getJoueurCourant() == joueur && !partie.estTerminee()) {
            if (afficher) {
                afficherSituation(partie, joueur);
                afficher = false;
            }
            String saisie = lireLigne(invite(partie, doubleTir)).strip();
            if (saisie.equalsIgnoreCase(QUITTER)) {
                return false;
            }
            if (saisie.equalsIgnoreCase(AIDE) || saisie.equals("?")) {
                afficherAide(partie);
                continue;
            }
            try {
                Coup coup = partie.jouer(Commandes.lire(saisie));
                if (coup.ordre().estUnTirSimple() && coup.tirs().get(0).resultat() == ResultatTir.DEJA_JOUE) {
                    sortie.println("Vous avez déjà tiré en " + coup.ordre().cible() + ".");
                } else {
                    sortie.println(Annonces.decrire(coup, joueur));
                    enregistrer(partie, parametres);
                    doubleTir |= coup.ordre().capacite() == Capacite.DOUBLE_TIR;
                    afficher = true;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                sortie.println(e.getMessage());
            }
        }
        return true;
    }

    private static String invite(Partie partie, boolean doubleTir) {
        if (doubleTir) {
            return (partie.getTirsRestantsCeTour() > 1 ? "Premier tir" : "Second tir") + " (par exemple B7) : ";
        }
        if (!partie.capacitesDisponibles().isEmpty()) {
            return "Votre tir (par exemple B7), une capacité (« " + AIDE + " » pour la liste) ou Q pour quitter : ";
        }
        return "Votre tir (par exemple B7, Q pour quitter) : ";
    }

    private void afficherSituation(Partie partie, Joueur joueur) {
        Joueur adversaire = partie.adversaireDe(joueur);
        String titreAdverse = adversaire.estHumain() ? "Flotte " + Annonces.de(adversaire.getNom()) : "Flotte ennemie";
        VuePlateau vue = adversaire.getPlateau().getVueAdverse();
        List<TypeBateau> flotte = partie.getConfiguration().flotte();
        sortie.println();
        sortie.print(RenduConsole.deuxGrilles(joueur.getPlateau(), "Votre flotte", vue, titreAdverse));
        sortie.println(RenduConsole.LEGENDE);
        sortie.println(RenduConsole.suiviFlotte("Votre flotte", flotte,
                joueur.getPlateau().getVueAdverse().getTypesRestants()));
        sortie.println(RenduConsole.suiviFlotte(titreAdverse, flotte, vue.getTypesRestants()));
        for (Detection detection : vue.getDetections()) {
            RenduConsole.radar(detection, vue).ifPresent(sortie::println);
        }
        if (partie.getConfiguration().capacites()) {
            List<String> restantes = new ArrayList<>();
            for (Capacite capacite : Capacite.values()) {
                if (partie.getCharges(joueur, capacite) > 0) {
                    restantes.add(capacite.getNom().toLowerCase(Locale.ROOT));
                }
            }
            sortie.println(restantes.isEmpty() ? "Capacités : toutes utilisées."
                    : "Capacités : " + String.join(", ", restantes) + " (« " + AIDE + " » pour les utiliser).");
        }
    }

    private void afficherAide(Partie partie) {
        sortie.println("Commandes :");
        if (partie.getConfiguration().capacites()) {
            Commandes.AIDE.forEach(sortie::println);
            sortie.println("Chaque capacité sert une fois par partie, une seule par tour, avant de tirer.");
        } else {
            sortie.println(Commandes.AIDE.get(0));
            sortie.println(Commandes.AIDE.get(Commandes.AIDE.size() - 1));
            sortie.println("Les capacités spéciales ne sont pas activées dans cette partie.");
        }
    }

    /** Rappelle à un joueur les coups joués par son adversaire depuis son propre dernier coup. */
    private void raconterCoupsAdverses(Partie partie, Joueur lecteur) {
        List<Coup> historique = partie.getHistorique();
        int debut = 0;
        for (int i = historique.size() - 1; i >= 0; i--) {
            if (historique.get(i).auteur() == lecteur) {
                debut = i + 1;
                break;
            }
        }
        for (Coup coup : historique.subList(debut, historique.size())) {
            sortie.println(Annonces.decrire(coup, lecteur));
        }
    }

    private void afficherBilan(Partie partie, ParametresPartie parametres) {
        Joueur joueur1 = partie.getJoueur1();
        Joueur joueur2 = partie.getJoueur2();
        Joueur gagnant = partie.getGagnant().orElseThrow();
        sortie.println();
        if (parametres.contreLOrdinateur()) {
            sortie.print(RenduConsole.deuxGrilles(joueur1.getPlateau(), "Votre flotte",
                    joueur2.getPlateau()::getEtatPourProprietaire, "Flotte ennemie dévoilée"));
            sortie.println(RenduConsole.LEGENDE);
            sortie.println(gagnant == joueur1 ? "*** Victoire ! Vous avez coulé toute la flotte ennemie. ***"
                    : "*** Défaite : l'ordinateur a coulé toute votre flotte. ***");
            sortie.println("Vous : " + Annonces.bilan(partie.getStatistiques(joueur1)) + ". Ordinateur : "
                    + Annonces.bilan(partie.getStatistiques(joueur2)) + ".");
            AnalysePartie.raconter(partie, joueur1, joueur1).forEach(sortie::println);
            return;
        }
        Joueur perdant = partie.adversaireDe(gagnant);
        sortie.print(RenduConsole.deuxGrilles(joueur1.getPlateau(), "Flotte " + Annonces.de(joueur1.getNom()),
                joueur2.getPlateau()::getEtatPourProprietaire, "Flotte " + Annonces.de(joueur2.getNom())));
        sortie.println(RenduConsole.LEGENDE);
        sortie.println("*** " + gagnant.getNom() + " gagne : toute la flotte " + Annonces.de(perdant.getNom())
                + " est coulée. ***");
        sortie.println(joueur1.getNom() + " : " + Annonces.bilan(partie.getStatistiques(joueur1)) + ". "
                + joueur2.getNom() + " : " + Annonces.bilan(partie.getStatistiques(joueur2)) + ".");
        AnalysePartie.raconter(partie, joueur1).forEach(sortie::println);
        AnalysePartie.raconter(partie, joueur2).forEach(sortie::println);
    }

    private void enregistrerStatistiques(Partie partie, ParametresPartie parametres, Duration duree) {
        try {
            StatistiquesCumulees statistiques = StatistiquesCumulees.charger(dossiers.statistiques());
            if (parametres.contreLOrdinateur()) {
                statistiques.ajouterPartieContreOrdinateur(partie, parametres.difficulte(), duree);
            } else {
                statistiques.ajouterPartieADeux();
            }
            statistiques.sauvegarder();
            if (parametres.contreLOrdinateur()) {
                sortie.println("Vos statistiques : " + ResumeStatistiques.enUneLigne(statistiques) + ".");
            }
        } catch (IOException | IllegalArgumentException e) {
            sortie.println("Les statistiques n'ont pas pu être enregistrées : " + e.getMessage());
        }
    }

    private void afficherStatistiques() {
        sortie.println();
        List<ResumeStatistiques.Ligne> lignes;
        try {
            lignes = ResumeStatistiques.lignes(StatistiquesCumulees.charger(dossiers.statistiques()));
        } catch (IOException | IllegalArgumentException e) {
            sortie.println("Les statistiques ne peuvent pas être lues : " + e.getMessage());
            return;
        }
        if (lignes.isEmpty()) {
            sortie.println("Aucune partie terminée pour l'instant : vos statistiques s'afficheront ici.");
            return;
        }
        sortie.println("Vos statistiques :");
        int largeur = lignes.stream().mapToInt(ligne -> ligne.libelle().length()).max().orElse(0);
        for (ResumeStatistiques.Ligne ligne : lignes) {
            sortie.println("  " + ligne.libelle() + " ".repeat(largeur - ligne.libelle().length()) + " : "
                    + ligne.valeur());
        }
    }

    /** Enregistre la partie après un coup, pour pouvoir la reprendre ou la revoir. */
    private void enregistrer(Partie partie, ParametresPartie parametres) {
        try {
            FichierPartie.ecrire(EnregistrementPartie.de(partie, parametres), dossiers.dernierePartie());
            partieEnregistree = true;
        } catch (IOException e) {
            partieEnregistree = false;
            if (!echecEnregistrementSignale) {
                echecEnregistrementSignale = true;
                sortie.println("La partie ne peut pas être enregistrée (" + e.getMessage()
                        + ") : elle ne pourra pas être reprise.");
            }
        }
    }

    /**
     * Retourne la dernière partie enregistrée si elle n'est pas terminée.
     *
     * @return la partie à reprendre, ou une valeur vide s'il n'y en a pas ou si son fichier est illisible
     */
    private Optional<EnregistrementPartie> partieInterrompue() {
        if (!Files.isRegularFile(dossiers.dernierePartie())) {
            return Optional.empty();
        }
        try {
            EnregistrementPartie enregistrement = FichierPartie.lire(dossiers.dernierePartie());
            return enregistrement.estTerminee() ? Optional.empty() : Optional.of(enregistrement);
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            return Optional.empty();
        }
    }

    /** Fait défiler l'écran, puis attend que le joueur suivant soit prêt. */
    private void passerLaMain(String nom, String action) {
        for (int i = 0; i < LIGNES_POUR_CACHER; i++) {
            sortie.println();
        }
        sortie.println("=== Au tour " + Annonces.de(nom) + " ===");
        lireLigne(nom + ", appuyez sur Entrée pour " + action + ". ");
    }

    private static String ouiNon(boolean parDefaut) {
        return parDefaut ? "(O/n) : " : "(o/N) : ";
    }

    private int lireEntier(String invite, int minimum, int maximum, int parDefaut) {
        while (true) {
            String saisie = lireLigne(invite).strip();
            if (saisie.isEmpty()) {
                return parDefaut;
            }
            try {
                int valeur = Integer.parseInt(saisie);
                if (valeur >= minimum && valeur <= maximum) {
                    return valeur;
                }
            } catch (NumberFormatException e) {
                // la saisie n'est pas un nombre : le message ci-dessous l'explique
            }
            sortie.println("Tapez un nombre entre " + minimum + " et " + maximum + ".");
        }
    }

    private boolean demanderOuiNon(String invite, boolean parDefaut) {
        while (true) {
            String saisie = lireLigne(invite).strip().toLowerCase(Locale.ROOT);
            if (saisie.isEmpty()) {
                return parDefaut;
            }
            if (saisie.equals("o") || saisie.equals("oui")) {
                return true;
            }
            if (saisie.equals("n") || saisie.equals("non")) {
                return false;
            }
            sortie.println("Répondez o (oui) ou n (non).");
        }
    }

    private String lireLigne(String invite) {
        sortie.print(invite);
        sortie.flush();
        String ligne;
        try {
            ligne = entree.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (ligne == null) {
            throw new FinDeSaisie();
        }
        return ligne;
    }

    /** Les choix du menu principal. */
    private enum Choix {
        /** Une partie contre l'ordinateur. */
        CONTRE_ORDINATEUR("Jouer contre l'ordinateur"),
        /** Une partie à deux sur le même écran. */
        A_DEUX("Jouer à deux sur le même écran"),
        /** Reprendre la partie interrompue ; proposé seulement s'il y en a une. */
        REPRENDRE("Reprendre la partie interrompue"),
        /** Afficher les statistiques. */
        STATISTIQUES("Voir vos statistiques"),
        /** Quitter le jeu. */
        QUITTER("Quitter");

        /** Le texte affiché dans le menu. */
        private final String libelle;

        Choix(String libelle) {
            this.libelle = libelle;
        }
    }

    /** Signale que l'entrée du joueur est terminée. */
    private static final class FinDeSaisie extends RuntimeException {
        private static final long serialVersionUID = 1L;

        FinDeSaisie() {
            super("Fin de l'entrée");
        }
    }
}
