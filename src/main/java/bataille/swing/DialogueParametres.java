package bataille.swing;

import bataille.ia.Difficulte;
import bataille.modele.Capacite;
import bataille.modele.ConfigurationPartie;
import bataille.modele.ModeleFlotte;
import bataille.partie.Joueur;
import bataille.partie.ModeJeu;
import bataille.partie.ParametresPartie;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;

/**
 * La boîte de dialogue « Nouvelle partie » : mode de jeu, noms, niveau de l'ordinateur, grille, flotte,
 * capacités spéciales et mode de placement.
 */
final class DialogueParametres extends JDialog {
    private static final long serialVersionUID = 1L;

    /** Le mode contre l'ordinateur. */
    private final JRadioButton choixOrdinateur = new JRadioButton(ModeJeu.CONTRE_ORDINATEUR.toString());
    /** Le mode à deux. */
    private final JRadioButton choixADeux = new JRadioButton(ModeJeu.DEUX_JOUEURS.toString());
    /** Le nom du premier joueur. */
    private final JTextField champNom1;
    /** Le libellé du nom du premier joueur, qui dépend du mode. */
    private final JLabel libelleNom1 = new JLabel();
    /** Le nom du second joueur. */
    private final JTextField champNom2;
    /** Le libellé du nom du second joueur. */
    private final JLabel libelleNom2 = new JLabel("Joueur 2 :");
    /** Le niveau de l'ordinateur. */
    private final JComboBox<Difficulte> choixNiveau = new JComboBox<>(Difficulte.values());
    /** Le libellé du niveau. */
    private final JLabel libelleNiveau = new JLabel("Niveau de l'ordinateur :");
    /** La taille de la grille. */
    private final JSpinner choixTaille;
    /** Le modèle de flotte. */
    private final JComboBox<ModeleFlotte> choixFlotte = new JComboBox<>(ModeleFlotte.values());
    /** Si les capacités spéciales sont activées. */
    private final JCheckBox caseCapacites = new JCheckBox("Capacités spéciales : radar, bombardement, torpille, "
            + "double tir");
    /** Si les joueurs placent leurs bateaux eux-mêmes. */
    private final JCheckBox casePlacementManuel = new JCheckBox();
    /** Le message d'erreur quand la flotte ne tient pas dans la grille. */
    private final JLabel erreur = new JLabel(" ");
    /** Les paramètres validés, ou {@code null} si le joueur annule. */
    private transient ParametresPartie resultat;

    private DialogueParametres(Frame parent, ParametresPartie initiaux) {
        super(parent, "Nouvelle partie", true);
        champNom1 = new JTextField(initiaux.nomJoueur1(), 16);
        champNom2 = new JTextField(initiaux.nomJoueur2(), 16);
        ButtonGroup modes = new ButtonGroup();
        modes.add(choixOrdinateur);
        modes.add(choixADeux);
        choixOrdinateur.setSelected(initiaux.contreLOrdinateur());
        choixADeux.setSelected(!initiaux.contreLOrdinateur());
        choixOrdinateur.addActionListener(evenement -> adapterAuMode());
        choixADeux.addActionListener(evenement -> adapterAuMode());
        choixNiveau.setSelectedItem(initiaux.difficulte());
        ConfigurationPartie configuration = initiaux.configuration();
        choixTaille = new JSpinner(new SpinnerNumberModel(configuration.taille(), ConfigurationPartie.TAILLE_MIN,
                ConfigurationPartie.TAILLE_MAX, 1));
        for (ModeleFlotte modele : ModeleFlotte.values()) {
            if (modele.getTypes().equals(configuration.flotte())) {
                choixFlotte.setSelectedItem(modele);
            }
        }
        caseCapacites.setSelected(configuration.capacites());
        StringBuilder aide = new StringBuilder("<html>Chaque capacité sert une fois par partie, à la place d'un tir :");
        for (Capacite capacite : Capacite.values()) {
            aide.append("<br>• ").append(capacite.getNom()).append(" : ").append(capacite.getDescription());
        }
        caseCapacites.setToolTipText(aide.append("</html>").toString());
        casePlacementManuel.setSelected(initiaux.placementManuel());
        erreur.setForeground(new Color(0xB00020));
        choixTaille.addChangeListener(evenement -> effacerErreur());
        choixFlotte.addActionListener(evenement -> effacerErreur());

        JPanel choixMode = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        choixMode.add(choixOrdinateur);
        choixMode.add(choixADeux);
        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        ajouterLigne(formulaire, 0, new JLabel("Mode :"), choixMode);
        ajouterLigne(formulaire, 1, libelleNom1, champNom1);
        ajouterLigne(formulaire, 2, libelleNom2, champNom2);
        ajouterLigne(formulaire, 3, libelleNiveau, choixNiveau);
        ajouterLigne(formulaire, 4, new JLabel("Taille de la grille :"), choixTaille);
        ajouterLigne(formulaire, 5, new JLabel("Flotte :"), choixFlotte);
        ajouterLigne(formulaire, 6, new JLabel(""), caseCapacites);
        ajouterLigne(formulaire, 7, new JLabel(""), casePlacementManuel);
        ajouterLigne(formulaire, 8, new JLabel(""), erreur);

        JButton jouer = new JButton("Jouer");
        jouer.addActionListener(evenement -> valider());
        JButton annuler = new JButton("Annuler");
        annuler.addActionListener(evenement -> dispose());
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boutons.add(annuler);
        boutons.add(jouer);
        getRootPane().setDefaultButton(jouer);
        getRootPane().registerKeyboardAction(evenement -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        getContentPane().add(formulaire, BorderLayout.CENTER);
        getContentPane().add(boutons, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        adapterAuMode();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /**
     * Affiche la boîte de dialogue et attend la réponse du joueur.
     *
     * @param parent la fenêtre du jeu
     * @param initiaux les paramètres proposés au départ, mode compris
     * @return les paramètres choisis, ou une valeur vide si le joueur annule
     */
    static Optional<ParametresPartie> demander(Frame parent, ParametresPartie initiaux) {
        DialogueParametres dialogue = new DialogueParametres(parent, initiaux);
        dialogue.setVisible(true);
        return Optional.ofNullable(dialogue.resultat);
    }

    private static void ajouterLigne(JPanel formulaire, int ligne, JLabel libelle, JComponent champ) {
        GridBagConstraints contraintes = new GridBagConstraints();
        contraintes.gridy = ligne;
        contraintes.insets = new Insets(4, 4, 4, 4);
        contraintes.anchor = GridBagConstraints.LINE_END;
        formulaire.add(libelle, contraintes);
        contraintes.gridx = 1;
        contraintes.anchor = GridBagConstraints.LINE_START;
        contraintes.fill = GridBagConstraints.HORIZONTAL;
        formulaire.add(champ, contraintes);
    }

    /** Montre les champs utiles au mode choisi : le second nom à deux, le niveau contre l'ordinateur. */
    private void adapterAuMode() {
        boolean aDeux = choixADeux.isSelected();
        libelleNom1.setText(aDeux ? "Joueur 1 :" : "Votre nom :");
        libelleNom2.setVisible(aDeux);
        champNom2.setVisible(aDeux);
        libelleNiveau.setVisible(!aDeux);
        choixNiveau.setVisible(!aDeux);
        casePlacementManuel.setText(aDeux ? "Chacun place ses bateaux lui-même" : "Je place mes bateaux moi-même");
        erreur.setText(" ");
        pack();
    }

    private static String nettoyer(String saisi, String parDefaut) {
        String nom = saisi.strip();
        if (nom.isEmpty()) {
            return parDefaut;
        }
        return nom.length() > Joueur.LONGUEUR_NOM_MAX ? nom.substring(0, Joueur.LONGUEUR_NOM_MAX) : nom;
    }

    private void valider() {
        boolean aDeux = choixADeux.isSelected();
        String nom1 = nettoyer(champNom1.getText(), ParametresPartie.NOM_PAR_DEFAUT);
        String nom2 = nettoyer(champNom2.getText(), ParametresPartie.NOM_PAR_DEFAUT_2);
        if (aDeux && nom1.equalsIgnoreCase(nom2)) {
            montrerErreur("Les deux joueurs doivent avoir des noms différents.");
            return;
        }
        int taille = (Integer) choixTaille.getValue();
        ModeleFlotte modele = (ModeleFlotte) choixFlotte.getSelectedItem();
        try {
            ConfigurationPartie configuration = ConfigurationPartie.de(taille, modele)
                    .avecCapacites(caseCapacites.isSelected());
            resultat = new ParametresPartie(aDeux ? ModeJeu.DEUX_JOUEURS : ModeJeu.CONTRE_ORDINATEUR, nom1, nom2,
                    (Difficulte) choixNiveau.getSelectedItem(), configuration, casePlacementManuel.isSelected());
            dispose();
        } catch (IllegalArgumentException e) {
            montrerErreur(e.getMessage());
        }
    }

    private void effacerErreur() {
        if (!" ".equals(erreur.getText())) {
            erreur.setText(" ");
            pack();
        }
    }

    private void montrerErreur(String message) {
        erreur.setText(Textes.paragraphe(message, 380));
        pack();
    }
}
