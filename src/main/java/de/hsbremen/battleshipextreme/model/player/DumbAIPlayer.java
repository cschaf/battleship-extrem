package de.hsbremen.battleshipextreme.model.player;

import java.util.ArrayList;

import de.hsbremen.battleshipextreme.model.Field;
import de.hsbremen.battleshipextreme.model.Orientation;

/**
 * Dumb AI - shoots randomly
 * 
 * AI-Benchmark: ~ 77 rounds
 *
 */

public class DumbAIPlayer extends AIPlayer {
	Field[] nextTargetsArray;

	public DumbAIPlayer(int boardSize, int destroyers, int frigates, int corvettes, int submarines) {
		super(boardSize, destroyers, frigates, corvettes, submarines);
		this.name = "Dumme KI";
		;
	}

	public void makeAiTurn(ArrayList<Player> availablePlayers) throws Exception {
		Orientation orientation;
		boolean hasTurnBeenMade = false;

		chooseShipToShootWithRandomly();

		// Gegner zuf�llig w�hlen
		int randomEnemyIndex = generateRandomNumber(0, availablePlayers.size() - 1);
		this.currentEnemy = availablePlayers.get(randomEnemyIndex);

		// zuf�llig schie�en
		do {
			orientation = (generateRandomNumber(0, 1) == 0) ? Orientation.Horizontal : Orientation.Vertical;
			Field field = generateField(orientation, this.currentShip.getSize());
			try {
				hasTurnBeenMade = super.makeTurn(this.currentEnemy, field.getXPos(), field.getYPos(), orientation);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} while (!hasTurnBeenMade);

	}

}
