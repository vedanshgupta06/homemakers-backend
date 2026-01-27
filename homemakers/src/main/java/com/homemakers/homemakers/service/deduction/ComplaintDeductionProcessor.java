package com.homemakers.homemakers.service.deduction;

import com.homemakers.homemakers.model.DeductionSourceType;
import com.homemakers.homemakers.model.event.ComplaintEvent;
import com.homemakers.homemakers.service.deduction.rule.ComplaintDeductionRule;
import org.springframework.stereotype.Service;

@Service
public class ComplaintDeductionProcessor {

    private final ComplaintDeductionRule rule;
    private final DeductionGenerator generator;

    public ComplaintDeductionProcessor(
            ComplaintDeductionRule rule,
            DeductionGenerator generator
    ) {
        this.rule = rule;
        this.generator = generator;
    }

    public void process(ComplaintEvent event) {

        // Step 1: check applicability
        if (!rule.supports(event)) {
            return;
        }

        // Step 2: evaluate rule
        DeductionResult result = rule.evaluate(event);

        if (result == null) {
            return;
        }

        // Step 3: generate deduction (ONLY place where save happens)
        generator.generate(
                event.getProviderId(),
                DeductionSourceType.COMPLAINT,
                event.getComplaintId(),
                result
        );
    }
}
