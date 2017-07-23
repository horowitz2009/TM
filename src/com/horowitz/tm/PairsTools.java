package com.horowitz.tm;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

import Catalano.Core.IntRange;
import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Filters.ColorFiltering;
import Catalano.Imaging.Filters.Threshold;

public class PairsTools {

  public static boolean areMatching(Rectangle slot1, Rectangle slot2) throws AWTException {

    Robot robot = new Robot();
    BufferedImage im1 = robot.createScreenCapture(slot1);
    BufferedImage im2 = robot.createScreenCapture(slot2);
    FastBitmap fb1 = new FastBitmap(im1);
    FastBitmap fb2 = new FastBitmap(im2);

    return isBoom(fb1) && isBoom(fb2);

  }

  private static boolean isBoom(FastBitmap fb) {

    FastBitmap fb2 = new FastBitmap(fb);

    int r = 196;
    int g = 166;
    int b = 79;
    int offset = 20;

    ColorFiltering colorFiltering = new ColorFiltering(new IntRange(r - offset, r + offset),
        new IntRange(g - offset, g + offset), new IntRange(b - offset, b + offset));
    colorFiltering.applyInPlace(fb);
    fb.toGrayscale();
    Threshold t = new Threshold(10);
    t.applyInPlace(fb);
    // fb.saveAsBMP("hmm2t.bmp");

    int cnt1 = countPixels(fb);

    if (cnt1 > 100) {
      colorFiltering = new ColorFiltering(new IntRange(250, 255), new IntRange(250, 255), new IntRange(250, 255));
      colorFiltering.applyInPlace(fb2);
      // fb2.saveAsBMP("hmm3.bmp");
      fb2.toGrayscale();
      t.applyInPlace(fb2);
      // fb2.saveAsBMP("hmm3t.bmp");
      int cnt2 = countPixels(fb2);
      return cnt2 > 100;
    }
    return false;

  }

  private static int countPixels(FastBitmap fb) {
    int cnt = 0;
    for (int x = 0; x < fb.getHeight(); x++) {
      for (int y = 0; y < fb.getWidth(); y++) {
        if (fb.getGray(x, y) > 0)
          cnt++;

      }
    }
    return cnt;
  }

}
