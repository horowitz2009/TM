package com.horowitz.tm;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

class Slot {
  Coords coords;
  BufferedImage image;
  boolean active;
  Rectangle area;

  public Slot(int row, int col, boolean active) {
    super();
    coords = new Coords(row, col);
    this.active = active;
    this.image = null;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (active ? 1231 : 1237);
    result = prime * result + ((coords == null) ? 0 : coords.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Slot other = (Slot) obj;
    if (active != other.active)
      return false;
    if (coords == null) {
      if (other.coords != null)
        return false;
    } else if (!coords.equals(other.coords))
      return false;
    return true;
  }

}