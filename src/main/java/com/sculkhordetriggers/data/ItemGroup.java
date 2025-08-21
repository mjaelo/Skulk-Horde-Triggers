package com.sculkhordetriggers.data;

import java.util.List;

public record ItemGroup(
    String groupName,
    TriggerType triggerType,
    String triggerValue,
    List<String> keywords,
    List<String> namespaces
) {
    public enum TriggerType {
        DIMENSION,
        BIOME,
        EFFECT,
        ADVANCEMENT,
        ITEM,
        MOB
    }
}
