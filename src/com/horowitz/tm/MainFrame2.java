package com.horowitz.tm;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import com.horowitz.commons.DateUtils;
import com.horowitz.commons.GameErrorException;
import com.horowitz.commons.MouseRobot;
import com.horowitz.commons.MyImageIO;
import com.horowitz.commons.MyLogger;
import com.horowitz.commons.Pixel;
import com.horowitz.commons.RobotInterruptedException;
import com.horowitz.commons.Service;
import com.horowitz.commons.Settings;
import com.horowitz.commons.SimilarityImageComparator;
import com.horowitz.macros.AbstractGameProtocol;
import com.horowitz.macros.Task;
import com.horowitz.macros.TaskManager;

public class MainFrame2 extends JFrame {

  private final static Logger LOGGER = Logger.getLogger("MAIN");

  private static String APP_TITLE = "TM v0.25";

  private MouseRobot mouse;

  private Settings settings;

  private ScreenScanner scanner;
  private SimilarityImageComparator _pairsComparator;

  private boolean stopAllThreads;

  private Task practiceTask;
  private Task sponsorTask;
  private Task matchTask;
  private Task checkDuelsTask;
  private Task premiumTask;
  private Task ballTask;
  private Task bankTask;
  private Task pairsTask;

  private TaskManager taskManager;

  protected boolean duelsFull;

  private Stats stats;

  private Task clubTask;

  public static void main(String[] args) {

    try {
      boolean isTestmode = false;
      if (args.length > 0) {
        for (String arg : args) {
          System.err.println(arg);
          if (arg.equalsIgnoreCase("test")) {
            isTestmode = true;
            break;
          }
        }
      }
      MainFrame2 frame = new MainFrame2(isTestmode);
      frame.pack();
      frame.setSize(new Dimension(frame.getSize().width + 8, frame.getSize().height + 8));
      int w = 285;// frame.getSize().width;
      final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int h = (int) (screenSize.height * 0.3);
      int x = screenSize.width - w;
      int y = (screenSize.height - h) / 2;
      frame.setBounds(x, y, w, h);

      frame.setVisible(true);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings("serial")
  private void init() throws AWTException {

    try {

      // LOADING DATA
      settings = Settings.createSettings("tm.properties");
      if (!settings.containsKey("tasks.balls")) {
        setDefaultSettings();
      }
      stats = new Stats();

      scanner = new ScreenScanner(settings);
      scanner.getMatcher().setSimilarityThreshold(.90d);

      mouse = scanner.getMouse();
      _pairsComparator = new SimilarityImageComparator(0.04, 3000);
      _pairsComparator.setErrors(8);

      // testImages();

      ballTask();
      practiceTaskSIM();
      practiceTaskPAIRS();
      matchTask();
      bankTask();
      premiumTask();
      sponsorTask();
      clubTask();
      checkDuelsTask();

      taskManager = new TaskManager(mouse);
      taskManager.addTask(practiceTask);
      taskManager.addTask(pairsTask);
      taskManager.addTask(checkDuelsTask);
      taskManager.addTask(matchTask);
      taskManager.addTask(bankTask);
      taskManager.addTask(premiumTask);
      taskManager.addTask(sponsorTask);
      taskManager.addTask(ballTask);
      taskManager.addTask(clubTask);
      stopAllThreads = false;

    } catch (Exception e1) {
      System.err.println("Something went wrong!");
      e1.printStackTrace();
      System.exit(1);
    }

    initLayout();

    //reapplySettings();

    //runSettingsListener();

  }

  private void testImages() {
    try {
      scanner.getMatcher().setSimilarityThreshold(.90d);
      Pixel c = scanner.getMatcher().findMatch(scanner.getImageData("prizeMatches.bmp").getImage(),
          scanner.getImageData("grandPrize.bmp").getImage(), Color.RED);
      System.err.println("MATCHER: " + c);
      // _pairsComparator.setErrors(8);
      // _pairsComparator.setPrecision(2000);
      // boolean hmm =
      // sameImage(scanner.getImageData("prizeMatches.bmp").getImage(),
      // scanner.getImageData("grandPrize.bmp").getImage());
      // System.err.println("SAME:" + hmm);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  private void checkDuelsTask() {
    checkDuelsTask = new Task("Check Duels", 1);
    checkDuelsTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {
        if (_matchesToggle.isSelected()) {
          handlePopups();

          try {
            mouse.delay(1000);
            // check duels here
            int x = scanner.getTopLeft().x + scanner.getGameWidth() / 2;
            int y = scanner.getTopLeft().y + 20;
            mouse.mouseMove(x, y);
            mouse.delay(3000);
            Pixel p = scanner.scanOne("DuelsClock.bmp", new Rectangle(x - 70, y, 140, 67), false);
            if (p == null) {
              LOGGER.info("DUELS FULL");
              duelsFull = true;
              ((AbstractGameProtocol) matchTask.getProtocol()).sleep(0);
            } else
              duelsFull = false;
            mouse.delay(3000);
          } catch (IOException e) {
            e.printStackTrace();
          } catch (AWTException e) {
            e.printStackTrace();
          }

          sleep(1);
        }
      }
    });
  }

  private void matchTask() {
    matchTask = new Task("MATCH", 1);
    matchTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {
        if (_matchesToggle.isSelected()) {
          int matches = 0;
          int maxMatches = 1;

          try {
            do {
              scanner.scanOneFast(scanner.getImageData("centerCourt.bmp", scanner._scanArea, 0, 105),
                  scanner._scanArea, true);
              mouse.delay(3000);
              Pixel p = scanner.scanOneFast("centerCourtTitle.bmp", scanner._scanArea, false);
              if (p != null) {
                LOGGER.info("entered center court...");
                // Rectangle area = new Rectangle(p.x - 194, p.y + 129, 650,
                // 34);
                int code = clickMatch(p);
                boolean success = code == 1;
                if (success) {
                  success = false;
                  boolean won = false;
                  // is it victory?
                  Pixel pv = scanner.scanOneFast("victory.bmp", scanner._scanArea, false);
                  if (pv != null) {
                    stats.register("Matches");
                    stats.register("Won");
                    success = true;
                    won = true;
                  } else {
                    pv = scanner.scanOneFast("defeat.bmp", scanner._scanArea, false);
                    if (pv != null) {
                      stats.register("Matches");
                      stats.register("Lost");
                      success = true;
                      won = false;
                    }
                  }
                  if (success) {
                    if (settings.getBoolean("ping", false)) {
                      captureScreen("ping/match ");
                      deleteOlder("ping", "match", -1, 24);
                    }

                    p = scanner.scanOneFast("Continue.bmp", scanner._scanArea, true);
                    if (p == null && won) {
                      Rectangle area = new Rectangle(pv.x, pv.y + 380, 170, 60);
                      p = scanner.scanOne("PrizeMatches.bmp", area, true);
                      if (p != null) {
                        handleAwards();
                      }
                    }

                    if (p != null) {
                      handlePopups();
                      mouse.delay(1000);
                    }
                    clickBankDirectly();

                  }
                  matches++;

                } else {
                  int minutes = settings.getInt("tasks.matches.sleep", 5);
                  if (code < 0)
                    minutes = 0;
                  bankTask.getProtocol().reset();
                  LOGGER.info("sleep " + minutes + " minutes");
                  sleep(minutes * 60000);
                  break;// exit the loop
                }
              } else {
                LOGGER.info("something not right!!!");
                break;
              }
            } while (matches < maxMatches);

          } catch (IOException | AWTException e) {
            e.printStackTrace();
          }
        }
      }

      private int clickMatch(Pixel p) throws RobotInterruptedException, IOException, AWTException {
        mouse.delay(3000);
        Rectangle slot1Area = new Rectangle(p.x + 97, p.y + 221, 5, 13);
        Rectangle slot2Area = new Rectangle(p.x + 97, p.y + 323, 5, 13);
        // Rectangle slot3Area = new Rectangle(p.x + 97, p.y + 425, 5, 13);
        Pixel pp = null;
        Pixel ph = scanner.scanOneFast("easyGreen.bmp", slot1Area, false);
        if (ph != null) {
          pp = new Pixel(ph.x + 332, ph.y - 42);
        } else {
          ph = scanner.scanOneFast("easyGreen.bmp", slot2Area, false);
          if (ph != null) {
            pp = new Pixel(ph.x + 332, ph.y - 42);
          }
        }

        // try medium opponent
        if (pp == null && settings.getBoolean("tasks.matches.medium", false)) {
          ph = scanner.scanOneFast("mediumOrange.bmp", slot1Area, false);
          if (ph != null) {
            pp = new Pixel(ph.x + 332, ph.y - 42);
          }
        }

        if (pp == null && duelsFull)
          pp = new Pixel(p.x + 424, p.y + 180);

        if (pp != null) {
          mouse.click(pp);
          mouse.delay(3000);

          Pixel pq = scanner.scanOneFast("playerComp.bmp", scanner._scanArea, false);
          if (pq != null) {
            mouse.click(pq.x + 198, pq.y + 257);
            LOGGER.info("match");
            mouse.delay(3000);
            return 1;// job well done
          } else
            return 0;// no duels, so sleep

        } else
          return -1;// no suitable opponents, may be => don't sleep, try again
                    // soon
      }
    });
  }

  private void premiumTask() {
    premiumTask = new Task("Premium", 1);
    premiumTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {

        try {
          mouse.mouseMove(scanner.getParkingPoint());
          handlePopups();
          mouse.mouseMove(scanner.getParkingPoint());

          mouse.click(scanner.getTopLeft().x + scanner.getGameWidth() / 2, scanner.getTopLeft().y + 63);
          mouse.delay(3000);
          Pixel p = scanner.scanOneFast("premiumFree1.bmp", scanner._scanArea, false);
          if (p != null) {
            LOGGER.info("premium 1");
            mouse.click(p.x + 38, p.y + 103);
            mouse.delay(3000);

            // scroller
            mouse.click(p.x + 558, p.y + 355);
            mouse.delay(2000);
            // p = scanner.scanOneFast("premiumFree2.bmp", scanner._scanArea,
            // false);
            // if (p != null) {
            LOGGER.info("premium 2");
            mouse.click(p.x + 41, p.y + 131);
            mouse.delay(3000);
            // }
          }

          LOGGER.info("sleep 10min");
          sleep(10 * 60000);
          handlePopups();

        } catch (IOException | AWTException e) {
          e.printStackTrace();
        }

      }
    });
  }

  private void sponsorTask() {
    sponsorTask = new Task("Sponsor", 1);
    sponsorTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {

        try {
          Pixel p = scanner.scanOneFast("sponsor.bmp", scanner._scanArea, false);
          if (p != null) {
            mouse.click(p.x + 6, p.y + 6);
            mouse.delay(3000);
            p = scanner.scanOneFast("Rocky.bmp", scanner._scanArea, false);
            if (p != null) {
              LOGGER.info("sponsor opened");
              mouse.click(p.x, p.y + 303);
              mouse.delay(500);
              mouse.click(p.x + 357, p.y + 303);
              mouse.delay(2500);
              LOGGER.info("sleep 5min");
              sleep(5 * 60000);
              handlePopups();
            }
          }

        } catch (IOException | AWTException e) {
          e.printStackTrace();
        }

      }
    });
  }

  private void clubTask() {
    clubTask = new Task("Club", 1);
    clubTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {

        try {
          handlePopups();
          Pixel p = scanner.scanOneFast("club.bmp", scanner._scanArea, false);
          if (p == null) {
            // move se
            Pixel m = new Pixel(scanner.getTopLeft().x + scanner.getGameWidth() / 2,
                scanner.getTopLeft().y + scanner.getGameHeight() / 2);
            // mouse.mouseMove(m);
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            screenSize.width += 100;
            screenSize.height += 100;
            int xx = screenSize.width - scanner.getGameWidth();
            int yy = screenSize.height - scanner.getGameHeight();
            xx /= 2;
            yy /= 2;

            // SE
            LOGGER.info("drag SE");
            mouse.dragFast(m.x - xx, m.y - yy, m.x + xx, m.y + yy, false, false);
            mouse.delay(200);
            mouse.mouseMove(scanner.getParkingPoint());

            // try again now
            p = scanner.scanOneFast("club.bmp", scanner._scanArea, false);
          }

          if (p != null) {
            LOGGER.info("Go to club...");
            mouse.click(p.x, p.y);
            mouse.delay(3000);
            dragSE();
            mouse.delay(1200);
            checkForMoney();
            mouse.delay(500);
            dragW();
            mouse.delay(1200);
            checkForMoney();
            mouse.delay(500);
            dragN();
            mouse.delay(1200);
            checkForMoney();
            mouse.delay(500);
            dragE();
            mouse.delay(1200);
            checkForMoney();
            mouse.delay(500);
            // refresh
            sleep(15 * 60000);
            refresh();
          }

        } catch (IOException | AWTException e) {
          e.printStackTrace();
        }

      }

      private void checkForMoney() throws RobotInterruptedException, IOException, AWTException {
        Pixel p = null;
        do {
          p = scanner.scanOneFast("money.bmp", scanner._scanArea, true);
          LOGGER.info("money..." + p);
          if (p != null) {
            stats.register("Money");
            mouse.delay(4000);
          }
        } while (p != null);
      }
    });
  }

  private int ballsCnt = 0;

  private void bankTask() {
    bankTask = new Task("BANK", 1);
    bankTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {
        if (_bankToggle.isSelected())
          try {
            handlePopups();
            clickBankDirectly();
            sleep(1 * 60000);// 1 min
          } catch (IOException e) {
            e.printStackTrace();
          } catch (AWTException e) {
            e.printStackTrace();
          }
      }
    });

  }

  private void dragSE() throws RobotInterruptedException {
    drag(1, 1);
  }

  private void dragW() throws RobotInterruptedException {
    drag(-1, 0);
  }

  private void dragN() throws RobotInterruptedException {
    drag(0, -1);
  }

  private void dragE() throws RobotInterruptedException {
    drag(1, 0);
  }

  private void drag(int ewDir, int nsDir) throws RobotInterruptedException {
    Pixel m = new Pixel(scanner.getTopLeft().x + scanner.getGameWidth() / 2,
        scanner.getTopLeft().y + scanner.getGameHeight() / 2);
    // mouse.mouseMove(m);
    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    screenSize.width += 100;
    screenSize.height += 100;
    int xx = screenSize.width - scanner.getGameWidth();
    int yy = screenSize.height - scanner.getGameHeight();
    xx /= 2;
    yy /= 2;

    int x1 = 0;
    int y1 = 0;
    int x2 = 0;
    int y2 = 0;
    if (ewDir != 0) {
      x1 = -ewDir;
      x2 = ewDir;
    }
    if (nsDir != 0) {
      y1 = -nsDir;
      y2 = nsDir;
    }
    if (ewDir != 0 && nsDir != 0) {
      // both directions
    }

    // hmm
    mouse.dragFast(m.x + xx * x1, m.y + yy * y1, m.x + xx * x2, m.y + yy * y2, false, false);
    mouse.delay(200);
    mouse.mouseMove(scanner.getParkingPoint());

  }

  private void ballTask() {
    ballTask = new Task("BALLS", 1);
    ballTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void reset() {
        super.reset();
        ballsCnt = 0;
      }

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {
        int limit = settings.getInt("balls.limit", 0);
        if (_ballsToggle.isSelected())
          try {

            if (limit > 0 && ballsCnt < limit || limit <= 0) {

              boolean move = settings.getBoolean("balls.move", true);
              Pixel p = null;
              handlePopups();
              mouse.delay(100);
              if (move) {
                // move approach
                Pixel m = new Pixel(scanner.getTopLeft().x + scanner.getGameWidth() / 2,
                    scanner.getTopLeft().y + scanner.getGameHeight() / 2);
                // mouse.mouseMove(m);
                final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                screenSize.width += 100;
                screenSize.height += 100;
                int xx = screenSize.width - scanner.getGameWidth();
                int yy = screenSize.height - scanner.getGameHeight();
                xx /= 2;
                yy /= 2;

                // SE
                LOGGER.info("drag SE");
                mouse.dragFast(m.x - xx, m.y - yy, m.x + xx, m.y + yy, false, false);
                mouse.delay(200);
                mouse.mouseMove(scanner.getParkingPoint());
                Rectangle area = new Rectangle(scanner._fullArea);
                area.width -= 500;
                area.height -= 280;
                // scanner.writeArea(area, "area1.jpg");
                clickBalls(area);

                // W
                LOGGER.info("drag W");
                mouse.dragFast(m.x + xx, m.y + yy, m.x - xx, m.y + yy, false, false);
                mouse.delay(200);
                mouse.mouseMove(scanner.getParkingPoint());
                area.width = 570 + xx * 2;
                area.x = scanner.getBottomRight().x - area.width;
                clickBalls(area);

                // N
                LOGGER.info("drag N");
                mouse.dragFast(m.x - xx, m.y + yy, m.x - xx, m.y - yy, false, false);
                mouse.delay(200);
                mouse.mouseMove(scanner.getParkingPoint());
                area = new Rectangle(scanner._fullArea);
                area.height = 280 + 70 + yy * 2;
                area.width = 570 + xx * 2;
                area.x = scanner.getBottomRight().x - area.width;
                area.y = scanner.getBottomRight().y - area.height;
                clickBalls(area);

                // E
                LOGGER.info("drag E");
                mouse.dragFast(m.x - xx, m.y - yy, m.x + xx, m.y - yy, false, false);
                mouse.delay(200);
                mouse.mouseMove(scanner.getParkingPoint());
                area.width = scanner.getGameWidth() - 500;
                area.x = scanner.getTopLeft().x;
                clickBalls(area);

                LOGGER.info("drag C");
                mouse.dragFast(m.x + xx, m.y - yy, m.x, m.y, false, false);
                mouse.delay(500);
                mouse.mouseMove(scanner.getParkingPoint());
              } else {
                clickBalls(scanner._fullArea);
              }
              mouse.delay(500);
              refresh();
            }
            int minutes = settings.getInt("balls.sleep", 20);
            LOGGER.info("BALLS SO FAR: " + ballsCnt);
            LOGGER.info("sleep " + minutes + " min");
            sleep(minutes * 60000);
          } catch (Exception e) {
            LOGGER.info("BALLS ERR");
          }

        LOGGER.info("BALLS: " + ballsCnt + " / " + limit);
      }

      private void clickBalls(Rectangle area) throws RobotInterruptedException, IOException, AWTException {
        int limit = settings.getInt("balls.limit", 0);
        if (limit > 0 && ballsCnt < limit || limit <= 0) {
          Pixel p = scanner.scanOneFast("ball.bmp", area, true);
          if (p != null) {
            ballsCnt++;
            stats.register("Balls");
          }
          p = scanner.scanOneFast("ball.bmp", area, true);
          if (p != null) {
            ballsCnt++;
            stats.register("Balls");
          }
        }
      }
    });
  }

  private void practiceTaskSIM() {
    practiceTask = new Task("PRACTICE", 1);
    practiceTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {
        if (_practiceToggle.isSelected())
          try {
            // 1.
            mouse.delay(500);
            String minigame = settings.getProperty("minigame", "Items");

            mouse.click((scanner.getTopLeft().x + scanner.getGameWidth() / 2) - 130, scanner.getTopLeft().y + 63);
            mouse.delay(3000);

            // Pixel p = scanner.scanOneFast("centerCourt.bmp",
            // scanner._scanArea, false);

            // if (p != null) {
            {// mouse.click(p.x + 321, p.y - 61);
             // mouse.delay(4000);
              Pixel p = scanner.scanOneFast("practiceArena.bmp", scanner._fullArea, false);
              if (p != null) {
                LOGGER.info("Practice arena...");
                Rectangle area = new Rectangle(p.x - 194, p.y + 129, 650, 34);
                // scanner.writeArea(area, "hmm.bmp");
                Pixel pq = scanner.scanOneFast(minigame + ".bmp", area, false);
                if (pq == null) {
                  for (int i = 0; i < 6; i++) {
                    mouse.click(p.x - 214, p.y + 295);
                    mouse.delay(500);
                  }
                }
                pq = scanner.scanOneFast(minigame + ".bmp", area, false);
                if (pq == null) {
                  for (int i = 0; i < 6; i++) {
                    mouse.click(p.x + 476, p.y + 295);
                    mouse.delay(1400);
                    pq = scanner.scanOneFast(minigame + ".bmp", area, false);
                    if (pq != null)
                      break;
                  }
                }
                if (pq != null) {
                  mouse.click(pq.x + 79, pq.y + 286);
                  LOGGER.info(minigame);
                  mouse.delay(4000);
                  p = scanner.scanOneFast("practiceArena.bmp", scanner._scanArea, false);
                  if (p != null) {
                    LOGGER.info("SUCCESS");
                    mouse.click(p.x + 500, p.y + 12);
                    mouse.delay(2000);
                  } else {
                    LOGGER.info("no energy");
                    LOGGER.info("sleep 5min");
                    sleep(5 * 60000);
                    handlePopups();
                  }
                } else {
                  LOGGER.info("Uh oh! Can't find " + minigame + "!");
                  // handlePopups(false);
                  refresh();
                }
              }
            }
          } catch (IOException | AWTException e) {
            e.printStackTrace();
          }

      }
    });
  }

  private void practiceTaskPAIRS() {
    pairsTask = new Task("PRACTICE PAIRS", 1);
    pairsTask.setProtocol(new PairsProtocol());
  }

  private final class PairsProtocol extends AbstractGameProtocol {

    private boolean done = false;
    private long time = 0l;

    @Override
    public void execute() throws RobotInterruptedException, GameErrorException {
      if (_pairsToggle.isSelected())
        try {
          // 1.
          mouse.delay(500);
          String minigame = "Pairs";

          mouse.click((scanner.getTopLeft().x + scanner.getGameWidth() / 2) - 130, scanner.getTopLeft().y + 63);
          mouse.delay(3000);

          // Pixel p = scanner.scanOneFast("centerCourt.bmp",
          // scanner._scanArea, false);

          // if (p != null) {
          {// mouse.click(p.x + 321, p.y - 61);
           // mouse.delay(4000);
            Pixel p = scanner.scanOneFast("practiceArena.bmp", scanner._fullArea, false);
            if (p != null) {
              LOGGER.info("Practice arena...");
              Rectangle area = new Rectangle(p.x - 194, p.y + 129, 650, 134);
              // scanner.writeArea(area, "hmm.bmp");
              Pixel pq = scanner.scanOneFast(minigame + ".bmp", area, false);
              if (pq == null) {
                for (int i = 0; i < 6; i++) {
                  mouse.click(p.x - 214, p.y + 295);
                  mouse.delay(500);
                }
              }
              pq = scanner.scanOneFast(minigame + ".bmp", area, false);
              if (pq == null) {
                for (int i = 0; i < 6; i++) {
                  mouse.click(p.x + 476, p.y + 295);
                  mouse.delay(1400);
                  pq = scanner.scanOneFast(minigame + ".bmp", area, false);
                  if (pq != null)
                    break;
                }
              }
              if (pq != null) {
                mouse.click(pq.x, pq.y + 226);
                mouse.delay(4000);
                if (!doPairs()) {
                  LOGGER.info("no energy");
                  LOGGER.info("sleep 5min");
                  sleep(5 * 60000);
                  handlePopups();
                } else {
                  mouse.delay(6000);

                  // AFTER GAME
                  captureScreen("ping/pairs ");
                  deleteOlder("ping", "pairs", -1, 24);
                  stats.register("Pairs");
                  p = scanner.scanOneFast("ContinueBrown.bmp", scanner._scanArea, true);
                  if (p == null) {
                    int xx = (scanner.getTopLeft().x + scanner.getGameWidth() / 2);

                    p = scanner.scanOneFast("prizePractice.bmp", scanner._scanArea, true);
                    if (p != null) {
                      handleAwards();
                    } else {
                      mouse.click(xx - 124, pq.y + 285 - 18);
                      mouse.click(xx - 62, pq.y + 285 - 18);
                      mouse.click(xx - 0, pq.y + 285 - 18);
                      mouse.delay(3000);
                      refresh();
                    }
                  }

                  handlePopups();
                  mouse.delay(1000);
                }
              } else {
                LOGGER.info("Uh oh! Can't find " + minigame + "!");
                // handlePopups(false);
                refresh();
              }
            }
          }
        } catch (IOException | AWTException e) {
          e.printStackTrace();
        }

    }

    public boolean doPairs() throws RobotInterruptedException, IOException, AWTException {
      boolean started = false;
      int turn = 0;
      Pixel p = null;
      do {
        turn++;
        p = scanner.scanOneFast("clockAnchor.bmp", scanner._fullArea, false);
        LOGGER.info("check is started..." + p);
        if (p != null) {
          started = true;
          break;
        } else
          mouse.delay(400);
      } while (!started && turn < 12);

      if (started) {
        final Pixel pp = p;
        // good
        int slotSize = 80;
        int gap = 7;
        int mcols = 8;
        int mrows = 4;
        int mwidth = mcols * (slotSize + gap) - gap;
        int mheight = mrows * (slotSize + gap) - gap;
        Rectangle gameArea = new Rectangle(p.x + 308 - 80 - 7 - 80 - 7, p.y + 143, mwidth, mheight);
        int slotsNumber = 0;

        do {
          try {
            slotsNumber = mrows * mcols;
            done = false;

            // CREATE THREAD
            Thread t = new Thread(new Runnable() {
              public void run() {
                try {
                  long threadTime = System.currentTimeMillis();
                  do {

                    Rectangle clockArea = new Rectangle(pp.x + 857 + 34, pp.y + 499 + 35, 15, 24);
                    BufferedImage multiplier = scanSlot(clockArea);
                    if (!sameImage(scanner.getImageData("MultiplierZero.bmp").getImage(), multiplier)) {
                      if (time == 0) {
                        time = System.currentTimeMillis();
                        LOGGER.info("UH OH...");
                      }
                    }
                    Thread.sleep(100);
                  } while (!done && System.currentTimeMillis() - threadTime < 90 * 1000);
                  LOGGER.info("T DONE");
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });

            // MATRIX
            Map<Coords, Slot> matrix = new HashMap<Coords, Slot>();
            for (int row = 1; row <= mrows; row++) {
              for (int col = 1; col <= mcols; col++) {
                Slot slot = new Slot(row, col, true);
                Rectangle slotArea = new Rectangle(gameArea.x + (col - 1) * (slotSize + gap) + 20,
                    gameArea.y + (row - 1) * (slotSize + gap) + 20, 40, 40);
                slot.area = slotArea;
                matrix.put(slot.coords, slot);
              }
            }

            for (int row = 1; row <= mrows; row++) {
              for (int col = 1; col <= mcols; col++) {
                Slot slot = matrix.get(new Coords(row, col));

                slot.image = scanSlot(slot.area);

                if (!sameImage(scanner.getImageData("back.bmp").getImage(), slot.image)) {
                  // slot.image = null;
                  slot.active = false;
                  slotsNumber--;
                }
              }
            }

            boolean first = true;
            Coords prev = null;
            Coords openSlot = null;
            // ODD number check
            if ((slotsNumber % 2) != 0) {
              LOGGER.info("ODD NUMBER OF SLOTS");
              for (int row = 1; row <= mrows && first; row++) {
                for (int col = 1; col <= mcols && first; col++) {
                  Slot slot = matrix.get(new Coords(row, col));

                  if (!slot.active) {
                    // check is flipped one
                    Rectangle cornerArea = new Rectangle(slot.area);
                    cornerArea.x -= 20;
                    cornerArea.y -= 20;
                    cornerArea.width = 10;
                    cornerArea.height = 10;

                    Pixel ppp = _pairsComparator.findImage(scanner.getImageData("TLCorner.bmp").getImage(),
                        scanSlot(cornerArea), Color.RED);
                    if (ppp != null) {
                      // found it
                      slot.active = true;
                      slotsNumber++;
                      first = false;
                      prev = slot.coords;
                      openSlot = slot.coords;
                      break;
                    }
                  }
                }
              }
            }

            LOGGER.info("Slots: " + slotsNumber);

            if (slotsNumber > 0) {
              t.start();

              for (int row = 1; row <= mrows; row++) {
                for (int col = 1; col <= mcols; col++) {
                  Coords coords = new Coords(row, col);
                  Slot slot = matrix.get(coords);
                  if (openSlot != null && coords.equals(openSlot)) {
                    LOGGER.info("openslot...");

                  } else {
                    if (slot.active) {
                      mouse.click(slot.area.x, slot.area.y);
                      if (first) {
                        prev = coords;
                        mouse.delay(140);
                      } else {
                        mouse.delay(600);
                        slot.image = scanSlot(slot.area);
                        Slot prevSlot = matrix.get(prev);
                        prevSlot.image = scanSlot(prevSlot.area);
                        // if (sameImage(prevSlot.image, slot.image)) {
                        // // we have match, so remove both
                        // prevSlot.image = null;
                        // slot.image = null;
                        // LOGGER.info("UH OH! Time is ticking now");
                        // time = System.currentTimeMillis();
                        // }

                        if (time != 0) {
                          if (System.currentTimeMillis() - time > 3000) {

                            LOGGER.info("click something ... " + (System.currentTimeMillis() - time));
                            mouse.delay(400);
                            clickMatches(mcols, mrows, matrix, 1);
                            time = System.currentTimeMillis();
                            mouse.delay(200);

                          } else {
                            LOGGER.info("wait..." + (System.currentTimeMillis() - time));
                          }
                        }

                      }
                      // scanner.writeImage(slot.image, "slot_flipped" +
                      // slot.coords.toString() + ".jpg");

                      first = !first;
                      // mouse.delay(150);
                    }
                  }
                }
              }
              done = true;
              time = 0l;

              // now find matches
              clickMatches(mcols, mrows, matrix, -1);

              mouse.delay(2000);
            }
          } catch (Exception e) {
            LOGGER.info("WHAT HAPPENED? " + e.getMessage());
          }
          // END OF CYCLE
        } while (slotsNumber > 0);

      }

      return started;
    }

    private boolean clickMatches(int mcols, int mrows, Map<Coords, Slot> matrix, int maxClicks)
        throws RobotInterruptedException, AWTException {
      int clicks = 0;
      for (int row1 = 1; row1 <= mrows; row1++) {
        for (int col1 = 1; col1 <= mcols; col1++) {

          for (int row2 = 1; row2 <= mrows; row2++) {
            for (int col2 = 1; col2 <= mcols; col2++) {

              Coords c1 = new Coords(row1, col1);
              Coords c2 = new Coords(row2, col2);
              // System.err.println(c1 + " - " + c2);
              if (c1.equals(c2)) {
                // System.err.println("SKIP");
              } else {
                if (maxClicks < 0 || clicks < maxClicks) {
                  LOGGER.fine("CLICK PAIRS: " + c1 + " and " + c2);
                  Slot slot1 = matrix.get(c1);
                  Slot slot2 = matrix.get(c2);
                  if (slot1.active && slot2.active && sameImage(slot1.image, slot2.image)) {
                    clicks++;
                    mouse.click(slot1.area.x, slot1.area.y);
                    mouse.delay(200);
                    mouse.click(slot2.area.x, slot2.area.y);
                    mouse.delay(500);
                    slot1.active = false;
                    slot2.active = false;

                    if (maxClicks > 0 && clicks == maxClicks)
                      return true;
                  }
                }
              }
            }
          }
        }
      }
      return clicks > 0;
    }

  }

  private static class Slot {
    Coords coords;
    BufferedImage image;
    boolean active;
    Rectangle area;

    public Slot(int row, int col, boolean active) {
      super();
      coords = new Coords(row, col);
      this.active = active;
      this.image = null;
    }

  }

  private static class Coords {
    int row;
    int col;

    public Coords(int row, int col) {
      this.row = row;
      this.col = col;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + col;
      result = prime * result + row;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Coords other = (Coords) obj;
      if (col != other.col)
        return false;
      if (row != other.row)
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[" + row + ", " + col + "]";
    }

  }

  private BufferedImage scanSlot(Rectangle slotArea) throws AWTException {
    BufferedImage slotImage = new Robot().createScreenCapture(slotArea);
    return slotImage;
  }

  private boolean sameImage(BufferedImage one, BufferedImage two) {
    Pixel pixel = _pairsComparator.findImage(one, two);
    return pixel != null;
  }

  /**
   * @deprecated
   */
  private void pairsTask() {
    pairsTask = new Task("Pairs", 1);
    pairsTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {
        try {
          // 1.
          mouse.delay(500);
          String minigame = "Pairs";// TODO make it settings

          Pixel p = scanner.scanOneFast("centerCourt.bmp", scanner._scanArea, false);
          // scanner.scanOneFast("practiceCourt.bmp", scanner._scanArea, true);
          // mouse.delay(4000);
          // Pixel p = scanner.scanOneFast("practiceArena.bmp",
          // scanner._fullArea, false);
          if (p != null) {
            mouse.click(p.x + 321, p.y - 61);
            mouse.delay(4000);
            p = scanner.scanOneFast("practiceArena.bmp", scanner._fullArea, false);
            if (p != null) {
              LOGGER.info("Practice arena...");
              Rectangle area = new Rectangle(p.x - 194, p.y + 129, 650, 34);
              // scanner.writeArea(area, "hmm.bmp");
              Pixel pq = scanner.scanOneFast(minigame + ".bmp", area, false);
              if (pq == null) {
                for (int i = 0; i < 6; i++) {
                  mouse.click(p.x - 214, p.y + 295);
                  mouse.delay(500);
                }
              }
              pq = scanner.scanOneFast(minigame + ".bmp", area, false);
              if (pq == null) {
                for (int i = 0; i < 6; i++) {
                  mouse.click(p.x + 476, p.y + 295);
                  mouse.delay(1400);
                  pq = scanner.scanOneFast(minigame + ".bmp", area, false);
                  if (pq != null)
                    break;
                }
              }
              if (pq != null) {
                // minigame visible
                Rectangle barea = new Rectangle(pq.x - 77, pq.y + 266, 204, 37);
                boolean playIt = settings.getBoolean("minigame.play", false);
                Pixel b = scanner.scanOneFast(playIt ? "Play.bmp" : "Simulate.bmp", barea, false);

                if (b != null) {
                  if (playIt) {
                    mouse.click(b);
                    LOGGER.info("Playing " + minigame);
                    mouse.delay(4000);
                    playPairs();
                  } else {
                    mouse.click(b);
                    LOGGER.info(minigame);
                    mouse.delay(4000);
                    p = scanner.scanOneFast("practiceArena.bmp", scanner._scanArea, false);
                  }

                  if (p != null) {
                    LOGGER.info("SUCCESS");
                    mouse.click(p.x + 500, p.y + 12);
                    mouse.delay(2000);
                  } else {
                    LOGGER.info("no energy");
                    LOGGER.info("sleep 5min");
                    sleep(5 * 60000);
                    handlePopups();
                  }
                }

              } else {
                LOGGER.info("Uh oh! Can't find " + minigame + "!");
                // handlePopups(false);
                refresh();
              }
            }
          }
        } catch (IOException | AWTException e) {
          e.printStackTrace();
        }

      }
    });
  }

  private void playPairs() {
    // it's supposed to have the game started...

  }

  private void setDefaultSettings() {
    settings.setProperty("tasks.balls", "true");
    settings.setProperty("tasks.practice", "true");
    settings.setProperty("tasks.pairs", "false");
    settings.setProperty("tasks.bank", "true");
    settings.setProperty("tasks.matches", "true");
    settings.setProperty("tasks.matches.sleep", "21");

    settings.saveSettingsSorted();
  }

  private void initLayout() {
    setTitle(APP_TITLE);

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setAlwaysOnTop(true);

    JPanel rootPanel = new JPanel(new BorderLayout());
    getContentPane().add(rootPanel, BorderLayout.CENTER);

    // CONSOLE
    rootPanel.add(buildConsole(), BorderLayout.CENTER);

    // TOOLBARS
    JToolBar mainToolbar1 = createToolbar1();
    JToolBar mainToolbar2 = createToolbar2();

    JPanel toolbars = new JPanel(new GridLayout(0, 1));
    toolbars.add(mainToolbar1);
    ///toolbars.add(mainToolbar2);

    Box north = Box.createVerticalBox();
    north.add(toolbars);
    //north.add(createStatsPanel());
    rootPanel.add(north, BorderLayout.NORTH);
  }

  private Map<String, JLabel> _labels = new HashMap<String, JLabel>();

  private Component createStatsPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    GridBagConstraints gbc2 = new GridBagConstraints();
    JLabel l;
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc2.gridx = 2;
    gbc2.gridy = 1;

    gbc.insets = new Insets(2, 2, 2, 2);
    gbc.anchor = GridBagConstraints.WEST;
    gbc2.insets = new Insets(2, 4, 2, 16);
    gbc2.anchor = GridBagConstraints.EAST;

    // Matches
    panel.add(new JLabel("Matches:"), gbc);
    l = new JLabel(" ");
    _labels.put("Matches", l);
    panel.add(l, gbc2);

    // Won
    gbc.gridx += 2;
    gbc2.gridx += 2;
    panel.add(new JLabel("Won:"), gbc);
    l = new JLabel(" ");
    _labels.put("Won", l);
    panel.add(l, gbc2);

    // Lost
    gbc.gridx += 2;
    gbc2.gridx += 2;
    panel.add(new JLabel("Lost:"), gbc);
    l = new JLabel(" ");
    _labels.put("Lost", l);
    panel.add(l, gbc2);

    // SECOND LINE
    gbc.gridy++;
    gbc2.gridy++;
    gbc.gridx = 1;
    gbc2.gridx = 2;

    // Pairs
    panel.add(new JLabel("Pairs:"), gbc);
    l = new JLabel(" ");
    _labels.put("Pairs", l);
    panel.add(l, gbc2);

    // Stars
    gbc.gridx += 2;
    gbc2.gridx += 2;
    panel.add(new JLabel("Balls:"), gbc);
    l = new JLabel(" ");
    _labels.put("Balls", l);
    panel.add(l, gbc2);

    // FAKE
    gbc2.gridx = 100;
    gbc2.gridy = 1;
    gbc2.weightx = 1.0f;
    gbc2.weighty = 1.0f;
    panel.add(new JLabel(""), gbc2);

    Iterator<String> i = _labels.keySet().iterator();
    while (i.hasNext()) {
      String key = (String) i.next();
      _labels.get(key).setText("" + 0);
    }

    stats.addPropertyChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("ALL")) {
          Iterator<String> i = _labels.keySet().iterator();
          while (i.hasNext()) {
            String key = (String) i.next();
            _labels.get(key).setText("" + 0);
          }
        } else {
          JLabel l = _labels.get(evt.getPropertyName());
          if (l != null)
            l.setText("" + evt.getNewValue());
        }
      }
    });

    return panel;
  }

  private JToggleButton _pingToggle;

  private JToggleButton _slowToggle;

  private JToggleButton _ballsToggle;
  private JToggleButton _practiceToggle;
  private JToggleButton _pairsToggle;
  private JToggleButton _matchesToggle;
  private JToggleButton _bankToggle;

  private CaptureDialog captureDialog;

  private Container buildConsole() {
    final JTextArea outputConsole = new JTextArea(8, 14);

    Handler handler = new Handler() {

      @Override
      public void publish(LogRecord record) {
        String text = outputConsole.getText();
        if (text.length() > 3000) {
          int ind = text.indexOf("\n", 2000);
          if (ind <= 0)
            ind = 2000;
          text = text.substring(ind);
          outputConsole.setText(text);
        }
        outputConsole.append(record.getMessage());
        outputConsole.append("\n");
        outputConsole.setCaretPosition(outputConsole.getDocument().getLength());
        // outputConsole.repaint();
      }

      @Override
      public void flush() {
        outputConsole.repaint();
      }

      @Override
      public void close() throws SecurityException {
        // do nothing

      }
    };
    LOGGER.addHandler(handler);

    return new JScrollPane(outputConsole);
  }

  private boolean isRunning(String threadName) {
    boolean isRunning = false;
    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
    for (Iterator<Thread> it = threadSet.iterator(); it.hasNext();) {
      Thread thread = it.next();
      if (thread.getName().equals(threadName)) {
        isRunning = true;
        break;
      }
    }
    return isRunning;
  }

  private JToolBar createToolbar2() {
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);

    // Balls
    _ballsToggle = new JToggleButton("Balls");
    toolbar.add(_ballsToggle);
    _ballsToggle.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        boolean b = e.getStateChange() == ItemEvent.SELECTED;
        LOGGER.info("Balls: " + (b ? "on" : "off"));
        settings.setProperty("tasks.balls", "" + b);
        settings.saveSettingsSorted();

      }
    });

    // Practice
    _practiceToggle = new JToggleButton("Practice");
    toolbar.add(_practiceToggle);
    _practiceToggle.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        boolean b = e.getStateChange() == ItemEvent.SELECTED;
        LOGGER.info("Practice: " + (b ? "on" : "off"));
        settings.setProperty("tasks.practice", "" + b);
        settings.saveSettingsSorted();
      }
    });

    // Practice Pairs
    _pairsToggle = new JToggleButton("Pairs");
    toolbar.add(_pairsToggle);
    _pairsToggle.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        boolean b = e.getStateChange() == ItemEvent.SELECTED;
        LOGGER.info("Pairs: " + (b ? "on" : "off"));
        settings.setProperty("tasks.pairs", "" + b);
        settings.saveSettingsSorted();
      }
    });

    // Matches
    _matchesToggle = new JToggleButton("Matches");
    toolbar.add(_matchesToggle);

    _matchesToggle.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        boolean b = e.getStateChange() == ItemEvent.SELECTED;
        LOGGER.info("Matches: " + (b ? "on" : "off"));
        settings.setProperty("tasks.matches", "" + b);
        settings.saveSettingsSorted();

      }
    });

    // Bank
    _bankToggle = new JToggleButton("Bank");
    toolbar.add(_bankToggle);

    _bankToggle.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        boolean b = e.getStateChange() == ItemEvent.SELECTED;
        LOGGER.info("Bank: " + (b ? "on" : "off"));
        settings.setProperty("tasks.bank", "" + b);
        settings.saveSettingsSorted();

      }
    });
    return toolbar;
  }

  @SuppressWarnings("serial")
  private JToolBar createToolbar1() {
    JToolBar mainToolbar1 = new JToolBar();
    mainToolbar1.setFloatable(false);

//    // SCAN
//    {
//      AbstractAction action = new AbstractAction("Scan") {
//        public void actionPerformed(ActionEvent e) {
//          Thread myThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//              try {
//                scan();
//              } catch (RobotInterruptedException e) {
//                e.printStackTrace();
//              }
//            }
//          });
//
//          myThread.start();
//        }
//      };
//      mainToolbar1.add(action);
//    }
//    // RUN MAGIC
//    {
//      AbstractAction action = new AbstractAction("Do magic") {
//
//        public void actionPerformed(ActionEvent e) {
//          runMagic();
//        }
//
//      };
//      mainToolbar1.add(action);
//    }

    // Pairs
    {

      AbstractAction action = new AbstractAction("Do Pairs") {
        public void actionPerformed(ActionEvent e) {
          Thread t = new Thread(new Runnable() {
            public void run() {
              try {
                new PairsProtocol().doPairs();
              } catch (RobotInterruptedException | IOException | AWTException e1) {
              }

            }
          });
          t.start();
        }

      };
      mainToolbar1.add(action);
    }

//    // Reset
//    {
//      AbstractAction action = new AbstractAction("Reset") {
//        public void actionPerformed(ActionEvent e) {
//          Thread t = new Thread(new Runnable() {
//            public void run() {
//              ((AbstractGameProtocol) ballTask.getProtocol()).reset();
//              stats.clear();
//              taskManager.updateAll();
//            }
//          });
//          t.start();
//        }
//
//      };
//      mainToolbar1.add(action);
//
//    }
    // // STOP MAGIC
    // {
    // AbstractAction action = new AbstractAction("Stop") {
    // public void actionPerformed(ActionEvent e) {
    // Thread myThread = new Thread(new Runnable() {
    //
    // @Override
    // public void run() {
    // LOGGER.info("Stopping BB Gun");
    // _stopAllThreads = true;
    // }
    // });
    //
    // myThread.start();
    // }
    // };
    // mainToolbar1.add(action);
    // }

    return mainToolbar1;
  }

  private void setupLogger() {
    try {
      MyLogger.setup();
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Problems with creating the log files");
    }
  }

  @SuppressWarnings("serial")
  private static class CaptureDialog extends JFrame {
    Point _startPoint;
    Point _endPoint;
    Rectangle _rect;
    boolean inDrag;

    public CaptureDialog() {
      super("hmm");
      setUndecorated(true);
      getRootPane().setOpaque(false);
      getContentPane().setBackground(new Color(0, 0, 0, 0.05f));
      setBackground(new Color(0, 0, 0, 0.05f));

      _startPoint = null;
      _endPoint = null;
      inDrag = false;

      // events

      addMouseListener(new MouseAdapter() {

        @Override
        public void mousePressed(MouseEvent e) {
          inDrag = true;

        }

        @Override
        public void mouseClicked(MouseEvent e) {

          if (e.getButton() == MouseEvent.BUTTON1) {
            if (_startPoint == null) {
              LOGGER.info("clicked once " + e.getButton() + " (" + e.getX() + ", " + e.getY() + ")");
              _startPoint = e.getPoint();
              repaint();
            } else {
              _endPoint = e.getPoint();
              // LOGGER.info("clicked twice " + e.getButton() +
              // " (" + e.getX() + ", " + e.getY() + ")");
              setVisible(false);
              LOGGER.info("AREA: " + _rect);
            }
          } else if (e.getButton() == MouseEvent.BUTTON3) {
            _startPoint = null;
            _endPoint = null;
            repaint();
          }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          // LOGGER.info("REL:"+e);

          if (inDrag && _endPoint != null && _startPoint != null) {
            // LOGGER.info("end of drag " + e.getButton() + " (" +
            // e.getX() + ", " + e.getY() + ")");
            inDrag = false;
            setVisible(false);
            LOGGER.info("AREA: " + _rect);
            // HMM
            dispose();
          }

        }

      });

      addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
          // LOGGER.info("move " + e.getPoint());
          _endPoint = e.getPoint();
          repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
          if (_startPoint == null) {
            _startPoint = e.getPoint();
          }
          _endPoint = e.getPoint();
          repaint();
          // LOGGER.info("DRAG:" + e);
        }

      });

    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);
      if (_startPoint != null && _endPoint != null) {
        g.setColor(Color.RED);
        int x = Math.min(_startPoint.x, _endPoint.x);
        int y = Math.min(_startPoint.y, _endPoint.y);
        int w = Math.abs(_startPoint.x - _endPoint.x);
        int h = Math.abs(_startPoint.y - _endPoint.y);
        _rect = new Rectangle(x, y, w, h);

        g.drawRect(x, y, w, h);

        // g.setColor(Color.GRAY);
        // g.drawString("[" + w + ", " + h + "]", w / 2 - 13, h / 2 -
        // 3);
        g.setColor(Color.RED);
        g.drawString(x + ", " + y + ", [" + w + ", " + h + "]", x + 3, y + 13);
      }
    }
  }

  private void scan() throws RobotInterruptedException {
    try {
      mouse.savePosition();
      scanner.reset();
      LOGGER.info("Scanning...");
      setTitle(APP_TITLE + " ...");

      boolean found = scanner.locateGameArea(false);
      if (found) {
        // scanner.checkAndAdjustRock();
        // _mapManager.update();
        // _buildingManager.update();

        LOGGER.info("Coordinates: " + scanner.getTopLeft() + " - " + scanner.getBottomRight());

        LOGGER.info("GAME FOUND! TM READY!");
        setTitle(APP_TITLE + " READY");

        mouse.restorePosition();

      } else {
        LOGGER.info("CAN'T FIND THE GAME!");
        setTitle(APP_TITLE);
      }
    } catch (Exception e1) {
      LOGGER.log(Level.WARNING, e1.getMessage());
      e1.printStackTrace();
    }

  }

  private void loadStats() {
    // try {
    //
    // Iterator<String> i = _labels.keySet().iterator();
    // while (i.hasNext()) {
    // String key = (String) i.next();
    // _labels.get(key).setText("" + 0);
    // }
    //
    // List<DispatchEntry> des = new JsonStorage().loadDispatchEntries();
    // for (DispatchEntry de : des) {
    // JLabel l = _labels.get(de.getDest());
    // if (l != null) {
    // l.setText("" + (Integer.parseInt(l.getText()) + de.getTimes()));
    // }
    //
    // }
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
  }

  private Rectangle generateMiniArea(Pixel p) {
    return new Rectangle(p.x - 2 - 18, p.y - 50 + 35, 44, 60);
  }

  public MainFrame2(boolean isTestmode) throws HeadlessException, AWTException {
    super();

    // _testMode = isTestmode;
    setupLogger();
    init();
  }

  private void doMagic() {
    assert scanner.isOptimized();
    setTitle(APP_TITLE + " RUNNING");
    stopAllThreads = false;
    try {
      mouse.saveCurrentPosition();
      _fstart = System.currentTimeMillis();
      do {
        taskManager.executeAll();
        // ballTask.execute();
        handlePopups();
        // practiceTask.execute();
        // matchTask.execute();
        // handlePopups(false);
        LOGGER.info("jobs done. wait 5 secs");
        mouse.delay(5000);
      } while (!stopAllThreads);

    } catch (RobotInterruptedException e) {
      LOGGER.info("INTERRUPTED!");
      setTitle(APP_TITLE);
    } catch (GameErrorException e) {
      LOGGER.info("CRITICAL ERROR: " + e.getCode() + ". " + e.getMessage());
    }
  }

  private void runOnce() throws RobotInterruptedException {
    long mandatoryRefresh = settings.getInt("autoRefresh.mandatoryRefresh", 45) * 60 * 1000;
    long now = System.currentTimeMillis();
    mouse.checkUserMovement();

    // TODO

    mouse.checkUserMovement();

    // // 1. SCAN
    handlePopups();

    mouse.mouseMove(scanner.getParkingPoint());

    mouse.delay(200);
  }

  private void refresh() {
    LOGGER.info("refresh...");
    mouse.click(scanner.getParkingPoint());
    try {
      Robot robot = new Robot();
      robot.keyPress(KeyEvent.VK_F5);
      robot.keyRelease(KeyEvent.VK_F5);
    } catch (AWTException e) {
    }

    try {
      Thread.sleep(8000);
    } catch (InterruptedException e) {
    }
    LOGGER.info("refresh done");

  }

  private void refresh(boolean bookmark) throws AWTException, IOException, RobotInterruptedException {
    deleteOlder(".", "refresh", 5, -1);
    LOGGER.info("Time to refresh...");
    scanner.captureGameArea("refresh ");
    Pixel p;
    if (!bookmark) {
      if (scanner.isOptimized()) {
        p = scanner.getBottomRight();
        p.y += 4;
        p.x -= 4;
      } else {
        p = new Pixel(0, 510);
      }
      mouse.click(p.x, p.y);
      try {
        Robot robot = new Robot();
        robot.keyPress(KeyEvent.VK_F5);
        robot.keyRelease(KeyEvent.VK_F5);
      } catch (AWTException e) {
      }
      try {
        Thread.sleep(15000);
      } catch (InterruptedException e) {
      }
      // scanner.reset();

      boolean done = false;
      for (int i = 0; i < 17 && !done; i++) {
        LOGGER.info("after refresh recovery try " + (i + 1));

        handlePopups();
        mouse.delay(200);
        handlePopups();
        mouse.delay(200);
        handlePopups();
        mouse.delay(200);
        // LOCATE THE GAME
        if (scanner.locateGameArea(false) && !scanner.isWide()) {
          LOGGER.info("Game located successfully!");
          done = true;
        } else {
          processRequests();
        }
        if (i > 8) {
          captureScreen("refresh trouble ");
        }
      }
      if (done) {
        // runMagic();
        captureScreen("refresh done ");
      } else {
        // blah
        // try bookmark
      }

      // not sure why shipsTasks gets off after refresh
      reapplySettings();

    } else {
      // try {
      // p = scanner.generateImageData("tsFavicon2.bmp", 8, 7).findImage(new
      // Rectangle(0, 30, 400, 200));
      // _mouse.click(p.x, p.y);
      // } catch (IOException e) {
      // }
    }
  }

  private Long _lastPing = System.currentTimeMillis();

  private void ping() {
    if (System.currentTimeMillis() - _lastPing > settings.getInt("ping.time", 120) * 1000) {
      captureScreen(null);
      _lastPing = System.currentTimeMillis();
    }

  }

  private Long _speedTime = null;

  private long _fstart;

  private void handlePopups() throws RobotInterruptedException {
    boolean found = false;
    try {
      LOGGER.info("Popups...");
      Pixel p = scanner.scanOneFast("x.bmp", scanner._scanArea, false);
      if (p != null) {
        found = true;
        mouse.click(p.x + 16, p.y + 16);
        mouse.delay(200);
      }
      found = found || scanner.scanOneFast("Continue.bmp", scanner._scanArea, true) != null;
      found = found || scanner.scanOneFast("ContinueBrown.bmp", scanner._scanArea, true) != null;
      if (!found) {
        p = scanner.scanOneFast("grandPrize.bmp", scanner._scanArea, true);
        if (p != null) {
          handleAwards();
          mouse.delay(1000);
          p = scanner.scanOneFast("x.bmp", scanner._scanArea, true);
          found = true;
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    } catch (AWTException e) {
      e.printStackTrace();
    }

  }

  private void handleAwards() {
    try {
      mouse.delay(4000);
      Pixel p = scanner.scanOneFast("ballReward.bmp", scanner._scanArea, false);
      if (p != null) {
        mouse.click(p);
        mouse.delay(3500);
        mouse.click(p.x + 218, p.y);
        mouse.delay(3500);
        captureScreen("ping/awards ");
        deleteOlder("ping", "awards", -1, 24);
        mouse.click(p.x + 218, p.y + 146); // pick up
        mouse.delay(3500);
      }
    } catch (RobotInterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (AWTException e) {
      e.printStackTrace();
    }
  }

  private void reapplySettings() {

    // toggles

    // boolean popups =
    // "true".equalsIgnoreCase(_settings.getProperty("popups"));
    // if (popups != _popupsToggle.isSelected()) {
    // _popupsToggle.setSelected(popups);
    // }
    //
    // boolean gates = "true".equalsIgnoreCase(_settings.getProperty("gates"));
    // if (gates != _gatesToggle.isSelected()) {
    // _gatesToggle.setSelected(gates);
    // }
    //
    // boolean slow = "true".equalsIgnoreCase(_settings.getProperty("slow"));
    // if (slow != _slowToggle.isSelected()) {
    // _slowToggle.setSelected(slow);
    // }
    //
    // boolean ping = "true".equalsIgnoreCase(_settings.getProperty("ping"));
    // if (ping != _pingToggle.isSelected()) {
    // _pingToggle.setSelected(ping);
    // }
    //
    // boolean ping2 = "true".equalsIgnoreCase(_settings.getProperty("ping2"));
    // if (ping2 != _ping2Toggle.isSelected()) {
    // _ping2Toggle.setSelected(ping2);
    // }
    //
    boolean balls = "true".equalsIgnoreCase(settings.getProperty("tasks.balls"));
    if (balls != _ballsToggle.isSelected()) {
      _ballsToggle.setSelected(balls);
    }

    boolean practice = "true".equalsIgnoreCase(settings.getProperty("tasks.practice"));
    if (practice != _practiceToggle.isSelected()) {
      _practiceToggle.setSelected(practice);
    }

    boolean pairs = "true".equalsIgnoreCase(settings.getProperty("tasks.pairs"));
    if (pairs != _pairsToggle.isSelected()) {
      _pairsToggle.setSelected(pairs);
    }

    boolean matches = "true".equalsIgnoreCase(settings.getProperty("tasks.matches"));
    if (matches != _matchesToggle.isSelected()) {
      _matchesToggle.setSelected(matches);
    }

    boolean bank = "true".equalsIgnoreCase(settings.getProperty("tasks.bank"));
    if (bank != _bankToggle.isSelected()) {
      _bankToggle.setSelected(bank);
    }

    //
    // boolean ar =
    // "true".equalsIgnoreCase(_settings.getProperty("autoRefresh"));
    // if (ar != _autoRefreshToggle.isSelected()) {
    // _autoRefreshToggle.setSelected(ar);
    // }
    //
    // // agenda
    // String ag = _settings.getProperty("agenda", "DEFAULT");
    // if (!ag.equals(_agenda != null ? _agenda.getName() : "")) {
    // setAgenda(ag);
    // }

  }

  private void stopMagic() {
    stopAllThreads = true;
    LOGGER.info("Stopping...");
    int tries = 10;
    boolean stillRunning = true;
    mouse.mouseMove(scanner.getSafePoint());
    for (int i = 0; i < tries && stillRunning; ++i) {
      stillRunning = isRunning("MAGIC");
      if (stillRunning) {
        LOGGER.info("Magic still working...");
        try {
          Thread.sleep(5000);
          // for (int j = 0; j < 150; j++) {
          // _mouse.mouseMove(scanner.getSafePoint());
          // Thread.sleep(100);
          // }
        } catch (InterruptedException e) {
        }
      } else {
        LOGGER.info("MAGIC STOPPED");
        setTitle(APP_TITLE);
      }
    }
    stopAllThreads = false;
  }

  private void processRequests() {
    Service service = new Service();

    String[] requests = service.getActiveRequests();
    for (String r : requests) {

      if (r.startsWith("stop")) {
        service.inProgress(r);
        stopMagic();
        captureScreen(null);

      } else if (r.startsWith("run") || r.startsWith("start")) {
        service.inProgress(r);
        stopMagic();
        runMagic();
        captureScreen(null);

      } else if (r.startsWith("agenda")) {
        service.inProgress(r);
        stopMagic();
        // doAgenda();
        captureScreen(null);
      } else if (r.startsWith("click")) {
        service.inProgress(r);
        processClick(r);

      } else if (r.startsWith("refresh")) {
        service.inProgress(r);
        try {
          stopMagic();
          refresh(false);
        } catch (AWTException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        } catch (RobotInterruptedException e) {
          e.printStackTrace();
        }

      } else if (r.startsWith("ping") || r.startsWith("p")) {
        service.inProgress(r);
        LOGGER.info("Ping...");
        captureScreen(null);
        service.done(r);
      }
    }

    // service.purgeOld(1000 * 60 * 60);// 1 hour old
  }

  private void processClick(String r) {
    try {
      String[] ss = r.split("_");
      int x = Integer.parseInt(ss[1]);
      int y = Integer.parseInt(ss[2]);
      mouse.click(x, y);
      try {
        mouse.delay(1000);
      } catch (RobotInterruptedException e) {
      }
    } finally {
      new Service().done(r);
    }
  }

  private void runSettingsListener() {
    Thread requestsThread = new Thread(new Runnable() {
      public void run() {
        // new Service().purgeAll();
        boolean stop = false;
        do {
          // LOGGER.info("......");
          try {
            settings.loadSettings();
            reapplySettings();
            processRequests();
          } catch (Throwable t) {
            // hmm
            t.printStackTrace();
          }
          try {
            Thread.sleep(20000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }

        } while (!stop);
      }

    }, "REQUESTS");

    requestsThread.start();

  }

  private void deleteOlder(String folder, String prefix, int amountFiles, int hours) {
    File f = new File(folder);
    File[] files = f.listFiles();
    List<File> targetFiles = new ArrayList<File>(6);
    int cnt = 0;
    for (File file : files) {
      if (!file.isDirectory() && file.getName().startsWith(prefix)) {
        if (hours < 0 || (hours > 0 && System.currentTimeMillis() - file.lastModified() > hours * 60 * 60000)) {
          targetFiles.add(file);
          cnt++;
        }
      }
    }

    if (amountFiles > 0 && cnt > amountFiles) {
      // sort them before delete them
      Collections.sort(targetFiles, new Comparator<File>() {
        public int compare(File o1, File o2) {
          if (o1.lastModified() > o2.lastModified())
            return 1;
          else if (o1.lastModified() < o2.lastModified())
            return -1;
          return 0;
        };
      });

      int c = cnt - 5;
      // delete some files
      for (int i = 0; i < c; i++) {
        File fd = targetFiles.get(i);
        fd.delete();
      }

    } else if (amountFiles < 0) {
      // delete all
      for (int i = 0; i < targetFiles.size(); i++) {
        File fd = targetFiles.get(i);
        fd.delete();
      }
    }
  }

  private void captureScreen(String filename) {
    captureArea(null, filename);
  }

  private void captureArea(Rectangle area, String filename) {
    if (filename == null)
      filename = "ping ";
    if (area == null) {
      final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      area = new Rectangle(0, 0, screenSize.width, screenSize.height);
    }
    writeImage(area, filename + DateUtils.formatDateForFile(System.currentTimeMillis()) + ".jpg");
    deleteOlder(".", "ping", 8, -1);
  }

  public void writeImage(Rectangle rect, String filename) {
    try {
      writeImage(new Robot().createScreenCapture(rect), filename);
    } catch (AWTException e) {
      e.printStackTrace();
    }
  }

  public void writeImage(BufferedImage image, String filename) {

    try {
      int ind = filename.lastIndexOf("/");
      if (ind > 0) {
        String path = filename.substring(0, ind);
        File f = new File(path);
        f.mkdirs();
      }
      File file = new File(filename);
      MyImageIO.write(image, filename.substring(filename.length() - 3).toUpperCase(), file);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void runMagic() {
    Thread myThread = new Thread(new Runnable() {
      @Override
      public void run() {
        LOGGER.info("Let's get rolling...");

        if (!scanner.isOptimized()) {
          try {
            scan();
          } catch (RobotInterruptedException e) {
            e.printStackTrace();
          }
        }

        if (scanner.isOptimized()) {
          // DO THE JOB
          doMagic();
        } else {
          LOGGER.info("I need to know where the game is!");
        }
      }
    }, "MAGIC");

    myThread.start();
  }

  private void clickBankDirectly() throws RobotInterruptedException, IOException, AWTException {
    mouse.click(scanner.getBottomRight().x - 84, scanner.getTopLeft().y + 68);
    mouse.delay(1500);
    Pixel p = scanner.scanOneFast("Finances.bmp", scanner._scanArea, false);
    if (p != null) {
      mouse.click(p.x - 194, p.y + 368);
      mouse.delay(1500);
    }
    handlePopups();
  }

}