package com.horowitz.tm;

import java.util.HashMap;

public interface PairsMatrixFactory {
  HashMap<Coords, Slot> build();
}
