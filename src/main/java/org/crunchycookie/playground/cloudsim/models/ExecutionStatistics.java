/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.models;

import java.util.HashMap;
import java.util.Map;

public class ExecutionStatistics {

  // VmId -> HostId.
  private Map<Integer, Integer> vmToHostMapping;

  // HostId -> EC2Character.
  private Map<Integer, EC2InstanceCharacteristics> vmToEC2Characteristics;

  public ExecutionStatistics() {
    vmToHostMapping = new HashMap<>();
    vmToEC2Characteristics = new HashMap<>();
  }

  public void setVmToHostMapping(Integer vmId, Integer hostId) {
    this.vmToHostMapping.put(vmId, hostId);
  }

  public void setVmToEC2Characteristics(Integer vmId, EC2InstanceCharacteristics vm) {
    this.vmToEC2Characteristics.put(vmId, vm);
  }

  public Map<Integer, Integer> getVmToHostMapping() {
    return vmToHostMapping;
  }

  public void setVmToHostMapping(Map<Integer, Integer> vmToHostMapping) {
    this.vmToHostMapping = vmToHostMapping;
  }

  public Integer getMappedHost(Integer vmId) {
    return vmToHostMapping.get(vmId);
  }

  public Map<Integer, EC2InstanceCharacteristics> getVmToEC2Characteristics() {
    return vmToEC2Characteristics;
  }

  public void setVmToEC2Characteristics(
      Map<Integer, EC2InstanceCharacteristics> vmToEC2Characteristics) {
    this.vmToEC2Characteristics = vmToEC2Characteristics;
  }
}
