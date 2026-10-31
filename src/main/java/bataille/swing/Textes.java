package bataille.swing;

import bataille.partie.Annonces;
import bataille.partie.Joueur;

/** Petits utilitaires de texte pour l'interface. */
final class Textes {

    private Textes() {
    }

    /**
     * Protège un texte pour l'insérer dans un libellé HTML de Swing : un nom de joueur peut contenir {@code <}.
     *
     * @param texte le texte brut
     * @return le texte où {@code &}, {@code <} et {@code >} sont remplacés par leurs entités
     */
    static String html(String texte) {
        return texte.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Construit un libellé HTML qui revient à la ligne à la largeur donnée.
     *
     * @param texte le texte brut, dont les retours à la ligne sont conservés
     * @param largeur la largeur du texte, en pixels
     * @return le libellé HTML
     */
    static String paragraphe(String texte, int largeur) {
        // Le rendu HTML de Swing compte 1,3 pixel par « px » CSS, mais exactement 1 pixel par « pt ».
        return "<html><body style='width: " + largeur + "pt'>" + html(texte).replace("\n", "<br>") + "</body></html>";
    }

    /**
     * Nomme la flotte d'un joueur : « Flotte d'Alice », ou « Flotte de l'ordinateur ».
     *
     * @param joueur le joueur
     * @return le nom de sa flotte
     */
    static String flotte(Joueur joueur) {
        return joueur.estHumain() ? "Flotte " + Annonces.de(joueur.getNom()) : "Flotte de l'ordinateur";
    }
}
