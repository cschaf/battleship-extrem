package de.hsbremen.battleshipextreme.client.workers;

import de.hsbremen.battleshipextreme.client.Controller;
import de.hsbremen.battleshipextreme.client.GamePanel;
import de.hsbremen.battleshipextreme.model.player.Player;
import de.hsbremen.battleshipextreme.model.ship.ShipType;

import javax.swing.*;
import java.awt.*;

/**
 * Created by cschaf on 17.06.2015.
 */
public class ShipStatusUpdater extends SwingWorker<Integer, Object> {
    Controller ctrl;
    GamePanel panelGame;
    private Player player;

    public ShipStatusUpdater(Controller ctrl, GamePanel panelGame, Player player) {
        this.ctrl = ctrl;
        this.panelGame = panelGame;
        this.player = player;
    }

    @Override
    protected Integer doInBackground() throws Exception {
        Color green = new Color(0, 180, 0);
        Color red = new Color(180, 0, 0);
        JLabel[] shipFields = panelGame.getLabelDestroyer();
        if (player.getShipCount(ShipType.DESTROYER) == 0) {
            ctrl.UpdateShipLabelColors(shipFields, red);
        } else {
            ctrl.UpdateShipLabelColors(shipFields, green);
        }
        panelGame.getLabelDestroyerShipCount().setText("" + player.getShipCount(ShipType.DESTROYER));

        shipFields = panelGame.getLabelFrigate();
        if (player.getShipCount(ShipType.FRIGATE) == 0) {
            ctrl.UpdateShipLabelColors(shipFields, red);
        } else {
            ctrl.UpdateShipLabelColors(shipFields, green);
        }
        panelGame.getLabelFrigateShipCount().setText("" + player.getShipCount(ShipType.FRIGATE));

        shipFields = panelGame.getLabelCorvette();
        if (player.getShipCount(ShipType.CORVETTE) == 0) {
            ctrl.UpdateShipLabelColors(shipFields, red);
        } else {
            ctrl.UpdateShipLabelColors(shipFields, green);
        }
        panelGame.getLabelCorvetteShipCount().setText("" + player.getShipCount(ShipType.CORVETTE));

        shipFields = panelGame.getLabelSubmarine();
        if (player.getShipCount(ShipType.SUBMARINE) == 0) {
            ctrl.UpdateShipLabelColors(shipFields, red);
        } else {
            ctrl.UpdateShipLabelColors(shipFields, green);
        }
        panelGame.getLabelSubmarineShipCount().setText("" + player.getShipCount(ShipType.SUBMARINE));
        return 1;
    }
}
