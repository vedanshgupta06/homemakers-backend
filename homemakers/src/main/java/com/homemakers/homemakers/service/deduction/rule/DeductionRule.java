package com.homemakers.homemakers.service.deduction.rule;

import com.homemakers.homemakers.service.deduction.DeductionResult;

public interface DeductionRule<T> {

    /**
     * Whether this rule applies to the given event.
     */
    boolean supports(T event);

    /**
     * Evaluate the event and return the deduction result.
     * Called only if supports(event) == true.
     */
    DeductionResult evaluate(T event);
}
