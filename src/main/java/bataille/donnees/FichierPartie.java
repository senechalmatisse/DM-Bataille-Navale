package bataille.donnees;

import bataille.ia.Difficulte;
import bataille.modele.ConfigurationPartie;
import bataille.modele.Ordre;
import bataille.modele.PositionBateau;
import bataille.modele.TypeBateau;
import bataille.partie.EnregistrementPartie;
import bataille.partie.EnregistrementPartie.OrdreJoue;
import bataille.partie.ModeJeu;
import bataille.partie.ParametresPartie;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lit et écrit une partie enregistrée dans un fichier texte UTF-8, lisible et modifiable à la main :
 * <pre>
 * # Bataille navale : partie enregistrée
 * format=1
 * mode=CONTRE_ORDINATEUR
 * joueur1=Matisse
 * joueur2=Joueur 2
 * difficulte=DIFFICILE
 * taille=10
 * flotte=PORTE_AVIONS,CROISEUR,CONTRE_TORPILLEUR,SOUS_MARIN,TORPILLEUR
 * capacites=true
 * bateaux1=PORTE_AVIONS@B2:H CROISEUR@H4:V ...
 * bateaux2=...
 * coup=1 TIR B7
 * coup=2 RADAR E5
 * </pre>
 */
public final class FichierPartie {
    /** La première ligne du fichier. */
    private static final String ENTETE = "# Bataille navale : partie enregistrée";
    /** La version du format, pour reconnaître un fichier d'une autre version. */
    private static final int FORMAT = 1;

    private FichierPartie() {
    }

    /**
     * Écrit une partie enregistrée ; crée le dossier si besoin.
     *
     * @param enregistrement la partie
     * @param fichier le fichier à écrire, remplacé s'il existe
     * @throws IOException si l'écriture échoue
     */
    public static void ecrire(EnregistrementPartie enregistrement, Path fichier) throws IOException {
        ParametresPartie parametres = enregistrement.parametres();
        ConfigurationPartie configuration = parametres.configuration();
        List<String> lignes = new ArrayList<>();
        lignes.add(ENTETE);
        lignes.add("format=" + FORMAT);
        lignes.add("mode=" + parametres.mode().name());
        lignes.add("joueur1=" + parametres.nomJoueur1());
        lignes.add("joueur2=" + parametres.nomJoueur2());
        lignes.add("difficulte=" + parametres.difficulte().name());
        lignes.add("taille=" + configuration.taille());
        lignes.add("flotte=" + configuration.flotte().stream().map(TypeBateau::name).collect(Collectors.joining(",")));
        lignes.add("capacites=" + configuration.capacites());
        lignes.add("bateaux1=" + enTexte(enregistrement.flotte1()));
        lignes.add("bateaux2=" + enTexte(enregistrement.flotte2()));
        for (OrdreJoue ordre : enregistrement.ordres()) {
            lignes.add("coup=" + ordre.joueur() + " " + ordre.ordre().enTexte());
        }
        Path dossier = fichier.toAbsolutePath().getParent();
        if (dossier != null) {
            Files.createDirectories(dossier);
        }
        Files.write(fichier, lignes, StandardCharsets.UTF_8);
    }

    private static String enTexte(List<PositionBateau> positions) {
        return positions.stream().map(PositionBateau::enTexte).collect(Collectors.joining(" "));
    }

    /**
     * Lit une partie enregistrée.
     *
     * @param fichier le fichier à lire
     * @return la partie enregistrée
     * @throws IOException si le fichier ne peut pas être lu ou n'est pas une partie enregistrée
     */
    public static EnregistrementPartie lire(Path fichier) throws IOException {
        List<String> lignes = Files.readAllLines(fichier, StandardCharsets.UTF_8);
        Map<String, String> valeurs = new HashMap<>();
        List<OrdreJoue> ordres = new ArrayList<>();
        try {
            for (int i = 0; i < lignes.size(); i++) {
                String ligne = lignes.get(i).strip();
                if (ligne.isEmpty() || ligne.startsWith("#")) {
                    continue;
                }
                int egal = ligne.indexOf('=');
                if (egal < 0) {
                    throw new IllegalArgumentException("ligne " + (i + 1) + " sans « = » : " + ligne);
                }
                String cle = ligne.substring(0, egal).strip();
                String valeur = ligne.substring(egal + 1).strip();
                if (cle.equals("coup")) {
                    int espace = valeur.indexOf(' ');
                    if (espace < 0) {
                        throw new IllegalArgumentException("ligne " + (i + 1) + " : coup incomplet");
                    }
                    ordres.add(new OrdreJoue(Integer.parseInt(valeur.substring(0, espace)),
                            Ordre.depuisTexte(valeur.substring(espace + 1))));
                } else {
                    valeurs.put(cle, valeur);
                }
            }
            if (Integer.parseInt(obligatoire(valeurs, "format")) != FORMAT) {
                throw new IllegalArgumentException("format " + valeurs.get("format") + " inconnu");
            }
            List<TypeBateau> flotte = Arrays.stream(obligatoire(valeurs, "flotte").split(","))
                    .map(String::strip).map(TypeBateau::valueOf).toList();
            ConfigurationPartie configuration = new ConfigurationPartie(
                    Integer.parseInt(obligatoire(valeurs, "taille")), flotte,
                    Boolean.parseBoolean(obligatoire(valeurs, "capacites")));
            ParametresPartie parametres = new ParametresPartie(ModeJeu.valueOf(obligatoire(valeurs, "mode")),
                    obligatoire(valeurs, "joueur1"), valeurs.get("joueur2"),
                    Difficulte.valueOf(obligatoire(valeurs, "difficulte")), configuration, false);
            return new EnregistrementPartie(parametres, positions(obligatoire(valeurs, "bateaux1")),
                    positions(obligatoire(valeurs, "bateaux2")), ordres);
        } catch (IllegalArgumentException e) {
            throw new IOException("Ce fichier n'est pas une partie enregistrée valide (" + e.getMessage() + ").", e);
        }
    }

    private static String obligatoire(Map<String, String> valeurs, String cle) {
        String valeur = valeurs.get(cle);
        if (valeur == null || valeur.isEmpty()) {
            throw new IllegalArgumentException("« " + cle + " » manquant");
        }
        return valeur;
    }

    private static List<PositionBateau> positions(String texte) {
        return Arrays.stream(texte.strip().split("\\s+")).map(PositionBateau::depuisTexte).toList();
    }
}
