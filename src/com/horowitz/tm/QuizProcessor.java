package com.horowitz.tm;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import com.horowitz.commons.DateUtils;
import com.horowitz.commons.ImageData;
import com.horowitz.commons.MouseRobot;
import com.horowitz.commons.Pixel;
import com.horowitz.commons.Settings;
import com.horowitz.commons.SimilarityImageComparator;

public class QuizProcessor {

  private final static Logger LOGGER = Logger.getLogger("MAIN");

  public static void main(String[] args) {
    Settings settings = Settings.createSettings("tm.properties");

    ScreenScanner scanner = new ScreenScanner(settings);
    scanner.getMatcher().setSimilarityThreshold(.90d);
    try {
      long start = System.currentTimeMillis();
      QuizProcessor quizProcessor = new QuizProcessor(scanner, settings);
      quizProcessor.setTournamentMode(true);
      //quizProcessor.processFolder("C:/backup/quizT1");
      // quizProcessor.processOutputFolder("C:/backup/quizT1/output", true);

      quizProcessor.checkDBHealth();
      // quizProcessor.processSourceFolder();

      // quizMaster.processOutputFolder("C:/backup/OUTPUT", true);
      // quizMaster.processOutputFolder("C:/backup/OUTPUT2", true);
      // quizMaster.processOutputFolder("C:/backup/OUTPUT2/output", false);

      // quizProcessor.testSearch("C:/backup/OUTPUT2/output",
      // "C:/backup/NODUP/Q-20171225-165845-858.png");

      // quizProcessor.testSearch("C:/backup/OUTPUT2/output",
      // "C:/backup/NODUP/Q-20171225-163936-964.png");

      // Q-20171225-163936-964

      // quizMaster.processOutputFolder("C:/backup/NODUP", false);
      // Q-20171225-165937-338.png

      long end = System.currentTimeMillis();
      System.err.println("TIME: " + DateUtils.fancyTime2(end - start));
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private boolean tournamentMode = false;
  private Rectangle aArea;
  private Rectangle aDisplayedArea;

  private SimilarityImageComparator comparator;

  private boolean done;

  private MouseRobot mouse;

  private Rectangle qaArea;

  private Rectangle qArea;

  private Rectangle qDisplayedArea;
  private List<Question> questions;

  private ScreenScanner scanner;

  private Settings settings;
  private ImageData topLeftCorner;

  public QuizProcessor(ScreenScanner scanner, Settings settings) throws IOException {
    super();
    this.scanner = scanner;
    this.comparator = new SimilarityImageComparator(0.1, QuizParams.COMPARATOR_PRECISION);
    comparator.setErrors(6);
    this.settings = settings;
    this.mouse = scanner.getMouse();
    questions = new ArrayList<>(0);
    scanner.getImageData(QuizParams.TOP_LEFT_CORNER).setColorToBypass(Color.red);
    scanner.getImageData(QuizParams.TOP_LEFT_CORNER_TOUR).setColorToBypass(Color.red);
    resetAreas();
  }
  
  public void checkDBHealth() {
    try {
      loadQuestions();
      
      for (Question q : questions) {
        if (q.correctAnswer <= 0) {
          System.err.println(q);
        }
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private Question buildQuestion(final File file) throws IOException {
    final String id = file.getName().split("\\.")[0];
    Question q = new Question();
    q.qFilename = file.getName();
    q.qImage = ImageIO.read(file);
    // System.err.println(id);
    File[] answers = file.getParentFile().listFiles(new FileFilter() {
      @Override
      public boolean accept(File f) {
        return f.getName().startsWith(id) && !f.getName().equals(file.getName());
      }
    });
    for (int i = 0; i < answers.length; i++) {
      // System.err.print(answers[i].getName() + "  ");
      if (answers[i].getName().indexOf("CORRECT") >= 0)
        q.correctAnswer = i + 1;
      BufferedImage image = ImageIO.read(answers[i]);
      q.aImages[i] = image;
      q.aFilenames[i] = answers[i].getName();
      // it works but necessary right now //String ss =
      // answers[i].getName().substring(id.length()+2, id.length() + 3);
    }
    // System.err.println(q);
    return q;
  }

  private Pixel checkQuestion(BufferedImage qaImage, Question q) throws IOException, AWTException {
    BufferedImage aImage = getSubimage(qaImage, aArea, true);
    BufferedImage corner = scanner.getImageData(
        tournamentMode ? QuizParams.TOP_LEFT_CORNER_TOUR : QuizParams.TOP_LEFT_CORNER).getImage();// topLeftCorner2T.png
    Pixel c1 = scanner.findOneFast(corner, aImage, null, false, false);

    if (c1 != null && c1.y < 20) {
      aImage = QuizParams.toBW(aImage);
      // scanner.writeImageTS(aImage, "aimage now.png");
      int xOff = 13;
      int yOff = 10;
      BufferedImage correctImage = q.getCorrectImage();
      // scanner.writeImageTS(correctImage, "correct.png");
      if (correctImage != null) {
        BufferedImage[] aImages = new BufferedImage[4];
        {
          Rectangle area = new Rectangle(c1.x + xOff, c1.y + yOff, 257 - 2 * xOff, 67 - 2 * yOff);
          aImages[0] = getSubimage(aImage, area, false);
          if (compare(aImages[0], correctImage)) {
            LOGGER.info("111111");
            return new Pixel(c1.x + aArea.x + 208, c1.y + aArea.y + 39);
          }
        }
        {
          Rectangle area = new Rectangle(c1.x + 270 + xOff, c1.y + yOff, 257 - 2 * xOff, 67 - 2 * yOff);
          aImages[1] = getSubimage(aImage, area, false);
          if (compare(aImages[1], correctImage)) {
            LOGGER.info("222222");
            return new Pixel(c1.x + aArea.x + 39 + 270, c1.y + aArea.y + 39);
          }
        }
        {
          Rectangle area = new Rectangle(c1.x + xOff, c1.y + 80 + yOff, 257 - 2 * xOff, 67 - 2 * yOff);
          aImages[2] = getSubimage(aImage, area, false);
          if (compare(aImages[2], correctImage)) {
            LOGGER.info("333333");
            return new Pixel(c1.x + aArea.x + 208, c1.y + aArea.y + 39 + 80);
          }
        }
        {
          Rectangle area = new Rectangle(c1.x + 270 + xOff, c1.y + 80 + yOff, 257 - 2 * xOff, 67 - 2 * yOff);
          aImages[3] = getSubimage(aImage, area, false);
          if (compare(aImages[3], correctImage)) {
            LOGGER.info("444444");
            return new Pixel(c1.x + aArea.x + 39 + 270, c1.y + aArea.y + 39 + 80);
          }
        }
        // LOGGER.info("???");
      }

    }

    return null;

  }

  private boolean compare(BufferedImage image1, BufferedImage image2) {

    return comparator.compareNew(image1, image2);
  }

  private boolean compare(Question q1, Question q2) {
    boolean qsame = comparator.compareNew(q1.qImage, q2.qImage);
    boolean asame = true;
    if (qsame) {
      for (int i = 0; i < 4; i++) {
        asame = asame && comparator.compareNew(q1.aImages[i], q2.aImages[i]);
      }
    }
    if (qsame && asame) {
      if (q1.correctAnswer == 0)
        q1.correctAnswer = q2.correctAnswer;
    }
    return qsame && asame;
  }

  private void deleteTree(File folder) {
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

  private List<Question> eliminateDuplicates(List<Question> questions) {
    List<Question> clean = new ArrayList<>(300);

    for (int i = 0; i < questions.size(); i++) {
      Question q1 = questions.get(i);
      for (int j = i + 1; j < questions.size(); j++) {
        Question q2 = questions.get(j);

        if (q1.dup || q2.dup)
          continue;
        if (compare(q1, q2)) {
          q2.dup = true;
        }

      }
      if (!q1.dup)
        clean.add(q1);
      System.err.print(".");
      if ((i + 1) % 100 == 0)
        System.err.println();
    }
    return clean;
  }

  public Pixel findAnswer(BufferedImage qaImage, List<Question> possibleQuestions) throws IOException, AWTException {
    for (Question q : possibleQuestions) {
      Pixel p = checkQuestion(qaImage, q);
      if (p != null)
        return p;
    }
    return null;
  }

  private Map<String, List<Question>> findSimilarQuestions(List<Question> questions) {
    Map<String, List<Question>> map = new HashMap<String, List<Question>>();
    for (int i = 0; i < questions.size(); i++) {
      Question q1 = questions.get(i);

      List<Question> list;
      list = new ArrayList<>(4);
      list.add(q1);
      for (int j = 0; j < questions.size(); j++) {
        Question q2 = questions.get(j);
        if (i != j && !q2.dup) {
          ;
          boolean qsame = comparator.compareNew(q1.qImage, q2.qImage);
          if (qsame) {
            list.add(q2);
            q2.dup = true;
          }
        }
      }
      if (list.size() > 1) {
        map.put(q1.qFilename, list);
      }

    }
    return map;
  }

  public List<Question> getPossibleQuestions(BufferedImage qaImage) {

    long start = System.currentTimeMillis();
    BufferedImage qImage = qaImage.getSubimage(qArea.x, qArea.y, qArea.width, qArea.height);
    qImage = QuizParams.toBW(qImage);
    List<Question> possibleQuestions = new ArrayList<>(5);
    for (Question q : questions) {
      // scanner.writeImageTS(q.qImage, "q .png");
      if (comparator.compareNew(q.qImage, qImage)) {
        System.err.println("FOUND: " + q);
        possibleQuestions.add(q);
      }
    }
    // scanner.writeImageTS(qImage, "qimage.png");
    long end = System.currentTimeMillis();
    System.err.println("time: " + (end - start));
    return possibleQuestions;
  }

  public List<Question> getQuestions() {
    return questions;
  }

  private BufferedImage getSubimage(BufferedImage image, Rectangle area, boolean remaining) {
    int w = remaining ? image.getWidth() - area.x : area.width;
    int h = remaining ? image.getHeight() - area.y : area.height;
    BufferedImage res;
    try {
      res = image.getSubimage(area.x, area.y, w, h);
      return res;
    } catch (Exception e) {
      if (!remaining)
        return getSubimage(image, area, true);
    }
    return image;
  }

  public int loadQuestions() throws IOException {
    this.questions = loadQuestions(QuizParams.DB_DESTINATION);
    return questions.size();
  }

  private List<Question> loadQuestions(String path) throws IOException {
    List<Question> res = null;
    File dir = new File(path);
    File outputDir = new File(path + "/output");
    if (!outputDir.exists())
      outputDir.mkdir();
    else {
      for (File f : outputDir.listFiles()) {
        f.delete();
      }
    }
    if (dir.exists() && dir.isDirectory()) {
      System.out.println("processing " + path);
      File[] files = dir.listFiles(new FileFilter() {

        @Override
        public boolean accept(File f) {
          return f.isFile() && f.getName().indexOf("-A") < 0;
        }
      });
      // File[] files2 = Arrays.copyOf(files, files.length);
      System.err.println("Processing " + files.length + " files...");
      res = new ArrayList<>(1500);
      for (int i = 0; i < files.length; i++) {
        // Q-20171225-163924-588
        res.add(buildQuestion(files[i]));
        System.err.print(".");
        if ((i + 1) % 100 == 0)
          System.err.println();
      }
      System.err.println();
    }
    return res;
  }

  private Question processFile1(BufferedImage image) throws AWTException, IOException {
    BufferedImage aImage = getSubimage(image, aArea, true);
    boolean found = false;
    BufferedImage corner = scanner.getImageData(
        tournamentMode ? QuizParams.TOP_LEFT_CORNER_TOUR : QuizParams.TOP_LEFT_CORNER).getImage();
    Pixel c1 = scanner.findOneFast(corner, aImage, null, false, false);
    Pixel c2 = null;
    Pixel c3 = null;
    Pixel c4 = null;

    if (c1 != null && c1.y < 20) {
      Rectangle a2Area = new Rectangle(c1.x + 270, c1.y, 30, 50);
      BufferedImage a2Image = getSubimage(aImage, a2Area, false);

      c2 = scanner.findOneFast(corner, a2Image, null, false, false);
      if (c2 != null) {
        Rectangle a4Area = new Rectangle(c1.x + 270, c1.y + 80, 30, 50);
        BufferedImage a4Image = getSubimage(aImage, a4Area, false);
        c4 = scanner.findOneFast(corner, a4Image, null, false, false);
        if (c4 != null) {
          Rectangle a3Area = new Rectangle(c1.x, c1.y + 80, 30, 50);
          BufferedImage a3Image = getSubimage(aImage, a3Area, false);
          c3 = scanner.findOneFast(corner, a3Image, null, false, false);
          if (c3 != null) {
            found = true;

          }
        }
      }

      System.err.println(c1 + " " + c2 + " " + c3 + " " + c4);
      if (found) {
        // ok this is the best picture with q and a
        BufferedImage qImage = image.getSubimage(qArea.x, qArea.y, qArea.width, qArea.height);
        String timestamp = QuizParams.SIMPLE_DATE_FORMAT.format(new Date());
        // String newFilename = outputDir.getAbsolutePath() + "/Q-" + timestamp
        // + ".bmp";
        Question q = new Question();
        q.qFilename = "Q-" + timestamp;
        q.qImage = qImage;
        // System.err.println(newFilename);
        // MyImageIO.writeImage(qImage, newFilename);

        // answers
        // pill:257x67 13,10
        int xOff = 13;
        int yOff = 10;
        // A1
        {
          Rectangle area = new Rectangle(c1.x + xOff, c1.y + yOff, 257 - 2 * xOff, 67 - 2 * yOff);
          q.aFilenames[0] = "Q-" + timestamp + "-A1";
          q.aImages[0] = getSubimage(aImage, area, false);
          // MyImageIO.writeImage(a1Image, newFilename);
        }
        {
          Rectangle area = new Rectangle(c1.x + 270 + xOff, c1.y + yOff, 257 - 2 * xOff, 67 - 2 * yOff);
          q.aFilenames[1] = "Q-" + timestamp + "-A2";
          q.aImages[1] = getSubimage(aImage, area, false);
        }
        {
          Rectangle area = new Rectangle(c1.x + xOff, c1.y + 80 + yOff, 257 - 2 * xOff, 67 - 2 * yOff);
          q.aFilenames[2] = "Q-" + timestamp + "-A3";
          q.aImages[2] = getSubimage(aImage, area, false);
        }
        {
          Rectangle area = new Rectangle(c1.x + 270 + xOff, c1.y + 80 + yOff, 257 - 2 * xOff, 67 - 2 * yOff);
          q.aFilenames[3] = "Q-" + timestamp + "-A4";
          q.aImages[3] = getSubimage(aImage, area, false);
        }
        return q;
      }// end of found

    }
    return null;
  }

  private void processFile2(BufferedImage image, Question lastQuestion) throws AWTException, IOException {
    BufferedImage aImage = getSubimage(image, aArea, true);
    boolean found = false;
    // BufferedImage corner =
    // scanner.getImageData("topLeftCorner2T.png").getImage();
    BufferedImage correct = scanner.getImageData("correctAnswer.bmp").getImage();
    Pixel c = scanner.findOneFast(correct, aImage, null, false, false);
    if (c != null) {
      System.err.println("correct: " + c);
      // where
      int aa = 0;
      if (c.x < 15) {
        if (c.y < 50) {
          aa = 1;
        } else {
          aa = 3;
        }
      } else {
        if (c.y < 50) {
          aa = 2;
        } else
          aa = 4;
      }
      System.err.println("CORRECT ANSWER IS: " + aa);
      lastQuestion.correctAnswer = aa;
    }
  }

  public void processSourceFolder() {
    int cnt = 0;
    try {
      processFolder(QuizParams.SOURCE_FOLDER);

      try {
        cnt = processOutputFolder(QuizParams.SOURCE_FOLDER + "/output", true);

        copyFilesToDB();

        deleteSourceFolder();
        
        LOGGER.info(cnt + " qurestions successfully added to DB!");
      } catch (Exception e) {
        LOGGER.info("Failed to process quiz/output folder");
        e.printStackTrace();
      }

    } catch (IOException | AWTException e) {
      LOGGER.info("Failed to process quiz folder");
      e.printStackTrace();
    }
  }

  private void deleteSourceFolder() {
    File srcDir = new File(QuizParams.SOURCE_FOLDER);
    deleteTree(srcDir);
    if (!srcDir.exists()) {
      srcDir.mkdir();
    }
  }

  private void copyFilesToDB() {
    try {
      File srcDir = new File(QuizParams.SOURCE_FOLDER + "/output/output");
      File destDir = new File(QuizParams.DB_DESTINATION);
      FileUtils.copyDirectory(srcDir, destDir);
    } catch (IOException e) {
      LOGGER.info("Failed to copy files from quiz to db");
      e.printStackTrace();
    }
  }

  private void processFolder(String path) throws IOException, AWTException {
    File dir = new File(path);
    File outputDir = new File(path + "/output");
    if (!outputDir.exists())
      outputDir.mkdir();
    else {
      for (File f : outputDir.listFiles()) {
        f.delete();
      }
    }
    if (dir.exists() && dir.isDirectory()) {
      System.out.println("processing " + path);
      File[] files = dir.listFiles();
      int cnt = 0;
      int cntQ = 0;
      Question lastQuestion = null;
      for (File f : files) {
        if (f.isFile() && (f.getName().endsWith(".bmp") || f.getName().endsWith(".png"))) {
          cnt++;
          System.out.println(f);
          BufferedImage image = ImageIO.read(f);
          if (lastQuestion == null) {
            Question q = processFile1(image);

            if (q != null) {
              lastQuestion = q;
              cntQ++;
            }
          } else {
            processFile2(image, lastQuestion);
            // if (lastQuestion == null ||
            // lastQuestion.qFilename.equals(q.qFilename))
            // question found
            lastQuestion.writeImages(outputDir, "png");
            lastQuestion = null;
          }
        }
      }
      System.out.println("" + cnt + " files");
      System.out.println("" + cntQ + " questions found");
    } else {
      System.err.println(path + " does not exist or not a directory");
    }
    System.out.println("done");
  }

  private int processOutputFolder(String path, boolean checkAnswers) throws IOException, AWTException {
    File outputDir = new File(path + "/output");
    deleteTree(outputDir);
    if (!outputDir.exists())
      outputDir.mkdir();

    List<Question> questions = loadQuestions(path);
    thresholdImages(questions);
    System.err.println("Loaded " + questions.size() + " questions.");

    if (checkAnswers) {
      System.err.println("Looking for duplicates...");
      List<Question> noDuplicates = eliminateDuplicates(questions);
      System.err.println("\nQuestions without duplicates: " + noDuplicates.size());
      System.err.println("saving...");
      for (Question q : noDuplicates) {
        q.writeImages2(outputDir);
      }
      System.err.println();
      System.err.println("done");
      return noDuplicates.size();
    } else {
      System.err.println("Looking for similar questions and different set of answers...");
      Map<String, List<Question>> map = findSimilarQuestions(questions);
      saveMap(map, outputDir);
      return -1;
    }
  }

  private void resetAreas() throws IOException {
    qArea = new Rectangle(115, 6, 396, 74);
    aArea = new Rectangle(0, 127, 527, 160);
    
  }

  private void saveMap(Map<String, List<Question>> map, File outputDir) {
    for (String key : map.keySet()) {
      String timestamp = QuizParams.SIMPLE_DATE_FORMAT.format(new Date());
      File subDir = new File(outputDir.getAbsolutePath() + "/similar-" + timestamp);
      subDir.mkdir();
      List<Question> qs = map.get(key);
      for (Question q : qs) {
        q.writeImages2(subDir);
      }
    }

  }

  public void testSearch(String path, String question) throws IOException {
    if (questions == null)
      questions = loadQuestions(path);
    // thresholdImages(questions);
    long start = System.currentTimeMillis();
    BufferedImage qImage = ImageIO.read(new File(question));
    qImage = QuizParams.toBW(qImage);
    for (Question q : questions) {
      if (comparator.compareNew(q.qImage, qImage)) {
        System.err.println("FOUND: " + q);
      }
    }
    long end = System.currentTimeMillis();
    System.err.println("time: " + (end - start));
  }

  private void thresholdImages(List<Question> questions) {
    for (Question q : questions) {
      q.qImage = QuizParams.toBW(q.qImage);
      for (int i = 0; i < 4; i++) {
        q.aImages[i] = QuizParams.toBW(q.aImages[i]);
      }
    }
  }

  public boolean isTournamentMode() {
    return tournamentMode;
  }

  public void setTournamentMode(boolean tournamentMode) {
    this.tournamentMode = tournamentMode;
  }

}

// Rectangle area = new Rectangle(c1.x, c1.y, 257, 67);
// Rectangle area = new Rectangle(c1.x + 270, c1.y, 257, 67);
// Rectangle area = new Rectangle(c1.x, c1.y + 80, 257, 67);
// Rectangle area = new Rectangle(c1.x + 270, c1.y + 80, 257, 67);
