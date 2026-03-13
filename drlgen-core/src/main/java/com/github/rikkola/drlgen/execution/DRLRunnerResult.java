package com.github.rikkola.drlgen.execution;

import java.util.List;

public record DRLRunnerResult(List<Object> objects, int firedRules) {
}
