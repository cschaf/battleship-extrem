package de.hsbremen.battleshipextreme.client;

import de.hsbremen.battleshipextreme.client.listener.ServerErrorListener;
import de.hsbremen.battleshipextreme.client.listener.ServerGameBrowserListener;
import de.hsbremen.battleshipextreme.client.listener.ServerObjectReceivedListener;
import de.hsbremen.battleshipextreme.model.FieldState;
import de.hsbremen.battleshipextreme.model.Game;
import de.hsbremen.battleshipextreme.model.Orientation;
import de.hsbremen.battleshipextreme.model.Settings;
import de.hsbremen.battleshipextreme.model.exception.*;
import de.hsbremen.battleshipextreme.model.network.IServerObjectReceivedListener;
import de.hsbremen.battleshipextreme.model.network.NetworkClient;
import de.hsbremen.battleshipextreme.model.player.AIPlayer;
import de.hsbremen.battleshipextreme.model.player.Player;
import de.hsbremen.battleshipextreme.model.player.PlayerType;
import de.hsbremen.battleshipextreme.model.player.Target;
import de.hsbremen.battleshipextreme.model.ship.Ship;
import de.hsbremen.battleshipextreme.model.ship.ShipType;
import de.hsbremen.battleshipextreme.network.ITransferable;
import de.hsbremen.battleshipextreme.network.TransferableObjectFactory;
import de.hsbremen.battleshipextreme.network.eventhandling.EventArgs;
import de.hsbremen.battleshipextreme.network.eventhandling.listener.IErrorListener;
import de.hsbremen.battleshipextreme.network.transfarableObject.NetGame;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Controller {

    private Game game;
    private GUI gui;
    private NetworkClient network;

    private IServerObjectReceivedListener serverObjectReceivedListener;
    private IErrorListener serverErrorListener;
    private ServerGameBrowserListener serverGameBrowserListener;
    private ITransferable lastTurn;
    private boolean playerIsReloading;

    public Controller(Game game, GUI gui) {
        this.game = game;
        this.gui = gui;
        this.network = new NetworkClient();

        this.serverErrorListener = new ServerErrorListener(this.gui, this.network);
        this.serverObjectReceivedListener = new ServerObjectReceivedListener(this.gui, game, network, this);

        addServerErrorListeners();

        addMenuListeners();
        addServerConnectionListener();
        addServerGameBrowserListeners();
    }

    public void initializeGame(Settings settings) throws Exception {
        if (settings != null) {
            game.initialize(settings);
        }

        initializeGameView();

        // wenn erster Spieler AI ist, automatisch anfangen
        if (game.getCurrentPlayer().getType() == PlayerType.SMART_AI) {
            placeAiShips();
        }
    }

    private void loadGame() {
        try {
            game.load(Settings.SAVEGAME_FILENAME);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        initializeGameView();

        boolean hasMadeTurn = game.hasCurrentPlayerMadeTurn();
        setPlayerBoardEnabled(false);
        setEnemySelectionEnabled(true);
        setEnemyBoardEnabled(!hasMadeTurn);
        setDoneButtonEnabled(hasMadeTurn);
        String message = hasMadeTurn ? game.getCurrentPlayer() + " has made his Turn" : game.getCurrentPlayer() + " is shooting";
        setInfoLabelMessage(message);
    }

    private void initializeGameView() {
        createBoardPanels(game.getBoardSize());
        gui.showPanel(GUI.GAME_PANEL);
        updateEnemyBoard();
        updatePlayerBoard();
        updateEnemySelection();
        setEnemySelectionEnabled(false);
        setEnemyBoardEnabled(false);
        setShipSelectionEnabled(false);
        setDoneButtonEnabled(false);
        setInfoLabelMessage(game.getCurrentPlayer() + " is placing ships ");
    }

    private void createBoardPanels(int boardSize) {
        GamePanel panelGame = gui.getPanelGame();
        if (panelGame.getPanelPlayerBoard().getComponentCount() > 0) {
            panelGame.getPanelPlayerBoard().removeAll();
            panelGame.getPanelEnemyBoard().removeAll();
        }
        panelGame.getPanelPlayerBoard().initializeBoardPanel("You", boardSize);
        panelGame.getPanelEnemyBoard().initializeBoardPanel("Enemy", boardSize);
        gui.getFrame().pack();
        addPlayerBoardListener();
        addEnemyBoardListener();
        addBoardMouseListener(panelGame.getPanelPlayerBoard().getButtonsField());
        addBoardMouseListener(panelGame.getPanelEnemyBoard().getButtonsField());
    }

    private void addMenuListeners() {
        gui.getMenuItemMainMenu().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.showPanel(GUI.MAIN_MENU_PANEL);
                setSaveButtonEnabled(false);
            }
        });

        gui.getMenuItemNewGame().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.showPanel(GUI.SETTINGS_PANEL);
                setSaveButtonEnabled(false);
            }
        });

        gui.getMenuItemSaveGame().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    game.save(Settings.SAVEGAME_FILENAME);
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });

        gui.getMenuItemLoadGame().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    loadGame();
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });

        gui.getMenuItemQuitGame().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        gui.getPanelMainMenu().getButtonLocalGame().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setupSettingsPanelForLocalGame();
                gui.showPanel(GUI.SETTINGS_PANEL);
            }
        });

        gui.getPanelMainMenu().getButtonLoadGame().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    loadGame();
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });

        gui.getPanelMainMenu().getButtonMultiplayerGame().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.showPanel(GUI.SERVER_CONNECTION_PANEL);
            }
        });

        gui.getPanelMainMenu().getButtonQuitGame().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        addNextLookAndFeelListener();
        addApplySettingsListener();
        addShipSelectionListeners();
        addEnemySelectionListener();
        addDoneButtonListener();
        addShowYourShipsButtonListener();
    }

    private void addNextLookAndFeelListener() {
        gui.getMenuItemNextLookAndFeel().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i;
                UIManager.LookAndFeelInfo[] lafInfo = UIManager.getInstalledLookAndFeels();
                for (i = 0; i < lafInfo.length; i++) {
                    if (lafInfo[i].getClassName() == UIManager.getLookAndFeel().getClass().getName()) {
                        break;
                    }
                }
                i = (i >= lafInfo.length - 1) ? i = 0 : i + 1;
                try {
                    UIManager.setLookAndFeel(lafInfo[i].getClassName());
                } catch (ClassNotFoundException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (InstantiationException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (IllegalAccessException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (UnsupportedLookAndFeelException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                SwingUtilities.updateComponentTreeUI(gui.getFrame());
                // gui.getFrame().pack();
            }
        });
    }

    public void appendGameLogEntry(String message){
        gui.getPanelGame().getTextAreaGameLog().append(message + "\r\n\r\n");
    }
    private void addPlayerBoardListener() {
        final GamePanel panelGame = gui.getPanelGame();
        JButton[][] playerBoard = panelGame.getPanelPlayerBoard().getButtonsField();
        for (int i = 0; i < playerBoard.length; i++) {
            for (int j = 0; j < playerBoard.length; j++) {
                playerBoard[i][j].addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        FieldButton fieldButton = (FieldButton) e.getSource();
                        try {
                            placeShip(fieldButton.getxPos(), fieldButton.getyPos(), panelGame.getRadioButtonHorizontalOrientation().isSelected());
                        } catch (ShipAlreadyPlacedException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        } catch (FieldOutOfBoardException e1) {
                            setInfoLabelMessage("Ship can not be placed here");
                            e1.printStackTrace();
                        } catch (ShipOutOfBoardException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    private void addBoardMouseListener(final JButton[][] board) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                board[i][j].addMouseListener(new MouseListener() {

                    public void mouseClicked(MouseEvent e) {
                        // TODO Auto-generated method stub

                    }

                    public void mousePressed(MouseEvent e) {
                        // TODO Auto-generated method stub

                    }

                    public void mouseReleased(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            GamePanel pg = gui.getPanelGame();
                            if (pg.getRadioButtonHorizontalOrientation().isSelected()) {
                                pg.getRadioButtonVerticalOrientation().setSelected(true);
                            } else {
                                pg.getRadioButtonHorizontalOrientation().setSelected(true);
                            }
                        }
                    }

                    public void mouseEntered(MouseEvent e) {
                        FieldButton fieldButton = (FieldButton) e.getSource();
                        updatePreview(fieldButton.getxPos(), fieldButton.getyPos(), board);
                    }

                    public void mouseExited(MouseEvent e) {
                        // TODO Auto-generated method stub

                    }
                });
            }
        }
    }

    private void addEnemyBoardListener() {
        final GamePanel panelGame = gui.getPanelGame();
        JButton[][] playerBoard = panelGame.getPanelEnemyBoard().getButtonsField();
        for (int i = 0; i < playerBoard.length; i++) {
            for (int j = 0; j < playerBoard.length; j++) {
                playerBoard[i][j].addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        FieldButton fieldButton = (FieldButton) e.getSource();
                        String attackingPlayerName = game.getCurrentPlayer().getName();
                        String attackedPlayerName = panelGame.getComboBoxEnemySelection().getSelectedItem().toString();
                        int xPos = fieldButton.getxPos();
                        int yPos = fieldButton.getyPos();
                        boolean isHorizontal = panelGame.getRadioButtonHorizontalOrientation().isSelected();
                        Ship currentShip = game.getCurrentPlayer().getCurrentShip();
                        String orientation = isHorizontal ? "horizontal" : "vertical";
                        if (network.isConnected()) {
                            lastTurn = TransferableObjectFactory.CreateTurn(attackingPlayerName, attackedPlayerName, xPos, yPos, isHorizontal, currentShip);
                            setEnemyBoardEnabled(false);
                            gui.getPanelGame().getButtonDone().setEnabled(true);
                        } else {
                            try {
                                boolean turnMade = makeTurn(attackedPlayerName, xPos, yPos, isHorizontal);
                                if (turnMade){
                                    appendGameLogEntry("Player " + attackingPlayerName + " attacked " + attackedPlayerName
                                            + " " + orientation + " with ship " + currentShip.getType().toString() + " at start Field X:" + xPos + "  Y: " + yPos);
                                }
                            } catch (FieldOutOfBoardException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                        }
                    }
                });
            }
        }
    }

    private void addApplySettingsListener() {
        gui.getPanelSettings().getButtonApplySettings().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SettingsPanel panelSettings = gui.getPanelSettings();
                int players = Integer.parseInt(panelSettings.getTextFieldPlayers().getText());
                int aiPlayers = Integer.parseInt(panelSettings.getTextFieldAiPlayers().getText());
                int dumbAiPlayers = 0;
                int boardSize = Integer.parseInt(panelSettings.getTextFieldBoardSize().getText());
                int destroyers = Integer.parseInt(panelSettings.getTextFieldDestroyers().getText());
                int frigates = Integer.parseInt(panelSettings.getTextFieldFrigates().getText());
                int corvettes = Integer.parseInt(panelSettings.getTextFieldCorvettes().getText());
                int submarines = Integer.parseInt(panelSettings.getTextFieldSubmarines().getText());
                String gameName = panelSettings.getTextFieldGameName().getText();
                String gamePassword = panelSettings.getTextFieldGamePassword().getText();
                Settings settings = new Settings(players, aiPlayers, dumbAiPlayers, boardSize, destroyers, frigates, corvettes, submarines);
                boolean valid = false;
                try {
                    settings.validate();
                    valid = true;
                } catch (InvalidPlayerNumberException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (InvalidNumberOfShipsException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (BoardTooSmallException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                if (valid) {
                    if (network.isConnected()) {
                        if (gameName.length() > 3 && !gameName.startsWith(" ")) {
                            network.getSender().sendGame(gameName, gamePassword, settings);
                            gui.showPanel(GUI.SERVER_CONNECTION_PANEL);
                        } else{
                            network.getErrorHandler().errorHasOccurred(new EventArgs<ITransferable>(this, TransferableObjectFactory.CreateMessage("Game name have to be more then 3 characters!")));
                        }
                    } else {
                        try {
                            initializeGame(settings);
                        } catch (Exception e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void addShipSelectionListeners() {
        gui.getPanelGame().getRadioButtonDestroyer().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectShip(ShipType.DESTROYER);
            }
        });
        gui.getPanelGame().getRadioButtonFrigate().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectShip(ShipType.FRIGATE);
            }
        });
        gui.getPanelGame().getRadioButtonCorvette().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectShip(ShipType.CORVETTE);
            }
        });
        gui.getPanelGame().getRadioButtonSubmarine().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectShip(ShipType.SUBMARINE);
            }
        });
    }

    private void addEnemySelectionListener() {
        GamePanel panelGame = gui.getPanelGame();
        final JComboBox<String> enemyComboBox = panelGame.getComboBoxEnemySelection();
        enemyComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateEnemyBoard();
            }
        });
    }

    private void addDoneButtonListener() {
        GamePanel panelGame = gui.getPanelGame();
        panelGame.getButtonDone().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (network.isConnected()) {
                    nextOnline();
                } else {
                    next();
                }
            }
        });
    }

    private void addShowYourShipsButtonListener() {
        GamePanel panelGame = gui.getPanelGame();
        panelGame.getButtonShowYourShips().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JToggleButton btn = (JToggleButton) e.getSource();
                if (btn.isSelected()) {
                    showPlayerBoard();
                } else {
                    if (network.isConnected()) {
                        showEmptyPlayerBoard(game.getConnectedAsPlayer());
                    } else {
                        showEmptyPlayerBoard(game.getCurrentPlayer().getName());
                    }
                }
            }
        });
    }

    private void showPlayerBoard() {
        updatePlayerBoard();
    }

    private void showEmptyPlayerBoard(String playerName) {
        FieldState[][] fieldStates = game.getPlayerByName(playerName).getFieldWithStateEmpty();
        updateBoardColors(gui.getPanelGame().getPanelPlayerBoard().getButtonsField(), fieldStates);
    }

    private void addServerObjectReceivedListeners() {
        network.addServerObjectReceivedListener(serverObjectReceivedListener);
    }

    private void addServerErrorListeners() {
        network.addErrorListener(serverErrorListener);
    }

    private void removeServerErrorListeners() {
        network.removeErrorListener(serverErrorListener);
    }

    private void removeServerObjectReceivedListeners() {
        network.removeServerObjectReceivedListener(serverObjectReceivedListener);
    }

    public void selectShip(ShipType shipType) {
        game.getCurrentPlayer().setCurrentShipByType(shipType);
    }

    private void placeShip(int xPos, int yPos, boolean isHorizontal) throws ShipAlreadyPlacedException, FieldOutOfBoardException, ShipOutOfBoardException {
        Orientation orientation = isHorizontal ? Orientation.HORIZONTAL : Orientation.VERTICAL;
        Player currentPlayer = game.getCurrentPlayer();

        boolean possible = currentPlayer.placeShip(xPos, yPos, orientation);
        if (possible) {
            currentPlayer.nextShip();
        }

        if (currentPlayer.hasPlacedAllShips()) {
            setPlayerBoardEnabled(false);
            setDoneButtonEnabled(true);
            setInfoLabelMessage(game.getCurrentPlayer() + " placed all ships");
        }

        updatePlayerBoard();
        updateShipSelection(game.getCurrentPlayer());
    }

    public boolean makeTurn(String enemyName, int xPos, int yPos, boolean isHorizontal) throws FieldOutOfBoardException {
        Orientation orientation = isHorizontal ? Orientation.HORIZONTAL : Orientation.VERTICAL;
        boolean possible = false;

        Player enemy = game.getPlayerByName(enemyName);
        possible = game.makeTurn(enemy, xPos, yPos, orientation);
        if (possible) {
            updateEnemyBoard();
            setEnemyBoardEnabled(false);
            setDoneButtonEnabled(true);
            setInfoLabelMessage(game.getCurrentPlayer() + " attacked " + enemy);
        }
        return possible;
    }

    public boolean makeOnlineTurn(String attackingPlayerName, String enemyName, int xPos, int yPos, boolean isHorizontal) throws FieldOutOfBoardException {
        Orientation orientation = isHorizontal ? Orientation.HORIZONTAL : Orientation.VERTICAL;
        boolean possible = false;
        Player enemy = game.getPlayerByName(enemyName);
        possible = game.makeTurn(enemy, xPos, yPos, orientation);
        // Hier wird noch nicht alles richtig angezeigt!
        if (possible) {
            if (enemy.getName().equals(game.getConnectedAsPlayer())) {
                updatePlayerBoard(game.getConnectedAsPlayer());
            }

            if (game.getConnectedAsPlayer().equals(attackingPlayerName)) {
                updateEnemyBoard();
            }

            setInfoLabelMessage(game.getCurrentPlayer() + " attacked " + enemy);
        }
        return possible;
    }

    public void nextOnline() {
        if (!game.isGameover()) {
            if (!game.isReady()) {
                if (game.getCurrentPlayer().hasPlacedAllShips()) {
                    // Schicke jetzt das Board an den Server
                    network.getSender().sendBoard(game.getCurrentPlayer().getBoard());
                    setBoardsEnabled(false);
                    gui.getPanelGame().getButtonDone().setEnabled(false);
                }
            } else {
                // alle Spieler habe ihre Schiffe gesetzt
                setBoardsEnabled(false);
                gui.getPanelGame().getButtonDone().setEnabled(false);
                if (lastTurn != null) {
                    network.getSender().sendTurn(lastTurn);
                    lastTurn = null;
                }

                if (playerIsReloading) {
                    network.getSender().sendPlayerIsReloading();
                    playerIsReloading = false;
                }
            }
        } else {
            network.getSender().sendPlayerWon();
        }
    }

    private void next() {
        if (!game.isGameover()) {
            if (!game.isReady()) {
                if (game.getCurrentPlayer().hasPlacedAllShips()) {
                    game.nextPlayer();
                    setInfoLabelMessage(game.getCurrentPlayer() + " is placing ships");
                    if (game.getCurrentPlayer().getType() == PlayerType.SMART_AI) {
                        placeAiShips();
                    } else {
                        setPlayerBoardEnabled(true);
                        setDoneButtonEnabled(false);
                    }
                }
            } else {
                gui.getPanelGame().getButtonShowYourShips().setEnabled(true);
                gui.getPanelGame().getButtonShowYourShips().setSelected(false);
                setEnemySelectionEnabled(true);
                setSaveButtonEnabled(true);
                game.nextPlayer();
                updateEnemySelection();
                if (game.getCurrentPlayer().areAllShipsReloading()) {
                    setInfoLabelMessage("All ships of " + game.getCurrentPlayer() + " are reloading");
                    setEnemyBoardEnabled(false);
                    setShipSelectionEnabled(false);
                } else {
                    if (game.getCurrentPlayer().getType() == PlayerType.SMART_AI) {
                        makeAiTurn();
                    } else {
                        setInfoLabelMessage(game.getCurrentPlayer() + " is shooting");
                        enableAvailableShips();
                        selectFirstAvailableShipType();
                        setEnemyBoardEnabled(true);
                        gui.getPanelGame().getButtonDone().setEnabled(false);
                    }
                }
            }
            if (gui.getPanelGame().getButtonShowYourShips().isSelected()) {
                updatePlayerBoard();
            }
            updateShipSelection(game.getCurrentPlayer());
        } else {
            setInfoLabelMessage(game.getWinner() + " won ");
        }
    }

    private void placeAiShips() {
        AIPlayer ai = (AIPlayer) game.getCurrentPlayer();
        try {
            ai.placeShips();
        } catch (ShipAlreadyPlacedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FieldOutOfBoardException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ShipOutOfBoardException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        setPlayerBoardEnabled(false);
        setDoneButtonEnabled(true);
        updatePlayerBoard();
    }

    private void makeAiTurn() {
        try {
            Target shot = game.makeAiTurn();
            appendGameLogEntry("Player " + game.getCurrentPlayer().getName() + " attacked " + gui.getPanelGame().getComboBoxEnemySelection().getSelectedItem().toString()
                    + " " + shot.getOrientation().toString() + " with ship " + game.getCurrentPlayer().getCurrentShip().getType()+ " at start Field X:" + shot.getX() + "  Y: " + shot.getY());

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        AIPlayer ai = (AIPlayer) game.getCurrentPlayer();
        Player currentEnemy = game.getPlayers()[ai.getCurrentEnemyIndex()];
        gui.getPanelGame().getComboBoxEnemySelection().setSelectedItem(currentEnemy.getName());
        setInfoLabelMessage(game.getCurrentPlayer() + " attacks " + currentEnemy);
        updateEnemyBoard();
    }

    public void setInfoLabelMessage(String message) {
        gui.getPanelGame().getLabelInfo().setText(message);
    }

    private void selectFirstAvailableShipType() {
        ShipType availableShipType = game.getCurrentPlayer().getTypeOFirstAvailableShip();
        switch (availableShipType) {
            case DESTROYER:
                gui.getPanelGame().getRadioButtonDestroyer().setSelected(true);
                break;
            case CORVETTE:
                gui.getPanelGame().getRadioButtonCorvette().setSelected(true);
                break;
            case FRIGATE:
                gui.getPanelGame().getRadioButtonFrigate().setSelected(true);
                break;
            case SUBMARINE:
                gui.getPanelGame().getRadioButtonSubmarine().setSelected(true);
            default:
                break;
        }
        selectShip(availableShipType);
    }

    public void updateShipSelection(Player player) {
        GamePanel panelGame = gui.getPanelGame();
        Color green = new Color(0, 180, 0);
        Color red = new Color(180, 0, 0);
        JLabel[] shipFields = gui.getPanelGame().getLabelDestroyer();
        if (player.getShipCount(ShipType.DESTROYER) == 0) {
            UpdateShipLabelColors(shipFields, red);
        } else {
            UpdateShipLabelColors(shipFields, green);
        }
        panelGame.getLabelDestroyerShipCount().setText("" + player.getShipCount(ShipType.DESTROYER));

        shipFields = gui.getPanelGame().getLabelFrigate();
        if (player.getShipCount(ShipType.FRIGATE) == 0) {
            UpdateShipLabelColors(shipFields, red);
        } else {
            UpdateShipLabelColors(shipFields, green);
        }
        panelGame.getLabelFrigateShipCount().setText("" + player.getShipCount(ShipType.FRIGATE));

        shipFields = gui.getPanelGame().getLabelCorvette();
        if (player.getShipCount(ShipType.CORVETTE) == 0) {
            UpdateShipLabelColors(shipFields, red);
        } else {
            UpdateShipLabelColors(shipFields, green);
        }
        panelGame.getLabelCorvetteShipCount().setText("" + player.getShipCount(ShipType.CORVETTE));

        shipFields = gui.getPanelGame().getLabelSubmarine();
        if (player.getShipCount(ShipType.SUBMARINE) == 0) {
            UpdateShipLabelColors(shipFields, red);
        } else {
            UpdateShipLabelColors(shipFields, green);
        }
        panelGame.getLabelSubmarineShipCount().setText("" + player.getShipCount(ShipType.SUBMARINE));
    }

    private void UpdateShipLabelColors(JLabel[] shipFields, Color color) {
        for (int i = 0; i < shipFields.length; i++) {
            shipFields[i].setBackground(color);
        }
    }

    public void updateEnemySelection() {
        GamePanel panelGame = gui.getPanelGame();
        ArrayList<Player> enemies = game.getEnemiesOfCurrentPlayer();
        JComboBox<String> enemyComboBox = panelGame.getComboBoxEnemySelection();
        enemyComboBox.removeAllItems();
        for (Player enemy : enemies) {
            enemyComboBox.addItem(enemy.getName());
        }
    }

    public void updateEnemyOnlineSelection(String playerName) {
        GamePanel panelGame = gui.getPanelGame();
        ArrayList<Player> enemies = game.getEnemiesOfPlayer(playerName);
        JComboBox<String> enemyComboBox = panelGame.getComboBoxEnemySelection();
        enemyComboBox.removeAllItems();
        for (Player enemy : enemies) {
            enemyComboBox.addItem(enemy.getName());
        }
    }

    public void updatePlayerBoard() {
        GamePanel panelGame = gui.getPanelGame();
        JButton[][] board;
        FieldState[][] fieldStates = null;
        board = panelGame.getPanelPlayerBoard().getButtonsField();
        try {
            fieldStates = game.getCurrentPlayer().getFieldStates(true);
            updateBoardColors(board, fieldStates);
        } catch (FieldOutOfBoardException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void updatePlayerBoard(String playerName) {
        GamePanel panelGame = gui.getPanelGame();
        JButton[][] board;
        FieldState[][] fieldStates = null;
        board = panelGame.getPanelPlayerBoard().getButtonsField();
        try {
            fieldStates = game.getPlayerByName(playerName).getFieldStates(true);
            updateBoardColors(board, fieldStates);
        } catch (FieldOutOfBoardException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void updateEnemyBoard() {
        GamePanel panelGame = gui.getPanelGame();
        JButton[][] board;
        board = panelGame.getPanelEnemyBoard().getButtonsField();
        Player enemy = game.getPlayerByName("" + panelGame.getComboBoxEnemySelection().getSelectedItem());
        try {
            if (enemy != null) {
                FieldState[][] fieldStates = enemy.getFieldStates(false);
                updateBoardColors(board, fieldStates);
            }
        } catch (FieldOutOfBoardException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setDoneButtonEnabled(boolean enabled) {
        gui.getPanelGame().getButtonDone().setEnabled(enabled);
    }

    private void setShipSelectionEnabled(boolean enabled) {
        GamePanel panelGame = gui.getPanelGame();
        panelGame.getRadioButtonDestroyer().setEnabled(enabled);
        panelGame.getRadioButtonFrigate().setEnabled(enabled);
        panelGame.getRadioButtonCorvette().setEnabled(enabled);
        panelGame.getRadioButtonSubmarine().setEnabled(enabled);
    }

    private void setSaveButtonEnabled(boolean enabled) {
        gui.getMenuItemSaveGame().setEnabled(enabled);
    }

    public void setEnemySelectionEnabled(boolean enabled) {
        gui.getPanelGame().getComboBoxEnemySelection().setEnabled(enabled);
        gui.getPanelGame().getButtonApplyEnemy().setEnabled(enabled);
    }

    private void updateBoardColors(JButton[][] board, FieldState[][] fieldStates) {
        int boardSize = fieldStates.length;
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                FieldState f = fieldStates[i][j];
                if (f != null) {
                    switch (fieldStates[i][j]) {
                        case DESTROYED:
                            board[i][j].setIcon(gui.getDestroyedIcon());
                            break;
                        case HIT:
                            board[i][j].setIcon(gui.getHitIcon());
                            break;
                        case MISSED:
                            board[i][j].setIcon(gui.getMissedIcon());
                            break;
                        case HAS_SHIP:
                            board[i][j].setIcon(gui.getShipIcon());
                            break;
                        case IS_EMPTY:
                            board[i][j].setIcon(null);
                        default:
                            break;
                    }
                } else {
                    // board[i][j].setBackground(GUI.UNKNOWN_COLOR);
                    board[i][j].setIcon(null);
                }
            }
        }
    }

    private void updatePreview(int startX, int startY, JButton[][] board) {
        int boardSize = board.length;
        boolean isHorizontal = gui.getPanelGame().getRadioButtonHorizontalOrientation().isSelected();
        Orientation orientation = isHorizontal ? Orientation.HORIZONTAL : Orientation.VERTICAL;
        int xDirection = isHorizontal ? 1 : 0;
        int yDirection = isHorizontal ? 0 : 1;
        int x;
        int y;
        int range;
        boolean possible = false;

        // Farben zur�cksetzen
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                board[i][j].setBackground(GUI.EMPTY_COLOR);
            }
        }

        if (!game.isReady()) {
            possible = isItPossibleToPlaceShip(startX, startY, orientation);
            range = game.getCurrentPlayer().getCurrentShip().getSize();
        } else {
            possible = isItPossibleToShoot(startX, startY);
            range = game.getCurrentPlayer().getCurrentShip().getShootingRange();
        }

        for (int i = 0; i < range; i++) {
            x = startX + i * xDirection;
            y = startY + i * yDirection;
            Color c = possible ? GUI.PREVIEW_COLOR : GUI.NOT_POSSIBLE_COLOR;
            if (x < boardSize && y < boardSize) {
                board[y][x].setBackground(c);
            }
        }
    }

    private boolean isItPossibleToPlaceShip(int startX, int startY, Orientation orientation) {
        try {
            if (game.getCurrentPlayer().isItPossibleToPlaceShip(startX, startY, orientation)) {
                return true;
            }
        } catch (ShipOutOfBoardException e) {
        } catch (ShipAlreadyPlacedException e) {
        } catch (FieldOutOfBoardException e) {
        }
        return false;
    }

    private boolean isItPossibleToShoot(int startX, int startY) {
        Player currentEnemy = game.getPlayerByName(gui.getPanelGame().getComboBoxEnemySelection().getSelectedItem() + "");
        FieldState fs = null;
        try {
            fs = currentEnemy.getFieldStates(false)[startY][startX];
        } catch (FieldOutOfBoardException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return fs == null;
    }

    private void setBoardEnabled(JButton[][] board, boolean enabled) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                board[i][j].setEnabled(enabled);
            }
        }
    }

    public void setPlayerBoardEnabled(boolean enabled) {
        setBoardEnabled(gui.getPanelGame().getPanelPlayerBoard().getButtonsField(), enabled);
    }

    public void setEnemyBoardEnabled(boolean enabled) {
        setBoardEnabled(gui.getPanelGame().getPanelEnemyBoard().getButtonsField(), enabled);
    }

    private void enableAvailableShips() {
        GamePanel panelGame = gui.getPanelGame();
        Player currentPlayer = game.getCurrentPlayer();
        panelGame.getRadioButtonDestroyer().setEnabled(currentPlayer.isShipOfTypeAvailable(ShipType.DESTROYER));
        panelGame.getRadioButtonFrigate().setEnabled(currentPlayer.isShipOfTypeAvailable(ShipType.FRIGATE));
        panelGame.getRadioButtonCorvette().setEnabled(currentPlayer.isShipOfTypeAvailable(ShipType.CORVETTE));
        panelGame.getRadioButtonSubmarine().setEnabled(currentPlayer.isShipOfTypeAvailable(ShipType.SUBMARINE));
    }

    private void addServerConnectionListener() {

        gui.getPanelServerConnection().getPnlServerConnectionBar().getBtnConnect().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!network.isConnected()) {
                    addServerObjectReceivedListeners();
                    // hole Verbindungsdaten
                    network.setIp(gui.getPanelServerConnection().getPnlServerConnectionBar().getTbxIp().getText());
                    network.setPort(Integer.parseInt(gui.getPanelServerConnection().getPnlServerConnectionBar().getTbxPort().getText()));
                    // Verbinde zum Server
                    network.connect();
                    // Sende login
                    network.getSender().sendLogin(gui.getPanelServerConnection().getPnlServerConnectionBar().getTbxUsername().getText());
                    game.setConnectedAsPlayer(gui.getPanelServerConnection().getPnlServerConnectionBar().getTbxUsername().getText());
                }
            }
        });

        gui.getPanelServerConnection().getPnlServerConnectionBar().getBtnDisconnect().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeServerObjectReceivedListeners();
                network.dispose();
                gui.getPanelServerConnection().getPnlServerConnectionBar().setEnabledAfterStartStop(true);
                gui.getPanelServerConnection().getPnlServerGameBrowser().getTblModel().removeAllGames();
                gui.getPanelServerConnection().getPnlServerGameBrowser().getBtnCreate().setEnabled(false);
                gui.getPanelServerConnection().getPnlServerGameBrowser().getBtnRefresh().setEnabled(false);
            }
        });

        gui.getPanelServerConnection().getPnlServerGameBrowser().getBtnRefresh().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                network.getSender().requestGameList();
            }
        });

        gui.getPanelServerConnection().getPnlServerGameBrowser().getBtnBack().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.showPanel(GUI.MAIN_MENU_PANEL);
            }
        });

        gui.getPanelServerConnection().getPnlServerGameBrowser().getBtnCreate().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setupSettingsPanelForMultiplayerGame();
                gui.showPanel(GUI.SETTINGS_PANEL);
            }
        });

        gui.getPanelGame().getButtonSendMessage().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        gui.getPanelServerConnection().getPnlServerGameBrowser().getBtnJoin().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int rowIndex = gui.getPanelServerConnection().getPnlServerGameBrowser().getTblGames().getSelectedRow();
                if (rowIndex > -1) {
                    GameListModel model = (GameListModel) gui.getPanelServerConnection().getPnlServerGameBrowser().getTblGames().getModel();
                    NetGame game = model.getGame(rowIndex);
                    createPasswordPrompt(game);
                }
            }
        });

        gui.getPanelGame().getTextFieldChatMessage().addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {

            }

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }

            public void keyReleased(KeyEvent e) {

            }
        });
    }

    private void setupSettingsPanelForMultiplayerGame() {
        // enable/disable controls for necessary game options
        SettingsPanel settings = gui.getPanelSettings();
        settings.getTextFieldAiPlayers().setEnabled(false);
        settings.getTextFieldAiPlayers().setVisible(false);
        settings.getLabelAiPlayers().setVisible(false);

        settings.getLabelGameName().setEnabled(true);
        settings.getLabelGameName().setVisible(true);

        settings.getTextFieldGameName().setEnabled(true);
        settings.getTextFieldGameName().setVisible(true);

        settings.getLabelGamePassword().setEnabled(true);
        settings.getLabelGamePassword().setVisible(true);

        settings.getTextFieldGamePassword().setVisible(true);
        settings.getTextFieldGamePassword().setEnabled(true);
    }

    private void setupSettingsPanelForLocalGame() {
        // enable/disable controls for necessary game options
        SettingsPanel settings = gui.getPanelSettings();
        settings.getTextFieldAiPlayers().setEnabled(true);
        settings.getTextFieldAiPlayers().setVisible(true);
        settings.getLabelAiPlayers().setVisible(true);

        settings.getLabelGameName().setEnabled(false);
        settings.getLabelGameName().setVisible(false);

        settings.getTextFieldGameName().setEnabled(false);
        settings.getTextFieldGameName().setVisible(false);

        settings.getLabelGamePassword().setEnabled(false);
        settings.getLabelGamePassword().setVisible(false);

        settings.getTextFieldGamePassword().setVisible(false);
        settings.getTextFieldGamePassword().setEnabled(false);
    }

    private void sendMessage() {
        String username = gui.getPanelServerConnection().getPnlServerConnectionBar().getTbxUsername().getText();
        String msg = gui.getPanelGame().getTextFieldChatMessage().getText();
        network.getSender().sendMessage(username, msg);
        gui.getPanelGame().getTextFieldChatMessage().setText("");
    }

    private void addServerGameBrowserListeners() {
        this.serverGameBrowserListener = new ServerGameBrowserListener(this);
        gui.getPanelServerConnection().getPnlServerGameBrowser().getTblGames().getColumnModel().addColumnModelListener(serverGameBrowserListener);
        gui.getPanelServerConnection().getPnlServerGameBrowser().getTblGames().addMouseListener(serverGameBrowserListener);
    }

    private void removeServerGameBrowserListeners() {
        gui.getPanelServerConnection().getPnlServerGameBrowser().getTblGames().getColumnModel().removeColumnModelListener(serverGameBrowserListener);
    }

    public void resizeServerGameListColumns() {
        JTable tbl = gui.getPanelServerConnection().getPnlServerGameBrowser().getTblGames();
        Dimension tableSize = tbl.getSize();
        tbl.getColumn("Name").setWidth(Math.round((tableSize.width - 202)));
        tbl.getColumn("Player").setWidth(45);
        tbl.getColumn("Created at").setWidth(127);
        tbl.getColumn("PW").setWidth(30);
    }

    public void join(String id) {
        network.join(id);
    }

    public void initializeClientAfterJoined(NetGame game) {
        try {
            initializeGame(game.getSettings());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setBoardsEnabled(boolean state) {
        setEnemyBoardEnabled(state);
        setPlayerBoardEnabled(state);
    }

    public void setPlayerNames(ArrayList<String> names) {
        for (int i = 0; i < game.getPlayers().length; i++) {
            game.getPlayers()[i].setName(names.get(i));
        }
        updateEnemyOnlineSelection(game.getConnectedAsPlayer());
    }

    public boolean handleAllShipsAreReloading() {
        if (game.getCurrentPlayer().areAllShipsReloading()) {
            setInfoLabelMessage("All ships of " + game.getCurrentPlayer() + " are reloading");
            setEnemyBoardEnabled(false);
            setShipSelectionEnabled(false);
            return true;
        } else {
            setInfoLabelMessage(game.getCurrentPlayer() + " is shooting");
            enableAvailableShips();
            selectFirstAvailableShipType();
            return false;
        }
    }

    public void setPlayerIsReloading(boolean playerIsReloading) {
        this.playerIsReloading = playerIsReloading;
    }

    public void createPasswordPrompt(NetGame game) {
        PasswordInputPanel panel = new PasswordInputPanel();
        String[] options = new String[]{"OK", "Cancel"};
        int option = JOptionPane.showOptionDialog(null, panel, "Password for " + game.getName(), JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        // pressing OK button
        if (option == JOptionPane.OK_OPTION) {
            char[] password = panel.getTbxPassword().getPassword();
            String strPassword = new String(password);

            if (strPassword.equals(game.getPassword())) {
                join(game.getId());
            } else {
                JOptionPane.showMessageDialog(null, "Wrong password!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
