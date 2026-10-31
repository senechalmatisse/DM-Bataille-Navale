package bataille.modele;

import java.util.Objects;
import java.util.Optional;

/**
 * Le compte rendu d'un tir : la case visée, le résultat et, si un bateau vient d'être coulé, son type.
 *
 * @param cible la case visée
 * @param resultat le résultat du tir
 * @param bateauCoule le type du bateau coulé, présent seulement quand le résultat est {@link ResultatTir#COULE}
 */
public record Tir(Coordonnee cible, ResultatTir resultat, Optional<TypeBateau> bateauCoule) {

    /**
     * Vérifie la cohérence du compte rendu.
     *
     * @param cible la case visée
     * @param resultat le résultat du tir
     * @param bateauCoule le type du bateau coulé, présent seulement quand le résultat est COULE
     */
    public Tir {
        Objects.requireNonNull(cible, "cible");
        Objects.requireNonNull(resultat, "resultat");
        Objects.requireNonNull(bateauCoule, "bateauCoule");
        if ((resultat == ResultatTir.COULE) != bateauCoule.isPresent()) {
            throw new IllegalArgumentException("Le type du bateau coulé accompagne un tir COULE, et lui seul.");
        }
    }

    /**
     * Construit le compte rendu d'un tir sans bateau coulé.
     *
     * @param cible la case visée
     * @param resultat MANQUE, TOUCHE ou DEJA_JOUE
     * @return le compte rendu
     */
    public static Tir de(Coordonnee cible, ResultatTir resultat) {
        return new Tir(cible, resultat, Optional.empty());
    }

    /**
     * Construit le compte rendu d'un tir qui coule un bateau.
     *
     * @param cible la case visée
     * @param type le type du bateau coulé
     * @return le compte rendu
     */
    public static Tir coule(Coordonnee cible, TypeBateau type) {
        return new Tir(cible, ResultatTir.COULE, Optional.of(type));
    }

    /**
     * Décrit le tir pour un message, par exemple « B7 : coulé (le croiseur) ».
     *
     * @return la description du tir
     */
    public String decrire() {
        String description = cible + " : " + resultat.getLibelle();
        return bateauCoule.map(type -> description + " (" + type.getNomAvecArticle() + ")").orElse(description);
    }
}
