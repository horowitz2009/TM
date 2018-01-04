package com.horowitz.tm;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import com.horowitz.commons.CrazyImageComparator;
import com.horowitz.commons.DateUtils;
import com.horowitz.commons.ImageData;
import com.horowitz.commons.MouseRobot;
import com.horowitz.commons.Pixel;
import com.horowitz.commons.RobotInterruptedException;
import com.horowitz.commons.Settings;
import com.horowitz.commons.TemplateMatcher;

public class QuizProcessor {

  private final static Logger LOGGER = Logger.getLogger("MAIN");

  private boolean tournamentMode = false;
  private Rectangle aArea;
  private Rectangle aDisplayedArea;

  // private CrazyImageComparator comparator;
  private TemplateMatcher matcher;

  private boolean done;

  private MouseRobot mouse;

  private Rectangle qaArea;

  private Rectangle qArea;

  private Rectangle qDisplayedArea;
  private List<Question> questions;

  private ScreenScanner scanner;
  private ScreenScanner scannerInt;
  private ScreenScanner scannerA;

  private Settings settings;
  private ImageData topLeftCorner;

  public QuizProcessor(ScreenScanner scannerP, Settings settings) throws IOException {
    super();
    scanner = scannerP;
    
    scannerInt = new ScreenScanner(settings);
    scannerInt.comparator.setPrecision(QuizParams.COMPARATOR_PRECISION_INT);
    scannerInt.comparator.setMaxBWErrors(12);

    scannerA = new ScreenScanner(settings);
    scannerA.comparator.setPrecision(QuizParams.COMPARATOR_PRECISION_INT);
    scannerA.comparator.setThreshold(0.85);
    // this.scannerA.comparator.setBW(true);

    matcher = new TemplateMatcher();

    this.settings = settings;
    this.mouse = scanner.getMouse();
    questions = new ArrayList<>(0);
    
    scanner.getImageData(QuizParams.TOP_LEFT_CORNER).setColorToBypass(Color.red);
    scanner.getImageData(QuizParams.TOP_LEFT_CORNER_TOUR).setColorToBypass(Color.red);
    
    scannerA.getImageData(QuizParams.TOP_LEFT_CORNER).setColorToBypass(Color.red);
    scannerA.getImageData(QuizParams.TOP_LEFT_CORNER_TOUR).setColorToBypass(Color.red);
    resetAreas();
  }

  public void testMatcher(String f1, String f2) {
    try {
      TemplateMatcher matcher = new TemplateMatcher();
      matcher.setSimilarityThreshold(.95d);
      BufferedImage image1 = scanner.getImageData(f1).getImage();
      // image1 = QuizParams.toBW(image1);
      BufferedImage image2 = scanner.getImageData(f2).getImage();

      // 1
      long start = System.currentTimeMillis();
      Pixel p = matcher.findMatch(image1, image2, null);
      long end = System.currentTimeMillis();
      System.out.println("matcher: " + p + "   " + (end - start));

      // 2
      start = System.currentTimeMillis();
      p = matcher.findMatchQ(image1, image2, null);
      end = System.currentTimeMillis();
      System.out.println(p + "   " + (end - start));

      // 3
      start = System.currentTimeMillis();
      p = matcher.findMatchQBW(image1, image2);
      end = System.currentTimeMillis();
      System.out.println("matcher bw: " + p + "   " + (end - start));

      // 4
      scannerA.comparator.setPrecision(30);
      start = System.currentTimeMillis();
      p = scannerA.comparator.findImage(image1, image2);
      end = System.currentTimeMillis();
      System.out.println("4 comp: " + p + "   " + (end - start));

      // 5
      CrazyImageComparator crazy = new CrazyImageComparator();
      crazy.setPrecision(45);
      crazy.setThreshold(0.9995);
      start = System.currentTimeMillis();
      p = crazy.findImage(image1, image2);
      end = System.currentTimeMillis();
      System.out.println("5 comp: " + p + "   " + (end - start));
      // 6
      start = System.currentTimeMillis();
      p = crazy.findImageQ(image1, image2);
      end = System.currentTimeMillis();
      System.out.println("6 comp q: " + p + "   " + (end - start));

      // 7
      image1 = QuizParams.toBW(image1);
      image2 = QuizParams.toBW(image2);
      start = System.currentTimeMillis();
      p = crazy.findImage(image1, image2);
      end = System.currentTimeMillis();
      System.out.println("7 comp bw: " + p + "   " + (end - start));

      // 8
      crazy.setPrecision(45);
      crazy.setThreshold(0.9995);
      crazy.setBW(true);
      start = System.currentTimeMillis();
      p = crazy.findImage(image1, image2);
      end = System.currentTimeMillis();
      System.out.println("8 comp: " + p + "   " + (end - start));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void findQuestionByQuestion(String questionFilename) {
    // comparator.setPrecision(500);
    try {
      BufferedImage aa = scanner.getImageData(questionFilename).getImage();
      // aa = QuizParams.toBW(aa);
      if (this.questions.isEmpty())
        loadQuestions();
      int k = 0;
      for (Question q : questions) {
        Pixel pq = scanner.comparator.findImage(aa, q.qImage);
        if (pq != null)
          System.err.println(q);
        // System.err.println("" + k++);
        Pixel[] ps = new Pixel[4];
      }
      System.err.println("DONE");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void findQuestionByAnswer(String answerFilename) {
    TemplateMatcher matcher = new TemplateMatcher();
    // comparator.setPrecision(500);
    try {
      BufferedImage aa = scannerA.getImageData(answerFilename).getImage();
      aa = QuizParams.toBW(aa);
      if (this.questions.isEmpty())
        loadQuestions();
      int k = 0;
      for (Question q : questions) {
        // Pixel pq = comparator.findImage(aa, q.qImage);
        // if (pq != null)
        // System.err.println(q);
        // System.err.println("" + k++);
        Pixel[] ps = new Pixel[4];
        for (int j = 0; j < ps.length; j++) {
          // ps[j] = matcher.findMatchQBW(aa, q.aImages[j], null);
          ps[j] = scannerA.comparator.findImage(aa, q.aImages[j]);
          if (ps[j] != null) {
            System.err.println(q);
            System.err.println("A:" + (j + 1));
          }
        }
      }
      System.err.println("DONE");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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

  public void checkDBForDuplicates() {
    try {
      loadQuestions();
      File outputDir = new File(QuizParams.DB_DESTINATION + "/output");
      List<Question> noDuplicates = eliminateDuplicates(questions);

      Collections.sort(noDuplicates, new Comparator<Question>() {
        @Override
        public int compare(Question q1, Question q2) {
          return (int) (q1.lastModified - q2.lastModified);
        }
      });
      for (Question q : noDuplicates) {
        q.writeImages2(outputDir);
      }
    } catch (IOException e) {
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

    q.lastModified = file.lastModified();
    // System.err.println(q);
    return q;
  }

  private Pixel checkQuestion(BufferedImage qaImage, Question q) throws IOException, AWTException,
      RobotInterruptedException {
    BufferedImage aImage = getSubimage(qaImage, aArea, true);

    Pixel c1 = scanner.findImage("topLeftCorner6.bmp", aImage);

    // BufferedImage corner = scanner.getImageData(
    // tournamentMode ? QuizParams.TOP_LEFT_CORNER_TOUR :
    // QuizParams.TOP_LEFT_CORNER).getImage();// topLeftCorner2T.png
    // Pixel c1 = scanner.findOneFast(corner, aImage, null, false, false);

    if (c1 != null) {
      aImage = QuizParams.toBW(aImage);
      // scanner.writeImageTS(aImage, "aimage now.png");
      int xOff = 13 + 3;
      int yOff = 10 + 3;
      boolean debug = false;
      BufferedImage correctImage = q.getCorrectImage();
      if (debug)
        scanner.writeImageTS(correctImage, "correct.png");
      if (correctImage != null) {
        correctImage = QuizParams.toBW(correctImage);
        if (debug)
          scanner.writeImageTS(correctImage, "correctImage.png");
        BufferedImage[] aImages = new BufferedImage[4];
        {
          Rectangle area = new Rectangle(c1.x + xOff, c1.y + yOff, 257 - 2 * xOff, 67 - 2 * yOff);
          aImages[0] = getSubimage(aImage, area, false);
          if (debug)
            scanner.writeImageTS(aImages[0], "aimage 0.png");
          if (scannerA.comparator.findImage(QuizParams.toBW(aImages[0]), correctImage) != null) {
            LOGGER.info("1");
            return new Pixel(c1.x + aArea.x + 208, c1.y + aArea.y + 39);
          }
        }
        {
          Rectangle area = new Rectangle(c1.x + 270 + xOff, c1.y + yOff, 257 - 2 * xOff, 67 - 2 * yOff);
          aImages[1] = getSubimage(aImage, area, false);
          if (debug)
            scanner.writeImageTS(aImages[1], "aimage 1.png");
          if (scannerA.comparator.findImage(QuizParams.toBW(aImages[1]), correctImage) != null) {
            LOGGER.info("2");
            return new Pixel(c1.x + aArea.x + 39 + 270, c1.y + aArea.y + 39);
          }
        }
        {
          Rectangle area = new Rectangle(c1.x + xOff, c1.y + 80 + yOff, 257 - 2 * xOff, 67 - 2 * yOff);
          aImages[2] = getSubimage(aImage, area, false);
          if (debug)
            scanner.writeImageTS(aImages[2], "aimage 2.png");
          if (scannerA.comparator.findImage(QuizParams.toBW(aImages[2]), correctImage) != null) {
            LOGGER.info("3");
            return new Pixel(c1.x + aArea.x + 208, c1.y + aArea.y + 39 + 80);
          }
        }
        {
          Rectangle area = new Rectangle(c1.x + 270 + xOff, c1.y + 80 + yOff, 257 - 2 * xOff, 67 - 2 * yOff);
          aImages[3] = getSubimage(aImage, area, false);
          if (debug)
            scanner.writeImageTS(aImages[3], "aimage 3.png");
          if (scannerA.comparator.findImage(QuizParams.toBW(aImages[3]), correctImage) != null) {
            LOGGER.info("4");
            return new Pixel(c1.x + aArea.x + 39 + 270, c1.y + aArea.y + 39 + 80);
          }
        }
        // LOGGER.info("???");
      }

    }

    return null;

  }

  private boolean compare(BufferedImage image1, BufferedImage image2) {
    // scanner.writeImageTS(image1, "image1.png");
    // scanner.writeImageTS(image2, "image2.png");
    return scannerA.comparator.compare(image1, image2);
    // return scanner.getMatcher().findMatchQ(image1, image2, null) != null;
  }

  private boolean compareAndMerge(Question q1, Question q2, boolean mergeCorrectAnswer, boolean atLeastTwo) {
    boolean qsame = scanner.comparator.compare(q1.qImage, q2.qImage);
    boolean asame = true;
    int sameAnswers = 0;
    if (qsame) {
      for (int i = 0; i < 4; i++) {
        if (q2.aImages[i] != null
            && scannerA.comparator.compare(QuizParams.toBW(q1.aImages[i]), QuizParams.toBW(q2.aImages[i])))
          sameAnswers++;
      }
    }
    if (mergeCorrectAnswer && qsame && asame) {
      if (q1.correctAnswer == 0) {
        q1.correctAnswer = q2.correctAnswer;
      } else if (q2.correctAnswer == 0) {
        q2.correctAnswer = q1.correctAnswer;
      }
    }
    asame = sameAnswers == 4 || (atLeastTwo && sameAnswers > 1);
    return qsame && asame;
  }

  private List<Question> eliminateDuplicates(List<Question> questions) {
    List<Question> clean = new ArrayList<>(550);

    for (int i = 0; i < questions.size(); i++) {
      Question q1 = questions.get(i);
      for (int j = 0; j < questions.size(); j++) {
        if (i != j) {
          Question q2 = questions.get(j);

          if (q1.dup || q2.dup)
            continue;
          if (compareAndMerge(q1, q2, true, false)) {
            // ok we have dups, but which one is better
            if (q2.lastModified > q1.lastModified) {
              // q2 is newer
              q1.dup = true;
              break;
            } else {
              q2.dup = true;
            }
          }
        }
      }// for j
       // if (!q1.dup)
       // clean.add(q1);

      System.err.print(".");
      if ((i + 1) % 100 == 0)
        System.err.println();
    }// for i
    for (Question q : questions) {
      if (!q.dup)
        clean.add(q);
    }
    return clean;
  }

  private List<Question> getOnlyNewQuestions(List<Question> newQuestions) {
    List<Question> clean = new ArrayList<>(newQuestions.size());

    for (int i = 0; i < newQuestions.size(); i++) {
      Question qNew = newQuestions.get(i);
      boolean foundInDB = false;
      for (int j = 0; j < questions.size(); j++) {
        Question q2 = questions.get(j);

        if (compareAndMerge(q2, qNew, true, false)) {
          foundInDB = true;
          // qNew.dup = true;
          break;
        }

      }
      if (!foundInDB)
        clean.add(qNew);
      System.err.print(".");
      if ((i + 1) % 100 == 0)
        System.err.println();
    }
    return clean;
  }

  public Pixel findAnswer(BufferedImage qaImage, List<Question> possibleQuestions) throws IOException, AWTException,
      RobotInterruptedException {
    int answer = -1;
    boolean different = true;
    for (Question q : possibleQuestions) {
      if (q.correctAnswer > 0) {
        if (answer == -1) {
          answer = q.correctAnswer;
          different = false;
        } else {
          if (answer != q.correctAnswer)
            different = true;
        }
      }
      Pixel p = checkQuestion(qaImage, q);
      if (p != null)
        return p;
    }

    // I'm here because the scanning of answers didn't work
    if (!different && answer > 0) {
      System.err.println("PLAN B " + answer);
      Pixel p = null;
      switch (answer) {
      case 1:
        p = new Pixel(aArea.x + 208, aArea.y + 54);
        break;
      case 2:
        p = new Pixel(aArea.x + 39 + 270, aArea.y + 54);
        break;
      case 3:
        p = new Pixel(aArea.x + 208, aArea.y + 54 + 80);
        break;
      case 4:
        p = new Pixel(aArea.x + 39 + 270, aArea.y + 54 + 80);
        break;
      }

      return p;
    } else {
      System.out.println("DAMNnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn");
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
          boolean qsame = scanner.comparator.compare(q1.qImage, q2.qImage);
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
    // comparator.setPrecision(60);
    for (Question q : questions) {
      // scanner.writeImageTS(QuizParams.toBW(q.qImage), "q1 .png");
      // scanner.writeImageTS(qImage, "q2 .png");
      if (scanner.comparator.compare(QuizParams.toBW(q.qImage), qImage)) {
        System.err.println("FOUND: " + q);
        possibleQuestions.add(q);
      }
    }
    // scanner.writeImageTS(qImage, "qimage.png");
    long end = System.currentTimeMillis();
    // comparator.setPrecision(45);
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
    dir.mkdirs();
    if (dir.exists() && dir.isDirectory()) {
      System.out.println("loading questions from " + path);
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

  public void processSourceFolder(String folder) {
    int cnt = 0;
    try {
      processFolder(folder, false);

      try {
        cnt = processOutputFolder(folder + "/output", QuizParams.DB_DESTINATION, true);

        // copyFilesToDB();

        // deleteSourceFolder();

        LOGGER.info(cnt + " questions successfully added to DB!");
      } catch (Exception e) {
        LOGGER.info("Failed to process quiz/output folder");
        e.printStackTrace();
      }

    } catch (Exception e) {
      LOGGER.info("Failed to process quiz folder");
      e.printStackTrace();
    }
  }

  private void deleteSourceFolder() {
    File srcDir = new File(QuizParams.SOURCE_FOLDER);
    QuizParams.deleteTree(srcDir);
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

  private int processOutputFolder(String path, String outputFolder, boolean checkAnswers) throws IOException,
      AWTException {
    File outputDir = new File(outputFolder);
    // QuizParams.deleteTree(outputDir);
    if (!outputDir.exists())
      outputDir.mkdir();
    if (questions.isEmpty())
      loadQuestions();

    List<Question> newQuestions = loadQuestions(path);
    // thresholdImages(questions);
    System.err.println("Loaded " + newQuestions.size() + " new questions.");

    if (checkAnswers) {
      System.err.println("Looking for duplicates...");
      LOGGER.info("Looking for duplicates...");
      List<Question> noDuplicates = getOnlyNewQuestions(newQuestions);
      System.err.println("\nQuestions new to DB: " + noDuplicates.size());
      System.err.println("saving...");
      for (Question q : noDuplicates) {
        q.writeImages2(outputDir);
        questions.add(q);
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
    aArea = new Rectangle(0, 140, 527, 147);

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
      if (scanner.comparator.compare(q.qImage, qImage)) {
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

  private void processFolder(String path, boolean deleteOutput) throws IOException, AWTException,
      RobotInterruptedException {
    File dir = new File(path);
    File outputDir = new File(path + "/output");
    if (!outputDir.exists()) {
      outputDir.mkdir();
    } else {
      if (deleteOutput)
        for (File f : outputDir.listFiles()) {
          f.delete();
        }
    }
    if (dir.exists() && dir.isDirectory()) {
      System.out.println("processing " + path);
      File[] files = dir.listFiles();
      int cnt = 0;
      int cntQnoA = 0;
      List<Question> questions = new ArrayList<>();
      for (File f : files) {
        if (f.isFile() && (f.getName().endsWith(".bmp") || f.getName().endsWith(".png"))) {
          cnt++;
          // System.out.println(f);
          BufferedImage image = ImageIO.read(f);
          Question q = processFile1(image, true);

          if (q != null) {
            // good question
            boolean dup = false;
            for (Question question : questions) {
              if (compareAndMerge(question, q, false, false)) {
                dup = true;
                break;
              }
            }
            if (!dup) {
              q.originalFile = f.getName();
              questions.add(q);
            }
          } else {
            processFile2(image, questions);
          }
          // any not good files will be ignored
        }
      }
      for (Question question : questions) {
        question.writeImages(outputDir, "png");
        if (question.correctAnswer == 0) {
          cntQnoA++;
          System.err.print(question.originalFile + " --> ");
          System.err.println(question);
        }
      }
      System.out.println("" + cnt + " files");
      System.out.println("" + questions.size() + " questions found");
      System.out.println("" + cntQnoA + " questions without answer");
      LOGGER.info("Processed " + cnt + " files.");
      LOGGER.info("" + questions.size() + " questions found");
      LOGGER.info("" + cntQnoA + " questions without answer");

    } else {
      System.err.println(path + " does not exist or not a directory");
    }
    System.out.println("done");
  }

  private Question processFile1(BufferedImage image, boolean stopOnFailure) throws AWTException, IOException,
      RobotInterruptedException {
    Question q = null;
    ImageData id = scanner.getImageData(tournamentMode ? QuizParams.QUESTION_DISPLAYED_TOUR
        : QuizParams.QUESTION_DISPLAYED);

    Pixel pq = scanner.comparator.findImage(id.getImage(), image.getSubimage(0, 0, 61, 60), id.getColorToBypass());
    if (pq != null) {
      // question visible
      q = new Question();
      String timestamp = QuizParams.SIMPLE_DATE_FORMAT.format(new Date());
      BufferedImage qImage = image.getSubimage(qArea.x, qArea.y, qArea.width, qArea.height);
      q.qFilename = "Q-" + timestamp;
      q.qImage = qImage;
      BufferedImage aImage = getSubimage(image, aArea, true);

      int[] xoff = new int[] { 0, 270, 0, 270 };
      int[] yoff = new int[] { 0, 0, 80, 80 };
      int xPad = 13;
      int yPad = 10;
      for (int i = 0; i < 4; i++) {
        ImageData a = scannerInt.getImageData("clearAnswer" + (i + 1) + ".png");
        BufferedImage aiImage = aImage.getSubimage(xoff[i], yoff[i], 20, 20);
        Pixel c = scannerInt.comparator.findImage(a.getImage(), aiImage, Color.red);
        if (c != null) {
          Rectangle area = new Rectangle(xoff[i] + xPad, yoff[i] + yPad, 257 - 2 * xPad, 67 - 2 * yPad);
          q.aFilenames[i] = "Q-" + timestamp + "-A" + (i + 1);
          q.aImages[i] = getSubimage(aImage, area, false);
          // MyImageIO.writeImage(a1Image, newFilename);
        } else {
          if (stopOnFailure) {
            return null;
          }
        }
      }// for

    }// good q

    return q;
  }

  private boolean processFile2(BufferedImage image, List<Question> questions) throws AWTException, IOException,
      RobotInterruptedException {
    // BufferedImage aImage = getSubimage(image, aArea, true);
    boolean found = false;

    ImageData id = scanner.getImageData(tournamentMode ? QuizParams.QUESTION_DISPLAYED_TOUR
        : QuizParams.QUESTION_DISPLAYED);
    Pixel pq = scanner.comparator.findImage(id.getImage(), image.getSubimage(0, 0, 61, 60), id.getColorToBypass());
    if (pq != null) {
      // question visible
      // find it in list
      Question theQuestion = null;
      BufferedImage qImage = image.getSubimage(qArea.x, qArea.y, qArea.width, qArea.height);
      // Question qq = processFile1(image, false);
      for (int i = questions.size() - 1; i >= 0; --i) {
        Question q = questions.get(i);
        // if (qq != null) {
        // //full compare
        // if (compare(q, qq, false, true)) {
        // theQuestion = q;
        // break;
        // }
        // } else {
        // plan B
        if (scanner.comparator.compare(q.qImage, qImage)) {
          theQuestion = q;
          break;
        }
        // }
      }
      if (theQuestion != null && theQuestion.correctAnswer == 0) {
        int aa = findGreenAnswer(image);

        if (aa > 0) {
          System.err.println("CORRECT ANSWER IS: " + aa);
          theQuestion.correctAnswer = aa;
          found = true;
        } else {
          System.err.println("NO CORRECT ANSWER FOUND!!!");
        }
      }
    }
    return found;
  }

  public int findGreenAnswer(BufferedImage image) throws IOException {
    int aa = 0;
    BufferedImage aImage = getSubimage(image, aArea, true);
    ImageData a1 = scanner.getImageData(tournamentMode ? "correctAnswer1T.png" : "correctAnswer1.png");
    BufferedImage a1Image = aImage.getSubimage(0, 0, 20, 20);
    Pixel c1 = scannerA.comparator.findImage(a1.getImage(), a1Image, Color.red);
    if (c1 != null) {
      aa = 1;
    } else {
      ImageData a2 = scanner.getImageData(tournamentMode ? "correctAnswer2T.png" : "correctAnswer2.png");
      BufferedImage a2Image = aImage.getSubimage(270, 0, 20, 20);
      Pixel c2 = scannerA.comparator.findImage(a2.getImage(), a2Image, Color.red);
      if (c2 != null) {
        aa = 2;
      } else {
        ImageData a3 = scanner.getImageData(tournamentMode ? "correctAnswer3T.png" : "correctAnswer3.png");
        BufferedImage a3Image = aImage.getSubimage(0, 80, 20, 20);
        Pixel c3 = scannerA.comparator.findImage(a3.getImage(), a3Image, Color.red);
        if (c3 != null) {
          aa = 3;
        } else {
          ImageData a4 = scanner.getImageData(tournamentMode ? "correctAnswer4T.png" : "correctAnswer4.png");
          BufferedImage a4Image = aImage.getSubimage(270, 80, 20, 20);
          Pixel c4 = scannerA.comparator.findImage(a4.getImage(), a4Image, Color.red);
          if (c4 != null) {
            aa = 4;
          }
        }
      }
    }
    return aa;
  }

  public static void main(String[] args) {
    Settings settings = Settings.createSettings("tm.properties");

    ScreenScanner scanner = new ScreenScanner(settings);
    scanner.getMatcher().setSimilarityThreshold(.99d);
    try {
      long start = System.currentTimeMillis();
      QuizProcessor quizProcessor = new QuizProcessor(scanner, settings);

      // C:\BACKUP\DBQUIZ\raw 20180101-101135-137\output

      // quizProcessor.findQuestionByQuestion("C:/BACKUP/DBQUIZ/raw 20180101-191104-564/output/Q-20180101-191106-653.png");
      // quizProcessor.testMatcher("clearAnswer1.png", "topLeftCorner.png");
       quizProcessor.testMatcher(
       "C:\\BACKUP\\DBQUIZ\\raw 20180101-101135-137\\output\\hmm\\Q-20171231-011633-718-A1-CORRECT.png",
       "C:\\BACKUP\\DBQUIZ\\raw 20180101-101135-137\\output\\hmm\\Q-20180101-101137-371-A2.png");
       quizProcessor.testMatcher(
           "C:\\BACKUP\\DBQUIZ\\raw 20180101-101135-137\\output\\hmm\\Q-20171231-011633-718-A1-CORRECT.png",
           "C:\\BACKUP\\DBQUIZ\\raw 20180101-101135-137\\output\\hmm\\Q-20180101-101137-371-A1-CORRECT.png");
      // quizProcessor.testMatcher("C:/BACKUP/DBQUIZ/DB/Q-20171230-183825-256.png",
      // "C:/BACKUP/DBQUIZ/Q-20180101-191106-653.png");
      // quizProcessor.testMatcher("test3.png", "test3.bmp");
      // quizProcessor.testMatcher("Q-20171225-164428-808-A1.png",
      // "Q-20171225-164428-808-A1.png");

      // quizProcessor.setTournamentMode(true);

      // quizProcessor.processFolder("C:/BACKUP/DBQUIZ/raw");
      // QuizParams.deleteTree(new File("C:/BACKUP/DBQUIZ/READY/output"));
      // quizProcessor.processOutputFolder("C:/BACKUP/DBQUIZ/READY",
      // "C:/BACKUP/DBQUIZ/READY/output", true);

      // quizProcessor.checkDBHealth();
      //////////////////////////////////////////////quizProcessor.checkDBForDuplicates();
      // quizProcessor.processSourceFolder();

      // quizProcessor.processFolder("C:/backup/quizBIG1");
      // quizProcessor.processOutputFolder("C:/backup/quizBIG1/output", true);

      // quizMaster.processOutputFolder("C:/backup/OUTPUT", true);
      // quizMaster.processOutputFolder("C:/backup/OUTPUT2", true);
      // quizProcessor.processOutputFolder("C:/backup/DB", true);

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
      e.printStackTrace();
    }
  }

}
