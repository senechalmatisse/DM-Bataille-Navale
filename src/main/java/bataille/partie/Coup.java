package bataille.partie;

import bataille.modele.Detection;
import bataille.modele.Ordre;
import bataille.modele.Tir;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Un coup joué : qui l'a joué, l'ordre donné et ce qu'il a produit.
 *
 * @param auteur le joueur qui a joué
 * @param ordre le tir simple ou la capacité utilisée
 * @param tirs les cases visées, dans l'ordre : une pour un tir simple, plusieurs pour un bombardement
 *             ou une torpille, aucune pour un radar ou un double tir
 * @param detection le résultat du radar, présent seulement pour un radar
 */
public record Coup(Joueur auteur, Ordre ordre, List<Tir> tirs, Optional<Detection> detection) {

    /**
     * Vérifie que le coup est complet.
     *
     * @param auteur le joueur qui a joué
     * @param ordre l'ordre donné
     * @param tirs les cases visées
     * @param detection le résultat du radar
     */
    public Coup {
        Objects.requireNonNull(auteur, "auteur");
        Objects.requireNonNull(ordre, "ordre");
        tirs = List.copyOf(tirs);
        Objects.requireNonNull(detection, "detection");
    }
}
