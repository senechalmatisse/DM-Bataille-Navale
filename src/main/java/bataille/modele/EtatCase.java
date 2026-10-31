package bataille.modele;

/**
 * Ce qu'un joueur voit d'une case.
 * <p>
 * Le propriétaire d'un plateau voit ses bateaux ({@link #EAU} ou {@link #BATEAU} tant que la case n'est pas visée) ;
 * l'adversaire ne voit que le résultat de ses tirs ({@link #INCONNUE} tant que la case n'est pas visée, ou
 * {@link #VIDE} si son radar y a constaté l'absence de bateau).
 */
public enum EtatCase {
    /** Case non visée, sans bateau : visible seulement par le propriétaire. */
    EAU,
    /** Case non visée occupée par un bateau : visible seulement par le propriétaire. */
    BATEAU,
    /** Case non visée vue par l'adversaire : son contenu est caché. */
    INCONNUE,
    /** Case non visée que le radar adverse a déclarée sans bateau. */
    VIDE,
    /** Case visée, sans bateau. */
    MANQUE,
    /** Case visée, occupée par un bateau pas encore coulé. */
    TOUCHE,
    /** Case visée, occupée par un bateau coulé. */
    COULE
}
