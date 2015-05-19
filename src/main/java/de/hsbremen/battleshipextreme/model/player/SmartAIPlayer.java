package de.hsbremen.battleshipextreme.model.player;

import java.util.ArrayList;

import de.hsbremen.battleshipextreme.model.Board;
import de.hsbremen.battleshipextreme.model.Field;
import de.hsbremen.battleshipextreme.model.FieldState;
import de.hsbremen.battleshipextreme.model.Orientation;
import de.hsbremen.battleshipextreme.model.exception.FieldOutOfBoardException;

/**
 * Smart AI - uses basic strategy
 * 
 * AI-Benchmark: ~ 40 rounds (77 rounds = random)
 *
 */
public class SmartAIPlayer extends AIPlayer {
	private Player nextEnemy;
	private Field[] nextTargetsArray;
	private Field currentFieldShotAt;
	private Orientation currentShotOrientation;

	private final int NORTH = 0;
	private final int SOUTH = 1;
	private final int EAST = 2;
	private final int WEST = 3;
	private final int NORTH_EAST = 4;
	private final int SOUTH_EAST = 5;
	private final int NORTH_WEST = 6;
	private final int SOUTH_WEST = 7;

	public SmartAIPlayer(int boardSize, int destroyers, int frigates, int corvettes, int submarines) {
		super(boardSize, destroyers, frigates, corvettes, submarines);
		this.name = "Schlaue KI";
	}

	@Override
	public void makeAiTurn(ArrayList<Player> availablePlayers) throws Exception {
		int currentDirection = 0;

		chooseShipToShootWithRandomly();

		if (!this.hasTargets()) {
			// wenn keine Ziele vorhanden sind
			// zuf�lligen Player zum angreifen ausw�hlen
			// aktuellen Gegner nicht mehr merken
			this.nextEnemy = null;
			int randomEnemyIndex = createRandomNumber(0, availablePlayers.size() - 1);
			this.currentEnemy = availablePlayers.get(randomEnemyIndex);

			// zuf�llig schie�en, Schuss merken
			// setzt currentFieldShotAt und currentShotOrientation
			shootRandomly();

			// pr�fen ob ein Feld getroffen wurde, wenn ja dann Feld merken
			ArrayList<Field> hitFields = getHitFields();

			// wenn Treffer, dann Gegner merken und n�chsten Sch�sse planen
			if (hitFields.size() > 0) {
				this.nextEnemy = this.currentEnemy;
				planNextShots(hitFields);
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
			Field target = this.nextTargetsArray[currentDirection];

			// wenn Richtung Osten oder Westen, dann Ausrichtung horizontal,
			// ansonsten vertikal
			Orientation orientation = (currentDirection == EAST || currentDirection == WEST) ? Orientation.Horizontal : Orientation.Vertical;

			// x und y abh�ngig von der Schussweite korrigieren, so dass
			// m�glichst
			// viele Felder getroffen werden
			int range = this.currentShip.getShootingRange() - 1;
			int adjustedX = adjustX(target, currentDirection, range);
			int adjustedY = adjustY(target, currentDirection, range);

			// schie�en
			makeTurn(this.currentEnemy, adjustedX, adjustedY, orientation);

			// wenn Treffer, dann nach n�chstem unbeschossenen Feld in
			// selbe Richtung suchen und als n�chstes Ziel speichern
			if (target.hasShip()) {
				// wenn Treffer
				if (target.getState() != FieldState.Destroyed) {
					int[] directionArray = getDirectionArray(currentDirection);
					Field newTarget = findNextTarget(target, directionArray[0], directionArray[1]);
					// neues Ziel in gleiche Richtung setzen
					this.nextTargetsArray[currentDirection] = newTarget;
				} else {
					// Schiff zerst�rt, komplettes targetArray l�schen,
					// da die Spur nicht mehr verfolgt werden muss
					this.nextTargetsArray = null;
					this.nextEnemy = null;
				}
			} else {
				// wenn kein Treffer, dann Target l�schen
				this.nextTargetsArray[currentDirection] = null;
			}
		}

	}

	private void shootRandomly() throws Exception {
		Orientation orientation;
		Field fieldShotAt;
		boolean hasTurnBeenMade = false;
		int boardSize = this.currentEnemy.getBoard().getSize();
		// zuf�llig schie�en
		do {

			// wiederhole die Erzeugung von zuf�lligen Koordinaten
			// bis ein Feld gefunden wird, an welches kein zerst�rtes
			// Schiff angrenzt,
			// (zwischen den Schiffen muss immer ein Feld frei sein)
			do {
				orientation = (createRandomNumber(0, 1) == 0) ? Orientation.Horizontal : Orientation.Vertical;
				fieldShotAt = createRandomField(0, board.getSize() - 1, 0, board.getSize() - 1);
			} while (surroundingFieldContainsShip(fieldShotAt));

			// wenn m�glich, den Schuss so ausrichten, dass alle
			// Schussfelder im Board sind
			int adjustedX = fieldShotAt.getXPos();
			int adjustedY = fieldShotAt.getYPos();
			// versuche x-Koordinate anzupassen
			if ((fieldShotAt.getXPos() + this.currentShip.getShootingRange() >= (boardSize)) && (orientation == Orientation.Horizontal)) {
				int overhang = (fieldShotAt.getXPos() + this.currentShip.getShootingRange()) - (boardSize);
				adjustedX = adjustX(fieldShotAt, WEST, overhang);
			}
			// versuche y-Koordinate anzupassen
			if ((fieldShotAt.getYPos() + this.currentShip.getShootingRange() >= (boardSize)) && (orientation == Orientation.Vertical)) {
				int overhang = (fieldShotAt.getYPos() + this.currentShip.getShootingRange()) - (boardSize);
				adjustedY = adjustY(fieldShotAt, NORTH, overhang);
			}
			fieldShotAt = new Field(adjustedX, adjustedY);
			hasTurnBeenMade = makeTurn(this.currentEnemy, adjustedX, adjustedY, orientation);

		} while (!hasTurnBeenMade);

		// Feld und Ausrichtung merken
		// TODO: Shot-Objekt?
		this.currentFieldShotAt = fieldShotAt;
		this.currentShotOrientation = orientation;

	}

	private boolean surroundingFieldContainsShip(Field fieldShotAt) throws FieldOutOfBoardException {
		// pr�ft ob ein umliegendes Feld schon ein zerst�rtes Schiff beinhaltet
		Board enemyBoard = this.currentEnemy.getBoard();
		int[] directions = new int[2];
		int x;
		int y;
		for (int i = 0; i < 8; i++) {
			directions = getDirectionArray(i);
			x = fieldShotAt.getXPos() + directions[0];
			y = fieldShotAt.getYPos() + directions[1];
			if (enemyBoard.containsFieldAtPosition(x, y)) {
				FieldState actualFieldState = enemyBoard.getField(x, y).getState();
				if (actualFieldState == FieldState.Destroyed) {
					return true;
				}
			}
		}

		return false;
	}

	private int adjustX(Field target, int currentDirection, int range) throws FieldOutOfBoardException {
		// wenn Richtung Westen , dann gehe Schussweite nach links um mehr
		// Felder zu treffen
		Board enemyBoard = this.currentEnemy.getBoard();
		int adjustedX = target.getXPos();
		int targetYPos = target.getYPos();

		if (currentDirection == WEST) {
			for (int i = 0; i < range; i++) {
				if (enemyBoard.containsFieldAtPosition(adjustedX - 1, targetYPos)) {
					if (!enemyBoard.getField(adjustedX - 1, target.getYPos()).isHit()) {
						adjustedX -= 1;

					} else
						break;
				}
			}
		}
		return adjustedX;
	}

	private int adjustY(Field target, int currentDirection, int range) throws FieldOutOfBoardException {
		// wenn Richtung Norden, um Schussweite hoch gehen, um mehr Felder zu
		// treffen
		Board enemyBoard = this.currentEnemy.getBoard();
		int adjustedY = target.getYPos();
		int targetXPos = target.getXPos();
		if (currentDirection == NORTH) {
			for (int i = 0; i < range; i++) {
				if (enemyBoard.containsFieldAtPosition(targetXPos, adjustedY - 1)) {
					if (!enemyBoard.getField(targetXPos, adjustedY - 1).isHit()) {
						adjustedY -= 1;
					} else
						break;
				}
			}
		}
		return adjustedY;
	}

	private boolean hasTargets() {
		// Ziele zum anvisieren �brig?
		boolean hasTargets;
		int i = 0;
		if (this.nextTargetsArray != null) {
			while ((i < 4) && (this.nextTargetsArray[i] == null)) {
				i++;
			}
		}
		hasTargets = (i < 4) && this.nextTargetsArray != null;
		return hasTargets;
	}

	private int getCurrentDirection() {
		// ermittle aktuelle Himmelsrichtung
		int currentDirection = 0;
		while ((currentDirection < 4) && (this.nextTargetsArray[currentDirection] == null)) {
			currentDirection++;
		}
		return currentDirection;
	}

	private void planNextShots(ArrayList<Field> hitFields) throws FieldOutOfBoardException {
		boolean isHorizontalHit = false;
		boolean isVerticalHit = false;
		// wenn mehrere Felder getroffen wurden, gucken ob die Schiffausrichtung
		// horizontal oder vertikal ist
		if (hitFields.size() > 1) {
			isHorizontalHit = hitFields.get(0).getYPos() == hitFields.get(1).getYPos();
			isVerticalHit = hitFields.get(0).getXPos() == hitFields.get(1).getXPos();
		}
		// bekommt ein getroffenes Feld �bergeben und guckt in alle
		// Himmelsrichtungen nach potenziellen Zielen
		this.nextTargetsArray = new Field[4];
		Field target = null;
		int[] directions = new int[2];
		for (int i = 0; i < 4; i++) {
			directions = getDirectionArray(i);
			if (!isHorizontalHit && !isVerticalHit) {
				target = findNextTarget(hitFields.get(0), directions[0], directions[1]);
			}
			if (isHorizontalHit && (i == EAST || i == WEST)) {
				target = findNextTarget(hitFields.get(0), directions[0], directions[1]);
			}
			if (isVerticalHit && (i == NORTH || i == SOUTH)) {
				target = findNextTarget(hitFields.get(0), directions[0], directions[1]);
			}

			// wenn potenzielles Ziel gefunden, dann Feld merken
			this.nextTargetsArray[i] = target;
		}
	}

	private int[] getDirectionArray(int direction) {
		// liefert ein Array mit x- und y-Richtung
		switch (direction) {
		case NORTH:
			return new int[] { 0, -1 };
		case SOUTH:
			return new int[] { 0, 1 };
		case EAST:
			return new int[] { 1, 0 };
		case WEST:
			return new int[] { -1, 0 };
		case NORTH_EAST:
			return new int[] { 1, -1 };
		case SOUTH_EAST:
			return new int[] { 1, 1 };
		case NORTH_WEST:
			return new int[] { -1, 1 };
		case SOUTH_WEST:
			return new int[] { -1, -1 };
		default:
			break;
		}
		return null;
	}

	private Field findNextTarget(Field field, int xDirection, int yDirection) throws FieldOutOfBoardException {
		// sucht nach dem n�chsten Feld als potenzielles Ziel, ausgehend vom
		// �bergebenen Feld
		Board enemyBoard = this.currentEnemy.getBoard();
		int step = 0;
		int x;
		int y;
		boolean endLoop = false;
		Field target = null;
		do {
			x = field.getXPos() + step * xDirection;
			y = field.getYPos() + step * yDirection;
			if (enemyBoard.containsFieldAtPosition(x, y)) {
				target = enemyBoard.getField(x, y);
				if ((target.getState() == FieldState.Missed) || (target.getState() == FieldState.Destroyed)) {
					// wenn Schiff verfehlt oder zerst�rt wurde, Ziel nicht
					// merken, Schleife abbrechen
					target = null;
					endLoop = true;
				} else if (!target.isHit()) {
					// wenn Feld noch nicht beschossen wurde, Schleife
					// abbrechen, Feld zur�ckgeben
					endLoop = true;
				}
			} else {
				// Feld nicht mehr im Board
				// Ziel nicht merken
				target = null;
				endLoop = true;
			}
			step++;
		} while (!endLoop);
		return target;
	}

	private ArrayList<Field> getHitFields() throws FieldOutOfBoardException {
		// pr�ft ob ein zuf�lliger Schuss (teilweise) getroffen hat
		// gibt den ersten gefundenen Treffer zur�ck
		// gibt null zur�ck, wenn es keinen Treffer gab
		int xDirection = this.currentShotOrientation == Orientation.Horizontal ? 1 : 0;
		int yDirection = this.currentShotOrientation == Orientation.Vertical ? 1 : 0;
		int x;
		int y;
		Field hitField = null;
		Board enemyBoard = this.currentEnemy.getBoard();
		ArrayList<Field> fields = new ArrayList<Field>();
		for (int i = 0; i < this.currentShip.getShootingRange(); i++) {
			x = this.currentFieldShotAt.getXPos() + i * xDirection;
			y = this.currentFieldShotAt.getYPos() + i * yDirection;
			if (enemyBoard.containsFieldAtPosition(x, y)) {
				hitField = enemyBoard.getField(x, y);
				if (hitField.hasShip()) {
					fields.add(hitField);
				}
			}
		}
		return fields;
	}
}
