package de.hsbremen.battleshipextreme;

import java.util.ArrayList;
import java.util.Scanner;

import de.hsbremen.battleshipextreme.model.Board;
import de.hsbremen.battleshipextreme.model.Field;
import de.hsbremen.battleshipextreme.model.FieldState;
import de.hsbremen.battleshipextreme.model.Game;
import de.hsbremen.battleshipextreme.model.Orientation;
import de.hsbremen.battleshipextreme.model.Settings;
import de.hsbremen.battleshipextreme.model.player.Player;
import de.hsbremen.battleshipextreme.model.ship.Ship;

public class Main {
	static Scanner input = new Scanner(System.in);

	public static void main(String[] args) throws Exception {
		Game game = createGame();
		gameLoop(game);
		System.out.println("Spiel zu Ende");
		System.out.println(game.getWinner() + " hat gewonnen!");
		input.close();
	}

	private static Game createGame() throws Exception {
		// bietet mehrere Wege ein Spiel zu erstellen
		Game game = null;
		System.out.println("(1) Erzeuge Spiel manuell");
		System.out.println("(2) Erzeuge Spiel automatisch");
		System.out.println("(3) Letztes Spiel fortsetzen");
		int choice = readIntegerWithMinMax(1, 3);
		switch (choice) {
		case 1:
			game = new Game(generateSettings());
			game.setBeginningPlayer(1);
			placeShips(game);
			break;
		case 2:
			game = new Game(new Settings(3, 10, 2, 1, 1, 1));
			game.setBeginningPlayer(1);
			placeShipsWithoutInput(game);
			break;
		case 3:
			game = new Game();
			game.load("saveGame.sav");
			break;
		}

		return game;
	}

	private static Settings generateSettings() {
		System.out.println("Einstellungen:");
		System.out.print("Anzahl der Spieler (2-6): ");
		int players = readIntegerWithMinMax(2, 6);
		System.out.print("Groesse des Spielfeldes (10-1000): ");
		int boardSize = readIntegerWithMinMax(10, 10000);
		System.out.print("Zerstoerer: ");
		int destroyers = readInteger();
		System.out.print("Fregatten: ");
		int frigates = readInteger();
		System.out.print("Korvetten: ");
		int corvettes = readInteger();
		System.out.print("U-Boote: ");
		int submarines = readInteger();

		return new Settings(players, boardSize, destroyers, frigates, corvettes, submarines);
	}

	private static void setPlayerNames(Player[] players) {
		System.out.println("\nSpielernamen:");

		for (Player player : players) {
			System.out.print("Name f�r " + player + " : ");
			player.setName(input.nextLine());
		}
	}

	private static void placeShips(Game game) throws Exception {
		// Schiffe manuell setzen
		Player player;
		do {
			player = game.getCurrentPlayer();
			System.out.println("\nPlatziere Schiffe fuer " + player + ":");

			for (Ship ship : player.getShips()) {
				System.out.println("\nPlatziere " + ship + ":");

				System.out.print("Zeile (1-" + player.getBoard().getSize() + "): ");
				int row = readIntegerWithMinMax(1, player.getBoard().getSize()) - 1;
				System.out.print("Spalte (1-" + player.getBoard().getSize() + "): ");
				int column = readIntegerWithMinMax(1, player.getBoard().getSize()) - 1;

				System.out.print("Orientierung (H/V): ");
				Orientation orientation = input.next().toUpperCase().charAt(0) == 'V' ? Orientation.Vertical : Orientation.Horizontal;

				player.placeShip(ship, column, row, orientation);

				System.out.println();
				System.out.println("Board von " + player);
				printBoard(player.getBoard(), true);
			}
			game.nextPlayer();
		} while (!game.isReady());
	}

	private static void placeShipsWithoutInput(Game game) throws Exception {
		// schnell Schiffe ohne Eingabe setzen
		Player player;
		do {
			player = game.getCurrentPlayer();
			for (int i = 0; i < player.getShips().length; i++) {
				Ship ship = player.getShips()[i];
				int row = 2 * i;
				int column = 0;
				Orientation orientation = Orientation.Horizontal;
				player.placeShip(ship, column, row, orientation);
			}
			System.out.println();
			System.out.println("Board von " + player);
			printBoard(player.getBoard(), true);
			game.nextPlayer();
		} while (!game.isReady());
	}

	private static void gameLoop(Game game) throws Exception {
		Ship ship;
		Player enemy;
		Player player;
		do {
			player = game.getCurrentPlayer();
			System.out.println(player + " ist an der Reihe.");

			// Auswahl des zu schie�enden Schiffs
			System.out.println("Welches Schiff soll schie�en?");
			ship = selectShip(player);

			// Auswahl des Gegners, auf den geschossen werden soll
			System.out.println("Auf welchen Spieler?");
			enemy = selectEnemy(game.getEnemiesOfCurrentPlayer());

			// Zug mit ausgew�hltem Schiff und Gegner ausf�hren
			makeTurn(player, ship, enemy);

			printBoards(player.getBoard(), enemy.getBoard());
			game.nextPlayer();
			game.save("saveGame.sav");
		} while (!game.isGameover());
	}

	private static Ship selectShip(Player player) {
		Ship ship;
		Ship[] ships = player.getShips();
		boolean isShipSelected = false;
		do {
			// Eingabe wiederholen bis Schiff gew�hlt wurde, das schie�en kann
			printShips(ships);
			ship = ships[readIntegerWithMinMax(0, ships.length - 1)];
			isShipSelected = player.selectShip(ship);
			if (!isShipSelected)
				System.out.println("Schiff l�dt nach");

		} while (!isShipSelected);
		return ship;
	}

	private static void printShips(Ship[] ships) {
		// Schiffe des Spielers und deren Munition/Leben anzeigen
		for (int i = 0; i < ships.length; i++) {
			System.out.println("(" + i + ") " + ships[i].getType() + "(reload:" + ships[i].getCurrentReloadTime() + "," + " health:" + ships[i].getSize() + ")");
		}
	}

	private static Player selectEnemy(ArrayList<Player> enemies) {
		// angreifbare Gegner anzeigen
		for (int i = 0; i < enemies.size(); i++) {
			System.out.println("(" + i + ")" + enemies.get(i));
		}
		return enemies.get(readIntegerWithMinMax(0, enemies.size() - 1));
	}

	private static void makeTurn(Player player, Ship ship, Player enemy) throws Exception {
		boolean hasTurnBeenMade;
		do {
			// Koordinaten einlesen, bis Schuss erfolgreich ausgef�hrt werden
			// kann
			System.out.print("Zeile (1-" + player.getBoard().getSize() + "): ");
			int row = readIntegerWithMinMax(1, player.getBoard().getSize()) - 1;
			System.out.print("Spalte (1-" + player.getBoard().getSize() + "): ");
			int column = readIntegerWithMinMax(1, player.getBoard().getSize()) - 1;
			System.out.print("Orientierung (H/V): ");
			Orientation orientation = input.next().toUpperCase().charAt(0) == 'V' ? Orientation.Vertical : Orientation.Horizontal;
			hasTurnBeenMade = player.makeTurn(enemy, column, row, orientation);
			if (!hasTurnBeenMade) {
				System.out.println("Zug nicht m�glich");
			}
		} while (!hasTurnBeenMade);
	}

	private static void printBoards(Board ownBoard, Board enemyBoard) {
		printBoard(ownBoard, true);
		printBoard(enemyBoard, false);
		System.out.println("O = getroffenes Schiff\nX = daneben\n+ = eigenes Schiff\n- = leer \nU = unbekannt\n! = zerst�rtes Schiff\n");
	}

	private static void printBoard(Board board, boolean isOwnBoard) {
		String s = isOwnBoard ? "\nEigenes Board" : "\nGegnerisches Board";
		System.out.println(s);
		Field[][] fields = board.getFields();
		for (int row = 0; row < fields.length; row++) {
			for (int column = 0; column < fields[row].length; column++) {
				Field field = fields[row][column];
				printState(field.getState(), isOwnBoard);
			}
			System.out.println();
		}
	}

	private static void printState(FieldState fieldState, boolean isOwnBoard) {
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

	private static int readInteger() {
		while (!input.hasNextInt()) {
			System.out.println("Eine Zahl eingeben!");
			input.next();
		}
		return input.nextInt();
	}

	private static int readIntegerWithMinMax(int min, int max) {
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
}
