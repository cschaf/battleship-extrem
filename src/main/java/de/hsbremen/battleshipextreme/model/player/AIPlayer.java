package de.hsbremen.battleshipextreme.model.player;

import java.util.ArrayList;
import java.util.Random;

import de.hsbremen.battleshipextreme.model.Board;
import de.hsbremen.battleshipextreme.model.Field;
import de.hsbremen.battleshipextreme.model.FieldState;
import de.hsbremen.battleshipextreme.model.Orientation;
import de.hsbremen.battleshipextreme.model.exception.FieldOutOfBoardException;
import de.hsbremen.battleshipextreme.model.ship.Ship;

public class AIPlayer extends Player {
	private Player currentEnemy;
	private Player nextEnemy;

	private final int NORTH = 0;
	private final int EAST = 1;
	private final int SOUTH = 2;
	private final int WEST = 3;

	Field[] nextTargetsArray;

	public AIPlayer(int boardSize, int destroyers, int frigates, int corvettes, int submarines) {
		super(boardSize, destroyers, frigates, corvettes, submarines);
		this.name = "AI" + currentId;
		this.type = PlayerType.AI;
	}

	public void placeShipsAutomatically() {
		boolean isItPossibleToPlaceShip;
		for (int i = 0; i < super.getShips().length; i++) {
			do {
				isItPossibleToPlaceShip = false;
				// zuf�llige Position generieren
				Orientation orientation;
				orientation = (generateRandomNumber(0, 1) == 0) ? Orientation.Horizontal : Orientation.Vertical;
				int[] coordinate = generateCoordinate(orientation, super.getCurrentShip().getSize());
				try {
					super.placeShip(coordinate[0], coordinate[1], orientation);
					isItPossibleToPlaceShip = true;
				} catch (Exception e) {
				}
			} while (!isItPossibleToPlaceShip);
			super.nextShip();
		}
	}

	public void makeTurnAutomatically(ArrayList<Player> availablePlayers) throws Exception {
		int[] coordinate;
		Orientation orientation;
		int currentDirection;
		boolean hasTurnBeenMade = false;

		chooseShipToShootWithRandomly();

		if (!this.hasTargets()) {
			// wenn keine Ziele vorhanden sind
			// zuf�lligen Player zum angreifen ausw�hlen
			// aktuellen Gegner nicht mehr merken
			this.nextEnemy = null;
			int randomEnemyIndex = generateRandomNumber(0, availablePlayers.size() - 1);
			this.currentEnemy = availablePlayers.get(randomEnemyIndex);

			// zuf�llig schie�en
			do {
				orientation = (generateRandomNumber(0, 1) == 0) ? Orientation.Horizontal : Orientation.Vertical;
				coordinate = generateCoordinate(orientation, this.currentShip.getSize());
				try {
					hasTurnBeenMade = super.makeTurn(this.currentEnemy, coordinate[0], coordinate[1], orientation);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while (!hasTurnBeenMade);

			// wenn Treffer, dann Gegner merken und n�chste Sch�sse planen
			if (didShotHitShip(coordinate[0], coordinate[1], orientation)) {
				this.nextEnemy = this.currentEnemy;
				planNextShots(coordinate[0], coordinate[1]);
			} else {
				// currentEnemy vergessen, wenn kein Feld getroffen
				// wurde
				this.nextEnemy = null;
			}
		} else {
			// wenn KI Spur verfolgt
			// den letzten Spieler ermitteln
			this.currentEnemy = this.nextEnemy;

			// Richtung ermitteln
			currentDirection = getCurrentDirection();

			// wenn Ziel vorhanden ist, dann auf Ziel schie�en
			int x = this.nextTargetsArray[currentDirection].getXPos();
			int y = this.nextTargetsArray[currentDirection].getYPos();

			// Ausrichtung beibehalten
			orientation = (currentDirection == 1 || currentDirection == 3) ? Orientation.Horizontal : Orientation.Vertical;
			hasTurnBeenMade = super.makeTurn(this.currentEnemy, x, y, orientation);
			// wenn Treffer, dann nach n�chstem unbeschossenem Feld in
			// selbe Richtung suchen und als n�chstes Ziel speichern
			if (didShotHitShip(x, y, orientation)) {
				int[] directionArray = determineDirection(currentDirection);
				int xDirection = directionArray[0];
				int yDirection = directionArray[1];
				Field newTarget = findNextFieldNotHit(x, y, xDirection, yDirection);
				this.nextTargetsArray[currentDirection] = newTarget;
			} else {
				// wenn kein Treffer, dann Target l�schen
				this.nextTargetsArray[currentDirection] = null;
			}
		}

	}

	private void chooseShipToShootWithRandomly() {
		// zuf�lliges freies Schiff zum Schie�en w�hlen
		ArrayList<Ship> availableShips = this.getAvailableShips();
		int randomShipIndex = generateRandomNumber(0, availableShips.size() - 1);
		this.currentShip = this.getAvailableShips().get(randomShipIndex);
	}

	private boolean hasTargets() {
		// Ziele zum anvisieren �brig
		// lebt der anvisierte Gegner noch?
		boolean hasTargets;

		int i = 0;
		if (this.nextTargetsArray != null) {
			while ((i < 4) && (this.nextTargetsArray[i] == null)) {
				i++;
			}
		}
		hasTargets = (i < 4) && this.nextTargetsArray != null && this.nextEnemy != null && !this.nextEnemy.hasLost();
		return hasTargets;
	}

	private int getCurrentDirection() {
		int currentDirection = 0;
		while ((currentDirection < 4) && (this.nextTargetsArray[currentDirection] == null)) {
			// Finde n�chstes Ziel
			currentDirection++;
		}
		return currentDirection;
	}

	private void planNextShots(int hitX, int hitY) throws FieldOutOfBoardException {

		this.nextTargetsArray = new Field[4];

		Field f;
		// finde Ziel n�rdlich vom Treffer
		f = findNextFieldNotHit(hitX, hitY, 0, 1);
		if (f != null)
			this.nextTargetsArray[0] = f;

		// finde Ziel �stlich vom Treffer
		f = findNextFieldNotHit(hitX, hitY, 1, 0);
		if (f != null)
			this.nextTargetsArray[1] = f;

		// finde Ziel s�dlich vom Treffer
		f = findNextFieldNotHit(hitX, hitY, 0, -1);
		if (f != null)
			this.nextTargetsArray[2] = f;

		// finde Ziel westlich vom Treffer
		f = findNextFieldNotHit(hitX, hitY, -1, 0);
		if (f != null)
			this.nextTargetsArray[3] = f;
	}

	private int[] determineDirection(int direction) {
		// wenn Treffer, dann in die selbe Richtung
		// weitergehen
		switch (direction) {
		case NORTH:
			return new int[] { 0, 1 };
		case EAST:
			return new int[] { -1, 0 };
		case SOUTH:
			return new int[] { 0, -1 };
		case WEST:
			return new int[] { 1, 0 };
		default:
			break;
		}
		return null;
	}

	private Field findNextFieldNotHit(int startX, int startY, int xDirection, int yDirection) throws FieldOutOfBoardException {
		Board enemyBoard = this.currentEnemy.getBoard();
		int step = 0;
		int x = startX + step * xDirection;
		int y = startY + step * yDirection;
		boolean endLoop = false;
		Field f = null;
		do {
			x = startX + step * xDirection;
			y = startY + step * yDirection;
			if (enemyBoard.containsFieldAtPosition(x, y)) {
				f = enemyBoard.getField(x, y);
				if ((f.getState() == FieldState.Missed) || (f.getState() == FieldState.Destroyed)) {
					// wenn Schiff verfehlt oder zerst�rt wurde, Ziel nicht
					// merken
					f = null;
					endLoop = true;
				} else if (!f.isHit()) {
					// wenn Feld noch nicht beschossen wurde, Feld merken
					endLoop = true;
				}
				step++;
			} else {
				f = null;
				endLoop = true;
			}
		} while (!endLoop);
		return f;
	}

	private boolean didShotHitShip(int x, int y, Orientation orientation) throws FieldOutOfBoardException {
		return this.currentEnemy.getBoard().getField(x, y).hasShip();
	}

	private int[] generateCoordinate(Orientation orientation, int shipSize) {
		int xPos;
		int yPos;
		int xMax;
		int yMax;

		if (orientation == Orientation.Horizontal) {
			xMax = this.board.getSize() - shipSize;
			yMax = this.board.getSize() - 1;
		} else {
			xMax = this.board.getSize() - 1;
			yMax = this.board.getSize() - shipSize;
		}

		xPos = generateRandomNumber(0, xMax);
		yPos = generateRandomNumber(0, yMax);

		int[] coordinate = { xPos, yPos };

		return coordinate;

	}

	private int generateRandomNumber(int min, int max) {
		Random random = new Random();
		return random.nextInt(max - min + 1) + min;
	}

	public Player getCurrentEnemy() {
		return currentEnemy;
	}
}
