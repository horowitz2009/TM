package com.horowitz.tm;

public class Attributes {

  public int a1;
  public int a2;
  public int a3;

  public Attributes(int a1, int a2, int a3) {
    super();
    this.a1 = a1;
    this.a2 = a2;
    this.a3 = a3;
  }

  public int sum() {
    return a1 + a2 + a3;
  }

  public double avg() {
    return sum() / 3;
  }

  public double deviation() {
    double m = avg();
    double s = (Math.pow(a1 - m, 2) + Math.pow(a2 - m, 2) + Math.pow(a3 - m, 2)) / 3;
    return Math.sqrt(s);
  }

  @Override
  public String toString() {
    return a1 + "  " + a2 + "  " + a3;
  }

}