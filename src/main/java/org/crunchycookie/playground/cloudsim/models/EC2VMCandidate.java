/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.models;

import static org.crunchycookie.playground.cloudsim.constants.EC2Constants.EC2_INSTANCE_TYPES;

import org.crunchycookie.playground.cloudsim.constants.EC2Constants.EC2Instances;

/**
 * This class represents a selected EC2 VM instance. Based on the tasks, a VMCandidate can keep
 * track of the available core count, and the available amount of memory.
 */
public class EC2VMCandidate {

  private final Enum<EC2Instances> type;

  private int availableCores;

  private int availableMemoryInMB;

  public EC2VMCandidate(Enum<EC2Instances> ec2InstanceType) {

    this.type = ec2InstanceType;

    EC2InstanceCharacteristics instanceCharacteristics = EC2_INSTANCE_TYPES.get(ec2InstanceType);
    this.availableCores = instanceCharacteristics.getNumberOfECU();
    this.availableMemoryInMB = instanceCharacteristics.getMemoryInGB() * 1024;
  }

  public int getAvailableCores() {
    return availableCores;
  }

  public void setAvailableCores(int availableCores) {
    this.availableCores = availableCores;
  }

  public int getAvailableMemoryInMB() {
    return availableMemoryInMB;
  }

  public void setAvailableMemoryInMB(int availableMemoryInMB) {
    this.availableMemoryInMB = availableMemoryInMB;
  }

  public Enum<EC2Instances> getType() {
    return type;
  }
}
