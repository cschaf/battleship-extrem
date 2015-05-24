package de.hsbremen.battleshipextreme.network;

import de.hsbremen.battleshipextreme.model.Field;
import de.hsbremen.battleshipextreme.model.Settings;
import de.hsbremen.battleshipextreme.model.player.Player;
import de.hsbremen.battleshipextreme.model.ship.Ship;
import de.hsbremen.battleshipextreme.network.transfarableObject.*;

/**
 * Created by cschaf on 26.04.2015.
 */
public class TransferableObjectFactory {
    public static ITransferable CreateClientMessage(String message, ITransferable sender) {
        return new ClientMessage(message, sender);
    }

    public static ITransferable CreateClientInfo(String username, String ip, int port) {
        return new ClientInfo(username, ip, port);
    }

    public static ITransferable CreateClientInfo(String username, String ip, int port, InfoSendingReason reason) {
        return new ClientInfo(username, ip, port, reason);
    }

    public static ITransferable CreateMessage(String message) {
        return new Message(message);
    }

    public static ITransferable CreateGame(String name, Settings settings) {
        return new Game(name, settings);
    }

    public static ITransferable CreateTurn(String gameId, Player from, Player to, Ship ship, Field field) {
        return new Turn(gameId, from, to,ship, field);
    }
}
