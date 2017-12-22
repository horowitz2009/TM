package com.horowitz.tm;

import java.util.HashMap;

public enum PairsLayout {
  NONE(null), 
  EASY_1(PredefinedPairsLayoutsFactory.buildEasy1()), 
  EASY_2(PredefinedPairsLayoutsFactory.buildEasy2()), 
  EASY_3(PredefinedPairsLayoutsFactory.buildEasy3());

  public HashMap<Coords, Slot> matrix;

  private PairsLayout(HashMap<Coords, Slot> matrix) {
    this.matrix = matrix;
  }

}
