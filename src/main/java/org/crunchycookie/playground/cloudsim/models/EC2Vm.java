/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.models;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;

public class EC2Vm extends Vm {

  private double hourlyRate;

  public EC2Vm(int id, int userId, double mips, int numberOfPes, int ram, double hourlyRate,
      CloudletScheduler cloudletScheduler) {

    super(id, userId, mips, numberOfPes, ram, 1000, 1000, "Xen", cloudletScheduler);
    this.hourlyRate = hourlyRate;
  }

  public double getHourlyRate() {
    return hourlyRate;
  }

  public void setHourlyRate(double hourlyRate) {
    this.hourlyRate = hourlyRate;
  }
}
