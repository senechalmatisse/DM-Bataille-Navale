package bataille.swing;

import bataille.ia.Difficulte;
import bataille.modele.ConfigurationPartie;
import bataille.modele.ModeleFlotte;
import bataille.partie.Joueur;
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;

/** La boîte de dialogue « Nouvelle partie » : nom, niveau, grille, flotte et mode de placement. */
final class DialogueParametres extends JDialog {
    private static final long serialVersionUID = 1L;

    /** Le nom du joueur. */
    private final JTextField champNom;
    /** Le niveau de l'ordinateur. */
    private final JComboBox<Difficulte> choixNiveau = new JComboBox<>(Difficulte.values());
    /** La taille de la grille. */
    private final JSpinner choixTaille;
    /** Le modèle de flotte. */
    private final JComboBox<ModeleFlotte> choixFlotte = new JComboBox<>(ModeleFlotte.values());
    /** Si le joueur place ses bateaux lui-même. */
    private final JCheckBox casePlacementManuel = new JCheckBox("Je place mes bateaux moi-même");
    /** Le message d'erreur quand la flotte ne tient pas dans la grille. */
    private final JLabel erreur = new JLabel(" ");
    /** Les paramètres validés, ou {@code null} si le joueur annule. */
    private transient ParametresPartie resultat;

    private DialogueParametres(Frame parent, ParametresPartie initiaux) {
        super(parent, "Nouvelle partie", true);
        champNom = new JTextField(initiaux.nomJoueur(), 16);
        choixNiveau.setSelectedItem(initiaux.difficulte());
        ConfigurationPartie configuration = initiaux.configuration();
        choixTaille = new JSpinner(new SpinnerNumberModel(configuration.taille(), ConfigurationPartie.TAILLE_MIN,
                ConfigurationPartie.TAILLE_MAX, 1));
        for (ModeleFlotte modele : ModeleFlotte.values()) {
            if (modele.getTypes().equals(configuration.flotte())) {
                choixFlotte.setSelectedItem(modele);
            }
        }
        casePlacementManuel.setSelected(initiaux.placementManuel());
        erreur.setForeground(new Color(0xB00020));

        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        ajouterLigne(formulaire, 0, "Votre nom :", champNom);
        ajouterLigne(formulaire, 1, "Niveau de l'ordinateur :", choixNiveau);
        ajouterLigne(formulaire, 2, "Taille de la grille :", choixTaille);
        ajouterLigne(formulaire, 3, "Flotte :", choixFlotte);
        ajouterLigne(formulaire, 4, "", casePlacementManuel);
        ajouterLigne(formulaire, 5, "", erreur);

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
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /**
     * Affiche la boîte de dialogue et attend la réponse du joueur.
     *
     * @param parent la fenêtre du jeu
     * @param initiaux les paramètres proposés au départ
     * @return les paramètres choisis, ou une valeur vide si le joueur annule
     */
    static Optional<ParametresPartie> demander(Frame parent, ParametresPartie initiaux) {
        DialogueParametres dialogue = new DialogueParametres(parent, initiaux);
        dialogue.setVisible(true);
        return Optional.ofNullable(dialogue.resultat);
    }

    private static void ajouterLigne(JPanel formulaire, int ligne, String libelle, JComponent champ) {
        GridBagConstraints contraintes = new GridBagConstraints();
        contraintes.gridy = ligne;
        contraintes.insets = new Insets(4, 4, 4, 4);
        contraintes.anchor = GridBagConstraints.LINE_END;
        formulaire.add(new JLabel(libelle), contraintes);
        contraintes.gridx = 1;
        contraintes.anchor = GridBagConstraints.LINE_START;
        contraintes.fill = GridBagConstraints.HORIZONTAL;
        formulaire.add(champ, contraintes);
    }

    private void valider() {
        String nom = champNom.getText().strip();
        if (nom.isEmpty()) {
            nom = ParametresPartie.NOM_PAR_DEFAUT;
        }
        if (nom.length() > Joueur.LONGUEUR_NOM_MAX) {
            nom = nom.substring(0, Joueur.LONGUEUR_NOM_MAX);
        }
        int taille = (Integer) choixTaille.getValue();
        ModeleFlotte modele = (ModeleFlotte) choixFlotte.getSelectedItem();
        try {
            ConfigurationPartie configuration = ConfigurationPartie.de(taille, modele);
            resultat = new ParametresPartie(nom, (Difficulte) choixNiveau.getSelectedItem(), configuration,
                    casePlacementManuel.isSelected());
            dispose();
        } catch (IllegalArgumentException e) {
            erreur.setText("<html><body style='width: 260px'>" + e.getMessage() + "</body></html>");
            pack();
        }
    }
}
