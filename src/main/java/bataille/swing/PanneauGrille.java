package bataille.swing;

import bataille.modele.Bateau;
import bataille.modele.Coordonnee;
import bataille.modele.Detection;
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
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Une grille de jeu dessinée case par case.
 * <p>
 * Chaque état a une forme en plus de sa couleur, pour rester lisible sans distinguer les couleurs :
 * un bateau est un bloc gris, un tir manqué un point, une touche une croix sur fond orange, un bateau coulé
 * une croix sur fond rouge sombre entouré d'un contour, une case vide d'après le radar des hachures.
 * Par-dessus s'affichent les zones sondées par radar, les cases du dernier coup, l'aperçu d'un placement ou d'une
 * capacité, et les animations. Une grille peut aussi être cachée, quand deux joueurs se relaient.
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
    /** Le fond d'une case déclarée vide par le radar. */
    static final Color VIDE = new Color(0xF3F8FC);
    /** La couleur des zones de radar. */
    static final Color RADAR = new Color(0x0B7285);
    /** Le contour de la case survolée. */
    private static final Color SURVOL = new Color(0xF0A500);
    /** Le contour des cases du dernier coup. */
    private static final Color MARQUE = new Color(0x111111);
    /** L'aperçu d'un bateau qui peut être placé. */
    private static final Color APERCU_VALIDE = new Color(43, 138, 62, 150);
    /** L'aperçu d'un bateau qui ne peut pas être placé. */
    private static final Color APERCU_INVALIDE = new Color(201, 42, 42, 150);
    /** Les cases visées par une capacité. */
    private static final Color VISEE = new Color(240, 165, 0, 120);
    /** Le contour de la zone visée par une capacité. */
    private static final Color VISEE_CONTOUR = new Color(0xB86E00);
    /** Le fond d'une grille cachée. */
    private static final Color CACHE = new Color(0x1D2B3A);
    /** Les ronds dans l'eau d'un tir manqué. */
    private static final Color ONDE = new Color(0x1C7ED6);
    /** La place réservée aux lettres et aux numéros. */
    private static final int MARGE = 26;
    /** L'intervalle entre deux images des animations, en millisecondes. */
    private static final int IMAGE_MS = 30;

    /** Le nombre de lignes et de colonnes. */
    private int taille;
    /** L'état affiché de chaque case. */
    private transient Function<Coordonnee, EtatCase> etats = coordonnee -> EtatCase.INCONNUE;
    /** Les bateaux dont le contour est tracé. */
    private transient List<Bateau> contours = List.of();
    /** Les zones sondées par radar. */
    private transient List<Detection> detections = List.of();
    /** Les cases de l'aperçu de placement. */
    private transient List<Coordonnee> apercu = List.of();
    /** Si l'aperçu montre un placement possible. */
    private boolean apercuValide;
    /** Les cases visées par la capacité en cours de choix. */
    private transient List<Coordonnee> visee = List.of();
    /** Les cases du dernier coup, encadrées. */
    private transient List<Coordonnee> marques = List.of();
    /** Le message qui remplace la grille quand elle est cachée, ou {@code null} si elle est visible. */
    private String cache;
    /** La case sous la souris, ou {@code null}. */
    private transient Coordonnee survol;
    /** Si la grille réagit aux clics. */
    private boolean interactif;
    /** L'action d'un clic gauche sur une case. */
    private transient Consumer<Coordonnee> actionClic = coordonnee -> { };
    /** L'action quand la souris change de case ({@code null} quand elle quitte la grille). */
    private transient Consumer<Coordonnee> actionSurvol = coordonnee -> { };
    /** L'action d'un clic droit. */
    private transient Runnable actionClicDroit = () -> { };
    /** Les animations en cours. */
    private final transient List<Animation> animations = new ArrayList<>();
    /** L'horloge des animations, arrêtée quand il n'y en a aucune. */
    private final Timer horloge;

    /**
     * Construit une grille vide.
     *
     * @param taille le nombre de lignes et de colonnes
     */
    PanneauGrille(int taille) {
        this.taille = taille;
        setToolTipText("");
        horloge = new Timer(IMAGE_MS, evenement -> avancerAnimations());
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
                if (!interactif || cache != null || coordonnee == null) {
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
            marques = List.of();
            detections = List.of();
            survol = null;
            arreterAnimations();
            revalidate();
        }
        repaint();
    }

    /**
     * Change les zones de radar affichées.
     *
     * @param nouvelles les détections du plateau affiché
     */
    void setDetections(List<Detection> nouvelles) {
        detections = List.copyOf(nouvelles);
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
     * Indique si la grille réagit aux clics.
     *
     * @return si la grille est interactive
     */
    boolean estInteractif() {
        return interactif;
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
     * Montre les cases que viserait une capacité.
     *
     * @param cases les cases visées, vide pour ne rien montrer
     */
    void setVisee(List<Coordonnee> cases) {
        visee = List.copyOf(cases);
        repaint();
    }

    /**
     * Encadre les cases du dernier coup.
     *
     * @param cases les cases à encadrer, vide pour n'en encadrer aucune
     */
    void setMarques(Collection<Coordonnee> cases) {
        marques = List.copyOf(cases);
        repaint();
    }

    /**
     * Cache la grille derrière un message, ou la montre à nouveau.
     *
     * @param message le message affiché à la place de la grille, sur une ou plusieurs lignes, ou {@code null}
     *        pour montrer la grille
     */
    void setCache(String message) {
        cache = message;
        if (message != null) {
            arreterAnimations();
        }
        repaint();
    }

    /**
     * Retourne la case sous la souris.
     *
     * @return la case survolée, ou {@code null}
     */
    Coordonnee getSurvol() {
        return survol != null && dansLaGrille(survol) ? survol : null;
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

    /**
     * Joue une animation sur des cases.
     *
     * @param effet l'animation
     * @param cases la case visée, ou les cases du bateau coulé ou de la zone sondée
     */
    void lancerEffet(Effet effet, List<Coordonnee> cases) {
        if (cache != null || cases.isEmpty()) {
            return;
        }
        animations.add(new Animation(effet, List.copyOf(cases), System.currentTimeMillis()));
        if (!horloge.isRunning()) {
            horloge.start();
        }
        repaint();
    }

    /** Arrête les animations en cours. */
    void arreterAnimations() {
        animations.clear();
        horloge.stop();
        repaint();
    }

    /**
     * Indique si une animation est en cours.
     *
     * @return vrai tant qu'une animation se joue
     */
    boolean animationEnCours() {
        return !animations.isEmpty();
    }

    @Override
    public Dimension getPreferredSize() {
        int cote = Math.max(16, Math.min(36, 400 / taille));
        return new Dimension(MARGE + taille * cote + 2, MARGE + taille * cote + 2);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(MARGE + taille * 12 + 2, MARGE + taille * 12 + 2);
    }

    @Override
    public String getToolTipText(MouseEvent evenement) {
        Coordonnee coordonnee = caseSous(evenement.getPoint());
        if (cache != null || coordonnee == null) {
            return null;
        }
        String texte = coordonnee + " : " + libelle(etats.apply(coordonnee));
        Optional<Detection> zone = detections.stream().filter(detection -> detection.couvre(coordonnee)).findFirst();
        if (zone.isPresent()) {
            Detection detection = zone.get();
            int nombre = detection.nombre();
            texte += " ; radar en " + detection.centre() + " : " + (nombre == 0 ? "aucun bateau"
                    : nombre + (nombre > 1 ? " cases de bateaux, " : " case de bateau, ")
                    + detection.restantes(etats) + " encore à trouver");
        }
        return texte;
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

    private boolean dansLaGrille(Coordonnee coordonnee) {
        return coordonnee.ligne() >= 0 && coordonnee.ligne() < taille
                && coordonnee.colonne() >= 0 && coordonnee.colonne() < taille;
    }

    private void avancerAnimations() {
        long maintenant = System.currentTimeMillis();
        animations.removeIf(animation -> maintenant - animation.debut() >= animation.effet().getDuree());
        if (animations.isEmpty()) {
            horloge.stop();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int cote = cote();
        if (cache != null) {
            dessinerCache(g, cote);
            g.dispose();
            return;
        }
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
        dessinerDetections(g, cote);
        dessinerAccents(g, cote);
        dessinerAnimations(g, cote);
        g.dispose();
    }

    private void dessinerCache(Graphics2D g, int cote) {
        int largeur = taille * cote + 2;
        g.setColor(CACHE);
        g.fillRoundRect(MARGE, MARGE, largeur, largeur, 14, 14);
        g.setColor(new Color(255, 255, 255, 28));
        g.setStroke(new BasicStroke(2f));
        for (int vague = 1; vague < 6; vague++) {
            int hauteur = MARGE + vague * largeur / 6;
            for (int x = MARGE + 10; x < MARGE + largeur - 30; x += 28) {
                g.drawArc(x, hauteur, 14, 8, 0, 180);
                g.drawArc(x + 14, hauteur, 14, 8, 180, 180);
            }
        }
        g.setColor(Color.WHITE);
        g.setFont(getFont().deriveFont(Font.BOLD, 15f));
        FontMetrics mesures = g.getFontMetrics();
        String[] lignes = cache.split("\n");
        int hauteurTexte = lignes.length * mesures.getHeight();
        int yTexte = MARGE + (largeur - hauteurTexte) / 2 + mesures.getAscent();
        for (String ligne : lignes) {
            g.drawString(ligne, MARGE + (largeur - mesures.stringWidth(ligne)) / 2, yTexte);
            yTexte += mesures.getHeight();
        }
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
            Rectangle zone = zone(bateau.getCases(), cote);
            g.setColor(bateau.estCoule() ? COULE.darker() : BATEAU.darker());
            int arrondi = cote / 2;
            g.drawRoundRect(zone.x + 2, zone.y + 2, zone.width - 4, zone.height - 4, arrondi, arrondi);
        }
    }

    private void dessinerDetections(Graphics2D g, int cote) {
        float[] tirets = {6f, 4f};
        int diametre = Math.max(14, Math.round(cote * 0.45f));
        g.setFont(getFont().deriveFont(Font.BOLD, diametre * 0.62f));
        FontMetrics mesures = g.getFontMetrics();
        for (Detection detection : detections) {
            Rectangle zone = zone(detection.cases(), cote);
            boolean epuisee = detection.restantes(etats) == 0;
            Color couleur = epuisee ? new Color(0x868E96) : RADAR;
            g.setColor(couleur);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, tirets, 0f));
            g.drawRoundRect(zone.x + 2, zone.y + 2, zone.width - 4, zone.height - 4, 8, 8);
            g.fillOval(zone.x + 4, zone.y + 4, diametre, diametre);
            g.setColor(Color.WHITE);
            String nombre = Integer.toString(detection.nombre());
            g.drawString(nombre, zone.x + 4 + (diametre - mesures.stringWidth(nombre)) / 2,
                    zone.y + 4 + (diametre + mesures.getAscent() - mesures.getDescent()) / 2);
        }
    }

    private void dessinerAccents(Graphics2D g, int cote) {
        g.setColor(MARQUE);
        g.setStroke(new BasicStroke(2f));
        for (Coordonnee coordonnee : marques) {
            if (dansLaGrille(coordonnee)) {
                g.drawRect(x(coordonnee.colonne(), cote) + 1, y(coordonnee.ligne(), cote) + 1, cote - 2, cote - 2);
            }
        }
        g.setColor(apercuValide ? APERCU_VALIDE : APERCU_INVALIDE);
        for (Coordonnee coordonnee : apercu) {
            if (dansLaGrille(coordonnee)) {
                g.fillRect(x(coordonnee.colonne(), cote) + 1, y(coordonnee.ligne(), cote) + 1, cote - 1, cote - 1);
            }
        }
        List<Coordonnee> viseeDansLaGrille = visee.stream().filter(this::dansLaGrille).toList();
        if (!viseeDansLaGrille.isEmpty()) {
            g.setColor(VISEE);
            for (Coordonnee coordonnee : viseeDansLaGrille) {
                g.fillRect(x(coordonnee.colonne(), cote) + 1, y(coordonnee.ligne(), cote) + 1, cote - 1, cote - 1);
            }
            Rectangle zone = zone(viseeDansLaGrille, cote);
            g.setColor(VISEE_CONTOUR);
            g.setStroke(new BasicStroke(3f));
            g.drawRect(zone.x + 1, zone.y + 1, zone.width - 2, zone.height - 2);
        }
        if (interactif && survol != null && apercu.isEmpty() && visee.isEmpty()) {
            g.setColor(SURVOL);
            g.setStroke(new BasicStroke(3f));
            g.drawRect(x(survol.colonne(), cote) + 2, y(survol.ligne(), cote) + 2, cote - 4, cote - 4);
        }
    }

    private void dessinerAnimations(Graphics2D g, int cote) {
        long maintenant = System.currentTimeMillis();
        for (Animation animation : animations) {
            float avancement = Math.min(1f, (maintenant - animation.debut()) / (float) animation.effet().getDuree());
            switch (animation.effet()) {
                case ECLABOUSSURE -> eclabousser(g, animation.cases().get(0), cote, avancement);
                case EXPLOSION -> exploser(g, animation.cases().get(0), cote, avancement);
                case NAUFRAGE -> faireClignoter(g, animation.cases(), cote, avancement);
                case BALAYAGE -> balayer(g, animation.cases(), cote, avancement);
                default -> throw new IllegalStateException("Animation inconnue : " + animation.effet());
            }
        }
    }

    private static void eclabousser(Graphics2D g, Coordonnee cible, int cote, float avancement) {
        int cx = x(cible.colonne(), cote) + cote / 2;
        int cy = y(cible.ligne(), cote) + cote / 2;
        int opacite = Math.round(230 * (1 - avancement));
        g.setColor(new Color(ONDE.getRed(), ONDE.getGreen(), ONDE.getBlue(), opacite));
        g.setStroke(new BasicStroke(Math.max(1.5f, cote / 14f)));
        for (int anneau = 0; anneau < 2; anneau++) {
            float progression = avancement - anneau * 0.25f;
            if (progression > 0) {
                int rayon = Math.round(cote * (0.12f + 0.6f * progression));
                g.drawOval(cx - rayon, cy - rayon, 2 * rayon, 2 * rayon);
            }
        }
        int goutte = Math.max(2, cote / 10);
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3 + 0.3;
            double distance = cote * (0.15 + 0.45 * avancement);
            int gx = cx + (int) Math.round(Math.cos(angle) * distance);
            int gy = cy + (int) Math.round(Math.sin(angle) * distance - cote * 0.25 * Math.sin(Math.PI * avancement));
            g.fillOval(gx - goutte / 2, gy - goutte / 2, goutte, goutte);
        }
    }

    private static void exploser(Graphics2D g, Coordonnee cible, int cote, float avancement) {
        float cx = x(cible.colonne(), cote) + cote / 2f;
        float cy = y(cible.ligne(), cote) + cote / 2f;
        float rayon = Math.max(1f, cote * (0.2f + 0.6f * avancement));
        int opacite = Math.round(255 * (1 - avancement));
        g.setPaint(new RadialGradientPaint(cx, cy, rayon, new float[] {0f, 0.45f, 1f}, new Color[] {
            new Color(255, 245, 160, opacite), new Color(255, 140, 0, opacite), new Color(200, 30, 0, 0)}));
        g.fill(new Ellipse2D.Float(cx - rayon, cy - rayon, 2 * rayon, 2 * rayon));
        g.setColor(new Color(255, 200, 60, opacite));
        g.setStroke(new BasicStroke(Math.max(1.5f, cote / 16f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4 + Math.PI / 8;
            double debut = cote * (0.25 + 0.3 * avancement);
            double fin = cote * (0.4 + 0.45 * avancement);
            int x1 = Math.round(cx + (float) (Math.cos(angle) * debut));
            int y1 = Math.round(cy + (float) (Math.sin(angle) * debut));
            int x2 = Math.round(cx + (float) (Math.cos(angle) * fin));
            int y2 = Math.round(cy + (float) (Math.sin(angle) * fin));
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private static void faireClignoter(Graphics2D g, List<Coordonnee> cases, int cote, float avancement) {
        float intensite = (float) Math.abs(Math.sin(avancement * 3 * Math.PI)) * (1 - avancement / 2);
        g.setColor(new Color(255, 255, 255, Math.round(190 * intensite)));
        for (Coordonnee coordonnee : cases) {
            g.fillRect(x(coordonnee.colonne(), cote) + 1, y(coordonnee.ligne(), cote) + 1, cote - 1, cote - 1);
        }
        Rectangle zone = zone(cases, cote);
        g.setColor(new Color(255, 60, 60, Math.round(255 * (1 - avancement))));
        g.setStroke(new BasicStroke(Math.max(3f, cote / 8f)));
        g.drawRoundRect(zone.x, zone.y, zone.width, zone.height, cote / 2, cote / 2);
    }

    private static void balayer(Graphics2D g, List<Coordonnee> cases, int cote, float avancement) {
        Rectangle zone = zone(cases, cote);
        Shape ancienne = g.getClip();
        g.clip(zone);
        float cx = (float) zone.getCenterX();
        float cy = (float) zone.getCenterY();
        float rayonMax = (float) Math.hypot(zone.width, zone.height) / 2;
        float rayon = rayonMax * avancement;
        int opacite = Math.round(210 * (1 - avancement));
        g.setColor(new Color(RADAR.getRed(), RADAR.getGreen(), RADAR.getBlue(), opacite / 3));
        g.fill(new Ellipse2D.Float(cx - rayon, cy - rayon, 2 * rayon, 2 * rayon));
        g.setColor(new Color(RADAR.getRed(), RADAR.getGreen(), RADAR.getBlue(), opacite));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Ellipse2D.Float(cx - rayon, cy - rayon, 2 * rayon, 2 * rayon));
        double angle = avancement * 2 * Math.PI;
        g.drawLine(Math.round(cx), Math.round(cy), Math.round(cx + (float) (Math.cos(angle) * rayonMax)),
                Math.round(cy + (float) (Math.sin(angle) * rayonMax)));
        g.setClip(ancienne);
    }

    /** Retourne le rectangle, en pixels, qui englobe des cases. */
    private static Rectangle zone(List<Coordonnee> cases, int cote) {
        int ligneMin = cases.stream().mapToInt(Coordonnee::ligne).min().orElse(0);
        int ligneMax = cases.stream().mapToInt(Coordonnee::ligne).max().orElse(0);
        int colonneMin = cases.stream().mapToInt(Coordonnee::colonne).min().orElse(0);
        int colonneMax = cases.stream().mapToInt(Coordonnee::colonne).max().orElse(0);
        return new Rectangle(x(colonneMin, cote), y(ligneMin, cote), (colonneMax - colonneMin + 1) * cote,
                (ligneMax - ligneMin + 1) * cote);
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
        g.setColor(etat == EtatCase.VIDE ? VIDE : EAU);
        g.fillRect(x, y, cote, cote);
        int marge = Math.max(2, cote / 9);
        switch (etat) {
            case BATEAU -> {
                g.setColor(BATEAU);
                g.fillRoundRect(x + marge, y + marge, cote - 2 * marge, cote - 2 * marge, 2 * marge, 2 * marge);
            }
            case MANQUE -> {
                int diametre = Math.max(4, cote / 4);
                g.setColor(MANQUE);
                g.fillOval(x + (cote - diametre) / 2, y + (cote - diametre) / 2, diametre, diametre);
            }
            case TOUCHE, COULE -> {
                g.setColor(etat == EtatCase.TOUCHE ? TOUCHE : COULE);
                g.fillRect(x + 1, y + 1, cote - 1, cote - 1);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(Math.max(2f, cote / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int debut = cote / 4;
                int fin = cote - cote / 4;
                g.drawLine(x + debut, y + debut, x + fin, y + fin);
                g.drawLine(x + fin, y + debut, x + debut, y + fin);
            }
            case VIDE -> {
                g.setColor(LIGNES);
                g.setStroke(new BasicStroke(1f));
                Shape ancienne = g.getClip();
                g.clipRect(x, y, cote, cote);
                for (int decalage = -cote; decalage < cote; decalage += Math.max(5, cote / 4)) {
                    g.drawLine(x + decalage, y + cote, x + decalage + cote, y);
                }
                g.setClip(ancienne);
            }
            default -> {
                // l'eau et les cases inconnues n'ont que leur fond
            }
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
            case VIDE -> "vide d'après le radar";
            case MANQUE -> "manqué";
            case TOUCHE -> "touché";
            case COULE -> "coulé";
        };
    }

    /**
     * Une animation en cours.
     *
     * @param effet l'animation
     * @param cases les cases animées
     * @param debut le moment où elle a commencé, en millisecondes
     */
    private record Animation(Effet effet, List<Coordonnee> cases, long debut) {
    }
}
