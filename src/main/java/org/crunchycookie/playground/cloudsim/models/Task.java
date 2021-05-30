/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.models;

public class Task {

  private String submissionTime;

  private int mis;

  private int minimumMemoryToExecute;

  private int minimumStorageToExecute;

  private int wallClockTime;

  public String getSubmissionTime() {
    return submissionTime;
  }

  public void setSubmissionTime(String submissionTime) {
    this.submissionTime = submissionTime;
  }

  public int getMis() {
    return mis;
  }

  public void setMis(int mis) {
    this.mis = mis;
  }

  public int getMinimumMemoryToExecute() {
    return minimumMemoryToExecute;
  }

  public void setMinimumMemoryToExecute(int minimumMemoryToExecute) {
    this.minimumMemoryToExecute = minimumMemoryToExecute;
  }

  public int getMinimumStorageToExecute() {
    return minimumStorageToExecute;
  }

  public void setMinimumStorageToExecute(int minimumStorageToExecute) {
    this.minimumStorageToExecute = minimumStorageToExecute;
  }

  public int getWallClockTime() {
    return wallClockTime;
  }

  public void setWallClockTime(int wallClockTime) {
    this.wallClockTime = wallClockTime;
  }
}
