/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.models;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;

public class Task {

  private String submissionTime;

  private long mis;

  private int minimumMemoryToExecute;

  private int minimumStorageToExecute;

  private int wallClockTime;

  public String getSubmissionTime() {
    return submissionTime;
  }

  public void setSubmissionTime(String submissionTime) {
    this.submissionTime = submissionTime;
  }

  public long getMis() {
    return mis;
  }

  public void setMis(long mis) {
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

  public Cloudlet getCloudletForTheTargetVm(int id, Vm vm) {

    // Cloudlet properties.
    int cloudletId = id;
    long length = this.getMis();
    long fileSize = 300;
    long outputSize = 300;

    // Asssumption: BW is utilized as per the same rate as with memory.
    return new Cloudlet(cloudletId, length, 1, fileSize, outputSize,
        new UtilizationModelFull(),
        new MinThreshouldBasedUtilizationModel(minimumMemoryToExecute / vm.getRam()),
        new MinThreshouldBasedUtilizationModel(minimumMemoryToExecute / vm.getRam()));
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    sb.append(getSubmissionTime());
    sb.append(" ");
    sb.append(getMis());
    sb.append(" ");
    sb.append(getMinimumMemoryToExecute());
    sb.append(" ");
    sb.append(getMinimumStorageToExecute());
    sb.append(" ");
    sb.append(getWallClockTime());

    return sb.toString();
  }
}
