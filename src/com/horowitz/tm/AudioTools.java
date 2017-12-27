package com.horowitz.tm;

import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioTools {

  private static void play(String filename) throws IOException {
    class AudioListener implements LineListener {
      private boolean done = false;

      @Override
      public synchronized void update(LineEvent event) {
        Type eventType = event.getType();
        if (eventType == Type.STOP || eventType == Type.CLOSE) {
          done = true;
          notifyAll();
        }
      }

      public synchronized void waitUntilDone() throws InterruptedException {
        while (!done) {
          wait();
        }
      }
    }
    AudioListener listener = new AudioListener();
    AudioInputStream audioInputStream;
    try {
      audioInputStream = AudioSystem.getAudioInputStream(AudioTools.class.getClassLoader().getResource("error.wav"));
      try {
        Clip clip = AudioSystem.getClip();
        clip.addLineListener(listener);
        clip.open(audioInputStream);
        try {
          clip.start();
          listener.waitUntilDone();
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
          clip.close();
        }
      } catch (LineUnavailableException e) {
        e.printStackTrace();
      } finally {
        audioInputStream.close();
      }
    } catch (UnsupportedAudioFileException e1) {
      e1.printStackTrace();
    }

  }

  public static void playWarning() {
    new Thread(new Runnable() {
      public void run() {
        try {
          play("error.wav");
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }).start();

  }

  public static void main(String[] args) {
    try {
      AudioTools.playWarning();
      //Thread.sleep(5000);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
