package com.horowitz.tm;

import java.awt.image.BufferedImage;
import java.io.File;

import com.horowitz.commons.MyImageIO;

public class Question {
  public String qFilename;
  public BufferedImage qImage;

  public int correctAnswer;

  public String[] aFilenames;
  public BufferedImage[] aImages;
  public boolean dup;

  public Question() {
    aFilenames = new String[4];
    aImages = new BufferedImage[4];
    dup = false;
  }

  public void writeImages(File outputDir, String ext) {
    MyImageIO.writeImage(qImage, outputDir.getAbsolutePath() + "/" + qFilename + "." + ext);
    if (correctAnswer > 0) {
      aFilenames[correctAnswer - 1] = aFilenames[correctAnswer - 1] + "-CORRECT";
    }
    for (int i = 0; i < aFilenames.length; i++) {
      MyImageIO.writeImage(aImages[i], outputDir.getAbsolutePath() + "/" + aFilenames[i] + "." + ext);

    }
  }

  public void writeImages2(File outputDir) {
    MyImageIO.writeImage(qImage, outputDir.getAbsolutePath() + "/" + qFilename);

    if (correctAnswer > 0 && aFilenames[correctAnswer - 1].indexOf("CORRECT") < 0) {
      String[] s = aFilenames[correctAnswer - 1].split("\\.");
      aFilenames[correctAnswer - 1] = s[0] + "-CORRECT" + "." + s[1];
    }

    for (int i = 0; i < aFilenames.length; i++) {
      MyImageIO.writeImage(aImages[i], outputDir.getAbsolutePath() + "/" + aFilenames[i]);
    }
  }

  @Override
  public String toString() {
    return this.qFilename + " correct: " + (correctAnswer);
  }

  public BufferedImage getCorrectImage() {
    if (correctAnswer > 0)
      return aImages[correctAnswer - 1];
    else
      return null;
  }
}
