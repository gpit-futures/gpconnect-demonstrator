package org.rippleosi.patient.problems.search;

import java.util.List;
import java.util.Map;

import org.rippleosi.common.exception.DataNotFoundException;
import org.rippleosi.common.service.AbstractQueryStrategy;
import org.rippleosi.patient.problems.model.ProblemDetails;

/**
 */
public class ProblemDetailsQueryStrategy extends AbstractQueryStrategy<ProblemDetails> {

    private final String problemId;

    ProblemDetailsQueryStrategy(String patientId, String problemId) {
        super(patientId);
        this.problemId = problemId;
    }

    @Override
    public String getQuery(String ehrId) {
        return "select a/uid/value as uid, " +
                "a_a/items/data[at0001]/items[at0002]/value/value as problem, " +
                "a_a/items/data[at0001]/items[at0002]/value/defining_code/code_string as problem_code, " +
                "a_a/items/data[at0001]/items[at0002]/value/defining_code/terminology_id/value as problem_terminology, " +
                "a_a/items/data[at0001]/items[at0009]/value/value as description, " +
                "a_a/items/data[at0001]/items[at0003]/value/value as onset_date " +
                "from EHR e[ehr_id/value='" + ehrId + "'] " +
                "contains COMPOSITION a[openEHR-EHR-COMPOSITION.care_summary.v0] " +
                "contains SECTION a_a[openEHR-EHR-SECTION.problems_issues_rcp.v1] " +
                "where a/name/value='Problem list' " +
                "and a/uid/value='" + problemId + "' ";
    }

    @Override
    public ProblemDetails transform(List<Map<String, Object>> resultSet) {

        if (resultSet.isEmpty()) {
            throw new DataNotFoundException("No results found");
        }

        Map<String, Object> data = resultSet.get(0);

        return new ProblemDetailsTransformer().transform(data);
    }
}