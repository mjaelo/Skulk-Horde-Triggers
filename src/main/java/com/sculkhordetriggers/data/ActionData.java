package com.sculkhordetriggers.data;

import java.util.List;

public record ActionData(
        List<TriggerData> triggers,
        List<EffectData> effects,
        String failMessage
) {}
