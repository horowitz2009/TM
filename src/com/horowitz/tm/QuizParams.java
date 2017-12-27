package com.horowitz.tm;

import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;

import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Filters.Threshold;

public class QuizParams {
  public static final String QUESTION_DISPLAYED = "questionDisplayed.bmp";
  public static final String QUESTION_DISPLAYED_TOUR = "questionDisplayedT.bmp";
  public static final String TOP_LEFT_CORNER_TOUR = "topLeftCorner2T.png";
  public static final String TOP_LEFT_CORNER = "topLeftCorner2.bmp";
  public static final String DB_DESTINATION = "C:/backup/OUTPUT2/output";
  public static final String SOURCE_FOLDER = "quiz";

  public final static String FILE_DATE_FORMAT = "yyyyMMdd-HHmmss-SSS";
  public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(FILE_DATE_FORMAT);
  public static final Threshold THRESHOLD = new Threshold(100);
  public static final int COMPARATOR_PRECISION = 10000;

  public static BufferedImage toBW(BufferedImage src) {
    FastBitmap fb = new FastBitmap(src);
    if (!fb.isGrayscale())
      fb.toGrayscale();
    THRESHOLD.applyInPlace(fb);
    return fb.toBufferedImage();
  }
}
