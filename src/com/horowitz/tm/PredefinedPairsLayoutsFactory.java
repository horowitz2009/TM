package com.horowitz.tm;

import java.util.HashMap;

public class PredefinedPairsLayoutsFactory {

  public static HashMap<Coords, Slot> buildEasy1() {
    HashMap<Coords, Slot> matrix = new HashMap<>();
    matrix.put(new Coords(2, 3), new Slot(2, 3, true));
    matrix.put(new Coords(2, 4), new Slot(2, 4, true));
    matrix.put(new Coords(2, 5), new Slot(2, 5, true));
    matrix.put(new Coords(2, 6), new Slot(2, 6, true));
    matrix.put(new Coords(3, 3), new Slot(3, 3, true));
    matrix.put(new Coords(3, 4), new Slot(3, 4, true));
    matrix.put(new Coords(3, 5), new Slot(3, 5, true));
    matrix.put(new Coords(3, 6), new Slot(3, 6, true));
    return matrix;
  }
  public static HashMap<Coords, Slot> buildEasy2() {
    HashMap<Coords, Slot> matrix = new HashMap<>();
    matrix.put(new Coords(2, 3), new Slot(2, 3, true));
    matrix.put(new Coords(2, 4), new Slot(2, 4, true));
    matrix.put(new Coords(2, 5), new Slot(2, 5, true));
    matrix.put(new Coords(2, 6), new Slot(2, 6, true));
    matrix.put(new Coords(3, 3), new Slot(3, 3, true));
    matrix.put(new Coords(3, 4), new Slot(3, 4, true));
    matrix.put(new Coords(3, 5), new Slot(3, 5, true));
    matrix.put(new Coords(3, 6), new Slot(3, 6, true));

    matrix.put(new Coords(1, 4), new Slot(1, 4, true));
    matrix.put(new Coords(1, 5), new Slot(1, 5, true));
    matrix.put(new Coords(4, 4), new Slot(4, 4, true));
    matrix.put(new Coords(4, 5), new Slot(4, 5, true));
    return matrix;
  }
  public static HashMap<Coords, Slot> buildEasy3() {
    HashMap<Coords, Slot> matrix = new HashMap<>();
    matrix.put(new Coords(3, 3), new Slot(3, 3, true));
    matrix.put(new Coords(3, 4), new Slot(3, 4, true));
    matrix.put(new Coords(3, 5), new Slot(3, 5, true));
    matrix.put(new Coords(3, 6), new Slot(3, 6, true));
    matrix.put(new Coords(4, 4), new Slot(4, 4, true));
    matrix.put(new Coords(4, 5), new Slot(4, 5, true));
    matrix.put(new Coords(2, 3), new Slot(2, 3, true));
    matrix.put(new Coords(2, 6), new Slot(2, 6, true));
    
    matrix.put(new Coords(2, 2), new Slot(2, 2, true));
    matrix.put(new Coords(2, 7), new Slot(2, 7, true));
    
    matrix.put(new Coords(1, 2), new Slot(1, 2, true));
    matrix.put(new Coords(1, 3), new Slot(1, 3, true));
    matrix.put(new Coords(1, 6), new Slot(1, 6, true));
    matrix.put(new Coords(1, 7), new Slot(1, 7, true));
    return matrix;
  }
}
