package in.xnnyygn.xraft.core.log.event;

import in.xnnyygn.xraft.core.log.entry.GroupConfigEntry;

public class GroupConfigEntryCommittedEvent extends AbstractEntryEvent<GroupConfigEntry> { // NOTE: htt, 成员变更提交事件，封装成员变更日志条目

    public GroupConfigEntryCommittedEvent(GroupConfigEntry entry) {
        super(entry);
    }

}
