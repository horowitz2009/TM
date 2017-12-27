package com.horowitz.tm;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.horowitz.commons.MouseRobot;
import com.horowitz.commons.MyImageIO;
import com.horowitz.commons.Pixel;
import com.horowitz.commons.RobotInterruptedException;
import com.horowitz.commons.Settings;
import com.horowitz.ocr.OCRe;

public class QuizMaster {

  private final static Logger LOGGER = Logger.getLogger("MAIN");

  private boolean tournamentMode = false;
  private ScreenScanner scanner;
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

  public QuizMaster(ScreenScanner scanner, Settings settings) throws IOException {
    super();
    this.scanner = scanner;
    this.settings = settings;
    this.mouse = scanner.getMouse();
    this.processor = new QuizProcessor(scanner, settings);
    scanner.getImageData(QuizParams.TOP_LEFT_CORNER).setColorToBypass(Color.red);
    scanner.getImageData(QuizParams.TOP_LEFT_CORNER_TOUR).setColorToBypass(Color.red);
  }

  public void stop() {
    done = true;
    stopQuestionThread = true;
    LOGGER.info("stopped");
    prev = null;
    lastP = null;
  }

  public void capture() throws AWTException, RobotInterruptedException, IOException {
    Pixel p = clockVisible();
    if (p != null) {
      LOGGER.info("GO");
      resetAreas(p);

      done = false;
      // scanner.captureArea(qDisplayedArea, "quiz/qdisparea.bmp", true);
      runQDisplayedThread(true);

      do {
        // LOGGER.info("c");
        // scan if question is displayed
        if (questionDisplayed) {
          // LOGGER.info("QV...");
          captureQuestion(qaArea);
          // playQuestion
          mouse.delay(250, false);
        } else
          mouse.delay(200, false);
      } while (!done);

    }
  }

  Pixel lastP = null;

  public void play() throws AWTException, RobotInterruptedException, IOException {
    Pixel p = clockVisible();
    if (p != null) {
      LOGGER.info("GO");
      resetAreas(p);

      boolean firstTime = true;
      done = false;
      // scanner.captureArea(qDisplayedArea, "quiz/qdisparea.bmp", true);
      runQDisplayedThread(true);
      boolean moved = false;
      do {
        // LOGGER.info("c");
        // scan if question is displayed
        if (questionDisplayed) {
          // LOGGER.info("QV...");
          List<Question> possibleQuestions = getPossibleQuestions(qaArea);
          if (!possibleQuestions.isEmpty()) {
            Pixel pa = scanForAnswer(qaArea, possibleQuestions);
            if (pa != null) {
              if (!moved) {
                mouse.mouseMove(pa);
                moved = true;
                mouse.click();
                mouse.delay(1500, false);
              }
              mouse.delay(200, false);
            } else {
              LOGGER.info("Question good, but no answer!");
              AudioTools.playWarning();
              captureQuestion(qaArea);
            }
          } else {
            if (firstTime) {
              LOGGER.info("UNKNOWN QUESTION!");
              AudioTools.playWarning();
              mouse.delay(2000, false);
              firstTime = false;
            } else {
              mouse.delay(200, false);
            }
            captureQuestion(qaArea);
            moved = false;
          }

        } else {
          moved = false;
          firstTime = true;
          lastP = null;
          mouse.delay(200, false);
        }
      } while (!done);

    }
  }

  private void resetAreas(Pixel p) throws IOException {
    qArea = new Rectangle(p.x + 194, p.y + 154, 527, 91);

    aArea = new Rectangle(p.x + 194, p.y + 294, 527, 147);
    qaArea = new Rectangle(p.x + 194, p.y + 154, 527, 287);

    qDisplayedArea = new Rectangle(qArea);
    qDisplayedArea.width = 61;
    qDisplayedArea.height = 60;

    aDisplayedArea = new Rectangle(aArea);
    // aDisplayedArea.y += 80;
    aDisplayedArea.width = 30;
    aDisplayedArea.height = 45;
  }

  public void loadQuestions() throws IOException {
    LOGGER.info("Loading questions...");
    int qs = processor.loadQuestions();
    LOGGER.info(qs + " questions loaded!");
  }

  public void processNewQuestions() throws IOException {
    LOGGER.info("Processing new questions...");
    processor.processSourceFolder();
  }

  private List<Question> getPossibleQuestions(Rectangle qaArea) throws AWTException {
    BufferedImage im = new Robot().createScreenCapture(qaArea);
    // im = QuizParams.toBW(im);
    List<Question> possibleQuestions = processor.getPossibleQuestions(im);
    return possibleQuestions;
  }

  private Pixel scanForAnswer(Rectangle qaArea, List<Question> possibleQuestions) throws AWTException, IOException {
    BufferedImage im = new Robot().createScreenCapture(qaArea);
    Pixel p = processor.findAnswer(im, possibleQuestions);
    if (p != null) {
      return new Pixel(p.x + qaArea.x, p.y + qaArea.y);
    }
    return null;
  }

  private void captureQuestion(Rectangle qaArea) {
    try {
      BufferedImage im = new Robot().createScreenCapture(qaArea);

      if (prev == null || !scanner.sameImage(prev, im)) {
        scanner.writeAreaTS(qaArea, "quiz/q.png");
        LOGGER.info("captured");
      } else {
        LOGGER.info("skip");
      }
      prev = im;
    } catch (AWTException e) {
      LOGGER.warning("oops " + e.getMessage());
      e.printStackTrace();
    }

  }

  private void runQDisplayedThread(final boolean waitForAnswer) {
    stopQuestionThread = false;
    Thread t = new Thread(new Runnable() {
      public void run() {
        try {
          int t = 0;
          do {
            t++;
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            // LOGGER.info("q?");
            Pixel pp = scanner.scanOne(tournamentMode ? QuizParams.QUESTION_DISPLAYED_TOUR
                : QuizParams.QUESTION_DISPLAYED, qDisplayedArea, false);
            // System.err.println(pp + " " + qDisplayedArea);
            questionDisplayed = pp != null;
            Pixel pp2 = null;
            if (waitForAnswer && pp != null) {
              pp2 = scanner.scanOne(tournamentMode ? QuizParams.TOP_LEFT_CORNER_TOUR : QuizParams.TOP_LEFT_CORNER,
                  aDisplayedArea, false);
              questionDisplayed = pp2 != null;
            }
            if (t >= 10) {
              LOGGER.info("." + (pp2 != null ? "..." : ""));
              t = 0;
              //LOGGER.info("q: " + pp + "   a: " + pp2);
            }
          } while (!stopQuestionThread);
        } catch (AWTException e) {
        } catch (RobotInterruptedException e) {
        } catch (IOException e) {
        }
      }
    });
    t.start();
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
      p = scanner.scanOneFast("clockAnchor.bmp", scanner._scanArea4, false);
      LOGGER.info("looking for clock..." + p);
      if (p != null) {
        started = true;
        return p;
      } else
        mouse.delay(400, false);
    } while (!started && turn < 12);

    return null;
  }

  public boolean isTournamentMode() {
    return tournamentMode;
  }

  public void setTournamentMode(boolean tournamentMode) {
    this.tournamentMode = tournamentMode;
    this.processor.setTournamentMode(tournamentMode);
  }
}
