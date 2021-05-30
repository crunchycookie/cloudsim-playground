/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.models;

import org.cloudbus.cloudsim.UtilizationModel;

public class MinThreshouldBasedUtilizationModel implements UtilizationModel {

  private double threshold;

  public MinThreshouldBasedUtilizationModel(double threshold) {

    this.threshold = threshold;
  }

  @Override
  public double getUtilization(double time) {
    return threshold;
  }
}
