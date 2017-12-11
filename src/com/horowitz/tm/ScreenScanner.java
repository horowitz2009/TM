package com.horowitz.tm;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;

import javax.imageio.ImageIO;

import com.horowitz.commons.BaseScreenScanner;
import com.horowitz.commons.ImageComparator;
import com.horowitz.commons.ImageData;
import com.horowitz.commons.ImageManager;
import com.horowitz.commons.ImageMask;
import com.horowitz.commons.MyImageIO;
import com.horowitz.commons.Pixel;
import com.horowitz.commons.RobotInterruptedException;
import com.horowitz.commons.Settings;

public class ScreenScanner extends BaseScreenScanner {

  private static final boolean DEBUG = false;

  public Rectangle _scanArea = null;
  public Rectangle _scanAreaC = null;

  private Rectangle _productionArea3;
  private Rectangle _productionArea2;
  private Rectangle _warehouseArea;;

  public Rectangle _popupArea;
  public Rectangle _popupAreaX;
  public Rectangle _popupAreaB;
  public Rectangle _logoArea;

  public Pixel _menuBR;

  public Rectangle _lastLocationButtonArea;

  public Rectangle _mapButtonArea;

  public Rectangle _campButtonArea;

  public Pixel _eastButtons;

  public Pixel _westButtons;

  public Rectangle _diggyCaveArea;

  public Rectangle _scampArea;

  private boolean _wide;

  public ScreenScanner(Settings settings) {
    super(settings);
  }

  public boolean isWide() {
    return _wide;
  }

  @Override
  public Rectangle generateWindowedArea(int width, int height) {
    if (_wide)
      width += 400;
    return super.generateWindowedArea(width, height);
  }

  @Override
  protected void setKeyAreas() throws IOException, AWTException, RobotInterruptedException {
    super.setKeyAreas();

    _scanArea = new Rectangle(_tl.x + 120, _tl.y + 85, getGameWidth() - 120 - 120, getGameHeight() - 85 - 85);
    _scanAreaC = new Rectangle(_tl.x + 120, _tl.y + 85, getGameWidth() - 120 - 120, getGameHeight() - 85 - 164);
    _fullArea = new Rectangle(_tl.x, _tl.y + 42, getGameWidth(), getGameHeight() - 42);

    getImageData("Continue.bmp", null, 19, 8);
    getImageData("ball.bmp", null, 8, 10);

  }

  public Pixel getParkingPoint() {
    return _parkingPoint;
  }

  public Rectangle getProductionArea3() {
    return _productionArea3;
  }

  public Rectangle getProductionArea2() {
    return _productionArea2;
  }

  public Rectangle getWarehouseArea() {
    return _warehouseArea;
  }

  public Rectangle getScanArea() {
    return _scanArea;
  }

  public ImageData getImageData(String filename) throws IOException {
    return getImageData(filename, _scanArea, 0, 0);
  }

  public ImageData getImageData(String filename, Rectangle defaultArea, int xOff, int yOff) throws IOException {
    // if (!new File(filename).exists())
    // return null;

    if (_imageDataCache.containsKey(filename)) {
      return _imageDataCache.get(filename);
    } else {
      ImageData imageData = null;
      try {
        imageData = new ImageData(filename, defaultArea, _comparator, xOff, yOff);
      } catch (IOException e) {
        System.err.println(e);
        return null;
      }
      if (imageData != null)
        _imageDataCache.put(filename, imageData);
      return imageData;
    }
  }

  public boolean locateGameArea(boolean fullScreen) throws AWTException, IOException, RobotInterruptedException {
    LOGGER.fine("Locating game area ... ");

    _tl = new Pixel(0, 0);

    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    _br = new Pixel(screenSize.width - 3, screenSize.height - 130);

    // public ImageData getImageData(String filename, Rectangle defaultArea, int
    // xOff, int yOff) throws IOException {

    final ImageData br = getImageData("bottomRight.bmp", new Rectangle(1444, 906, 138, 120), 110, 75);
    Pixel p = br.findImage();
    if (p == null) {
      // second try
      p = br.findImage(new Rectangle(screenSize.width / 2, 600, screenSize.width / 2 - 50, screenSize.height - 600));
    }
    if (p != null) {
      _br = p;
      Rectangle area = new Rectangle(_br.x - 60, _tl.y + 150, 60, getGameHeight() / 2);
      ImageData tr = getImageData("topRight.bmp", area, 0, 0);
      Pixel p2 = tr.findImage();
      if (p2 != null) {
        _tl.y = p2.y - 108;
      } else {
        _tl.y = 137;
      }
      LOGGER.fine("FINAL GAME COORDINATES: " + _tl + " - " + _br);
      setKeyAreas();
      return true;
    }

    return false;
  }

  public void writeArea(Rectangle rect, String filename) {
    MyImageIO.writeArea(rect, filename);
  }

  public void writeImage(BufferedImage image, String filename) {
    MyImageIO.writeImage(image, filename);
  }

  public void captureGameAreaDT() {
    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd  HH-mm-ss-SSS");
    String date = sdf.format(Calendar.getInstance().getTime());
    String filename = "popup " + date + ".png";
    captureGameArea(filename);
  }

  public void captureGameArea(String filename) {
    writeArea(new Rectangle(new Point(_tl.x, _tl.y), new Dimension(getGameWidth(), getGameHeight())), filename);
  }

  public Pixel locateImageCoords(String imageName, Rectangle[] area, int xOff, int yOff) throws AWTException,
      IOException, RobotInterruptedException {

    final Robot robot = new Robot();
    final BufferedImage image = ImageIO.read(ImageManager.getImageURL(imageName));
    Pixel[] mask = new ImageMask(imageName).getMask();
    BufferedImage screen;
    int turn = 0;
    Pixel resultPixel = null;
    // MouseRobot mouse = new MouseRobot();
    // mouse.saveCurrentPosition();
    while (turn < area.length) {

      screen = robot.createScreenCapture(area[turn]);
      List<Pixel> foundEdges = findEdge(image, screen, _comparator, null, mask);
      if (foundEdges.size() >= 1) {
        // found
        // AppConsole.print("found it! ");
        int y = area[turn].y;
        int x = area[turn].x;
        resultPixel = new Pixel(foundEdges.get(0).x + x + xOff, foundEdges.get(0).y + y + yOff);
        // System.err.println("AREA: [" + turn + "] " + area[turn]);
        break;
      }
      turn++;
    }
    // mouse.checkUserMovement();
    // AppConsole.println();
    return resultPixel;
  }

  public boolean isOptimized() {
    return _optimized && _br != null && _tl != null;
  }

  private List<Pixel> findEdge(final BufferedImage targetImage, final BufferedImage area, ImageComparator comparator,
      Map<Integer, Color[]> colors, Pixel[] indices) {
    if (DEBUG)
      try {
        MyImageIO.write(area, "PNG", new File("C:/area.png"));
      } catch (IOException e) {
        e.printStackTrace();
      }
    List<Pixel> result = new ArrayList<Pixel>(8);
    for (int i = 0; i < (area.getWidth() - targetImage.getWidth()); i++) {
      for (int j = 0; j < (area.getHeight() - targetImage.getHeight()); j++) {
        final BufferedImage subimage = area.getSubimage(i, j, targetImage.getWidth(), targetImage.getHeight());
        if (DEBUG)
          try {
            MyImageIO.write(subimage, "PNG", new File("C:/subimage.png"));
          } catch (IOException e) {
            e.printStackTrace();
          }
        if (comparator.compare(targetImage, subimage, colors, indices)) {
          // System.err.println("FOUND: " + i + ", " + j);
          result.add(new Pixel(i, j));
          if (result.size() > 0) {// increase in case of trouble
            break;
          }
        }
      }
    }
    return result;
  }

  public void scan() {
    try {
      Robot robot = new Robot();

      BufferedImage screenshot = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
      if (DEBUG)
        MyImageIO.write(screenshot, "PNG", new File("screenshot.png"));
    } catch (HeadlessException | AWTException | IOException e) {

      e.printStackTrace();
    }

  }

  public List<Pixel> compareImages(final BufferedImage image1, final BufferedImage image2, ImageComparator comparator,
      Pixel[] indices) {
    if (DEBUG)
      try {
        ImageIO.write(image2, "PNG", new File("area.png"));
      } catch (IOException e) {
        e.printStackTrace();
      }
    List<Pixel> result = new ArrayList<Pixel>(8);
    for (int i = 0; i <= (image2.getWidth() - image1.getWidth()); i++) {
      for (int j = 0; j <= (image2.getHeight() - image1.getHeight()); j++) {
        final BufferedImage subimage = image2.getSubimage(i, j, image1.getWidth(), image1.getHeight());
        if (DEBUG)
          try {
            MyImageIO.write(subimage, "PNG", new File("subimage.png"));
          } catch (IOException e) {
            e.printStackTrace();
          }

        boolean b = comparator.compare(image1, image2, null, indices);
        // System.err.println("equal: " + b);
        indices = null;
        b = comparator.compare(image1, image2, null, indices);
        // System.err.println("equal2: " + b);
        List<Pixel> list = comparator.findSimilarities(image1, subimage, indices);
        // System.err.println("FOUND: " + list);
      }
    }
    return result;
  }

  public Pixel getBottomRight() {
    return _br;
  }

  public Pixel getTopLeft() {
    return _tl;
  }

  public int getGameWidth() {
    int width = isOptimized() ? _br.x - _tl.x : Toolkit.getDefaultToolkit().getScreenSize().width;
    return width != 0 ? width : Toolkit.getDefaultToolkit().getScreenSize().width;
  }

  public Pixel ensureAreaInGame(Rectangle area) throws RobotInterruptedException {
    Rectangle gameArea = new Rectangle(_tl.x, _tl.y, getGameHeight(), getGameHeight());
    int yy = area.y - gameArea.y;

    int x1 = getGameWidth() / 2;
    int y1 = getGameHeight() / 2;

    if (yy < 0) {
      // too north
      _mouse.drag(_tl.x + 5, y1, _tl.x + 5, y1 - yy + 20);
    } else {
      yy = _br.y - (area.y + area.height);
      if (yy < 0)
        // too south
        _mouse.drag(_tl.x + 5, y1, _tl.x + 5, y1 + yy - 20);
    }

    int xx = area.x - _tl.x;

    if (xx < 0) {
      // too west
      _mouse.drag(x1, _br.y - 5, x1 - xx + 20, _br.y - 5);
    } else {
      xx = _br.x - (area.x + area.width);
      if (xx < 0)
        // too east
        _mouse.drag(x1, _br.y - 5, x1 + xx - 20, _br.y - 5);
    }
    return new Pixel(xx, yy);
  }

  public int getGameHeight() {
    if (isOptimized()) {
      return _br.y - _tl.y == 0 ? Toolkit.getDefaultToolkit().getScreenSize().height : _br.y - _tl.y;
    } else {
      return Toolkit.getDefaultToolkit().getScreenSize().height;
    }
  }

  public void addHandler(Handler handler) {
    LOGGER.addHandler(handler);
  }

  public ImageData generateImageData(String imageFilename) throws IOException {
    return new ImageData(imageFilename, null, _comparator, 0, 0);
  }

  public ImageData setImageData(String imageFilename) throws IOException {
    return getImageData(imageFilename, _scanArea, 0, 0);
  }

  public ImageData generateImageData(String imageFilename, int xOff, int yOff) throws IOException {
    return new ImageData(imageFilename, null, _comparator, xOff, yOff);
  }

  public ImageComparator getComparator() {
    return _comparator;
  }

  public Pixel getSafePoint() {
    return _safePoint;
  }

  public boolean handlePopups() throws IOException, AWTException, RobotInterruptedException {
    boolean found = false;
    int xx;
    Rectangle area;
    Pixel p = scanOneFast("greenX.bmp", null, true);
    found = p != null;
    if (!found) {

      // red x - wide popup
      xx = (getGameWidth() - 624) / 2;
      area = new Rectangle(_tl.x + xx + 624 - 30, _tl.y + 71, 60, 42);
      found = scanOneFast("redX.bmp", area, true) != null;

      // red x - tiny popup
      xx = (getGameWidth() - 282) / 2;
      area = new Rectangle(_tl.x + xx + 282, _tl.y + 71, 40, 40);
      found = found || scanOneFast("redX.bmp", area, true) != null;

    }

    return found;
  }

}
