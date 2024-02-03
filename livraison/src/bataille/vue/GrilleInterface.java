package bataille.vue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

import bataille.jeu.*;
import bataille.joueurs.*;
import bataille.jeu.bateaux.Bateau;
import bataille.vue.graphique.Arrondi;

/**
 * Une classe éxécutable avec interface qui permet à deux
 * joueurs de lancer une partie d'un jeu et d'y jouer.
 * 
 * @author Matisse Senechal, Ali Azou, Rafik Halit, Souleymane Barry, Université de Caen Normandie, France
 */
public class GrilleInterface extends JFrame implements ActionListener {
    /** 
     * La taille de la grille
     * Le nombre de bateaux
     * Le joueur n°1
     * Le joueur n°2
     * Le jeu auquel on veut jouer
     * La grille du joueur n°1
     * La grille du joueur n°2
     * La grille de boutons du joueur n°1
     * La grille de boutons du joueur n°2
     */
    private static final int TAILLE_GRILLE = 10, NB_BATEAUX = 4;
    private static final Color TOUCHE = Color.GREEN, MANQUE = Color.RED, PAS_CLIQUE = Color.WHITE;
    private static Joueur joueur1, joueur2;
    private static Jeu jeu;
    private JPanel grilleJ1, grilleJ2;
    private final JButton[][] grilleBoutonsJ1 = new JButton[TAILLE_GRILLE][TAILLE_GRILLE];
    private final JButton[][] grilleBoutonsJ2 = new JButton[TAILLE_GRILLE][TAILLE_GRILLE];

    /**
     * Construit une nouvelle instance
     * @param jeu Le jeu auquel on veut jouer
     */
    public GrilleInterface(Jeu jeu) {
        super("Bataille Navale");
        this.jeu = jeu;

        Container cp = this.getContentPane();
        cp.setLayout(new BorderLayout());

        JPanel grilleJoueurs = new JPanel(new GridLayout(1, 2, 50, 0));
        grilleJoueurs.setBackground(Color.LIGHT_GRAY);
        grilleJoueurs.setBorder(new Arrondi(15));
        grilleJ1 = new JPanel(new GridLayout(TAILLE_GRILLE + 1, TAILLE_GRILLE + 1));
        grilleJ2 = new JPanel(new GridLayout(TAILLE_GRILLE + 1, TAILLE_GRILLE + 1));

        //Ajoute une bordure de noire
        grilleJ1.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        grilleJ2.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        for (int ligne = 0; ligne <= TAILLE_GRILLE; ligne++) {
            for (int colonne = 0; colonne <= TAILLE_GRILLE; colonne++) {
                if (ligne == 0 && colonne == 0) {
                    grilleJ1.add(new JLabel(""));
                    grilleJ2.add(new JLabel(""));
                } else if (ligne == 0) {
                    grilleJ1.add(new JLabel(transformeLettre(colonne)), BorderLayout.CENTER);
                    grilleJ2.add(new JLabel(transformeLettre(colonne)), BorderLayout.CENTER);
                } else if (colonne == 0) {
                    grilleJ1.add(new JLabel("" + ligne));
                    grilleJ2.add(new JLabel("" + ligne));
                } else {
                    JButton boutonJ1 = new JButton("");
                    boutonJ1.setBorderPainted(false);
                    boutonJ1.setContentAreaFilled(false);
                    boutonJ1.setActionCommand("MerJ1," + ligne + "," + colonne);
                    boutonJ1.addActionListener(this);
                    grilleBoutonsJ1[ligne-1][colonne-1] = boutonJ1;
                    grilleJ1.add(boutonJ1);

                    JButton boutonJ2 = new JButton("");
                    Bateau caseG = jeu.recupCaseGrilleInit(ligne-1, colonne-1);
                    if(caseG != null)
                        boutonJ2.setBorder(new Arrondi(15));
                    else
                        boutonJ2.setBorderPainted(false);
                    boutonJ2.setContentAreaFilled(false);
                    boutonJ2.setEnabled(false);
                    boutonJ2.setActionCommand("MerJ2," + ligne + "," + colonne);
                    boutonJ2.addActionListener(this);
                    grilleBoutonsJ2[ligne-1][colonne-1] = boutonJ2;
                    grilleJ2.add(boutonJ2);
                }
            }
        }

        grilleJoueurs.add(grilleJ1, BorderLayout.WEST);
        grilleJoueurs.add(grilleJ2, BorderLayout.EAST);

        cp.add(grilleJoueurs, BorderLayout.CENTER);

        this.pack();
        //Afficher la fenêtre
        this.setVisible(true);
        //Affiche la fenêtre au centre de l'éxécution
        this.setLocationRelativeTo(null);
        //Termine le processus lorsqu'on ferme la fenêtre
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    /**
     * Met à jour le bouton sur lequel on a cliqué.
     * @param ligne La ligne de la grille
     * @param colonne La colonne de la grille
     * @param grilleBoutons La grille des boutons
     * @param couleur La couleur choisit
     */
    private void mettreAJourBoutons(int ligne, int colonne, JButton[][] grilleBoutons, boolean estBateau, Color couleur) {
        grilleBoutons[ligne][colonne].setContentAreaFilled(true);
        grilleBoutons[ligne][colonne].setBackground(couleur);
        grilleBoutons[ligne][colonne].setEnabled(false);
    }

    /**
     * Convertit une valeur en caractère
     * @param valeur La valeur à convertir en caractère
     */
    private String transformeLettre(int valeur) {
        Character conversionEnLettre = (char)(valeur+64);
        return conversionEnLettre.toString();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Joueur joueurActuel = jeu.getJoueurCourant();
        int ligne = -1;
        int colonne = -1;
        if(joueurActuel instanceof Humain)
            coupHumain(joueurActuel, ligne, colonne, e);
        else
            coupOrdinateur(joueurActuel, ligne, colonne);
        if(jeu.partieTerminee()) {
            JOptionPane.showMessageDialog(this, jeu.getGagnant().getNom() + " a gagné");
            System.exit(0);
        }
    }

    /**
     * Affiche les bordures du bateau coulé.
     * @param bateauCoule Le bateau coulé
     */
    private void afficheBateauCoule(Bateau bateauCoule) {
        int longueurBateau = bateauCoule.getLongueur();
        boolean directionBateau = bateauCoule.estHorizontal();
        int posX = bateauCoule.getPosX();
        int posY = bateauCoule.getPosY();
        int caseParcouruX = directionBateau ? 0 : 1;
        int caseParcouruY = directionBateau ? 1 : 0;

        for (int caseG=0; caseG < longueurBateau; caseG++) {
            int index = posX * grilleBoutonsJ1.length + posY;
            grilleBoutonsJ1[posX][posY].setBorderPainted(true);
            grilleBoutonsJ1[posX][posY].setBorder(new Arrondi(15));
            posX += caseParcouruX;
            posY += caseParcouruY;
        }
    }
    
    /**
     * Effectue le coup du joueur.
     * @param joueurActuel Le joueur courant
     * @param ligne La ligne de la grille
     * @param colonne La colonne de la grille
     */
    private void coupJoueur(Joueur joueurActuel, JButton[][] grilleBoutonsJoueur, int ligne, int colonne) {
        boolean estBateau = jeu.estUnBateau(ligne, colonne);
        Color couleur = estBateau ? TOUCHE : MANQUE;
        mettreAJourBoutons(ligne, colonne, grilleBoutonsJoueur, estBateau, couleur);
        jeu.setCaseGrilleTir(ligne, colonne);
    }

    /**
     * Effectue le coup du joueur humain.
     * @param joueurActuel Le joueur courant
     * @param ligne La ligne de la grille
     * @param colonne La colonne de la grille
     * @param clic Le clic effectué par le joueur
     */
    private void coupHumain(Joueur joueurActuel, int ligne, int colonne, ActionEvent clic) {
        String[] position = clic.getActionCommand().split(",");
        if (position[0].equals("MerJ1")) {
            ligne = Integer.parseInt(position[1])-1;
            colonne = Integer.parseInt(position[2])-1;
            coupJoueur(joueurActuel, grilleBoutonsJ1, ligne, colonne);
            if (jeu.estUnBateau(ligne, colonne)) {
                Bateau bateauTouche = jeu.recupCaseGrilleInitAdversaire(ligne, colonne);
                joueurActuel.ajoutPoint();
                if (jeu.bateauCoule(ligne, colonne)) {
                    afficheBateauCoule(bateauTouche);
                    String nomBateauCoule = bateauTouche.getNom();
                    JOptionPane.showMessageDialog(this, "Vous avez coulé un " + nomBateauCoule);
                }
            }
        }
        jeu.changeJoueur();
    }

    /**
     * Effectue le coup du joueur ordinateur.
     * @param joueurActuel Le joueur courant
     * @param ligne La ligne de la grille
     * @param colonne La colonne de la grille
     */
    private void coupOrdinateur(Joueur joueurActuel, int ligne, int colonne) {
        int[] coords;
        String nomOrdinateur = joueurActuel.getNom();
        do {
            coords = joueurActuel.coordonneesAlea(TAILLE_GRILLE, jeu);
            ligne = coords[0];
            colonne = coords[1];
        } while(!jeu.estDansLaGrille(ligne, colonne) || jeu.caseTouche(ligne, colonne));
        coupJoueur(joueurActuel, grilleBoutonsJ2, ligne, colonne);
        if(jeu.estUnBateau(ligne, colonne)) {
            if(jeu.bateauCoule(ligne, colonne)) {
                Bateau bateauCoule = jeu.recupCaseGrilleInitAdversaire(ligne, colonne);
                String nomBateauCoule = bateauCoule.getNom();
                JOptionPane.showMessageDialog(this, nomOrdinateur + " a coulé votre " + nomBateauCoule);
            }
            joueurActuel.ajoutPoint();
        }
        jeu.changeJoueur();
    }

    public static void main(String[] args) {
        joueur1 = new Humain();
        joueur2 = new Ordinateur();
        jeu = new Mer(joueur1, joueur2, TAILLE_GRILLE);
        jeu.getJoueurCourant().initListeBateaux(jeu, NB_BATEAUX);
        jeu.getJoueurCourant().initListeBateaux(jeu, NB_BATEAUX);
        new GrilleInterface(jeu);
    }
}
