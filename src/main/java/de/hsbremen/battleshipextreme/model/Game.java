package de.hsbremen.battleshipextreme.model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import de.hsbremen.battleshipextreme.model.exception.FieldOutOfBoardException;
import de.hsbremen.battleshipextreme.model.player.AIPlayer;
import de.hsbremen.battleshipextreme.model.player.HumanPlayer;
import de.hsbremen.battleshipextreme.model.player.Player;
import de.hsbremen.battleshipextreme.model.player.PlayerType;
import de.hsbremen.battleshipextreme.model.player.Target;
import de.hsbremen.battleshipextreme.model.ship.Ship;
import de.hsbremen.battleshipextreme.network.TransferableType;
import de.hsbremen.battleshipextreme.network.transfarableObject.TransferableObject;

public class Game extends TransferableObject {
	private static final long serialVersionUID = -8672232283887859447L;
	private Player[] players;
	private Player currentPlayer;
	private Player winner;
	private int turnNumber;
	private int roundNumber;
	private int boardSize;
	private boolean hasCurrentPlayerMadeTurn;
	private Settings settings;
	private Field[] markedFieldOfLastTurn;

	/**
	 * Reads the settings and initializes the necessary game objects.
	 * 
	 * @param settings
	 *            the game settings.
	 */
	public void initialize(Settings settings) {
		this.settings = settings;
		createPlayers(settings);

		// Spielernummern setzen
		for (int i = 0; i < players.length; i++) {
			players[i].setName(players[i].getName() + (i + 1));
		}

		boardSize = settings.getBoardSize();
		turnNumber = 0;
		roundNumber = 0;
		currentPlayer = players[0];
	}

	private void createPlayers(Settings settings) {
		// Spieler erzeugen
		int numberOfHumanPlayers = settings.getPlayers();
		int numberOfAIPlayers = settings.getSmartAiPlayers();
		int numberOfDumbAiPlayers = settings.getDumbAiPlayers();
		int numberOfPlayers = numberOfAIPlayers + numberOfHumanPlayers
				+ numberOfDumbAiPlayers;
		players = new Player[numberOfPlayers];
		for (int i = 0; i < numberOfPlayers; i++) {
			if (i < numberOfHumanPlayers) {
				players[i] = new HumanPlayer(settings.getBoardSize(),
						settings.getDestroyers(), settings.getFrigates(),
						settings.getCorvettes(), settings.getSubmarines());
			} else {
				if (i < numberOfAIPlayers + numberOfHumanPlayers) {
					players[i] = new AIPlayer(settings.getBoardSize(),
							settings.getDestroyers(), settings.getFrigates(),
							settings.getCorvettes(), settings.getSubmarines(),
							PlayerType.SMART_AI);
				} else {
					players[i] = new AIPlayer(settings.getBoardSize(),
							settings.getDestroyers(), settings.getFrigates(),
							settings.getCorvettes(), settings.getSubmarines(),
							PlayerType.DUMB_AI);
				}
			}
		}
	}

	public Target makeAiTurn() throws Exception {
		boolean wasShotPossible = false;
		// AI soll Zug automatisch machen
		AIPlayer ai = (AIPlayer) currentPlayer;
		Target shot = null;

		do {
			Player currentEnemy = selectAiEnemy(ai);
			ai.selectShip(ai.getAvailableShips(true).get(0));
			shot = ai.getTarget(currentEnemy.getFieldStates(false));
			wasShotPossible = makeTurn(currentEnemy, shot.getX(), shot.getY(),
					shot.getOrientation());
		} while (!wasShotPossible);

		return shot;
	}

	private Player selectAiEnemy(AIPlayer ai) {
		Player currentEnemy;
		do {
			currentEnemy = players[ai.getCurrentEnemyIndex()];
			// zuf�lligen Gegner ausw�hlen, wenn die KI keine Spur verfolgt,
			// ansonsten gemerkten Gegner beibehalten
			if (!ai.hasTargets() || currentEnemy.hasLost()
					|| ai.getType() == PlayerType.DUMB_AI
					|| ai.equals(currentEnemy)) {
				ai.setRandomEnemyIndex(players.length - 1);
				currentEnemy = players[ai.getCurrentEnemyIndex()];
			}
		} while (currentEnemy.hasLost() || ai.equals(currentEnemy));
		return currentEnemy;
	}

	public boolean makeTurn(Player enemy, int xPos, int yPos,
			Orientation orientation) throws FieldOutOfBoardException {
		Field[] markedFields = new Field[currentPlayer.getCurrentShip()
				.getShootingRange()];
		int xDirection = orientation == Orientation.HORIZONTAL ? 1 : 0;
		int yDirection = orientation == Orientation.VERTICAL ? 1 : 0;
		int x;
		int y;
		for (int i = 0; i < currentPlayer.getCurrentShip().getShootingRange(); i++) {
			x = xPos + i * xDirection;
			y = yPos + i * yDirection;
			boolean isShotPossible = enemy.markBoard(x, y);
			if (i == 0) {
				if (!isShotPossible) {
					// erstes Feld belegt, Schuss nicht m�glich
					return false;
				}
			}
			markedFields[i] = enemy.getBoard().getField(x, y);
		}
		boolean isDestroyed = false;
		ArrayList<Field> missedFields = new ArrayList<Field>();
		Field source = null;
		for (int i = 0; i < markedFields.length; i++) {
			if (markedFields[i].hasShip()) {
				if (markedFields[i].getShip().isDestroyed()) {
					source = markedFields[i];
					isDestroyed = true;
				}
			} else {
				missedFields.add(markedFields[i]);
			}
		}
		if (isDestroyed) {
			ArrayList<Field> shipFields = enemy.getBoard().getFieldsOfShip(
					source);
			shipFields.addAll(missedFields);
			markedFields = shipFields.toArray(markedFields);
		}
		currentPlayer.getCurrentShip().shoot();
		hasCurrentPlayerMadeTurn = true;
		this.markedFieldOfLastTurn = markedFields;
		return true;
	}

	/**
	 * Saves the this Game to a File
	 * 
	 * @throws Exception
	 *             if the file could not be saved
	 */
	public void save(String destinationPath) throws Exception {
		FileOutputStream saveFile = null;
		ObjectOutputStream save = null;
		try {
			saveFile = new FileOutputStream(destinationPath);
			save = new ObjectOutputStream(saveFile);
			save.writeObject(this);
			save.close();
		} catch (Exception ex) {
			throw ex;
		} finally {
			closeQuietly(saveFile);
			closeQuietly(save);
		}
	}

	/**
	 * Load a saved Game object
	 * 
	 * @throws Exception
	 *             if the game could not be loaded
	 */
	public void load(String destinationPath) throws Exception {
		FileInputStream saveFile = null;
		ObjectInputStream save = null;
		try {
			saveFile = new FileInputStream(destinationPath);
			save = new ObjectInputStream(saveFile);
			Game game = (Game) save.readObject();
			players = game.players;
			currentPlayer = game.currentPlayer;
			winner = game.winner;
			turnNumber = game.turnNumber;
			boardSize = game.boardSize;
			hasCurrentPlayerMadeTurn = game.hasCurrentPlayerMadeTurn;
			save.close();
		} catch (Exception ex1) {
			throw ex1;
		} finally {
			closeQuietly(saveFile);
			closeQuietly(save);
		}
	}

	/**
	 * Close a Stream quietly
	 */
	private void closeQuietly(InputStream stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Close a Stream quietly
	 */
	private void closeQuietly(OutputStream stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Increase turnNumber. Decrease the reload time of the current players'
	 * ships. Set the currentPlayer to the next player.
	 */
	public void nextPlayer() {
		turnNumber++;
		decreaseCurrentReloadTimeOfShips(currentPlayer);
		int currentPlayerIndex = Arrays.asList(players).indexOf(currentPlayer);
		// wenn letzter Spieler im Array, dann Index wieder auf 0 setzen,
		// ansonsten hochz�hlen
		currentPlayerIndex = (currentPlayerIndex >= players.length - 1) ? currentPlayerIndex = 0
				: currentPlayerIndex + 1;
		if (currentPlayerIndex == 0) {
			roundNumber++;
		}
		currentPlayer = players[currentPlayerIndex];
		hasCurrentPlayerMadeTurn = false;
	}

	/**
	 * Decreases the reload time of the ships, except for the ship that just
	 * shot.
	 */
	private void decreaseCurrentReloadTimeOfShips(Player player) {
		Ship[] ships = player.getShips();
		for (Ship ship : ships) {
			ship.decreaseCurrentReloadTime();
		}
	}

	/**
	 * Provides a list of enemies the current player may attack. Players that
	 * are lost or equal to the current player are filtered.
	 * 
	 * @return an ArrayList of Players
	 */
	public ArrayList<Player> getEnemiesOfCurrentPlayer() {
		// angreifbare Gegner des currentPlayers zur�ckgeben
		ArrayList<Player> enemies = new ArrayList<Player>();
		for (int i = 0; i < players.length; i++) {
			if (!players[i].hasLost()) {
				if (!currentPlayer.equals(players[i])) {
					enemies.add(players[i]);
				}
			}
		}
		return enemies;
	}

	/**
	 * Provides a list of enemies the current player may attack. Players that
	 * are lost or equal to the current player are filtered.
	 * 
	 * @return an ArrayList of Players
	 */
	public ArrayList<Player> getEnemiesOfPlayer(String name) {
		// angreifbare Gegner des currentPlayers zur�ckgeben
		ArrayList<Player> enemies = new ArrayList<Player>();
		for (int i = 0; i < players.length; i++) {
			if (!players[i].hasLost()) {
				if (!players[i].getName().equals(name)) {
					enemies.add(players[i]);
				}
			}
		}
		return enemies;
	}

	public Player getPlayerByName(String name) {
		for (Player player : players) {
			if (player.getName().equals(name)) {
				return player;
			}
		}
		return null;
	}

	public Settings getSettings() {
		return settings;
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	/**
	 * Returns true if the ships of all players have been placed. The method is
	 * used to determine if a game is ready to start.
	 * 
	 * @return true if all ships by all players are placed, else false
	 */
	public boolean isReady() {
		// pr�ft ob alle Schiffe gesetzt sind
		for (Player player : players)
			if (!player.hasPlacedAllShips()) {
				return false;
			}
		return true;
	}

	/**
	 * Check if the game is over. Set the game winner if the game is over.
	 * 
	 * @return true if the game is over, false if not
	 */
	public boolean isGameover() {
		int numberOfPlayersLeft = 0;
		Player potentialWinner = null;
		for (Player player : players) {
			if (!player.hasLost()) {
				numberOfPlayersLeft++;
				potentialWinner = player;
			}
		}
		if (numberOfPlayersLeft <= 1) {
			winner = potentialWinner;
		}
		return numberOfPlayersLeft <= 1;
	}

	public Player[] getPlayers() {
		return players;
	}

	public Player getCurrentPlayer() {
		return currentPlayer;
	}

	public void setCurrentPlayer(Player currentPlayer) {
		this.currentPlayer = currentPlayer;
	}

	public Player getWinner() {
		return winner;
	}

	public int getTurnNumber() {
		return turnNumber;
	}

	public int getRoundNumber() {
		return roundNumber;
	}

	public int getBoardSize() {
		return boardSize;
	}

	public boolean hasCurrentPlayerMadeTurn() {
		return hasCurrentPlayerMadeTurn;
	}

	public void setPlayerBoards(ArrayList<Board> playerBoards) {

		for (int i = 0; i < players.length; i++) {
			HashMap<Ship, ArrayList<Field>> shipMap = getShipMap(playerBoards
					.get(i));

			Player player = new HumanPlayer(boardSize, shipMap);
			player.setName(players[i].getName());
			for (Ship ship : player.getShips()) {
				ship.setPlaced(true);
			}
			players[i] = player;
		}
		currentPlayer = players[0];
	}

	private HashMap<Ship, ArrayList<Field>> getShipMap(Board board) {
		HashMap<Ship, ArrayList<Field>> shipMap = new HashMap<Ship, ArrayList<Field>>();
		Field[][] fields = board.getFields();
		for (int row = 0; row < board.getSize(); row++) {
			for (int column = 0; column < board.getSize(); column++) {
				if (fields[row][column].getShip() != null) {
					if (!shipMap.containsKey(fields[row][column].getShip())) {
						ArrayList<Field> shipFields = new ArrayList<Field>();
						shipFields.add(fields[row][column]);
						shipMap.put(fields[row][column].getShip(), shipFields);
					} else {
						ArrayList<Field> shipFields = shipMap
								.get(fields[row][column].getShip());
						shipFields.add(fields[row][column]);
						shipMap.put(fields[row][column].getShip(), shipFields);
					}
				}
			}
		}

		return shipMap;
	}

	public TransferableType getType() {
		return TransferableType.Game;
	}

	public Field[] getMarkedFieldOfLastTurn() {
		return markedFieldOfLastTurn;
	}
}
