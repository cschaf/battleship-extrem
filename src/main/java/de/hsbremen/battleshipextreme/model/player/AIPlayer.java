package de.hsbremen.battleshipextreme.model.player;

import java.util.ArrayList;
import java.util.Random;

import de.hsbremen.battleshipextreme.model.Board;
import de.hsbremen.battleshipextreme.model.Field;
import de.hsbremen.battleshipextreme.model.FieldState;
import de.hsbremen.battleshipextreme.model.Orientation;
import de.hsbremen.battleshipextreme.model.exception.FieldOutOfBoardException;
import de.hsbremen.battleshipextreme.model.exception.ShipAlreadyPlacedException;
import de.hsbremen.battleshipextreme.model.exception.ShipOutOfBoardException;
import de.hsbremen.battleshipextreme.model.ship.Submarine;

/**
 * This class provides methods that are used by all AIPlayers.
 *
 */

public class AIPlayer extends Player {

	// zum Merken des Gegners
	private int currentEnemyIndex;

	private Board enemyBoardRepresentation;

	// enth�lt 4 Felder f�r jede Himmelsrichtung
	private Field[] nextTargetsArray;

	private Target currentTarget;

	private static final int NORTH = 0;
	private static final int SOUTH = 1;
	private static final int EAST = 2;
	private static final int WEST = 3;
	private static final int NORTH_EAST = 4;
	private static final int SOUTH_EAST = 5;
	private static final int NORTH_WEST = 6;
	private static final int SOUTH_WEST = 7;

	private static final int MAX_TRIES_TO_PLACE_SHIP = 1000;

	public AIPlayer(int boardSize, int destroyers, int frigates, int corvettes, int submarines, PlayerType aiType) {
		super(boardSize, destroyers, frigates, corvettes, submarines);
		this.type = aiType;
		this.name = aiType.toString();
	}

	public void placeShips() throws ShipAlreadyPlacedException, FieldOutOfBoardException, ShipOutOfBoardException {
		boolean isItPossibleToPlaceShip;
		int i = 0;
		do {
			int counter = 0;
			do {
				currentShip = ships[i];
				isItPossibleToPlaceShip = false;
				Target shot = getRandomShipPlacementTarget();
				counter++;
				isItPossibleToPlaceShip = placeShip(shot.getX(), shot.getY(), shot.getOrientation());
			} while ((counter <= MAX_TRIES_TO_PLACE_SHIP) && (!isItPossibleToPlaceShip));

			if (counter >= MAX_TRIES_TO_PLACE_SHIP) {
				resetBoard();
				counter = 0;
				i = 0;
			} else {
				i++;
				nextShip();
			}
		} while (i < ships.length);
	}

	public Target getTarget(FieldState[][] fieldStates) throws Exception {
		if (type == PlayerType.DUMB_AI)
			return getRandomShot();

		enemyBoardRepresentation = buildBoardRepresentation(fieldStates);
		if (hasTargets()) {
			return getNextTarget();
		} else {
			return getNewTarget();
		}
	}

	private Target getNewTarget() throws Exception {
		// Wenn es mehr als 2 Mitspieler gibt oder die KI mit einem Schuss
		// gleichzeitig zwei Schiffe getroffen hat (nur mit Destroyer
		// m�glich), kann es sein, dass es getroffene Schiffe gibt, die sich
		// die KI nicht gemerkt hat.
		// Deshalb wird, wenn die KI keine vorgemerkten Ziele mehr hat, das
		// Feld nach getroffenen Schiffen abgesucht
		ArrayList<Field> hitFields = lookForHitFields();

		// wenn ein getroffenes Schiff gefunden wurde, dann plane die n�chsten
		// Sch�sse und greife das gefundene Ziel an
		if (hitFields.size() > 0) {
			planNextShots(hitFields);
			currentTarget = getNextTarget();
		} else {
			// wenn keine getroffenen Schiffe gefunden wurden
			// zuf�llig schie�en, Schuss merken
			currentTarget = getRandomShot();
		}
		return currentTarget;
	}

	private Target getNextTarget() throws Exception {
		int currentDirection = getCurrentDirection();

		// wenn der letzte Schuss ein Treffer war, dann nach n�chstem
		// unbeschossenen Feld in selbe Richtung suchen und als n�chstes Ziel
		// speichern

		// wenn es noch keinen Schuss gab, dann �berspringen
		// (kann der Fall sein, wenn die AI einen Treffer findet,
		// der von einem anderen Spieler verursacht wurde)
		if (currentTarget != null) {
			Field lastFieldShotAt = enemyBoardRepresentation.getField(currentTarget.getX(), currentTarget.getY());

			if (!isTargetDestroyed()) {
				if (lastFieldShotAt.getState() == FieldState.Hit) {
					int[] directionArray = getDirectionArray(currentDirection);
					Field newTarget = findNextTarget(lastFieldShotAt, directionArray[0], directionArray[1]);
					// neues Ziel in gleiche Richtung setzen
					nextTargetsArray[currentDirection] = newTarget;

				} else {
					// wenn kein Treffer, dann Target l�schen
					nextTargetsArray[currentDirection] = null;
				}
			} else {
				// Schiff zerst�rt, komplettes targetArray l�schen,
				// da die Spur nicht mehr verfolgt werden muss
				nextTargetsArray = null;
			}
		}

		// wenn (noch) vorgemerkte Ziele vorhanden sind
		if (hasTargets()) {
			Field target;
			currentDirection = getCurrentDirection();
			// aktuelles Ziel ermitteln
			target = nextTargetsArray[currentDirection];
			// wenn Richtung Osten oder Westen, dann Ausrichtung horizontal,
			// ansonsten vertikal
			Orientation orientation = (currentDirection == EAST || currentDirection == WEST) ? Orientation.Horizontal : Orientation.Vertical;

			// x und y abh�ngig von der Schussweite korrigieren, so dass
			// m�glichst viele Felder getroffen werden
			int range = this.currentShip.getShootingRange() - 1;
			int adjustedX = adjustX(target, currentDirection, range);
			int adjustedY = adjustY(target, currentDirection, range);
			currentTarget = new Target(adjustedX, adjustedY, orientation);
		} else {
			// wenn kein Ziel mehr vorhanden, neues Ziel suchen
			currentTarget = null;
			getNewTarget();
		}
		return currentTarget;
	}

	private boolean isTargetDestroyed() throws FieldOutOfBoardException {
		ArrayList<Field> hitFields = getHitFields();
		for (Field f : hitFields) {
			if (f.getState() == FieldState.Destroyed) {
				return true;
			}
		}
		return false;
	}

	private ArrayList<Field> lookForHitFields() throws FieldOutOfBoardException {
		ArrayList<Field> hitFields = new ArrayList<Field>();
		for (int i = 0; i < enemyBoardRepresentation.getSize(); i++) {
			for (int j = 0; j < enemyBoardRepresentation.getSize(); j++) {
				Field f = enemyBoardRepresentation.getField(j, i);
				if (f.getState() == FieldState.Hit) {
					hitFields.add(f);
				}
			}
		}
		return hitFields;
	}

	private Target getRandomShot() throws Exception {
		Orientation orientation;
		Field fieldShotAt;
		int boardSize = this.board.getSize();

		// zuf�llig schie�en
		if (type == PlayerType.DUMB_AI) {
			orientation = (createRandomNumber(0, 1) == 0) ? Orientation.Horizontal : Orientation.Vertical;
			fieldShotAt = createRandomField(0, boardSize - 1, 0, boardSize - 1);
			return new Target(fieldShotAt.getXPos(), fieldShotAt.getYPos(), orientation);
		}

		// wiederhole die Erzeugung von zuf�lligen Koordinaten
		// bis ein Feld gefunden wird, an welches kein zerst�rtes
		// Schiff angrenzt,
		// (zwischen den Schiffen muss immer ein Feld frei sein)
		do {
			orientation = (createRandomNumber(0, 1) == 0) ? Orientation.Horizontal : Orientation.Vertical;
			fieldShotAt = createRandomField(0, boardSize - 1, 0, boardSize - 1);
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

		return new Target(adjustedX, adjustedY, orientation);
	}

	private boolean surroundingFieldContainsShip(Field fieldShotAt) throws FieldOutOfBoardException {
		// pr�ft ob ein umliegendes Feld schon ein zerst�rtes Schiff beinhaltet
		int[] directions = new int[2];
		int x;
		int y;
		for (int i = 0; i < 8; i++) {
			directions = getDirectionArray(i);
			x = fieldShotAt.getXPos() + directions[0];
			y = fieldShotAt.getYPos() + directions[1];
			if (enemyBoardRepresentation.containsFieldAtPosition(x, y)) {
				FieldState actualFieldState = enemyBoardRepresentation.getField(x, y).getState();
				if (actualFieldState == FieldState.Destroyed) {
					return true;
				}
			}
		}

		return false;
	}

	private int adjustX(Field target, int currentDirection, int range) throws FieldOutOfBoardException {
		// wenn Richtung Westen, dann gehe Schussweite nach links um mehr
		// Felder zu treffen
		int adjustedX = target.getXPos();
		int targetYPos = target.getYPos();

		if (currentDirection == WEST) {
			for (int i = 0; i < range; i++) {
				if (enemyBoardRepresentation.containsFieldAtPosition(adjustedX - 1, targetYPos)) {
					if (!enemyBoardRepresentation.getField(adjustedX - 1, target.getYPos()).isHit()) {
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
		int adjustedY = target.getYPos();
		int targetXPos = target.getXPos();
		if (currentDirection == NORTH) {
			for (int i = 0; i < range; i++) {
				if (enemyBoardRepresentation.containsFieldAtPosition(targetXPos, adjustedY - 1)) {
					if (!enemyBoardRepresentation.getField(targetXPos, adjustedY - 1).isHit()) {
						adjustedY -= 1;
					} else
						break;
				}
			}
		}
		return adjustedY;
	}

	public boolean hasTargets() {
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

			// liegen die Felder horizontal aneinander?
			isHorizontalHit = hitFields.get(0).getYPos() == hitFields.get(1).getYPos() && ((Math.abs(hitFields.get(0).getXPos() - hitFields.get(1).getXPos()) == 1));
			// liegen die Felder vertikal aneinander
			isVerticalHit = hitFields.get(0).getXPos() == hitFields.get(1).getXPos() && ((Math.abs(hitFields.get(0).getYPos() - hitFields.get(1).getYPos()) == 1));
		}
		// bekommt ein getroffenes Feld �bergeben und guckt in alle
		// Himmelsrichtungen nach potenziellen Zielen
		nextTargetsArray = new Field[4];
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
			nextTargetsArray[i] = target;
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
		int step = 0;
		int x;
		int y;
		boolean endLoop = false;
		Field target = null;
		do {
			x = field.getXPos() + step * xDirection;
			y = field.getYPos() + step * yDirection;
			if (enemyBoardRepresentation.containsFieldAtPosition(x, y)) {
				target = enemyBoardRepresentation.getField(x, y);
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
		int xDirection = currentTarget.getOrientation() == Orientation.Horizontal ? 1 : 0;
		int yDirection = currentTarget.getOrientation() == Orientation.Vertical ? 1 : 0;
		int x;
		int y;
		Field hitField = null;
		ArrayList<Field> fields = new ArrayList<Field>();
		for (int i = 0; i < this.currentShip.getShootingRange(); i++) {
			x = currentTarget.getX() + i * xDirection;
			y = currentTarget.getY() + i * yDirection;
			if (enemyBoardRepresentation.containsFieldAtPosition(x, y)) {
				hitField = enemyBoardRepresentation.getField(x, y);
				if (hitField.hasShip()) {
					fields.add(hitField);
				}
			}
		}
		return fields;
	}

	private Target getRandomShipPlacementTarget() {
		// zuf�llige Position generieren
		Orientation orientation;
		orientation = (createRandomNumber(0, 1) == 0) ? Orientation.Horizontal : Orientation.Vertical;
		int xMax;
		int yMax;
		if (orientation == Orientation.Horizontal) {
			xMax = this.board.getSize() - this.getCurrentShip().getSize();
			yMax = this.board.getSize() - 1;
		} else {
			xMax = this.board.getSize() - 1;
			yMax = this.board.getSize() - this.getCurrentShip().getSize();

		}
		int xPos = createRandomNumber(0, xMax);
		int yPos = createRandomNumber(0, yMax);
		return new Target(xPos, yPos, orientation);
	}

	private Field createRandomField(int xMin, int xMax, int yMin, int yMax) {
		int xPos;
		int yPos;
		xPos = createRandomNumber(xMin, xMax);
		yPos = createRandomNumber(yMin, yMax);
		return new Field(xPos, yPos);
	}

	private int createRandomNumber(int min, int max) {
		Random random = new Random();
		return random.nextInt(max - min + 1) + min;
	}

	// baut anhand der bekannten Fieldstates eine Nachbildung des Boards
	private Board buildBoardRepresentation(FieldState[][] fieldStates) throws FieldOutOfBoardException {
		enemyBoardRepresentation = new Board(fieldStates.length);
		for (int i = 0; i < fieldStates.length; i++) {
			for (int j = 0; j < fieldStates[i].length; j++) {
				FieldState state = fieldStates[i][j];
				if (state != null) {
					Field f = enemyBoardRepresentation.getField(j, i);
					switch (state) {
					case Destroyed:
						Submarine submarine = new Submarine();
						submarine.setSize(0);
						f.setShip(submarine);
						f.setHit(true);
						break;
					case Missed:
						f.setHit(true);
						break;
					case Hit:
						f.setHit(true);
						f.setShip(new Submarine());
						break;
					default:
						break;
					}
				}
			}
		}
		return enemyBoardRepresentation;
	}

	public int getCurrentEnemyIndex() {
		return currentEnemyIndex;
	}

	public void setCurrentEnemyIndex(int currentEnemyIndex) {
		this.currentEnemyIndex = currentEnemyIndex;
	}

	public void setRandomEnemyIndex(int max) {
		this.currentEnemyIndex = createRandomNumber(0, max);
	}

}
