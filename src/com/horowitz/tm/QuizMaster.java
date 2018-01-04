package com.horowitz.tm;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import com.horowitz.commons.CrazyImageComparator;
import com.horowitz.commons.ImageData;
import com.horowitz.commons.MouseRobot;
import com.horowitz.commons.MyImageIO;
import com.horowitz.commons.Pixel;
import com.horowitz.commons.RobotInterruptedException;
import com.horowitz.commons.Settings;
import com.sun.org.apache.bcel.internal.generic.LSTORE;

public class QuizMaster {

  private PropertyChangeSupport support = new PropertyChangeSupport(this);

  private final static Logger LOGGER = Logger.getLogger("MAIN");

  private boolean tournamentMode = false;
  private ScreenScanner scanner;
  private ScreenScanner scannerInt;
  private Settings settings;
  private MouseRobot mouse;
  private boolean questionDisplayed = false;
  private boolean stopQuestionThread = false;
  private BufferedImage prev = null;

  private Rectangle qArea;

  private Rectangle aArea;

  private Rectangle qaArea;

  private Rectangle qDisplayedArea;

  private boolean done;

  private Rectangle aDisplayedArea;

  private QuizProcessor processor;

  public QuizMaster(ScreenScanner scannerP, Settings settings) throws IOException {
    super();
    this.settings = settings;
    
    scanner = scannerP;
    scanner.comparator.setPrecision(QuizParams.COMPARATOR_PRECISION);
    
    scannerInt = new ScreenScanner(settings);
    scannerInt.comparator.setPrecision(QuizParams.COMPARATOR_PRECISION_INT);
    
    this.mouse = scanner.getMouse();
    this.processor = new QuizProcessor(scanner, settings);
    scanner.getImageData(QuizParams.TOP_LEFT_CORNER).setColorToBypass(Color.red);
    scanner.getImageData("topLeftCorner6.bmp").setColorToBypass(Color.red);
    scannerInt.getImageData("topLeftCorner6.bmp").setColorToBypass(Color.red);
    scannerInt.getImageData("clearAnswer1T.png").setColorToBypass(Color.red);
    scanner.getImageData("correctAnswerTopLeft.png").setColorToBypass(Color.red);
    
    scanner.getImageData(QuizParams.TOP_LEFT_CORNER_TOUR).setColorToBypass(Color.red);
    scanner.getImageData(QuizParams.QUESTION_DISPLAYED).setColorToBypass(Color.green);

  }

  public void stop() {
    done = true;
    stopQuestionThread = true;
    LOGGER.info("stopped");
    prev = null;
    lastP = null;
  }

  public void capture() throws AWTException, RobotInterruptedException, IOException {
    // scanner.comparator.setPrecision(25);
    // scanner.comparator.setThreshold(0.93);
    Pixel p = clockVisible();
    if (p != null) {
      LOGGER.info("GO");
      resetAreas(p);

      done = false;
      // scanner.captureArea(qDisplayedArea, "quiz/qdisparea.bmp", true);
      runQDisplayedThread(true);

      do {
        if (questionDisplayed) {
          // captureQuestion(qaArea);
          mouse.delay(100, false);
        } else
          mouse.delay(100, false);
      } while (!done);

    }
  }

  Pixel lastP = null;

  private long qDisplayedTime;

  private void runQDisplayedThread(final boolean waitForAnswer) {
    stopQuestionThread = false;
    qDisplayedTime = 0;
    Thread t = new Thread(new Runnable() {
      public void run() {
        try {
          int t = 0;
          do {
            t++;
            Thread.sleep(30);
            ImageData qID = scanner.getImageData(tournamentMode ? QuizParams.QUESTION_DISPLAYED_TOUR
                : QuizParams.QUESTION_DISPLAYED);

            BufferedImage qaImage = new Robot().createScreenCapture(qaArea);
            BufferedImage qaQOnly = qaImage.getSubimage(0, 0, qDisplayedArea.width, qDisplayedArea.height);

            Pixel pp = scanner.comparator.findImage(qID.getImage(), qaQOnly, null);
            if (pp != null) {
              Thread.sleep(20);
              pp = scanner.comparator.findImage(qID.getImage(), qaQOnly, null);
            }

            BufferedImage a1Only = qaImage.getSubimage(0, 140, 20, 20);

            Pixel pp2 = null;
            if (waitForAnswer && pp != null) {
              ImageData aID = scannerInt.getImageData(tournamentMode ? "clearAnswer1T.png" : "clearAnswer1.png");

              pp2 = scannerInt.comparator.findImage(aID.getImage(), a1Only, Color.red);
              // pp2 = scannerInt.findImage("clearAnswer1.png", aDisplayedArea);
              if (pp2 != null) {
                captureQuestion(qaImage);
                Thread.sleep(200);
              } else {
                // try answered
                pp2 = scannerInt.findImage(tournamentMode ? "correctAnswer1T.png" : "correctAnswer1.png",
                    aDisplayedArea);// ,
                // BufferedImage im = new Robot().createScreenCapture(qaArea);
                int a = processor.findGreenAnswer(qaImage);
                if (a > 0) {
                  captureQuestion(qaImage);
                  Thread.sleep(200);
                }
              }
              questionDisplayed = pp != null && pp2 != null;
            }

            if (t >= 10) {
              LOGGER.info((pp != null ? "--q--" : "-----") + (pp2 != null ? "--a--" : "-----"));
              t = 0;
              // LOGGER.info("q: " + pp + "   a: " + pp2);
            }
          } while (!stopQuestionThread);
        } catch (Exception e) {
        }
      }
    });
    t.start();
  }

  public void play() throws AWTException, RobotInterruptedException, IOException {
    Pixel p = clockVisible();
    if (p != null) {
      LOGGER.info("GOGOGO");
      resetAreas(p);

      boolean firstTime = true;
      done = false;
      // scanner.captureArea(qDisplayedArea, "quiz/qdisparea.bmp", true);
      runQDisplayedThread(true);
      boolean moved = false;
      long lastMoved = System.currentTimeMillis();
      do {
        // LOGGER.info("c");
        // scan if question is displayed
        if (questionDisplayed) {
          // LOGGER.info("QV...");
          // captureQuestion(qaArea);
          // mouse.delay(100, false);
          List<Question> possibleQuestions = getPossibleQuestions(qaArea);
          // LOGGER.info(possibleQuestions.size() + " possible questions");
          support.firePropertyChange("QUESTIONS_FOUND", null, possibleQuestions.size());
          if (!possibleQuestions.isEmpty()) {
            Pixel pa = scanForAnswer(possibleQuestions);
            if (pa != null) {
              if (!moved) {
                mouse.mouseMove(pa);
                lastMoved = System.currentTimeMillis();
                moved = true;
                mouse.click();
                mouse.delay(3100, false);
              }
              mouse.delay(100, false);
            }
          } else {
            if (firstTime) {
              LOGGER.info("UNKNOWN QUESTION!");
              // AudioTools.playWarning();
              mouse.delay(100, false);
              firstTime = false;
            } else {
              mouse.delay(100, false);
            }
            // captureQuestion(qaArea);
            moved = false;
          }

        } else {
          moved = false;
          firstTime = true;
          lastP = null;
          mouse.delay(100, false);
        }

        // plan B
        if (System.currentTimeMillis() - lastMoved > 5000) {
          moved = false;
        }
      } while (!done);

    }
  }

  private void resetAreas(Pixel p) throws IOException {
    qArea = new Rectangle(p.x + 194, p.y + 154, 527, 91);

    aArea = new Rectangle(p.x + 194, p.y + 294, 527, 147);
    qaArea = new Rectangle(p.x + 194, p.y + 154, 527, 287);

    qDisplayedArea = new Rectangle(qArea);
    qDisplayedArea.width = 38;
    qDisplayedArea.height = 28;

    aDisplayedArea = new Rectangle(aArea);
    // aDisplayedArea.y += 80;
    // aDisplayedArea.x -= 2;
    // aDisplayedArea.y -= 2;
    aDisplayedArea.width = 20;
    aDisplayedArea.height = 20;

  }

  public void loadQuestions() throws IOException {
    LOGGER.info("Loading questions...");
    int qs = processor.loadQuestions();
    LOGGER.info(qs + " questions loaded!");
  }

  public void processNewQuestions() throws IOException {
    LOGGER.info("Processing new questions...");
    // processor.processSourceFolder();
  }

  private List<Question> getPossibleQuestions(Rectangle qaArea) throws AWTException {
    BufferedImage im = new Robot().createScreenCapture(qaArea);
    // im = QuizParams.toBW(im);
    List<Question> possibleQuestions = processor.getPossibleQuestions(im);
    return possibleQuestions;
  }

  private Pixel scanForAnswer(List<Question> possibleQuestions) throws AWTException, IOException,
      RobotInterruptedException {
    BufferedImage im = new Robot().createScreenCapture(qaArea);
    // scanner.comparator.setThreshold(0.99);
    // scanner.comparator.setPrecision(10);
    Pixel p = processor.findAnswer(im, possibleQuestions);
    if (p != null) {
      return new Pixel(p.x + qaArea.x, p.y + qaArea.y);
    }
    return null;
  }

  private void captureQuestion(Rectangle qaArea) {
    try {
      captureQuestion(new Robot().createScreenCapture(qaArea));
    } catch (AWTException e) {
    }
  }

  private void captureQuestion(BufferedImage im) {
    try {
      if (prev == null || !scanner.sameImage(prev, im)) {
        MyImageIO.writeImageTS(im, "quiz/q.png");
        LOGGER.info("captured");
      } else {
        // LOGGER.info("skip");
      }
      prev = im;
    } catch (Exception e) {
      LOGGER.warning("oops " + e.getMessage());
      e.printStackTrace();
    }

  }

  private void stopQuestionDisplayed() {
    stopQuestionThread = true;
  }

  private Pixel clockVisible() throws AWTException, RobotInterruptedException, IOException {
    Pixel p = null;
    boolean started = false;
    int turn = 0;
    do {
      turn++;
      // scanner.comparator.setPrecision(20);
      p = scanner.findImage("clockAnchor.bmp", scanner._scanArea4);
      LOGGER.info("looking for clock..." + p);
      if (p != null) {
        started = true;
        return p;
      } else
        mouse.delay(400, false);
    } while (!started && turn < 42);

    return null;
  }

  public boolean isTournamentMode() {
    return tournamentMode;
  }

  public void setTournamentMode(boolean tournamentMode) {
    this.tournamentMode = tournamentMode;
    this.processor.setTournamentMode(tournamentMode);
  }

  public void save() {
    try {
      File srcDir = new File(QuizParams.SOURCE_FOLDER);
      String timestamp = QuizParams.SIMPLE_DATE_FORMAT.format(new Date());
      File destDir = new File(QuizParams.DB_DESTINATION_RAW + " " + timestamp);
      FileUtils.copyDirectory(srcDir, destDir);
      QuizParams.emptyFolder(srcDir);
      LOGGER.info("Files moved");
      mouse.delay(2000, false);

      this.processor.processSourceFolder(destDir.getAbsolutePath());
    } catch (IOException e) {
      LOGGER.info("Failed to move files from quiz to db");
      e.printStackTrace();
    } catch (RobotInterruptedException e) {
      e.printStackTrace();
    }

  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    support.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    support.removePropertyChangeListener(listener);
  }

  public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    support.addPropertyChangeListener(propertyName, listener);
  }

  public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    support.removePropertyChangeListener(propertyName, listener);
  }
}
