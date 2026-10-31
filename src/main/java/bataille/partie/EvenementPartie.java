package bataille.partie;

import bataille.modele.Bateau;
import bataille.modele.Detection;
import bataille.modele.Ordre;
import bataille.modele.Tir;
import java.util.Objects;
import java.util.Optional;

/**
 * Ce qui se passe pendant une partie. Le moteur émet ces événements dans l'ordre, après avoir mis à jour son état :
 * les interfaces s'en servent pour les animations, le journal et les statistiques, sans connaître les règles.
 */
public sealed interface EvenementPartie {

    /**
     * Une case vient d'être visée, par un tir simple ou par une capacité.
     *
     * @param tireur le joueur qui a tiré
     * @param tir le compte rendu du tir
     */
    record TirEffectue(Joueur tireur, Tir tir) implements EvenementPartie {

        /**
         * Vérifie que l'événement est complet.
         *
         * @param tireur le joueur qui a tiré
         * @param tir le compte rendu du tir
         */
        public TirEffectue {
            Objects.requireNonNull(tireur, "tireur");
            Objects.requireNonNull(tir, "tir");
        }
    }

    /**
     * Un bateau vient d'être coulé.
     *
     * @param tireur le joueur qui l'a coulé
     * @param bateau le bateau coulé, avec ses cases
     */
    record BateauCoule(Joueur tireur, Bateau bateau) implements EvenementPartie {

        /**
         * Vérifie que l'événement est complet.
         *
         * @param tireur le joueur qui l'a coulé
         * @param bateau le bateau coulé
         */
        public BateauCoule {
            Objects.requireNonNull(tireur, "tireur");
            Objects.requireNonNull(bateau, "bateau");
        }
    }

    /**
     * Une capacité spéciale vient d'être utilisée. Ses tirs éventuels suivent, chacun dans un {@link TirEffectue}.
     *
     * @param joueur le joueur qui l'a utilisée
     * @param ordre l'ordre joué
     * @param detection le résultat du radar, présent seulement pour un radar
     */
    record CapaciteUtilisee(Joueur joueur, Ordre ordre, Optional<Detection> detection) implements EvenementPartie {

        /**
         * Vérifie que l'événement est complet.
         *
         * @param joueur le joueur qui l'a utilisée
         * @param ordre l'ordre joué
         * @param detection le résultat du radar, présent seulement pour un radar
         */
        public CapaciteUtilisee {
            Objects.requireNonNull(joueur, "joueur");
            Objects.requireNonNull(ordre, "ordre");
            Objects.requireNonNull(detection, "detection");
        }
    }

    /**
     * La main passe à l'autre joueur.
     *
     * @param joueurCourant le joueur dont c'est désormais le tour
     * @param numeroTour le numéro du tour qui commence, à partir de 1
     */
    record TourChange(Joueur joueurCourant, int numeroTour) implements EvenementPartie {

        /**
         * Vérifie que l'événement est complet.
         *
         * @param joueurCourant le joueur dont c'est désormais le tour
         * @param numeroTour le numéro du tour qui commence
         */
        public TourChange {
            Objects.requireNonNull(joueurCourant, "joueurCourant");
        }
    }

    /**
     * La partie est terminée : la flotte adverse du gagnant est entièrement coulée.
     *
     * @param gagnant le gagnant
     */
    record PartieTerminee(Joueur gagnant) implements EvenementPartie {

        /**
         * Vérifie que l'événement est complet.
         *
         * @param gagnant le gagnant
         */
        public PartieTerminee {
            Objects.requireNonNull(gagnant, "gagnant");
        }
    }
}
