package com.horowitz.tm;

import Catalano.Core.IntRange;
import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Filters.Threshold;
import Catalano.Imaging.Filters.ColorFiltering;

public class PairsTest {

  public static void main(String[] args) {

    FastBitmap fb = new FastBitmap("test/konus.bmp");
    FastBitmap fb2 = new FastBitmap(fb);

    int r = 196;
    int g = 166;
    int b = 79;
    int offset = 20;
    fb.saveAsBMP("hmm.bmp");

    ColorFiltering colorFiltering = new ColorFiltering(new IntRange(r - offset, r + offset),
        new IntRange(g - offset, g + offset), new IntRange(b - offset, b + offset));
    colorFiltering.applyInPlace(fb);
    // fb.saveAsBMP("hmm2.bmp");

    fb.toGrayscale();
    Threshold t = new Threshold(10);
    t.applyInPlace(fb);
    fb.saveAsBMP("hmm2t.bmp");

    countPixels(fb);


    colorFiltering = new ColorFiltering(new IntRange(250, 255), new IntRange(250, 255), new IntRange(250, 255));
    colorFiltering.applyInPlace(fb2);
    // fb2.saveAsBMP("hmm3.bmp");
    fb2.toGrayscale();
    t.applyInPlace(fb2);
    fb2.saveAsBMP("hmm3t.bmp");
    countPixels(fb2);
    
    System.err.println("done");

  }

  private static void countPixels(FastBitmap fb) {
    int cnt = 0;
    for (int x = 0; x < fb.getHeight(); x++) {
      for (int y = 0; y < fb.getWidth(); y++) {
        if(fb.getGray(x, y)>0)cnt++;
        
      }
    }
    System.err.println("cnt1: "+cnt + " of " + fb.getHeight() * fb.getWidth());
  }
}
