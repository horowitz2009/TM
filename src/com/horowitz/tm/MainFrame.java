package com.horowitz.tm;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import com.horowitz.commons.DateUtils;
import com.horowitz.commons.MouseRobot;
import com.horowitz.commons.MyImageIO;
import com.horowitz.commons.MyLogger;
import com.horowitz.commons.Pixel;
import com.horowitz.commons.RobotInterruptedException;
import com.horowitz.commons.Service;
import com.horowitz.commons.Settings;
import com.horowitz.seaport.GameErrorException;
import com.horowitz.seaport.model.AbstractGameProtocol;
import com.horowitz.seaport.model.Task;

public class MainFrame extends JFrame {

  private final static Logger LOGGER = Logger.getLogger("MAIN");

  private static String APP_TITLE = "TM v0.1";

  private MouseRobot mouse;

  private Settings settings;

  private ScreenScanner scanner;

  private boolean stopAllThreads;

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
      frame.pack();
      frame.setSize(new Dimension(frame.getSize().width + 8, frame.getSize().height + 8));
      int w = 285;// frame.getSize().width;
      final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int h = (int) (screenSize.height * 0.9);
      int x = screenSize.width - w;
      int y = (screenSize.height - h) / 2;
      frame.setBounds(x, y, w, h);

      frame.setVisible(true);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private Task practiceTask;
  private Task matchTask;
  private Task ballTask;

  @SuppressWarnings("serial")
  private void init() throws AWTException {

    try {

      // _ocr = new OCRB("ocr/digit");
      // _ocr.setErrors(1);

      // LOADING DATA
      settings = Settings.createSettings("settings.properties");
      if (!settings.containsKey("todo")) {
        setDefaultSettings();
      }

      scanner = new ScreenScanner(settings);
      mouse = scanner.getMouse();

      practiceTask = new Task("Practice", 1);
      matchTask = new Task("match", 1);// TODO this frequency -
      ballTask = new Task("ball", 1);// TODO this frequency -
                                     // figure it out
      ballTask.setProtocol(new AbstractGameProtocol() {
        @Override
        public void execute() throws RobotInterruptedException, GameErrorException {
          handlePopups(false);
          try {
            LOGGER.info("balls");
            Pixel p = scanner.scanOneFast("ball.bmp", scanner._fullArea, false);
            if (p != null) {
              LOGGER.info("click... " + p);
              mouse.click(p.x + 8, p.y + 8);
            }
          } catch (IOException e) {
          } catch (AWTException e) {
          }

        }

        @Override
        public boolean preExecute() throws AWTException, IOException, RobotInterruptedException {
          // TODO Auto-generated method stub
          return false;
        }

        @Override
        public void update() {
          // TODO Auto-generated method stub

        }
      });
      practiceTask.setProtocol(new AbstractGameProtocol() {

        @Override
        public void update() {
          // TODO Auto-generated method stub
          LOGGER.info("nothing to update");
        }

        @Override
        public boolean preExecute() throws AWTException, IOException, RobotInterruptedException {
          return false;
        }

        @Override
        public void execute() throws RobotInterruptedException, GameErrorException {
          LOGGER.info("Doing Practice...");
          mouse.delay(2000);
          // 1. click practiceCourt.bmp
          // 2. wait 3000
          // 3. check is open practiceArena.bmp
          // 4. find Quiz
          // 5. click simulate.bmp
          // 6. wait 4000
          // 7. if ok, do nothing, else reschedule the task for 3 minutes

          try {
            // 1.
            scanner.scanOneFast("practiceCourt.bmp", scanner._scanArea, true);
            mouse.delay(3000);
            Pixel p = scanner.scanOneFast("practiceArena.bmp", scanner._scanArea, false);
            if (p != null) {
              LOGGER.info("entered Practice arena...");
              Rectangle area = new Rectangle(p.x - 194, p.y + 129, 650, 34);
              // scanner.writeArea(area, "hmm.bmp");
              Pixel pq = scanner.scanOneFast("Quiz.bmp", area, false);
              if (pq == null) {
                for (int i = 0; i < 5; i++) {
                  mouse.click(p.x - 214, p.y + 295);
                  mouse.delay(500);
                }
              }
              pq = scanner.scanOneFast("Quiz.bmp", area, false);
              if (pq != null) {
                mouse.click(pq.x + 79, pq.y + 286);
                LOGGER.info("Simulate quiz...");
                mouse.delay(4000);
                p = scanner.scanOneFast("practiceArena.bmp", scanner._scanArea, false);
                if (p != null) {
                  LOGGER.info("SUCCESS");
                  mouse.click(p.x + 500, p.y + 12);
                  mouse.delay(2000);
                } else {
                  LOGGER.info("HMM");
                  mouse.click(scanner.getParkingPoint());
                  try {
                    Robot robot = new Robot();
                    robot.keyPress(KeyEvent.VK_F5);
                    robot.keyRelease(KeyEvent.VK_F5);
                  } catch (AWTException e) {
                  }

                  try {
                    Thread.sleep(10000);
                  } catch (InterruptedException e) {
                  }

                }
              } else {
                LOGGER.info("Uh oh! Can't find Quiz!");
                handlePopups(false);
              }
            }

          } catch (IOException | AWTException e) {
            e.printStackTrace();
          }

        }
      });
      matchTask.setProtocol(new AbstractGameProtocol() {

        @Override
        public void update() {
          // TODO Auto-generated method stub
          // LOGGER.info("nothing to update");
        }

        @Override
        public boolean preExecute() throws AWTException, IOException, RobotInterruptedException {
          return false;
        }

        @Override
        public void execute() throws RobotInterruptedException, GameErrorException {
          LOGGER.info("match");
          // mouse.delay(2000);
          // 1. click practiceCourt.bmp
          // 2. wait 3000
          // 3. check is open practiceArena.bmp
          // 4. find Quiz
          // 5. click simulate.bmp
          // 6. wait 4000
          // 7. if ok, do nothing, else reschedule the task for 3 minutes

          try {
            // 1.
            scanner.scanOneFast("centerCourt.bmp", scanner._scanArea, true);
            mouse.delay(3000);
            Pixel p = scanner.scanOneFast("centerCourtTitle.bmp", scanner._scanArea, false);
            if (p != null) {
              LOGGER.info("entered center court...");
              Rectangle area = new Rectangle(p.x - 194, p.y + 129, 650, 34);
              // scanner.writeArea(area, "hmm.bmp");
              mouse.click(p.x + 424, p.y + 180);
              mouse.delay(3000);
              Pixel pq = scanner.scanOneFast("playerComp.bmp", scanner._scanArea, false);
              if (pq != null) {
                mouse.click(pq.x + 198, pq.y + 257);
                LOGGER.info("Simulate match...");
                mouse.delay(4000);
                // grandprize
                p = scanner.scanOneFast("grandPrize.bmp", scanner._scanArea, false);
                if (p != null) {
                  LOGGER.info("Grand Prize!!!");
                  mouse.click(p.x + 35, p.y + 7);
                  // TODO tennis balls
                  mouse.delay(2000);
                } else {
                  p = scanner.scanOneFast("Continue.bmp", scanner._scanArea, false);
                  if (p != null) {
                    mouse.click(p);
                    mouse.delay(4000);
                    p = scanner.scanOneFast("centerCourtTitle.bmp", scanner._scanArea, false);
                    if (p != null) {
                      LOGGER.info("GOOD");
                    } else {
                      LOGGER.info("HMM");
                      mouse.click(scanner.getParkingPoint());
                      try {
                        Robot robot = new Robot();
                        robot.keyPress(KeyEvent.VK_F5);
                        robot.keyRelease(KeyEvent.VK_F5);
                      } catch (AWTException e) {
                      }
                      try {
                        Thread.sleep(10000);
                      } catch (InterruptedException e) {
                      }
                    }
                  }

                }
              }
            }

          } catch (IOException | AWTException e) {
            e.printStackTrace();
          }

        }
      });
      // taskManager.addTask(practiceTask);
      stopAllThreads = false;

    } catch (Exception e1) {
      System.err.println("Something went wrong!");
      e1.printStackTrace();
      System.exit(1);
    }

    initLayout();

    // reapplySettings();
    //
    // runSettingsListener();

  }

  private void setDefaultSettings() {
    settings.setProperty("popups", "false");
    settings.setProperty("gates", "false");
    settings.setProperty("slow", "false");
    settings.setProperty("ping", "false");
    settings.setProperty("ping2", "false");
    settings.setProperty("caravan", "false");
    settings.setProperty("kitchen", "true");
    settings.setProperty("foundry", "true");

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

    JPanel toolbars = new JPanel(new GridLayout(0, 1));
    toolbars.add(mainToolbar1);

    Box north = Box.createVerticalBox();
    north.add(toolbars);
    rootPanel.add(north, BorderLayout.NORTH);
  }

  private JToggleButton _pingToggle;

  private JToggleButton _slowToggle;

  private JToggleButton _caravanToggle;

  private JToggleButton _kitchenToggle;

  private JToggleButton _foundryToggle;

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

  @SuppressWarnings("serial")
  private JToolBar createToolbar1() {
    JToolBar mainToolbar1 = new JToolBar();
    mainToolbar1.setFloatable(false);

    // SCAN
    {
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
    {
      AbstractAction action = new AbstractAction("Do magic") {
        public void actionPerformed(ActionEvent e) {
          runMagic();
        }

      };
      mainToolbar1.add(action);
    }

    // TEST GO TO MAP
    {
      AbstractAction action = new AbstractAction("Do Agenda") {
        public void actionPerformed(ActionEvent e) {
          // doAgenda();
        }

      };
      mainToolbar1.add(action);
    }

    // TEST DO CAMP
    {
      AbstractAction action = new AbstractAction("Do Camp") {
        public void actionPerformed(ActionEvent e) {
          Thread t = new Thread(new Runnable() {
            public void run() {
              // try {
              // doCamp();
              // } catch (RobotInterruptedException e) {
              // e.printStackTrace();
              // } catch (IOException e) {
              // e.printStackTrace();
              // } catch (AWTException e) {
              // e.printStackTrace();
              // }

            }
          });
          t.start();
        }

      };
      mainToolbar1.add(action);
    }

    // TEST scan energy
    {
      AbstractAction action = new AbstractAction("SE") {
        public void actionPerformed(ActionEvent e) {
          Thread t = new Thread(new Runnable() {
            public void run() {
              // try {
              // scanEnergy();
              // } catch (AWTException e1) {
              // // TODO Auto-generated catch block
              // e1.printStackTrace();
              // }

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
  }

  private void doMagic() {
    assert scanner.isOptimized();
    setTitle(APP_TITLE + " RUNNING");
    stopAllThreads = false;
    try {
      mouse.saveCurrentPosition();
      _fstart = System.currentTimeMillis();
      do {
        ballTask.execute();
        handlePopups(false);
        practiceTask.execute();
        matchTask.execute();
        handlePopups(false);
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
    handlePopups(false);

    mouse.mouseMove(scanner.getParkingPoint());

    mouse.delay(200);
  }

  private void refresh(boolean bookmark) throws AWTException, IOException, RobotInterruptedException {
    deleteOlder("refresh", 5);
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

        handlePopups(i > 10);
        mouse.delay(200);
        handlePopups(i > 10);
        mouse.delay(200);
        handlePopups(i > 10);
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

  private void handlePopups(boolean wide) throws RobotInterruptedException {
    try {
      LOGGER.info("Popups...");
      Pixel p = scanner.scanOneFast("x.bmp", scanner._scanArea, false);
      if (p != null) {
        mouse.click(p.x + 16, p.y + 16);
        mouse.delay(200);
      }
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
    // boolean caravan =
    // "true".equalsIgnoreCase(_settings.getProperty("caravan"));
    // if (caravan != _caravanToggle.isSelected()) {
    // _caravanToggle.setSelected(caravan);
    // }
    //
    // boolean kitchen =
    // "true".equalsIgnoreCase(_settings.getProperty("kitchen"));
    // if (kitchen != _kitchenToggle.isSelected()) {
    // _kitchenToggle.setSelected(kitchen);
    // }
    //
    // boolean foundry =
    // "true".equalsIgnoreCase(_settings.getProperty("foundry"));
    // if (foundry != _foundryToggle.isSelected()) {
    // _foundryToggle.setSelected(foundry);
    // }
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

  private void deleteOlder(String prefix, int amountFiles) {
    File f = new File(".");
    File[] files = f.listFiles();
    List<File> targetFiles = new ArrayList<File>(6);
    int cnt = 0;
    for (File file : files) {
      if (!file.isDirectory() && file.getName().startsWith(prefix)) {
        targetFiles.add(file);
        cnt++;
      }
    }

    if (cnt > amountFiles) {
      // delete some files
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
      for (int i = 0; i < c; i++) {
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
    deleteOlder("ping", 8);
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

}
