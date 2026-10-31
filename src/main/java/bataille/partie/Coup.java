package bataille.partie;

import bataille.modele.Tir;
import java.util.Objects;

/**
 * Un coup joué : qui a tiré, et le compte rendu du tir.
 *
 * @param tireur le joueur qui a tiré
 * @param tir le compte rendu du tir
 */
public record Coup(Joueur tireur, Tir tir) {

    /**
     * Vérifie que le coup est complet.
     *
     * @param tireur le joueur qui a tiré
     * @param tir le compte rendu du tir
     */
    public Coup {
        Objects.requireNonNull(tireur, "tireur");
        Objects.requireNonNull(tir, "tir");
    }
}
