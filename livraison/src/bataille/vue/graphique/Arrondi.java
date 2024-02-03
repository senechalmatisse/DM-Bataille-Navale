package bataille.vue.graphique;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.Border;

/**
 * Une classe permettant de paramétrer les
 * bordures des composants de l'interface.
 * 
 * @author Inconnu
 */
public class Arrondi implements Border {
    /** L'incrustation de chaque côté */
    private int radiant;
    
    /**
     * Construit une nouvelle instance
     * @param radiant L'incrustation de chaque côté.
     */
    public Arrondi(int radiant) {
        this.radiant = radiant;
    }
    
    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(this.radiant+1, this.radiant+1, this.radiant+2, this.radiant);
    }
    @Override
    public boolean isBorderOpaque() {
        return true;
    }
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int largeur, int hauteur) {
        int arc = 25;
        int adjusteXY = 1;
        int adjusteLH = 2;
        Graphics2D g2 = (Graphics2D)g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawRoundRect(x+adjusteXY, y+adjusteXY, largeur-adjusteLH, hauteur-adjusteLH, arc, arc);
    }
}