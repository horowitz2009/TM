package com.horowitz.tm;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;

import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Filters.Threshold;

public class QuizParams {
  public static final String QUESTION_DISPLAYED = "questionDisplayed3.png";
  public static final String QUESTION_DISPLAYED_TOUR = "questionDisplayedT2.png";
  public static final String TOP_LEFT_CORNER_TOUR = "topLeftCornerT.png";
  public static final String TOP_LEFT_CORNER = "topLeftCorner6.bmp";
  public static final String DB_DESTINATION = "C:/BACKUP/DBQUIZ/DB";
  public static final String DB_DESTINATION_RAW = "C:/backup/DBQUIZ/raw";
  public static final String SOURCE_FOLDER = "quiz";
  public static final String TOP_LEFT_CORNER_ANSWERED = "topLeftCornerQA.png";
  public static final String TOP_LEFT_CORNER_ANSWERED_TOUR = "topLeftCornerTA.png";

  public final static String FILE_DATE_FORMAT = "yyyyMMdd-HHmmss-SSS";
  public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(FILE_DATE_FORMAT);
  public static final Threshold THRESHOLD = new Threshold(80);
  public static final int COMPARATOR_PRECISION = 45;
  public static final int COMPARATOR_PRECISION_INT = 10;

  public static BufferedImage toBW(BufferedImage src) {
    FastBitmap fb = new FastBitmap(src);
    if (!fb.isGrayscale())
      fb.toGrayscale();
    THRESHOLD.applyInPlace(fb);
    return fb.toBufferedImage();
  }
  
  public static void deleteTree(File folder) {
    if (folder.isDirectory()) {
      for (File f : folder.listFiles()) {
        if (f.isFile())
          f.delete();
        else
          deleteTree(f);
      }
      folder.delete();
    }
  }
  
  public static void emptyFolder(File folder) {
    if (folder.isDirectory()) {
      for (File f : folder.listFiles()) {
        if (f.isFile())
          f.delete();
      }
      folder.delete();
    }
  }
}
