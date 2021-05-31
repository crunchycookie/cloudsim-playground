/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.constants;

import java.util.Map;
import org.crunchycookie.playground.cloudsim.models.EC2InstanceCharacteristics;

public class EC2Constants {

  public static final Map<Enum<EC2Instances>, EC2InstanceCharacteristics> EC2_INSTANCE_TYPES = Map
      .of(
          EC2Instances.EC2_VM_ID_MEDIUM,
          new EC2InstanceCharacteristics(
              EC2Instances.EC2_VM_ID_MEDIUM,
              1,
              2,
              0.0333,
              1000
          ),
          EC2Instances.EC2_VM_ID_LARGE,
          new EC2InstanceCharacteristics(
              EC2Instances.EC2_VM_ID_LARGE,
              2,
              4,
              0.0666,
              1000
          ),
          EC2Instances.EC2_VM_ID_XLARGE,
          new EC2InstanceCharacteristics(
              EC2Instances.EC2_VM_ID_XLARGE,
              4,
              8,
              0.1332,
              1000
          ),
          EC2Instances.EC2_VM_ID_2XLARGE,
          new EC2InstanceCharacteristics(
              EC2Instances.EC2_VM_ID_2XLARGE,
              8,
              16,
              0.2664,
              1000
          )
      );

  public enum EC2Instances {
    EC2_VM_ID_MEDIUM,
    EC2_VM_ID_LARGE,
    EC2_VM_ID_XLARGE,
    EC2_VM_ID_2XLARGE
  }
}
