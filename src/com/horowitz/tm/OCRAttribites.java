package com.horowitz.tm;

import com.horowitz.ocr.OCRe;

public class OCRAttribites {

  /**
   * @param args
   */
    public static void main(String[] args) {
      OCRe ocr = new OCRe();
      //ocr.learn("ocrTemp", "coin", "ocrTemp/res", true);
      //ocr.learn("ocrTemp", "pass", "images/ocr/pass", true);
      ocr.getThreshold().setValue(160);
      ocr.learn("ocr", "s", "images/ocr/attrs", false);
  }

}
