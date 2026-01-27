package com.homemakers.homemakers.service.deduction.rule;

import com.homemakers.homemakers.model.DeductionType;
import com.homemakers.homemakers.model.event.ComplaintEvent;
import com.homemakers.homemakers.model.event.ComplaintSeverity;
import com.homemakers.homemakers.service.deduction.DeductionResult;
import org.springframework.stereotype.Service;

@Service
public class ComplaintDeductionRule
        implements DeductionRule<ComplaintEvent> {

    @Override
    public boolean supports(ComplaintEvent event) {
        return event.isValidated()
                && event.getSeverity() != ComplaintSeverity.LOW;
    }

    @Override
    public DeductionResult evaluate(ComplaintEvent event) {

        if (event.getSeverity() == ComplaintSeverity.MEDIUM) {
            return new DeductionResult(
                    DeductionType.SERVICE_QUALITY,
                    100,
                    "Validated medium severity complaint"
            );

        }

        // HIGH severity

        return new DeductionResult(
                DeductionType.SERVICE_QUALITY,
                300,
                "Validated high severity complaint"
        );

    }
}
