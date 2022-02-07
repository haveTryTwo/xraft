package in.xnnyygn.xraft.core.log.event;

import in.xnnyygn.xraft.core.log.entry.GroupConfigEntry;

public class GroupConfigEntryBatchRemovedEvent { // NOTE: htt, 成员变更批量删除事件

    private final GroupConfigEntry firstRemovedEntry; // NOTE: htt, 首个被删除的 节点日志条目（如新增节点或删除节点日志条目）

    public GroupConfigEntryBatchRemovedEvent(GroupConfigEntry firstRemovedEntry) {
        this.firstRemovedEntry = firstRemovedEntry;
    }

    public GroupConfigEntry getFirstRemovedEntry() {
        return firstRemovedEntry;
    }

}
