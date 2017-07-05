package com.horowitz.tm;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Hashtable;
import java.util.Map;

public class Stats {

  private Map<String, Integer> _map = new Hashtable<>();
  PropertyChangeSupport _support = new PropertyChangeSupport(this);

  public void register(String counterName) {
    Integer cnt = 0;
    if (_map.containsKey(counterName))
      cnt = _map.get(counterName);
    _map.put(counterName, cnt + 1);
    _support.firePropertyChange(counterName, cnt, (Integer)(cnt + 1));
  }

  public int getCount(String key) {
    int cnt = 0;
    if (_map.containsKey(key))
      cnt = _map.get(key);
    return cnt;
  }

  public void clear() {
    _map.clear();
    _support.firePropertyChange("ALL", true, false);
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    _support.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    _support.removePropertyChangeListener(listener);
  }

  public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    _support.addPropertyChangeListener(propertyName, listener);
  }

  public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    _support.removePropertyChangeListener(propertyName, listener);
  }

}
