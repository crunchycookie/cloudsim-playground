/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.models;

import org.crunchycookie.playground.cloudsim.constants.EC2Constants.EC2Instances;

public class EC2InstanceCharacteristics {

  public EC2InstanceCharacteristics(Enum<EC2Instances> id, int numberOfECU, int memoryInGB,
      double hourlyRateInUSD, int mips) {
    this.id = id;
    this.numberOfECU = numberOfECU;
    this.memoryInGB = memoryInGB;
    this.hourlyRateInUSD = hourlyRateInUSD;
    this.MIPS = mips;
  }

  public EC2InstanceCharacteristics() {
  }

  private Enum<EC2Instances> id;

  private int numberOfECU;

  private int memoryInGB;

  private double hourlyRateInUSD;

  private int MIPS;

  public Enum<EC2Instances> getId() {
    return id;
  }

  public void setId(Enum<EC2Instances> id) {
    this.id = id;
  }

  public int getNumberOfECU() {
    return numberOfECU;
  }

  public void setNumberOfECU(int numberOfECU) {
    this.numberOfECU = numberOfECU;
  }

  public int getMemoryInGB() {
    return memoryInGB;
  }

  public void setMemoryInGB(int memoryInGB) {
    this.memoryInGB = memoryInGB;
  }

  public double getHourlyRateInUSD() {
    return hourlyRateInUSD;
  }

  public void setHourlyRateInUSD(double hourlyRateInUSD) {
    this.hourlyRateInUSD = hourlyRateInUSD;
  }

  public int getMIPS() {
    return MIPS;
  }

  public void setMIPS(int MIPS) {
    this.MIPS = MIPS;
  }
}
