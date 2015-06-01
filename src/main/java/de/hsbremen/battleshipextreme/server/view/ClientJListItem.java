package de.hsbremen.battleshipextreme.server.view;

import java.awt.*;
import java.io.Serializable;

/**
 * Created by cschaf on 28.05.2015.
 */
public class ClientJListItem implements Serializable{
  private String ip;
  private int port;
  private String name;

  public ClientJListItem(String ip, int port, String name) {
    this.ip = ip;
    this.port = port;
    this.name = name;
  }

  @Override
  public String toString() {
    return this.name + " (" + this.port +  ")";
  }

  public String getIp() {
    return ip;
  }

  public int getPort() {
    return port;
  }

  public String getName() {
    return this.name;
  }
}