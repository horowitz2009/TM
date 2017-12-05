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
import java.util.Calendar;
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
import com.horowitz.ocr.OCRe;

public class MainFrame extends JFrame {

  private final static Logger LOGGER = Logger.getLogger("MAIN");
  private final static boolean SIMPLE = false;

  private static String APP_TITLE = "TM v47";

  private MouseRobot mouse;

  private Settings settings;

  private ScreenScanner scanner;
  private SimilarityImageComparator _pairsComparator;

  private boolean stopAllThreads;

  private JToggleButton _pingToggle;

  private JToggleButton _slowToggle;

  private JToggleButton _ballsToggle;
  private JToggleButton _sponsorsToggle;
  private JToggleButton _practiceToggle;
  private JToggleButton _pairsToggle;
  private JToggleButton _matchesToggle;
  private JToggleButton _bankToggle;
  private JToggleButton _clubToggle;
  private JToggleButton _clubDuelsToggle;
  private JToggleButton _sfToggle;

  private Task practiceTask;
  private Task sponsorTask;
  private Task matchTask;
  private Task checkDuelsTask;
  private Task premiumTask;
  private Task ballTask;
  private Task bankTask;
  private Task pairsTask;
  private Task sfTask;
  private Task rankingsTask;

  private TaskManager taskManager;

  protected boolean duelsFull;
  protected boolean duelsChecked;
  private int duels;
  private int duelsLimit;

  private Stats stats;

  private Task clubTask;

  private OCRe ocrDuels;
  private OCRe ocrAttrs;

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
      MainFrame frame = new MainFrame(isTestmode);

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
      ocrDuels = new OCRe("ocr/duels/d");
      ocrDuels.getThreshold().setValue(160);

      ocrAttrs = new OCRe("ocr/attrs/s");
      ocrAttrs.getOcrb().setErrors(2);
      ocrAttrs.getThreshold().setValue(160);

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
      sfTask();
      rankingsTask();
      taskManager = new TaskManager(mouse);
      if (!SIMPLE) {
        taskManager.addTask(sponsorTask);
        taskManager.addTask(practiceTask);
        taskManager.addTask(pairsTask);
        taskManager.addTask(checkDuelsTask);
        taskManager.addTask(matchTask);
        taskManager.addTask(bankTask);
        taskManager.addTask(sponsorTask);

        taskManager.addTask(sfTask);
        taskManager.addTask(premiumTask);
        taskManager.addTask(sponsorTask);
        taskManager.addTask(ballTask);
        taskManager.addTask(clubTask);
        taskManager.addTask(rankingsTask);
      }
      stopAllThreads = false;

    } catch (Exception e1) {
      System.err.println("Something went wrong!");
      e1.printStackTrace();
      System.exit(1);
    }

    initLayout();

    reapplySettings();

    runSettingsListener();

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

  private long lastTime = 0;

  private void rankingsTask() {
    // rankingsWindow.bmp
    // rankingsCup.bmp
    rankingsTask = new Task("Rankings check", 1);
    rankingsTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {

        handlePopups();
        if (_bankToggle.isSelected())
          try {
            int x = scanner.getTopLeft().x + scanner.getGameWidth() / 2;
            int y = scanner.getTopLeft().y;
            Rectangle area = new Rectangle(x - 50, y, 170, 50);
            Pixel p = scanner.scanOneFast("rankingsCup.bmp", area, false);
            if (p != null) {
              mouse.click(p.x + 25, p.y + 10);
            } else {
              // click blindly
              mouse.click(x + 137, y + 24);
            }
            mouse.delay(3000);

            p = scanner.scanOneFast("rankingsWindow.bmp", scanner._scanArea, false);
            if (p != null) {
              // we're in the jazz
              LOGGER.info("rankings check...");

              // practice -> pairs
              mouse.click(p.x + 429, p.y - 104);
              mouse.delay(2000);
              p.x -= 97;
              p.y -= 176;
              mouse.click(p.x + 775, p.y + 172);
              mouse.delay(2000);

              captureScreen("ping/rankings pairs1 ");
              // click the scroller and capture again
              int x1 = p.x + 736;
              int y1 = p.y + 138;
              mouse.dragFast(x1, y1, x1, y1 + 240, false, false);
              mouse.delay(1000);
              captureScreen("ping/rankings pairs2 ");

              deleteOlder("ping", "rankings", -1, 24);
            }

            boolean itsTime = false;

            Calendar cal = Calendar.getInstance();
            // 15min before 4am
            int h = cal.get(Calendar.HOUR_OF_DAY);

            if (h == 3) {
              int m = cal.get(Calendar.MINUTE);
              if (m >= 44 && m <= 59)
                itsTime = true;
            }
            int mm = itsTime ? 1 : 15;
            LOGGER.info("rankings: sleep " + mm + " minutes");
            sleep(mm * 60000);

          } catch (AWTException e) {
          } catch (IOException e) {
          }

      }
    });
  }

  private void sfTask() {
    sfTask = new Task("Summer Fiesta", 1);
    sfTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {
        if (_sfToggle.isSelected()) {
          handlePopups();
          boolean found = false;

          try {
            mouse.delay(500);

            scanner.scanOneFast(scanner.getImageData("summerFiesta.bmp", scanner._scanArea, 0, 0), null, true);
            mouse.delay(3000);
            Pixel p = scanner.scanOneFast("summerFiestaTitle.bmp", scanner._scanArea, false);
            if (p != null) {
              // GOOOOOOOD!
              LOGGER.info("Summer Fiesta...");

              if (settings.getBoolean("tasks.sf.build", false)) {
                // look for build button
                if (lookForBuild(p)) {
                  LOGGER.info("Summer: just built something...");
                  captureScreen("ping/summer build ");
                  mouse.delay(3000);
                }
              }

              // drag to the end
              int x1 = p.x + 557;
              int y1 = p.y + 75;
              int step = 55;
              mouse.dragFast(x1, y1 + 0 * step, x1, y1 + (4 + 1) * step, false, false);
              mouse.delay(1000);
              step = 104;
              Rectangle area = new Rectangle(p.x + 207 - 550, p.y + 475 - 425, 555, 425);
              // scanner.writeAreaTS(area, "area.bmp");
              Rectangle area1 = new Rectangle(area);
              area1.y = area.y + area.height - 130;
              area1.height = 130;
              // 1
              LOGGER.info("sf 1");
              found = scanPlus(area1);
              if (!found) {
                area1.y -= 125;
                LOGGER.info("sf 2");
                found = scanPlus(area1);
              }

              if (!found) {
                area1.y -= 125;
                LOGGER.info("sf 3");
                found = scanPlus(area1);
              }
              if (!found) {
                // time for drag
                int i = 2;
                mouse.delay(1000);
                mouse.dragFast(x1, y1 + (i + 1) * step, x1, y1 + (i) * step, false, false);
                mouse.delay(1000);
                area1.y = area.y + area.height - 130;
                area1.height = 130;
                LOGGER.info("sf 4");
                found = scanPlus(area1);
                if (!found) {
                  area1.y -= 125;
                  LOGGER.info("sf 5");
                  found = scanPlus(area1);
                }

                if (!found) {
                  area1.y -= 125;
                  LOGGER.info("sf 6");
                  found = scanPlus(area1);
                }
              }

              deleteOlder("ping", "summer", -1, 24);

            }

          } catch (Exception e) {
            e.printStackTrace();
          }
          if (found)
            sleep(2 * 60000);// 2min
          else
            sleep(1 * 30000);// 30sec
        }
      }

      private boolean scanPlus(Rectangle area) throws RobotInterruptedException, AWTException, IOException {
        boolean found = false;
        if (settings.getBoolean("tasks.sf.writeArea", false))
          scanner.writeAreaTS(area, "area.bmp");
        Pixel pp = scanner.scanOneFast(scanner.getImageData("sfPlus.bmp", area, 0, 0), area, true);
        if (pp != null) {
          // look for OK button
          mouse.mouseMove(scanner.getParkingPoint());
          mouse.delay(1000);
          pp = scanner.scanOneFast(scanner.getImageData("sfOK.bmp", scanner._scanArea, 0, 0), scanner._scanArea, true);
          if (pp != null) {
            // we're done here
            LOGGER.info("Summer: started a part...");
            mouse.delay(3500);
            captureScreen("ping/summer part ");
            found = true;
          }
        }
        return found;
      }

      private boolean lookForBuild(Pixel p) throws RobotInterruptedException, AWTException, IOException {
        int x1 = p.x + 557;
        int y1 = p.y + 75;
        int step = 55;
        mouse.click(x1, y1);
        mouse.delay(1000);
        mouse.dragFast(x1, y1 + 0 * step, x1, y1 + (4 + 1) * step, false, false);
        mouse.delay(1000);
        step = 104;
        boolean found = false;
        for (int i = 2; !found && i >= 1; i--) {
          // Rectangle area = new Rectangle(p.x + 207 - 550, p.y + 475 - 425,
          // 555, 425);
          // Rectangle area1 = new Rectangle(area);
          // nt col = 4;
          // area1.x = area.x + col * 110;
          // area1.width = 110;
          {// for (int j = 4; !found && j >= 0; j--) {
           // area1.x = area.x + j * 110;
           // area1.width = 110;
           // scanner.writeAreaTS(scanner._scanArea, "area" + j + i + ".bmp");
           // System.err.println("area" + j + i + ".bmp");
            Pixel pp = scanner.scanOne(scanner.getImageData("sfBuild.bmp", scanner._scanArea, 0, 0), null, true);
            if (pp != null) {
              // look for OK button
              mouse.mouseMove(scanner.getParkingPoint());
              mouse.delay(1000);
              String sfButton = settings.getBoolean("tasks.sf.toClub", true) ? "sfToClub.bmp" : "sfToSelf.bmp";
              pp = scanner.scanOneFast(scanner.getImageData(sfButton, scanner._scanArea, 0, 0), null, true);
              if (pp != null) {
                // we're done here
                mouse.delay(1000);
                found = true;
                break;
              }
            }
          }
          if (!found) {
            mouse.delay(1000);
            mouse.dragFast(x1, y1 + (i + 1) * step, x1, y1 + (i) * step, false, false);
            mouse.delay(1000);
          }
        }
        return found;
      }
    });
  }

  private void checkDuelsTask() {
    checkDuelsTask = new Task("Check Duels", 1);
    checkDuelsTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {
        if (_matchesToggle.isSelected()) {
          handlePopups();

          try {
            mouse.delay(500);
            // check duels here
            int x = scanner.getTopLeft().x + scanner.getGameWidth() / 2;
            int y = scanner.getTopLeft().y + 20;
            Rectangle area = new Rectangle(x - 170, y - 20, 170 * 2, 40);
            // scanner.writeAreaTS(area, "racketArea.bmp");
            Pixel p = scanner.scanOneFast("racketAnchor2.bmp", area, false);
            if (p != null) {
              x = p.x + 63;
              y = p.y + 11;
            } else {
              LOGGER.info("racket not found...");
              if (settings.getBoolean("pro", false)) {
                x -= 70;
              }
            }
            mouse.mouseMove(x, y);

            area = new Rectangle(x - 52, y - 8, 80, 13);
            // scanner.writeArea(area, "duels.bmp");
            duelsChecked = false;
            duelsFull = false;
            if (settings.getBoolean("duels.ocr.capture", false)) {
              if (lastTime == 0 || System.currentTimeMillis() - lastTime > 4 * 60000) {
                scanner.writeAreaTS(area, "duels.bmp");
                lastTime = System.currentTimeMillis();
              }
            }

            if (settings.getBoolean("duels.ocr", false)) {

              try {
                String d = ocrDuels.scanImage(new Robot().createScreenCapture(area));
                LOGGER.info("duels: [" + d + "]");
                String[] dd = d.split("/");
                if (dd.length == 2) {
                  int dcurrent = Integer.parseInt(dd[0]);
                  int dmax = Integer.parseInt(dd[1]);
                  LOGGER.info("Duels parsed: " + dcurrent + " / " + dmax);
                  duels = dcurrent;
                  duelsLimit = dmax;
                  duelsChecked = true;
                }

              } catch (Exception e) {
              }
            }

            if (!duelsChecked) {

              // //
              // // public Pixel scanOne(ImageData imageData, Rectangle area,
              // // boolean
              // // click, Color colorToBypass, boolean bwMode)
              // // public Pixel scanOneFast(ImageData imageData, Rectangle
              // area,
              // // boolean click, boolean bwMode) throws AWTException,
              // int dmin = 18;
              // int dmax = 21;
              // int duel = dmin;
              // boolean found = false;
              //
              // for (; duel <= dmax; duel++) {
              // Pixel ppp = scanner.scanOneFast("duels" + duel + ".bmp", area,
              // false, null, true, false);
              // if (ppp != null) {
              // found = true;
              // break;
              // }
              // }
              // if (found) {
              // LOGGER.info("DUELS " + duel);
              // duelsFull = true;
              // ((AbstractGameProtocol) matchTask.getProtocol()).sleep(0);
              // return;// skip the rest
              // }
              mouse.delay(2000);
              LOGGER.info("check duels...");
              p = scanner.scanOneFast("DuelsClock.bmp", new Rectangle(x - 70, y, 140, 67), false);
              if (p == null) {
                LOGGER.info("DUELS FULL");
                duelsFull = true;
                ((AbstractGameProtocol) matchTask.getProtocol()).sleep(0);
              } else
                duelsFull = false;
              mouse.delay(100);
            }
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

            try {
              checkForPrizes("Matches");
            } catch (Exception e) {
              LOGGER.info("Error doing grand prizes!");
              e.printStackTrace();
            }

            do {
              scanner.scanOneFast(scanner.getImageData("centerCourt.bmp", scanner._scanArea, -100, 135), null, true);
              mouse.delay(3000);
              Pixel p = scanner.scanOneFast("centerCourtTitle.bmp", scanner._scanArea, false);
              if (p != null) {
                p.x += 95;
                p.y -= 31;
              } else {
                p = scanner.scanOneFast("centerCourtVS.bmp", scanner._scanArea, false);
                if (p != null) {
                  p.x -= 21;
                  p.y -= 248;
                }
              }
              if (p != null) {
                LOGGER.info("entered center court...");
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
                      pv.x += 115;
                      pv.y -= 201;
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

                    Rectangle area = scanner._scanArea;
                    if (pv != null)
                      area = new Rectangle(pv.x, pv.y + 380, 280, 45);
                    
                    int turns = 0;
                    int solution = 0;
                    do {
                      p = scanner.scanOneFast("Continue.bmp", area, false);
                      if (p != null) {
                        mouse.click(p.x + 13, p.y + 13);
                        solution = 1;
                      } else {
                        p = scanner.scanOneFast("grandPrizeMIN.bmp", area, false);
                        if (p != null) {
                          mouse.click(p.x + 13, p.y + 13);
                          solution = 2;
                        }
                      }
                      if (solution == 0)
                        mouse.delay(333);
                      
                      turns++;
                    } while (turns < 3 && solution == 0);
                    
                    if (solution == 2) {
                      //awards
                      handleAwards();
                      clickBankDirectly();
                    } else if (solution == 1) {
                      clickBankDirectly();
                    } else {
                      //damn it! no button found 
                      mouse.click(pv.x + 129, pv.y + 410);
                      mouse.delay(300);
                      handleAwards();
                      clickBankDirectly();
                    }
                  
                    handlePopups();
                    mouse.delay(1000);
                    // clickBankDirectly();

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

      private Attributes scanAttrs(Rectangle slotArea) throws AWTException {
        Rectangle area1 = new Rectangle(slotArea);
        area1.x += 32;
        float third = area1.height / 3;
        area1.height = (int) third;
        area1.width -= 32;

        String s1 = ocrAttrs.scanImage(new Robot().createScreenCapture(area1));

        area1.y += third;
        String s2 = ocrAttrs.scanImage(new Robot().createScreenCapture(area1));

        // 2/3 down
        area1.y = (int) (slotArea.y + (2 * third));
        String s3 = ocrAttrs.scanImage(new Robot().createScreenCapture(area1));
        int a1 = 0;
        int a2 = 0;
        int a3 = 0;
        try {
          a1 = Integer.parseInt(s1);
        } catch (NumberFormatException e) {
          // TODO log something
        }
        try {
          a2 = Integer.parseInt(s2);
        } catch (NumberFormatException e) {
          // TODO log something
        }
        try {
          a3 = Integer.parseInt(s3);
        } catch (NumberFormatException e) {
          // TODO log something
        }

        return new Attributes(a1, a2, a3);
      }

      private int getBestOpponent(List<Attributes> attrs) {
        int min = Integer.MAX_VALUE;
        int index = -1;
        for (Attributes a : attrs) {
          int sum = a.sum();
          if (sum < min) {
            index++;
            min = sum;
          }
        }
        return index;
      }

      private int clickMatch(Pixel p) throws RobotInterruptedException, IOException, AWTException {
        mouse.delay(3000);

        Rectangle[] slotArea = new Rectangle[] { new Rectangle(p.x + 297, p.y + 163, 74, 70),
            new Rectangle(p.x + 297, p.y + 163 + 102, 74, 70), new Rectangle(p.x + 297, p.y + 163 + 204, 74, 70) };

        List<Attributes> attrs = new ArrayList<>();
        attrs.add(scanAttrs(slotArea[0]));
        attrs.add(scanAttrs(slotArea[1]));
        attrs.add(scanAttrs(slotArea[2]));
        LOGGER.info("A1: " + attrs.get(0));
        LOGGER.info("A2: " + attrs.get(1));
        LOGGER.info("A3: " + attrs.get(2));
        Pixel pp = null;
        int slot = -1;
        if (duelsFull) {
          // play the most easiest
          slot = getBestOpponent(attrs);

        } else {
          slot = -1;
          // play only if player is weak enough
          Rectangle myArea = new Rectangle(p.x - 69, p.y + 350, 74, 84);
          Attributes myAttrs = scanAttrs(myArea);
          myAttrs.a1 *= 1.04;
          myAttrs.a2 *= 1.04;
          myAttrs.a3 *= 1.04;
          LOGGER.info("M: " + myAttrs);
          int i = 0;
          for (Attributes a : attrs) {
            if (a.deviation() > 25) {
              LOGGER.info(a + " dev too big: " + a.deviation());
              i++;
            }
          }
          if (i == 0) {
            int diff1 = myAttrs.sum() - attrs.get(0).sum();
            int diff2 = myAttrs.sum() - attrs.get(1).sum();
            int diff3 = myAttrs.sum() - attrs.get(2).sum();
            int max = Integer.MIN_VALUE;
            LOGGER.info("diffs: " + diff1 + "  " + diff2 + "  " + diff3);
            // find the best - the bigger the gap, the better
            if (diff1 > max) {
              max = diff1;
              slot = 0;
            }
            if (diff2 > max) {
              max = diff2;
              slot = 1;
            }
            if (diff3 > max) {
              max = diff3;
              slot = 2;
            }

            if (max > settings.getInt("tasks.matches.minDiff", 40)
                && max < settings.getInt("tasks.matches.maxDiff", 400)) {
              // good
            } else {
              slot = -1;
            }

          }
        }
        LOGGER.info("SLOT: " + slot);
        if (slot >= 0) {
          pp = new Pixel(slotArea[slot].x + 137, slotArea[slot].y + 21);
        }
        if (pp != null) {
          mouse.click(pp);
          mouse.delay(3000);

          Pixel pq = scanner.scanOneFast("playerComp.bmp", scanner._scanArea, false);
          if (pq != null) {
            mouse.click(pq.x + 198, pq.y + 209);
            mouse.click(pq.x + 198, pq.y + 257);
            LOGGER.info("match");
            mouse.delay(1500);
            return 1;// job well done
          } else
            return 0;// no duels, so sleep

        } else
          return -1;// no suitable opponents, may be => don't sleep, try again
                    // soon
      }

    });
  }

  protected void checkForPrizes(String type) {
    try {
      Rectangle area = new Rectangle(scanner._fullArea);
      area.x = scanner.getBottomRight().x - 146;
      area.width = 146;
      area.y += 93;
      area.height -= 93;
      Pixel p = scanner.scanOneFast("GP" + type + ".bmp", area, false);
      if (p != null) {
        LOGGER.info("GRAND PRIZE " + type.toUpperCase());
        mouse.click(p);
        mouse.delay(2000);

        handleAwards();
      }
    } catch (AWTException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RobotInterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void premiumTask() {
    premiumTask = new Task("Premium", 1);
    premiumTask.setProtocol(new AbstractGameProtocol() {

      @Override
      public void execute() throws RobotInterruptedException, GameErrorException {
        if (_bankToggle.isSelected())
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

              // mouse.wheelDown(27);
              // scroller
              mouse.click(p.x + 547, p.y + 355);
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
        if (_sponsorsToggle.isSelected())
          try {
            Pixel p = scanner.scanOneFast("sponsor.bmp", scanner._scanArea, false);
            if (p != null) {
              mouse.click(p.x + 6, p.y + 6);
              mouse.delay(3000);
              p = scanner.scanOneFast("Rocky.bmp", scanner._scanArea, false);
              if (p != null) {
                int n = settings.getInt("tasks.sponsors.number", 1);
                LOGGER.info("sponsor opened");
                mouse.click(p.x, p.y + 303);
                mouse.delay(3500);
                if (n > 1) {
                  mouse.click(p.x + 357, p.y + 303);
                  mouse.delay(3500);
                  if (n > 2) {
                    // 3RD SPONSOR
                    mouse.click(p.x, p.y + 303 + 105);
                    mouse.delay(3500);
                    if (n > 3) {
                      // 4th SPONSOR
                      mouse.click(p.x + 357, p.y + 303 + 105);
                      mouse.delay(3000);
                    }
                  }
                }
                LOGGER.info("sleep 30sec");
                sleep(1 * 30000);
                handlePopups();
              }
            } else
              sleep(1);

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
          if (_clubToggle.isSelected() || _clubDuelsToggle.isSelected()) {
            handlePopups();
            Pixel p = scanner.scanOneFast("club.bmp", scanner._scanArea, false);
            if (p == null) {
              // move se
              Pixel m = new Pixel(scanner.getTopLeft().x + scanner.getGameWidth() / 2, scanner.getTopLeft().y
                  + scanner.getGameHeight() / 2);
              // mouse.mouseMove(m);
              final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
              screenSize.width = 1920 + 100;
              screenSize.height = 1080 + 100;
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
              mouse.delay(3500);
              p = null;
              if (settings.getBoolean("tasks.club.premium", false)) {
                Rectangle area = new Rectangle(scanner._fullArea);
                area.y = scanner._br.y - 195;
                area.height = 195;
                area.x += 300;
                area.width -= (300 + 150);
                p = scanner.scanOneFast("clubClaim.bmp", area, false);
                if (p != null) {
                  mouse.click(p.x + 20, p.y + 12);
                  mouse.delay(2400);
                  clickBankDirectly();
                  mouse.delay(2400);
                  clickBankDirectly();
                }
              } else {
                LOGGER.info("scan...");
                if (checkForMoney())
                  clickBankDirectly();
                checkForDuels();

                dragSE();
                mouse.delay(1200);
                if (checkForMoney())
                  clickBankDirectly();
                checkForDuels();

                dragW();
                mouse.delay(1200);
                if (checkForMoney())
                  clickBankDirectly();
                checkForDuels();

                dragN();
                mouse.delay(1200);
                if (checkForMoney())
                  clickBankDirectly();
                checkForDuels();

                dragE();
                mouse.delay(1200);
                checkForMoney();
                checkForDuels();

                clickBankDirectly();
              }
              // refresh
              sleep(15 * 60000);
              refresh();
            }
          }
        } catch (IOException | AWTException e) {
          e.printStackTrace();
        }

      }

      private boolean checkForMoney() throws RobotInterruptedException, IOException, AWTException {
        boolean found = false;
        if (_clubToggle.isSelected()) {
          Pixel p = null;
          do {
            p = scanner.scanOneFast("money.bmp", scanner._fullArea, true);
            if (p == null)
              p = scanner.scanOneFast("money2.bmp", scanner._fullArea, true);
            LOGGER.info("money..." + p);
            if (p != null) {
              stats.register("Money");
              mouse.delay(4000);
              found = true;
            }
          } while (p != null);
        }
        return found;
      }

      private void checkForDuels() throws RobotInterruptedException, IOException, AWTException {
        if (_clubDuelsToggle.isSelected()) {
          Pixel p = null;
          do {
            p = scanner.scanOneFast(scanner.getImageData("clubDuels.bmp", scanner._fullArea, 9, 17), scanner._fullArea,
                true);
            LOGGER.info("duels..." + p);
            if (p != null) {
              stats.register("Duels");
              mouse.delay(4000);
            }
          } while (p != null);
        }
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
    Pixel m = new Pixel(scanner.getTopLeft().x + scanner.getGameWidth() / 2, scanner.getTopLeft().y
        + scanner.getGameHeight() / 2);
    // mouse.mouseMove(m);
    final Dimension screenSize = new Dimension();
    screenSize.width = 1920 + 100;
    screenSize.height = 1080 + 100;
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

    // split the drag into two parts using the safer area in the center
    xx /= 2;
    yy /= 2;
    Pixel pp = new Pixel(m.x + xx * x1, m.y + yy * y1);
    mouse.dragFast(pp.x, pp.y, m.x + xx * x2, m.y + yy * y2, false, false);
    mouse.delay(200);
    mouse.dragFast(pp.x, pp.y, m.x + xx * x2, m.y + yy * y2, false, false);
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
                clickBalls(scanner._fullArea);

                drag(0, 1);
                clickBalls(scanner._fullArea);

                dragE();
                clickBalls(scanner._fullArea);

                dragW();
                clickBalls(scanner._fullArea);

                dragN();
                clickBalls(scanner._fullArea);

                dragE();
                LOGGER.info("scan balls last one...");
                clickBalls(scanner._fullArea);

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
        Pixel p = null;
        do {
          p = scanner.scanOneFast("ball.bmp", area, true);
          if (p != null) {
            stats.register("Balls");
            ballsCnt++;
            mouse.delay(200);
          }
        } while (p != null && (ballsCnt <= limit || limit <= 0));
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
    private int pairsScanned = 0;
    private Map<Coords, Slot> matrix;
    private int mcols;
    private int mrows;
    private List<Slot[]> matches;
    private List<Slot> scanned;

    @Override
    public void execute() throws RobotInterruptedException, GameErrorException {
      if (_pairsToggle.isSelected())
        try {

          checkForPrizes("Practice");

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
                  p = scanner.scanOneFast("ContinueBrown2.bmp", scanner._scanArea, true);
                  if (p == null)
                    p = scanner.scanOneFast("ContinueBrown.bmp", scanner._scanArea, true);
                  if (p == null) {
                    int xx = (scanner.getTopLeft().x + scanner.getGameWidth() / 2);

                    p = scanner.scanOneFast("prizePractice.bmp", scanner._scanArea, true);
                    if (p != null) {
                      handleAwards();
                    } else {
                      p = scanner.scanOneFast("PairsResultTitle.bmp", scanner._scanArea, true);
                      if (p != null) {
                        mouse.click(p.x + 52, p.y + 315);
                        mouse.click(p.x + 202, p.y + 315);
                        mouse.delay(5000);
                        handleAwards();
                      } else {
                        mouse.click(xx - 124, pq.y + 285 - 18);
                        mouse.click(xx - 62, pq.y + 285 - 18);
                        mouse.click(xx - 0, pq.y + 285 - 18);
                      }
                      mouse.delay(3000);
                      refresh();
                    }
                  } else
                    mouse.delay(6000);

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

    private Rectangle clockArea;

    private Pixel pairsStarted() throws AWTException, RobotInterruptedException, IOException {
      Pixel p = null;
      boolean started = false;
      int turn = 0;
      do {
        turn++;
        p = scanner.scanOneFast("clockAnchor.bmp", scanner._fullArea, false);
        LOGGER.info("check is started..." + p);
        if (p != null) {
          started = true;
          clockArea = new Rectangle(p.x + 897, p.y + 541, 18, 27);
          Pixel pp3 = scanner.scanOneFast("MultiplierZero2.bmp", clockArea, false);
          if (pp3 != null) {
            LOGGER.info("clock found: " + pp3);
            clockArea = new Rectangle(pp3.x - 1, pp3.y - 1, 18, 27);
          } else {
            Rectangle scanArea = scanner._scanArea;
            if (scanArea == null) {
              scanArea = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            }
            Rectangle area = new Rectangle(scanArea);
            area.x += 700;
            area.y += 500;
            area.width -= 700;
            area.height -= 500;
            pp3 = scanner.scanOneFast("MultiplierZero2.bmp", area, false);
            if (pp3 != null) {
              LOGGER.info("clock area: " + pp3);
              // scanner.writeAreaTS(clockArea, "clockArea.bmp");
              clockArea = new Rectangle(pp3.x - 1, pp3.y - 1, 18, 27);
            }
          }
          return p;
        } else
          mouse.delay(400);
      } while (!started && turn < 12);

      return null;
    }

    public boolean doPairs() throws RobotInterruptedException, IOException, AWTException {
      boolean can = false;
      do {
        Pixel p = pairsStarted();
        if (p != null) {

          time = 0;
          final Pixel pp = p;
          // good
          int slotSize = 80;
          int gapx = 12;
          int gapy = 10;
          mcols = 8;
          mrows = 4;
          int mwidth = mcols * (slotSize + gapx) - gapx;
          int mheight = mrows * (slotSize + gapy) - gapy;
          Rectangle gameArea = new Rectangle(p.x + 119, p.y + 123, mwidth, mheight);

          // do pairs
          int slotsNumber = 0;
          do {
            slotsNumber = workPairs(pp, slotSize, gapx, gapy, gameArea);
          } while (slotsNumber > 0);

          // repeat?
          can = false;
          if (settings.getBoolean("tasks.pairs.repeat", false)) {
            mouse.delay(3000);
            LOGGER.info("try to repeat...");
            Pixel p2 = scanner.scanOneFast("replayButton.bmp", scanner._scanArea, false);
            if (p2 != null) {
              mouse.click(p2.x, p2.y + 17);
              LOGGER.info("PLAY AGAIN in 4s");
              mouse.delay(4000);
              can = true;
            } else {
              LOGGER.info("done.");
            }
          }

        } else {
          // no energy or other problem
          return false;
        }
      } while (can);

      // at least once the pairs have been played
      return true;
    }

    private int workPairs(final Pixel pp, int slotSize, int gapx, int gapy, Rectangle gameArea) {
      int slotsNumber = mrows * mcols;
      try {
        done = false;
        matrix = new HashMap<Coords, Slot>();

        // CREATE THREAD
        final Thread t = createThread(pp);
        // final Thread ts = createThread2(pp);

        for (int row = 1; row <= mrows; row++) {
          for (int col = 1; col <= mcols; col++) {
            Slot slot = new Slot(row, col, true);
            Rectangle slotArea = new Rectangle(gameArea.x + (col - 1) * (slotSize + gapx) + 20, gameArea.y + (row - 1)
                * (slotSize + gapy) + 20, 40, 40);
            slot.area = slotArea;
            matrix.put(slot.coords, slot);
          }
        }
        // Pixel pp2 = scanner.scanOneFast("back2.bmp", scanner._scanArea,
        // false);
        // System.err.println(pp2);

        // INITIAL SCAN
        for (int row = 1; row <= mrows; row++) {
          for (int col = 1; col <= mcols; col++) {
            Slot slot = matrix.get(new Coords(row, col));

            slot.image = scanSlot(slot.area);

            if (!sameImage(scanner.getImageData("back2.bmp").getImage(), slot.image)) {
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

        matches = new ArrayList<>(slotsNumber / 2);
        // scanned = Collections.synchronizedList(new
        // ArrayList<Slot>(slotsNumber));//TODO sync?
        scanned = new ArrayList<Slot>(slotsNumber);

        // HIT IT!
        int slow = settings.getInt("doPairs.slow", 60);
        LOGGER.info("Slots: " + slotsNumber);

        if (slotsNumber > 0) {
          t.start();
          pairsScanned = 0;
          for (int row = 1; row <= mrows; row++) {
            for (int col = 1; col <= mcols; col++) {
              Coords coords = new Coords(row, col);
              final Slot slot = matrix.get(coords);

              if (openSlot != null && coords.equals(openSlot)) {
                LOGGER.info("openslot...");

              } else {
                if (slot.active) {
                  mouse.click(slot.area.x, slot.area.y);
                  if (first) {
                    prev = coords;
                    mouse.delay(slow);
                  } else {
                    final Slot prevSlot = matrix.get(prev);
                    mouse.delay(550);// was 600
                    slot.image = scanSlot(slot.area);
                    prevSlot.image = scanSlot(prevSlot.area);
                    new Thread(new Runnable() {
                      public void run() {
                        try {
                          Thread.sleep(250);
                          if (PairsTools.areMatching(slot.area, prevSlot.area)) {
                            prevSlot.active = false;
                            slot.active = false;
                            // scanned.remove(prevSlot);
                            // scanned.remove(slot);
                            time = System.currentTimeMillis() - 550 - 250;
                            LOGGER.info("MATCH2!!!");
                          }
                        } catch (Exception e) {
                          e.printStackTrace();
                        }

                      }
                    }).start();

                    pairsScanned++;
                    addToScanned(prevSlot, slot);

                    // if (settings.getBoolean("debug", false) && once) {
                    // // debug mf
                    // once = false;
                    // captureSlots(prevSlot, slot);
                    // }
                    // if (sameImage(prevSlot.image, slot.image)) {
                    // // we have match, so remove both
                    // prevSlot.image = null;
                    // slot.image = null;
                    // LOGGER.info("UH OH! Time is ticking now");
                    // time = System.currentTimeMillis();
                    // }

                    if (time != 0) {
                      if (System.currentTimeMillis() - time > 3000 && !matches.isEmpty()) {
                        LOGGER.info("click something ... " + (System.currentTimeMillis() - time));
                        // mouse.delay(400);
                        Slot[] slots = matches.get(0);
                        if (slots[0].coords.equals(prevSlot.coords) || slots[0].coords.equals(slot.coords)
                            || slots[1].coords.equals(prevSlot.coords) || slots[1].coords.equals(slot.coords)) {
                          LOGGER.info("hmm. wait a bit more...");
                          mouse.delay(650);
                        }
                        mouse.click(slots[0].area.x, slots[0].area.y);
                        mouse.delay(140);
                        mouse.click(slots[1].area.x, slots[1].area.y);
                        mouse.delay(600);
                        slots[0].active = false;
                        slots[1].active = false;
                        matches.remove(0);
                        time = System.currentTimeMillis();
                      } else {
                        LOGGER.info("wait..." + (System.currentTimeMillis() - time));
                        if (matches.isEmpty())
                          LOGGER.info("no matches yet :(");
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
          mouse.delay(700);
          clickMatches(mcols, mrows, matrix, -1);

          mouse.delay(2000);
        }
      } catch (RobotInterruptedException re) {
        slotsNumber = 0;
        LOGGER.info("interrupted");
      } catch (Exception e) {
        slotsNumber = 0;
        LOGGER.info("WHAT HAPPENED? " + e.getMessage());
      }
      // END OF CYCLE
      return slotsNumber;
    }

    private void addToScanned(Slot slot1, Slot slot2) {
      // long start = System.currentTimeMillis();
      // find matches
      List<Slot> toRemove = new ArrayList<>();
      boolean sl1Match = false;
      boolean sl2Match = false;
      for (int i = 0; i < scanned.size(); i++) {
        Slot sl = scanned.get(i);
        if (sl.active) {
          if (slot1.active && sameImage(sl.image, slot1.image)) {
            // found a match
            matches.add(new Slot[] { sl, slot1 });
            toRemove.add(sl);
            sl1Match = true;
          } else if (slot2.active && sameImage(sl.image, slot2.image)) {
            // found a match
            matches.add(new Slot[] { sl, slot2 });
            toRemove.add(sl);
            sl2Match = true;
          }
        }
      }

      for (Slot slot : toRemove) {
        scanned.remove(slot);
      }

      if (!sl1Match)
        scanned.add(slot1);
      if (!sl2Match)
        scanned.add(slot2);

      LOGGER.info("matches: " + matches.size());
      // LOGGER.info("time: " + (System.currentTimeMillis() - start));

    }

    private Thread createThread(final Pixel pp) throws AWTException, RobotInterruptedException, IOException {
      return new Thread(new Runnable() {
        public void run() {
          try {
            long threadTime = System.currentTimeMillis();
            do {
              if (scanner.scanOneFast("MultiplierZero2.bmp", clockArea, false) == null) {
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
    }

    private Thread createThread2(final Pixel pp) {
      return new Thread(new Runnable() {
        public void run() {
          try {
            for (int row1 = 1; row1 <= mrows; row1++) {
              for (int col1 = 1; col1 <= mcols; col1++) {

                for (int row2 = 1; row2 <= mrows; row2++) {
                  for (int col2 = 1; col2 <= mcols; col2++) {

                    Coords c1 = new Coords(row1, col1);
                    Coords c2 = new Coords(row2, col2);
                    // System.err.println(c1 + " - " + c2);
                    if (!c1.equals(c2)) {
                      Slot slot1 = matrix.get(c1);
                      Slot slot2 = matrix.get(c2);
                      if (slot1.active && slot2.active && sameImage(slot1.image, slot2.image)) {
                        matches.add(new Slot[] { slot1, slot2 });
                      }
                    }
                  }
                }
              }
            }

            LOGGER.info("matches: " + matches.size());
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
    }

    private boolean clickMatches(int mcols, int mrows, Map<Coords, Slot> matrix, int maxClicks)
        throws RobotInterruptedException, AWTException {
      int clicks = 0;
      boolean done = false;
      int slow = settings.getInt("doPairs.slow", 60);
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
                    mouse.delay(140 + slow);
                    mouse.click(slot2.area.x, slot2.area.y);

                    if (settings.getBoolean("debug", false)) {
                      // debug mf
                      if (!done) {
                        captureSlots(slot1, slot2);
                        done = true;
                      }
                    } else
                      mouse.delay(200 + 5 * slow);
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

    private void captureSlots2(Slot slot1, Slot slot2) {
      try {

        Robot robot = new Robot();
        mouse.delay(1350);
        scanner.writeImage(robot.createScreenCapture(slot1.area), "slot1_" + System.currentTimeMillis() + ".bmp");
        scanner.writeImage(robot.createScreenCapture(slot2.area), "slot2_" + System.currentTimeMillis() + ".bmp");

      } catch (Exception e) {
        e.printStackTrace();
      }

    }

    private void captureSlots(Slot slot1, Slot slot2) {
      try {
        // final List<BufferedImage> images1 = new ArrayList<>();
        final List<BufferedImage> images2 = new ArrayList<>();
        final List<Long> time = new ArrayList<>();
        int mi = 1000;
        Robot robot = new Robot();
        LOGGER.info("start capturing...");
        for (int i = 0; i < mi; i++) {
          // images1.add(robot.createScreenCapture(slot1.area));
          images2.add(robot.createScreenCapture(slot2.area));
          time.add(System.currentTimeMillis());
          Thread.sleep(10);
        }
        LOGGER.info("done");
        new Thread(new Runnable() {
          public void run() {
            LOGGER.info("saving ...");
            int i = 1;
            long t = time.get(0);
            for (BufferedImage bi : images2) {
              scanner.writeImage(bi, "slot2_" + i + "____" + (time.get(i - 1) - t) + ".bmp");
              i++;
            }
            i = 1;
            // for (BufferedImage bi : images1) {
            // scanner.writeImage(bi, "slot1_" + i++ + ".bmp");
            // }
            LOGGER.info("save done");
          }
        }).start();

      } catch (Exception e) {
        e.printStackTrace();
      }
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

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (active ? 1231 : 1237);
      result = prime * result + ((coords == null) ? 0 : coords.hashCode());
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
      Slot other = (Slot) obj;
      if (active != other.active)
        return false;
      if (coords == null) {
        if (other.coords != null)
          return false;
      } else if (!coords.equals(other.coords))
        return false;
      return true;
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
    // scanner.writeAreaTS(slotArea, "slotarea.bmp");
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
    if (!SIMPLE)
      toolbars.add(mainToolbar2);

    Box north = Box.createVerticalBox();
    north.add(toolbars);
    if (!SIMPLE)
      north.add(createStatsPanel());
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

    // Balls
    gbc.gridx += 2;
    gbc2.gridx += 2;
    panel.add(new JLabel("Balls:"), gbc);
    l = new JLabel(" ");
    _labels.put("Balls", l);
    panel.add(l, gbc2);

    // autoM
    gbc.gridx += 2;
    gbc2.gridx += 2;
    panel.add(new JLabel("autoM:"), gbc);
    l = new JLabel(" ");
    _labels.put("autoM", l);

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
    _ballsToggle = new JToggleButton("B");
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

    // Balls
    _sponsorsToggle = new JToggleButton("S");
    toolbar.add(_sponsorsToggle);
    _sponsorsToggle.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        boolean b = e.getStateChange() == ItemEvent.SELECTED;
        LOGGER.info("Sponsors: " + (b ? "on" : "off"));
        settings.setProperty("tasks.sponsors", "" + b);
        settings.saveSettingsSorted();

      }
    });

    // Summer Fiesta
    _sfToggle = new JToggleButton("SF");
    toolbar.add(_sfToggle);
    _sfToggle.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        boolean b = e.getStateChange() == ItemEvent.SELECTED;
        LOGGER.info("Summer Fiesta: " + (b ? "on" : "off"));
        settings.setProperty("tasks.sf", "" + b);
        settings.saveSettingsSorted();
      }
    });

    // Practice
    _practiceToggle = new JToggleButton("Pr");
    // toolbar.add(_practiceToggle);
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
    _matchesToggle = new JToggleButton("M");
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

    // Club
    _clubToggle = new JToggleButton("Cl M");
    toolbar.add(_clubToggle);

    _clubToggle.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        boolean b = e.getStateChange() == ItemEvent.SELECTED;
        LOGGER.info("Club Money: " + (b ? "on" : "off"));
        settings.setProperty("tasks.club.money", "" + b);
        settings.saveSettingsSorted();

      }
    });
    // Club
    _clubDuelsToggle = new JToggleButton("Cl D");
    toolbar.add(_clubDuelsToggle);

    _clubDuelsToggle.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        boolean b = e.getStateChange() == ItemEvent.SELECTED;
        LOGGER.info("Club Duels: " + (b ? "on" : "off"));
        settings.setProperty("tasks.club.duels", "" + b);
        settings.saveSettingsSorted();

      }
    });
    return toolbar;
  }

  @SuppressWarnings("serial")
  private JToolBar createToolbar1() {
    JToolBar mainToolbar1 = new JToolBar();
    mainToolbar1.setFloatable(false);
    // SCAN
    if (!SIMPLE) {
      AbstractAction action = new AbstractAction("Scan") {
        public void actionPerformed(ActionEvent e) {
          Thread myThread = new Thread(new Runnable() {
            @Override
            public void run() {
              try {
                scan();
              } catch (RobotInterruptedException e) {
                e.printStackTrace();
              }
            }
          });

          myThread.start();
        }
      };
      mainToolbar1.add(action);
    }
    // RUN MAGIC
    if (!SIMPLE) {
      AbstractAction action = new AbstractAction("Do magic") {

        public void actionPerformed(ActionEvent e) {
          runMagic();
        }

      };
      mainToolbar1.add(action);
    }

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

    // Reset
    if (!SIMPLE) {
      AbstractAction action = new AbstractAction("Reset") {
        public void actionPerformed(ActionEvent e) {
          Thread t = new Thread(new Runnable() {
            public void run() {
              ((AbstractGameProtocol) ballTask.getProtocol()).reset();
              stats.clear();
              taskManager.updateAll();
            }
          });
          t.start();
        }

      };
      mainToolbar1.add(action);

    }
    // Auto matches
    if (!SIMPLE) {
      AbstractAction action = new AbstractAction("AutoM") {
        public void actionPerformed(ActionEvent e) {
          Thread t = new Thread(new Runnable() {
            public void run() {
              setAutoMatches();
            }

          });
          t.start();
        }

      };
      mainToolbar1.add(action);

    }
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

  public MainFrame(boolean isTestmode) throws HeadlessException, AWTException {
    super();

    // _testMode = isTestmode;
    setupLogger();
    init();

    pack();
    setSize(new Dimension(getSize().width + 8, getSize().height + 8));
    int w = 285;// frame.getSize().width;
    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int h = (int) (screenSize.height * 0.9);
    if (SIMPLE)
      h = 250;
    int x = screenSize.width - w;
    int y = (screenSize.height - h) / 2;
    setBounds(x, y, w, h);

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
        LOGGER.info("jobs done...");
        mouse.delay(500);
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
    try {
      refresh(false);
    } catch (AWTException | IOException | RobotInterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // LOGGER.info("refresh...");
    // mouse.click(scanner.getParkingPoint());
    // try {
    // Robot robot = new Robot();
    // robot.keyPress(KeyEvent.VK_F5);
    // robot.keyRelease(KeyEvent.VK_F5);
    // } catch (AWTException e) {
    // }
    //
    // try {
    // Thread.sleep(8000);
    // } catch (InterruptedException e) {
    // }
    // LOGGER.info("refresh done");
    //
  }

  private void refresh(boolean bookmark) throws AWTException, IOException, RobotInterruptedException {
    deleteOlder(".", "refresh", 5, -1);
    LOGGER.info("Time to refresh...");
    // scanner.captureGameArea("refresh ");
    Pixel p;
    if (!bookmark) {
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
        // if (i > 8) {
        // captureScreen("refresh trouble ");
        // }
      }
      // if (done) {
      // // runMagic();
      // captureScreen("refresh done ");
      // } else {
      // // blah
      // // try bookmark
      // }

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
      Pixel p = scanner.scanOneFast("x.bmp", scanner._fullArea, false);
      if (p != null) {
        found = true;
        mouse.click(p.x + 16, p.y + 16);
        mouse.delay(200);
      }

      scanner.handleFBMessages(true);

      // found = found || scanner.scanOneFast("Continue.bmp", scanner._scanArea,
      // true) != null;
      // found = found || scanner.scanOneFast("ContinueBrown.bmp",
      // scanner._scanArea, true) != null;
      // if (!found) {
      // p = scanner.scanOneFast("grandPrize.bmp", scanner._scanArea, true);
      // if (p == null)
      // p = scanner.scanOne("PrizeMatches.bmp", scanner._scanArea, true);
      // if (p != null) {
      // handleAwards();
      // mouse.delay(1000);
      // p = scanner.scanOneFast("x.bmp", scanner._scanArea, true);
      // found = true;
      // }
      // }

    } catch (IOException e) {
      e.printStackTrace();
    } catch (AWTException e) {
      e.printStackTrace();
    }

  }

  private void handleAwards() {
    try {
      mouse.delay(3500);
      Pixel p = scanner.scanOneFast("ballReward.bmp", scanner._scanArea, false);
      if (p == null) {
        mouse.delay(1000);
        p = scanner.scanOneFast("ballReward.bmp", scanner._scanArea, false);
      }
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

    boolean sponsors = "true".equalsIgnoreCase(settings.getProperty("tasks.sponsors"));
    if (sponsors != _sponsorsToggle.isSelected()) {
      _sponsorsToggle.setSelected(sponsors);
    }

    boolean sf = "true".equalsIgnoreCase(settings.getProperty("tasks.sf"));
    if (sf != _sfToggle.isSelected()) {
      _sfToggle.setSelected(sf);
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

    boolean clubM = "true".equalsIgnoreCase(settings.getProperty("tasks.club.money"));
    if (clubM != _clubToggle.isSelected()) {
      _clubToggle.setSelected(clubM);
    }

    boolean clubD = "true".equalsIgnoreCase(settings.getProperty("tasks.club.duels"));
    if (clubD != _clubDuelsToggle.isSelected()) {
      _clubDuelsToggle.setSelected(clubD);
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
    } else {
      // we have problem finding the bank
      // try with image
      new Robot().mouseWheel(-3);
      mouse.delay(500);

      refresh();
      final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      Rectangle area = new Rectangle(screenSize);
      area.width /= 2;
      area.height /= 3;
      area.x += area.width - 1;
      p = scanner.scanOneFast("bankAnchor.bmp", area, false);
      if (p != null) {
        mouse.click(p.x + 9, p.y + 12);
        mouse.delay(1500);
        p = scanner.scanOneFast("Finances.bmp", scanner._scanArea, false);
        if (p != null) {
          mouse.click(p.x - 194, p.y + 368);
          mouse.delay(1500);
        }
      }
    }
    handlePopups();
  }

  public Calendar findNextMorningTime() {
    Calendar cal = Calendar.getInstance();
    // desired time: 6am
    int h = cal.get(Calendar.HOUR_OF_DAY);
    if (h > 6) {
      cal.add(Calendar.DAY_OF_MONTH, 1);
    }
    cal.set(Calendar.HOUR_OF_DAY, 6);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    return cal;
  }

  private void setAutoMatches() {
    if (!isRunning("AUTOM")) {
      final Calendar when = findNextMorningTime();
      Thread timer = new Thread(new Runnable() {
        public void run() {
          // desired time: 6am
          long remaining = 0l;
          do {
            long now = System.currentTimeMillis();
            remaining = when.getTimeInMillis() - now;
            _labels.get("autoM").setText(DateUtils.fancyTime2(remaining, false));
            try {
              Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
          } while (remaining > 0);

          // it's time
          LOGGER.info("Turning matches off...");
          _matchesToggle.setSelected(false);
        }
      }, "AUTOM");
      timer.start();
    }
  }
}
