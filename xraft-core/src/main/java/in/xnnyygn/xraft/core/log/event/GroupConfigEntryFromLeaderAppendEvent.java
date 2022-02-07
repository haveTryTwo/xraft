package in.xnnyygn.xraft.core.log.event;

import in.xnnyygn.xraft.core.log.entry.GroupConfigEntry;

public class GroupConfigEntryFromLeaderAppendEvent extends AbstractEntryEvent<GroupConfigEntry> { // NOTE: htt, 添加leaer事件， 封装成员变更日志条目

    public GroupConfigEntryFromLeaderAppendEvent(GroupConfigEntry entry) {
        super(entry);
    }

}
