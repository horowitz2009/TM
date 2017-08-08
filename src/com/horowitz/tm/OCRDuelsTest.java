package com.horowitz.tm;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.horowitz.ocr.OCRe;

public class OCRDuelsTest {

  @Test
  public void test() {
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

}
