package de.hsbremen.battleshipextreme;

import java.util.ArrayList;
import java.util.Scanner;

import de.hsbremen.battleshipextreme.model.Board;
import de.hsbremen.battleshipextreme.model.Field;
import de.hsbremen.battleshipextreme.model.FieldState;
import de.hsbremen.battleshipextreme.model.Game;
import de.hsbremen.battleshipextreme.model.Orientation;
import de.hsbremen.battleshipextreme.model.Settings;
import de.hsbremen.battleshipextreme.model.exception.BoardTooSmallException;
import de.hsbremen.battleshipextreme.model.exception.FieldOccupiedException;
import de.hsbremen.battleshipextreme.model.exception.FieldOutOfBoardException;
import de.hsbremen.battleshipextreme.model.exception.InvalidNumberOfShipsException;
import de.hsbremen.battleshipextreme.model.exception.InvalidPlayerNumberException;
import de.hsbremen.battleshipextreme.model.exception.ShipAlreadyPlacedException;
import de.hsbremen.battleshipextreme.model.exception.ShipOutOfBoardException;
import de.hsbremen.battleshipextreme.model.player.AIPlayer;
import de.hsbremen.battleshipextreme.model.player.Player;
import de.hsbremen.battleshipextreme.model.player.PlayerType;
import de.hsbremen.battleshipextreme.model.ship.Ship;

public class Main {
	public static final void main(String[] args) {
		new ConsoleGame();
	}
}

class ConsoleGame {
	private Scanner input;
	private static final String SAVEGAME_FILENAME = "savegame.sav";
	private Game game;

	public ConsoleGame() {
		input = new Scanner(System.in);
		createGame();
		gameLoop();
		System.out.println("Spiel zu Ende");
		System.out.println(game.getWinner() + " hat gewonnen!");
		input.close();
	}

	private void createGame() {
		// bietet mehrere Wege ein Spiel zu erstellen
		this.game = null;
		do {
			System.out.println("(1) Erzeuge Spiel manuell");
			System.out.println("(2) Erzeuge Spiel automatisch");
			System.out.println("(3) Zuletzt gespeichertes Spiel fortsetzen");
			int choice = readIntegerWithMinMax(1, 3);
			switch (choice) {
			case 1:
				createGameManually();
				break;
			case 2:
				createGameWithoutInput();
				break;
			case 3:
				tryToLoadGame();
			}
		} while (this.game == null);
	}

	private void createGameManually() {
		// Spiel mit manuellen Einstellungen erzeugen
		Settings settings = generateSettings();
		if (settings != null) {
			game = new Game(settings);
			game.setBeginningPlayer(0);
			placeShips();
		}
	}

	private void createGameWithoutInput() {
		// Spiel mit 3 KIs erzeugen
		Settings settings = null;

		try {
			settings = new Settings(0, 3, 10, 2, 1, 1, 1);
		} catch (BoardTooSmallException e) {
			e.printStackTrace();
		} catch (InvalidPlayerNumberException e) {
			e.printStackTrace();
		} catch (InvalidNumberOfShipsException e) {
			e.printStackTrace();
		}

		if (settings != null) {
			game = new Game(settings);
			game.setBeginningPlayer(0);
			placeShips();
		}
	}

	private Game tryToLoadGame() {
		// gespeichertes Spiel fortsetzen
		game = new Game();
		try {
			game.load(SAVEGAME_FILENAME);
		} catch (Exception e) {
			System.out.println("Spiel konnte nicht geladen werden");
			game = null;
		}
		return game;
	}

	private Settings generateSettings() {
		System.out.println("Einstellungen:");
		System.out.print("Anzahl der menschlichen Spieler (0-4): ");
		int players = readIntegerWithMinMax(0, 4);
		System.out.print("Anzahl der KI-Spieler (0-4): ");
		int aiPlayers = readIntegerWithMinMax(0, 4);
		System.out.print("Groesse des Spielfeldes (10-20): ");
		int boardSize = readIntegerWithMinMax(10, 20);
		System.out.print("Zerstoerer: ");
		int destroyers = readInteger();
		System.out.print("Fregatten: ");
		int frigates = readInteger();
		System.out.print("Korvetten: ");
		int corvettes = readInteger();
		System.out.print("U-Boote: ");
		int submarines = readInteger();

		try {
			return new Settings(players, aiPlayers, boardSize, destroyers, frigates, corvettes, submarines);
		} catch (BoardTooSmallException e1) {
			System.out.println("Das Board ist zu klein! Ben�tigte Prozentzahl freier Felder: " + e1.getMinPercentageOfFieldsThatShouldBeEmpty() + "%, dein Feld hat nur: "
					+ e1.getEmptyFieldPercentage() + "%");
		} catch (InvalidPlayerNumberException e) {
			System.out.println("Spieleranzahl muss zwischen " + e.getMinPlayers() + " und " + e.getMaxPlayers() + " liegen.");
		} catch (InvalidNumberOfShipsException e) {
			// TODO Auto-generated catch block
			System.out.println("Es muss mindestens ein Schiff existieren!");
		}
		return null;
	}

	private void setPlayerNames(Player[] players) {
		System.out.println("\nSpielernamen:");
		for (Player player : players) {
			System.out.print("Name f�r " + player + " : ");
			player.setName(input.nextLine());
		}
	}

	private void placeShips() {
		// Schiffe setzen
		do {
			Player currentPlayer = game.getCurrentPlayer();
			if (currentPlayer.getType() == PlayerType.AI) {
				game.placeShipsAutomatically();
			} else {
				placeShipsManually();
			}

			System.out.println();
			System.out.println("Board von " + currentPlayer);
			printBoard(currentPlayer.getBoard(), true);

		} while (!game.isReady());

	}

	private void placeShipsManually() {
		boolean isItPossibleToPlaceShip;
		do {
			// solange Schiffskoordinaten einlesen, bis keine Exception
			// auftritt
			int[] coordinates = readCoordinates(game.getCurrentPlayer().getBoard().getSize());
			Orientation orientation = readOrientation();
			isItPossibleToPlaceShip = false;
			try {
				game.placeShip(coordinates[1], coordinates[0], orientation);
				isItPossibleToPlaceShip = true;
			} catch (ShipAlreadyPlacedException e) {
				System.out.println("Schiff bereits gesetzt!");
			} catch (FieldOutOfBoardException e) {
				System.out.println("Feld nicht im Board!");
			} catch (ShipOutOfBoardException e) {
				System.out.println("Schiff (teilweise) nicht im Board!");
			} catch (FieldOccupiedException e) {
				System.out.println("Feld bereits belegt!");
			}
		} while (!isItPossibleToPlaceShip);
	}

	private void gameLoop() {
		Player currentPlayer;
		do {
			currentPlayer = game.getCurrentPlayer();
			if (currentPlayer.getType() == PlayerType.AI) {

				AIPlayer ai = (AIPlayer) currentPlayer;
				game.makeTurnAutomatically();

				// von AI beschossenes Board ausgeben
				System.out.println();
				printBoard(ai.getCurrentEnemy().getBoard(), false);
				System.out.println();
			} else {

				// m�gliche Spieleraktionen auflisten
				System.out.println();
				System.out.println(currentPlayer + " ist an der Reihe.");
				System.out.println();
				System.out.println("Was m�chtest du tun?");
				System.out.println("(1) Gegner angreifen");
				System.out.println("(2) Spiel speichern");
				System.out.println("(3) Spiel beenden");

				// Wahl einlesen
				int choice = readIntegerWithMinMax(1, 3);
				switch (choice) {
				case 1:
					makeTurnManually();
					break;
				case 2:
					saveGame();
					break;
				case 3:
					System.exit(0);
				}
			}

		} while (!game.isGameover());
	}

	private void makeTurnManually() {
		Player enemy;
		Player currentPlayer;
		currentPlayer = game.getCurrentPlayer();

		// Auswahl des zu schie�enden Schiffs
		System.out.println("Welches Schiff soll schie�en?");
		selectShip();

		// Auswahl des Gegners, auf den geschossen werden soll
		System.out.println("Auf welchen Spieler?");
		enemy = selectEnemy(game.getEnemiesOfCurrentPlayer());
		printBoard(enemy.getBoard(), false);

		boolean hasTurnBeenMade = false;
		do {
			// Koordinaten einlesen, bis Schuss erfolgreich ausgef�hrt werden
			// kann
			int[] coordinates = readCoordinates(currentPlayer.getBoard().getSize());

			try {
				hasTurnBeenMade = game.makeTurn(enemy, coordinates[1], coordinates[0], readOrientation());
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!hasTurnBeenMade) {
				System.out.println("Feld wurde bereits beschossen");
			}
		} while (!hasTurnBeenMade);

		printBoards(currentPlayer.getBoard(), enemy.getBoard());
	}

	private void saveGame() {
		try {
			game.save(SAVEGAME_FILENAME);
		} catch (Exception e) {
			System.err.print("Das Spiel konnte nicht gespeichert werden.");
			e.printStackTrace();
		}
		System.out.println();
		System.out.println("Spiel gespeichert.");
		System.out.println();
	}

	private Ship selectShip() {
		Player currentPlayer = game.getCurrentPlayer();
		ArrayList<Ship> availableShips = currentPlayer.getAvailableShips();
		Ship selectedShip;
		boolean isShipSelected = false;
		do {
			// Eingabe wiederholen bis Schiff gew�hlt wurde, das schie�en kann
			for (Ship s : availableShips) {
				System.out.println("(" + availableShips.indexOf(s) + ") " + s.getType() + "(reload:" + s.getCurrentReloadTime() + "," + " health:" + s.getSize() + ")");
			}
			selectedShip = availableShips.get(readIntegerWithMinMax(0, availableShips.size() - 1));
			isShipSelected = currentPlayer.selectShip(selectedShip);
			if (!isShipSelected)
				System.out.println("Schiff l�dt nach");
		} while (!isShipSelected);
		return selectedShip;
	}

	private Player selectEnemy(ArrayList<Player> enemies) {
		// angreifbare Gegner anzeigen
		for (int i = 0; i < enemies.size(); i++) {
			System.out.println("(" + i + ")" + enemies.get(i));
		}
		return enemies.get(readIntegerWithMinMax(0, enemies.size() - 1));
	}

	private void printBoards(Board ownBoard, Board enemyBoard) {
		System.out.println();
		System.out.println("Eigenes Board");
		System.out.println();
		printBoard(ownBoard, true);
		System.out.println();
		System.out.println("Board des Gegners");
		System.out.println();
		printBoard(enemyBoard, false);
		System.out.println("O = getroffenes Schiff\nX = daneben\n+ = eigenes Schiff\n- = leer \nU = unbekannt\n! = zerst�rtes Schiff\n");
	}

	private void printBoard(Board board, boolean isOwnBoard) {
		Field[][] fields = board.getFields();
		for (int row = 0; row < fields.length; row++) {
			for (int column = 0; column < fields[row].length; column++) {
				Field field = fields[row][column];
				printState(field.getState(), isOwnBoard);
			}
			System.out.println();
		}
	}

	private void printState(FieldState fieldState, boolean isOwnBoard) {
		String s = "";
		switch (fieldState) {
		case Destroyed:
			s = "!";
			break;
		case Hit:
			s = "O";
			break;
		case Missed:
			s = "X";
			break;
		case HasShip:
			s = isOwnBoard ? "+" : "?";
			break;
		case IsEmpty:
			s = isOwnBoard ? "-" : "?";
			break;
		default:
			break;
		}
		System.out.print(s);
	}

	private int readInteger() {
		while (!input.hasNextInt()) {
			System.out.println("Eine Zahl eingeben!");
			input.next();
		}
		return input.nextInt();
	}

	private int readIntegerWithMinMax(int min, int max) {
		int i;
		boolean isValid = false;
		do {
			i = readInteger();
			isValid = (i >= min) && (i <= max);
			if (!isValid)
				System.out.println("Zahl zwischen min " + min + " und " + max + " eingeben!");
		} while (!isValid);
		return i;
	}

	private int[] readCoordinates(int boardSize) {
		// Zeile einlesen
		System.out.print("Zeile (1-" + boardSize + "): ");
		int row = readIntegerWithMinMax(1, boardSize) - 1;

		// Spalte einlesen
		System.out.print("Spalte (1-" + boardSize + "): ");
		int column = readIntegerWithMinMax(1, boardSize) - 1;
		return new int[] { row, column };
	}

	private Orientation readOrientation() {
		// Ausrichtung einlesen
		System.out.print("Orientierung (H/V): ");
		Orientation orientation = input.next().toUpperCase().charAt(0) == 'V' ? Orientation.Vertical : Orientation.Horizontal;
		return orientation;
	}
}
