package de.hsbremen.battleshipextreme.network.transfarableObject;

import de.hsbremen.battleshipextreme.network.TransferableType;

import java.util.Vector;

/**
 * Created by cschaf on 02.06.2015.
 * Beinhaltet eine Liste aller Spielobjekte f�r den Client
 */
public class GameList extends TransferableObject {
    private Vector<NetGame> netGameList; // Liste mit Spielen

    public GameList(Vector<NetGame> netGameList) {
        this.netGameList = netGameList;
    }

    public TransferableType getType() {
        return TransferableType.GameList;
    }

    /**
     * Gibt die Liste der Spiele zur�ck
     */
    public Vector<NetGame> getNetGameList() {
        return netGameList;
    }
}
