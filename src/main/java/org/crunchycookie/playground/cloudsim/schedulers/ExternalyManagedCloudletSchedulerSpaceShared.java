/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.schedulers;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;

/**
 * This scheduler act as same as the CloudletSchedulerSpaceShared class, but accepts cloudlets only
 * if processing elements are availalbe. Any pending cloudlet is sent back to the broker.
 */
public class ExternalyManagedCloudletSchedulerSpaceShared extends CloudletSchedulerSpaceShared {

  public ExternalyManagedCloudletSchedulerSpaceShared() {
    super();
  }

  @Override
  public double cloudletSubmit(Cloudlet cloudlet, double fileTransferTime) {
    return super.cloudletSubmit(cloudlet, fileTransferTime);
  }

  public int getIdleCoresCount() {

    return currentCpus - usedPes;
  }
}
