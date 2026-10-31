package bataille.swing;

import bataille.modele.Bateau;
import bataille.modele.Coordonnee;
import bataille.modele.EtatCase;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Une grille de jeu dessinée case par case.
 * <p>
 * Chaque état a une forme en plus de sa couleur, pour rester lisible sans distinguer les couleurs :
 * un bateau est un bloc gris, un tir manqué un point, une touche une croix sur fond orange, un bateau coulé
 * une croix sur fond rouge sombre entouré d'un contour.
 */
final class PanneauGrille extends JComponent {
    private static final long serialVersionUID = 1L;

    /** La couleur de l'eau. */
    static final Color EAU = new Color(0xDCEBF7);
    /** La couleur des lignes de la grille. */
    static final Color LIGNES = new Color(0x9DB9D5);
    /** La couleur d'un bateau intact. */
    static final Color BATEAU = new Color(0x6B7785);
    /** La couleur du point d'un tir manqué. */
    static final Color MANQUE = new Color(0x35506B);
    /** Le fond d'une case touchée. */
    static final Color TOUCHE = new Color(0xE8590C);
    /** Le fond d'une case d'un bateau coulé. */
    static final Color COULE = new Color(0x8B1A1A);
    /** Le contour de la case survolée. */
    private static final Color SURVOL = new Color(0xF0A500);
    /** Le contour du dernier tir reçu. */
    private static final Color DERNIER_TIR = new Color(0x111111);
    /** L'aperçu d'un bateau qui peut être placé. */
    private static final Color APERCU_VALIDE = new Color(43, 138, 62, 150);
    /** L'aperçu d'un bateau qui ne peut pas être placé. */
    private static final Color APERCU_INVALIDE = new Color(201, 42, 42, 150);
    /** La place réservée aux lettres et aux numéros. */
    private static final int MARGE = 26;

    /** Le nombre de lignes et de colonnes. */
    private int taille;
    /** L'état affiché de chaque case. */
    private transient Function<Coordonnee, EtatCase> etats = coordonnee -> EtatCase.INCONNUE;
    /** Les bateaux dont le contour est tracé. */
    private transient List<Bateau> contours = List.of();
    /** Les cases de l'aperçu de placement. */
    private transient List<Coordonnee> apercu = List.of();
    /** Si l'aperçu montre un placement possible. */
    private boolean apercuValide;
    /** La case sous la souris, ou {@code null}. */
    private transient Coordonnee survol;
    /** La case du dernier tir reçu, ou {@code null}. */
    private transient Coordonnee dernierTir;
    /** Si la grille réagit aux clics. */
    private boolean interactif;
    /** L'action d'un clic gauche sur une case. */
    private transient Consumer<Coordonnee> actionClic = coordonnee -> { };
    /** L'action quand la souris change de case ({@code null} quand elle quitte la grille). */
    private transient Consumer<Coordonnee> actionSurvol = coordonnee -> { };
    /** L'action d'un clic droit. */
    private transient Runnable actionClicDroit = () -> { };

    /**
     * Construit une grille vide.
     *
     * @param taille le nombre de lignes et de colonnes
     */
    PanneauGrille(int taille) {
        this.taille = taille;
        setToolTipText("");
        MouseAdapter souris = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent evenement) {
                changerSurvol(caseSous(evenement.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent evenement) {
                changerSurvol(null);
            }

            @Override
            public void mouseClicked(MouseEvent evenement) {
                Coordonnee coordonnee = caseSous(evenement.getPoint());
                if (!interactif || coordonnee == null) {
                    return;
                }
                if (SwingUtilities.isRightMouseButton(evenement)) {
                    actionClicDroit.run();
                } else if (SwingUtilities.isLeftMouseButton(evenement)) {
                    actionClic.accept(coordonnee);
                }
            }
        };
        addMouseListener(souris);
        addMouseMotionListener(souris);
    }

    /**
     * Change ce que montre la grille.
     *
     * @param nouvelleTaille le nombre de lignes et de colonnes
     * @param nouveauxEtats l'état de chaque case
     * @param nouveauxContours les bateaux dont le contour est tracé
     */
    void afficher(int nouvelleTaille, Function<Coordonnee, EtatCase> nouveauxEtats, List<Bateau> nouveauxContours) {
        boolean tailleChangee = nouvelleTaille != taille;
        taille = nouvelleTaille;
        etats = Objects.requireNonNull(nouveauxEtats, "nouveauxEtats");
        contours = List.copyOf(nouveauxContours);
        if (tailleChangee) {
            dernierTir = null;
            revalidate();
        }
        repaint();
    }

    /**
     * Rend la grille cliquable ou non ; le curseur en main signale une grille cliquable.
     *
     * @param actif si la grille réagit aux clics
     */
    void setInteractif(boolean actif) {
        interactif = actif;
        setCursor(Cursor.getPredefinedCursor(actif ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    /**
     * Montre l'aperçu d'un bateau à placer.
     *
     * @param cases les cases du bateau, éventuellement hors de la grille
     * @param valide si le bateau peut être placé là
     */
    void setApercu(List<Coordonnee> cases, boolean valide) {
        apercu = List.copyOf(cases);
        apercuValide = valide;
        repaint();
    }

    /**
     * Signale la case du dernier tir reçu.
     *
     * @param coordonnee la case, ou {@code null} pour ne rien signaler
     */
    void setDernierTir(Coordonnee coordonnee) {
        dernierTir = coordonnee;
        repaint();
    }

    /**
     * Retourne la case sous la souris.
     *
     * @return la case survolée, ou {@code null}
     */
    Coordonnee getSurvol() {
        return survol;
    }

    /**
     * Définit l'action d'un clic gauche.
     *
     * @param action reçoit la case cliquée
     */
    void surClic(Consumer<Coordonnee> action) {
        actionClic = Objects.requireNonNull(action, "action");
    }

    /**
     * Définit l'action quand la souris change de case.
     *
     * @param action reçoit la case survolée, ou {@code null} quand la souris quitte la grille
     */
    void surSurvol(Consumer<Coordonnee> action) {
        actionSurvol = Objects.requireNonNull(action, "action");
    }

    /**
     * Définit l'action d'un clic droit.
     *
     * @param action l'action à lancer
     */
    void surClicDroit(Runnable action) {
        actionClicDroit = Objects.requireNonNull(action, "action");
    }

    @Override
    public Dimension getPreferredSize() {
        int cote = Math.max(18, Math.min(36, 420 / taille));
        return new Dimension(MARGE + taille * cote + 2, MARGE + taille * cote + 2);
    }

    @Override
    public String getToolTipText(MouseEvent evenement) {
        Coordonnee coordonnee = caseSous(evenement.getPoint());
        return coordonnee == null ? null : coordonnee + " : " + libelle(etats.apply(coordonnee));
    }

    private void changerSurvol(Coordonnee coordonnee) {
        if (!Objects.equals(coordonnee, survol)) {
            survol = coordonnee;
            actionSurvol.accept(coordonnee);
            repaint();
        }
    }

    private int cote() {
        return Math.max(8, Math.min((getWidth() - MARGE - 2) / taille, (getHeight() - MARGE - 2) / taille));
    }

    private Coordonnee caseSous(Point point) {
        int cote = cote();
        if (point.x < MARGE || point.y < MARGE) {
            return null;
        }
        int colonne = (point.x - MARGE) / cote;
        int ligne = (point.y - MARGE) / cote;
        return ligne < taille && colonne < taille ? new Coordonnee(ligne, colonne) : null;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int cote = cote();
        dessinerReperes(g, cote);
        for (int ligne = 0; ligne < taille; ligne++) {
            for (int colonne = 0; colonne < taille; colonne++) {
                dessinerCase(g, etats.apply(new Coordonnee(ligne, colonne)), x(colonne, cote), y(ligne, cote), cote);
            }
        }
        g.setColor(LIGNES);
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= taille; i++) {
            g.drawLine(x(0, cote), y(i, cote), x(taille, cote), y(i, cote));
            g.drawLine(x(i, cote), y(0, cote), x(i, cote), y(taille, cote));
        }
        dessinerContours(g, cote);
        dessinerAccents(g, cote);
        g.dispose();
    }

    private void dessinerReperes(Graphics2D g, int cote) {
        g.setColor(getForeground());
        g.setFont(getFont().deriveFont(Font.BOLD, Math.max(10f, Math.min(14f, cote * 0.4f))));
        FontMetrics mesures = g.getFontMetrics();
        for (int i = 0; i < taille; i++) {
            String lettre = Coordonnee.lettreColonne(i);
            g.drawString(lettre, x(i, cote) + (cote - mesures.stringWidth(lettre)) / 2, MARGE - 8);
            String numero = String.valueOf(i + 1);
            g.drawString(numero, MARGE - 6 - mesures.stringWidth(numero),
                    y(i, cote) + (cote + mesures.getAscent()) / 2 - 2);
        }
    }

    private void dessinerContours(Graphics2D g, int cote) {
        g.setStroke(new BasicStroke(Math.max(2f, cote / 12f)));
        for (Bateau bateau : contours) {
            Coordonnee debut = bateau.getOrigine();
            Coordonnee fin = bateau.getCases().get(bateau.getCases().size() - 1);
            g.setColor(bateau.estCoule() ? COULE.darker() : BATEAU.darker());
            int arrondi = cote / 2;
            g.drawRoundRect(x(debut.colonne(), cote) + 2, y(debut.ligne(), cote) + 2,
                    (fin.colonne() - debut.colonne() + 1) * cote - 4, (fin.ligne() - debut.ligne() + 1) * cote - 4,
                    arrondi, arrondi);
        }
    }

    private void dessinerAccents(Graphics2D g, int cote) {
        if (dernierTir != null && dernierTir.ligne() < taille && dernierTir.colonne() < taille) {
            g.setColor(DERNIER_TIR);
            g.setStroke(new BasicStroke(2f));
            g.drawRect(x(dernierTir.colonne(), cote) + 1, y(dernierTir.ligne(), cote) + 1, cote - 2, cote - 2);
        }
        g.setColor(apercuValide ? APERCU_VALIDE : APERCU_INVALIDE);
        for (Coordonnee coordonnee : apercu) {
            if (coordonnee.ligne() >= 0 && coordonnee.ligne() < taille
                    && coordonnee.colonne() >= 0 && coordonnee.colonne() < taille) {
                g.fillRect(x(coordonnee.colonne(), cote) + 1, y(coordonnee.ligne(), cote) + 1, cote - 1, cote - 1);
            }
        }
        if (interactif && survol != null && apercu.isEmpty()) {
            g.setColor(SURVOL);
            g.setStroke(new BasicStroke(3f));
            g.drawRect(x(survol.colonne(), cote) + 2, y(survol.ligne(), cote) + 2, cote - 4, cote - 4);
        }
    }

    private static int x(int colonne, int cote) {
        return MARGE + colonne * cote;
    }

    private static int y(int ligne, int cote) {
        return MARGE + ligne * cote;
    }

    /**
     * Dessine une case dans un état donné, avec sa forme et sa couleur.
     *
     * @param g le contexte de dessin
     * @param etat l'état de la case
     * @param x l'abscisse du coin haut gauche
     * @param y l'ordonnée du coin haut gauche
     * @param cote la largeur de la case
     */
    static void dessinerCase(Graphics2D g, EtatCase etat, int x, int y, int cote) {
        g.setColor(EAU);
        g.fillRect(x, y, cote, cote);
        int marge = Math.max(2, cote / 9);
        if (etat == EtatCase.BATEAU) {
            g.setColor(BATEAU);
            g.fillRoundRect(x + marge, y + marge, cote - 2 * marge, cote - 2 * marge, 2 * marge, 2 * marge);
        } else if (etat == EtatCase.MANQUE) {
            int diametre = Math.max(4, cote / 4);
            g.setColor(MANQUE);
            g.fillOval(x + (cote - diametre) / 2, y + (cote - diametre) / 2, diametre, diametre);
        } else if (etat == EtatCase.TOUCHE || etat == EtatCase.COULE) {
            g.setColor(etat == EtatCase.TOUCHE ? TOUCHE : COULE);
            g.fillRect(x + 1, y + 1, cote - 1, cote - 1);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(Math.max(2f, cote / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int debut = cote / 4;
            int fin = cote - cote / 4;
            g.drawLine(x + debut, y + debut, x + fin, y + fin);
            g.drawLine(x + fin, y + debut, x + debut, y + fin);
        }
    }

    /**
     * Retourne le libellé d'un état, pour les infobulles et la légende.
     *
     * @param etat l'état de la case
     * @return le libellé
     */
    static String libelle(EtatCase etat) {
        return switch (etat) {
            case EAU -> "eau";
            case BATEAU -> "bateau";
            case INCONNUE -> "pas encore visée";
            case MANQUE -> "manqué";
            case TOUCHE -> "touché";
            case COULE -> "coulé";
        };
    }
}
