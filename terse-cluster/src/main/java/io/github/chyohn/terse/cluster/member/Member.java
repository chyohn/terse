package io.github.chyohn.terse.cluster.member;

import io.github.chyohn.terse.cluster.config.ConfigConstant;
import io.github.chyohn.terse.cluster.utils.InetUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Member implements Serializable {
    int port;
    String ip;
    String address;
    MemberState state;
    Map<String, Object> extendInfo;
    int failAccessCnt = 0;

    public static Member of(String member) {

        String address = member;
        int port = ConfigConstant.DEFAULT_CLUSTER_PORT;
        String[] info = InetUtils.splitIPPortStr(address);
        if (info.length > 1) {
            address = info[0];
            port = Integer.parseInt(info[1]);
        }

        return Member.builder()
            .address(address + InetUtils.IP_PORT_SPLITER + port)
            .ip(address)
            .port(port)
            .state(MemberState.SUSPICIOUS)
            .build();
    }

    public static boolean isBasicInfoChanged(Member newMember, Member oldMember) {
        if (null == oldMember) {
            return null != newMember;
        }
        if (!oldMember.getIp().equals(newMember.getIp())) {
            return true;
        }
        if (oldMember.getPort() != newMember.getPort()) {
            return true;
        }
        if (!oldMember.getAddress().equals(newMember.getAddress())) {
            return true;
        }
        if (!oldMember.getState().equals(newMember.getState())) {
            return true;
        }
        return isChangedInExtendInfo(newMember, oldMember);
    }

    public static void copy(Member newMember, Member oldMember) {
        oldMember.setIp(newMember.getIp());
        oldMember.setPort(newMember.getPort());
        oldMember.setState(newMember.getState());
        oldMember.setExtendInfo(newMember.getExtendInfo());
        oldMember.setAddress(newMember.getAddress());
        oldMember.setFailAccessCnt(newMember.getFailAccessCnt());
    }

    public static boolean isChangedInExtendInfo(Member newMember, Member oldMember) {
        if (newMember.getExtendInfo() == null && oldMember.getExtendInfo() == null) {
            return false;
        }
        if (newMember.getExtendInfo() == null || oldMember.getExtendInfo() == null) {
            return true;
        }

        if (newMember.getExtendInfo().size() != oldMember.getExtendInfo().size()) {
            return true;
        }

        for (Entry<String, Object> newEntry : newMember.getExtendInfo().entrySet()) {
            if (!newEntry.getValue().equals(oldMember.getExtendInfo().get(newEntry.getKey()))) {
                return true;
            }
        }

        for (Entry<String, Object> oldEntry : oldMember.getExtendInfo().entrySet()) {
            if (!oldEntry.getValue().equals(newMember.getExtendInfo().get(oldEntry.getKey()))) {
                return true;
            }
        }

        return false;
    }

}
