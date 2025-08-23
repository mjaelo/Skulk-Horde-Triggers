package com.sculkhordetriggers.data;

import java.util.List;

public record TriggerData(
        TriggerType type,
        float probability,
        List<String> keywords
) {}
