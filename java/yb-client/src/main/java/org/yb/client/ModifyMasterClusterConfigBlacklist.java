// Copyright (c) YugaByte, Inc.

package org.yb.client;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.yb.annotations.InterfaceAudience;
import org.yb.Common.HostPortPB;
import org.yb.master.Master;


@InterfaceAudience.Public
public class ModifyMasterClusterConfigBlacklist extends AbstractModifyMasterClusterConfig {
  private List<HostPortPB> modifyHosts;
  private boolean isAdd;
  public ModifyMasterClusterConfigBlacklist(YBClient client, List<HostPortPB> modifyHosts,
      boolean isAdd) {
    super(client);
    this.modifyHosts = modifyHosts;
    this.isAdd = isAdd;
  }

  @Override
  protected Master.SysClusterConfigEntryPB modifyConfig(Master.SysClusterConfigEntryPB config) {
    // Modify the blacklist.
    Master.SysClusterConfigEntryPB.Builder configBuilder =
        Master.SysClusterConfigEntryPB.newBuilder(config);

    // Use a TreeSet so we can prune duplicates while keeping HostPortPB as storage.
    TreeSet<HostPortPB> finalHosts =
      new TreeSet<HostPortPB>(new Comparator<HostPortPB>() {
      @Override
      public int compare(HostPortPB a, HostPortPB b) {
        if (a.getHost().equals(b.getHost())) {
          int portA = a.getPort();
          int portB = b.getPort();
          if (portA < portB) {
            return -1;
          } else if (portA == portB) {
            return 0;
          } else {
            return 1;
          }
        } else {
          return a.getHost().compareTo(b.getHost());
        }
      }
    });
    // Add up the current list.
    finalHosts.addAll(config.getServerBlacklist().getHostsList());
    // Add or remove the given list of servers.
    if (isAdd) {
      finalHosts.addAll(modifyHosts);
    } else {
      finalHosts.removeAll(modifyHosts);
    }
    // Change the blacklist in the local config copy.
    Master.BlacklistPB blacklist =
        Master.BlacklistPB.newBuilder().addAllHosts(finalHosts).build();
    configBuilder.setServerBlacklist(blacklist);
    return configBuilder.build();
  }
}