package uk.gov.hscic.patient.careRecordHtml;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.NarrativeDt;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import ca.uhn.fhir.model.dstu2.valueset.NarrativeStatusEnum;
import java.text.SimpleDateFormat;
import java.util.List;
import uk.gov.hscic.OperationConstants;

public final class FhirSectionBuilder {

    private FhirSectionBuilder() { }

    public static Composition.Section build(HtmlPage htmlPage) {
        CodingDt coding = new CodingDt().setSystem(OperationConstants.SYSTEM_RECORD_SECTION).setCode(htmlPage.getCode()).setDisplay(htmlPage.getName());
        CodeableConceptDt codableConcept = new CodeableConceptDt().addCoding(coding);
        codableConcept.setText(htmlPage.getName());

        NarrativeDt narrative = new NarrativeDt();
        narrative.setStatus(NarrativeStatusEnum.GENERATED);
        narrative.setDivAsString(createHtmlContent(htmlPage));

        return new Composition.Section()
                .setTitle(htmlPage.getName())
                .setCode(codableConcept)
                .setText(narrative);
    }

    private static String createHtmlContent(HtmlPage htmlPage) {
        StringBuilder stringBuilder = new StringBuilder("<div>");

        // Add sections
        for (PageSection pageSection : htmlPage.getPageSections()) {
            stringBuilder.append("<div>");

            // Header
            stringBuilder.append("<h2>").append(pageSection.getHeader()).append("</h2>");

            // Date Range Banner
            if (pageSection.getFromDate() != null && pageSection.getToDate() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
                stringBuilder.append("<div><p>For the period '").append(dateFormat.format(pageSection.getFromDate())).append("' to '").append(dateFormat.format(pageSection.getToDate())).append("'</p></div>");
            } else {
                stringBuilder.append("<div><p>All relevant items subject to Patient preferences and/or legal exclusions</p></div>");
            }

            // Additional Banners
            if (!pageSection.getBanners().isEmpty()) {
                stringBuilder.append("<div>");

                for (String banner : pageSection.getBanners()) {
                    stringBuilder.append("<p>").append(banner).append("</p>");
                }

                stringBuilder.append("</div>");
            }

            // Table
            PageSectionHtmlTable table = pageSection.getTable();

            if (table == null || table.getRows().isEmpty()) {
                stringBuilder.append("<div><p>No '").append(pageSection.getHeader()).append("' data is recorded for this patient.</p></div>");
            } else {
                stringBuilder.append("<div><table><thead><tr>");

                for (String header : table.getHeaders()) {
                    stringBuilder.append("<th>").append(header).append("</th>");
                }

                stringBuilder.append("</tr></thead><tbody>");

                for (List<Object> row : table.getRows()) {
                    stringBuilder.append("<tr>");

                    for (Object object : row) {
                        stringBuilder.append("<td>").append(object).append("</td>");
                    }

                    stringBuilder.append("</tr>");
                }

                stringBuilder.append("</tbody></table></div>");
            }

            stringBuilder.append("</div>");
        }

        return stringBuilder.append("</div>").toString();
    }
}
