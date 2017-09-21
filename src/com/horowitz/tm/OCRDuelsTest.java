package com.horowitz.tm;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.horowitz.ocr.OCRe;

public class OCRDuelsTest {

  private OCRe ocre;

  @Test
  @Ignore
  public void testDuels() {
    try {
      OCRe ocre = new OCRe("ocr/duels/d");
      ocre.getThreshold().setValue(160);
      String s = ocre.scanImage("ocr/1825.bmp");
      System.err.println(s);
      assertEquals("18/25", s);

      s = ocre.scanImage("ocr/4270.bmp");
      System.err.println(s);
      assertEquals("4/270", s);

      s = ocre.scanImage("ocr/2025.bmp");
      System.err.println(s);
      assertEquals("20/25", s);

      s = ocre.scanImage("ocr/55270.bmp");
      System.err.println(s);
      assertEquals("55/270", s);

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Before
  public void setup() {
    try {
      ocre = new OCRe("ocr/attrs/s");
      ocre.getOcrb().setErrors(2);

      ocre.getThreshold().setValue(160);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @Test
  public void testAttrs() {
    try {
      test3Digit(406);
      test3Digit(391);
      test3Digit(395);
      test3Digit(391);
      test3Digit(426);
      test3Digit(425);
      test3Digit(464);
      test3Digit(461);
      test3Digit(452);
      test3Digit(433);
      test3Digit(442);
      test3Digit(429);
      test3Digit(432);

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  private void test3Digit(int number) throws IOException {
    String s = ocre.scanImage("ocr/" + number + ".bmp");
    System.err.println("scanned: " + s);
    assertEquals("" + number, s);

  }

  @Test
  public void testDev() {
    Attributes attrs = new Attributes(500, 499, 450);
    System.err.println(attrs + "     dev " + attrs.deviation());

    attrs = new Attributes(401, 401, 401);
    System.err.println(attrs + "     dev " + attrs.deviation());
    attrs = new Attributes(500, 50, 500);
    System.err.println(attrs + "     dev " + attrs.deviation());
    attrs = new Attributes(500, 5, 50);
    System.err.println(attrs + "     dev " + attrs.deviation());
    attrs = new Attributes(420, 380, 377);
    System.err.println(attrs + "     dev " + attrs.deviation());
    attrs = new Attributes(420, 410, 405);
    System.err.println(attrs + "     dev " + attrs.deviation());
  }
}
