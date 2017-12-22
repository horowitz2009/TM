package com.horowitz.tm;

import java.util.HashMap;

public enum PairsLayout {
  NONE(null), EASY_1(new PairsMatrixFactory() {
    @Override
    public HashMap<Coords, Slot> build() {
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
  }), EASY_2(new PairsMatrixFactory() {
    @Override
    public HashMap<Coords, Slot> build() {
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
  }), EASY_3(new PairsMatrixFactory() {
    @Override
    public HashMap<Coords, Slot> build() {
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
  });

  private PairsMatrixFactory factory;

  private PairsLayout(PairsMatrixFactory factory) {
    this.factory = factory;
  }

  public HashMap<Coords, Slot> buildMatrix() {
    return factory.build();
  }

}
