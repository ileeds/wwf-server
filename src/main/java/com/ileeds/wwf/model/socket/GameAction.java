package com.ileeds.wwf.model.socket;

public record GameAction(String playerKey, Direction direction) {
  public enum Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT
  }
}
