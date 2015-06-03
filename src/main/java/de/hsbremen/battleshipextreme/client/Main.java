package de.hsbremen.battleshipextreme.client;

import de.hsbremen.battleshipextreme.model.Game;
import de.hsbremen.battleshipextreme.model.network.NetworkClient;

public class Main {

    public static void main(String[] args) {
        Game game = new Game();
        GUI gui = new GUI();
        NetworkClient network = new NetworkClient();
        Controller controller = new Controller(game, network, gui);
    }
}
