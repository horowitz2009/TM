package com.horowitz.tm;

import java.io.IOException;

import com.horowitz.commons.CrazyImageComparator;
import com.horowitz.commons.ImageData;
import com.horowitz.commons.Pixel;
import com.horowitz.commons.Settings;
import com.horowitz.commons.TemplateMatcher;

public class ImageTester {

  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      testPixels();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private static void testPixels() throws IOException {
    Settings settings = Settings.createSettings("tm.properties");

    ScreenScanner scanner = new ScreenScanner(settings);
    TemplateMatcher matcher = new TemplateMatcher();
    CrazyImageComparator comparator = new CrazyImageComparator();
    ImageData image1 = scanner.getImageData("TEST.bmp");
    ImageData image2 = scanner.getImageData("TEST2.bmp");
    boolean b = matcher.compare(image1.getImage(), image2.getImage());
    System.err.println(b);
    Pixel p = comparator.findImage(image1.getImage(), image2.getImage());
    System.err.println(p);
  }
}
