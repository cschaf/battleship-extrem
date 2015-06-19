package de.hsbremen.battleshipextreme.model;

import de.hsbremen.battleshipextreme.model.exception.FieldOutOfBoardException;

import java.io.Serializable;

public class Board implements Serializable {
    private Field[][] fields;
    private int size;

    public Board(int size) {
        this.size = size;
        this.fields = new Field[size][size];
        for (int row = 0; row < fields.length; row++) {
            for (int column = 0; column < fields[row].length; column++) {
                this.fields[row][column] = new Field(column, row);
            }
        }
    }

    public Field[][] getFields() {
        return fields;
    }

    public Field getField(int x, int y) throws FieldOutOfBoardException {
        if (!this.containsFieldAtPosition(x, y))
            throw new FieldOutOfBoardException(new Field(x, y));
        return this.fields[y][x];
    }

    public boolean containsFieldAtPosition(int x, int y) {
        return (x < this.size) && (y < this.size) && (x >= 0) && (y >= 0);
    }

    public int getSize() {
        return size;
    }


    public FieldState[][] getFieldStates(boolean isOwnBoard) {
        FieldState[][] fieldStates = new FieldState[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                FieldState state = fields[j][i].getState();
                if ((state == FieldState.HAS_SHIP || state == FieldState.IS_EMPTY) && (!isOwnBoard)) {
                    fieldStates[i][j] = null;
                } else {
                    fieldStates[i][j] = state;
                }
            }
        }
        return fieldStates;
    }
}
