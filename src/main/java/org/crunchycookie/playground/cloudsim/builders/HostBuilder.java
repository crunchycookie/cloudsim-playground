/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.builders;

import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * A builder class to build a datacenter host in much friendly manner.
 */
public class HostBuilder {

  int id;

  int numberOfCores;

  int mipsPerCore;

  int amountOfRamInGBs;

  int amountOfStorageInTBs;

  int bandwidth;

  public HostBuilder(int id) {
    this.id = id;
  }

  public HostBuilder withNumberOfCores(int numberOfCores) {
    this.numberOfCores = numberOfCores;
    return this;
  }

  public HostBuilder withMipsPerCore(int mipsPerCore) {
    this.mipsPerCore = mipsPerCore;
    return this;
  }

  public HostBuilder withAmountOfRamInGBs(int amountOfRamInGBs) {
    this.amountOfRamInGBs = amountOfRamInGBs;
    return this;
  }

  public HostBuilder withBandwidth(int bandwidth) {
    this.bandwidth = bandwidth;
    return this;
  }

  public HostBuilder withAmountOfStorageInGBs(int amountOfStorageInTBs) {
    this.amountOfStorageInTBs = amountOfStorageInTBs;
    return this;
  }

  public Host build() {

    // Create processing elements;
    List<Pe> processingElements = new ArrayList<>();
    for (int coreIndex = 0; coreIndex < this.numberOfCores; coreIndex++) {
      processingElements.add(new Pe(coreIndex, new PeProvisionerSimple(this.mipsPerCore)));
    }

    return new Host(
        this.id,
        new RamProvisionerSimple(this.amountOfRamInGBs * 1024),
        new BwProvisionerSimple(this.bandwidth),
        this.amountOfStorageInTBs * 1024 * 1024,
        processingElements,
        new VmSchedulerTimeShared(processingElements)
    );
  }
}
