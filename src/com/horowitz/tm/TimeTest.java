package com.horowitz.tm;

import java.util.Calendar;

import com.horowitz.commons.DateUtils;

public class TimeTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    Calendar cal = findNextMorningTime(Calendar.getInstance());
    System.err.println(cal.getTime());
    
    cal.add(Calendar.HOUR_OF_DAY, -2);
    System.err.println(cal.getTime());
    
    
    cal = findNextMorningTime(cal);
    System.err.println(cal.getTime());
    
    
    long now = System.currentTimeMillis();
    long then = cal.getTimeInMillis();
    long t = then - now;
    
    System.err.println(DateUtils.fancyTime2(t, false));

  }
  
  public static Calendar findNextMorningTime(Calendar now) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(now.getTime());
    //desired time: 6am
    int h = cal.get(Calendar.HOUR_OF_DAY);
    if (h > 6) {
      cal.add(Calendar.DAY_OF_MONTH, 1);
    }
    cal.set(Calendar.HOUR_OF_DAY, 6);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    return cal;
  }

}
