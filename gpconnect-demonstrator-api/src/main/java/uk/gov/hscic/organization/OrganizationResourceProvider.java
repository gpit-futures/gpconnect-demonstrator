package uk.gov.hscic.organization;

import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Bundle.Entry;
import ca.uhn.fhir.model.dstu2.resource.OperationOutcome;
import ca.uhn.fhir.model.dstu2.resource.Organization;
import ca.uhn.fhir.model.dstu2.resource.Parameters;
import ca.uhn.fhir.model.dstu2.resource.Parameters.Parameter;
import ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.IssueSeverityEnum;
import ca.uhn.fhir.model.dstu2.valueset.IssueTypeEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hscic.OperationOutcomeFactory;
import uk.gov.hscic.SystemCode;
import uk.gov.hscic.SystemURL;
import uk.gov.hscic.model.organization.OrganizationDetails;

@Component
public class OrganizationResourceProvider implements IResourceProvider {

    @Autowired
    private GetScheduleOperation getScheduleOperation;

    @Autowired
    private OrganizationSearch organizationSearch;

    @Override
    public Class<Organization> getResourceType() {
        return Organization.class;
    }

    @Read()
    public Organization getOrganizationById(@IdParam IdDt organizationId) {
        OrganizationDetails organizationDetails = organizationSearch.findOrganizationDetails(organizationId.getIdPart());

        if (organizationDetails == null) {
            OperationOutcome operationalOutcome = new OperationOutcome();
            operationalOutcome.addIssue().setSeverity(IssueSeverityEnum.ERROR).setDetails("No organization details found for organization ID: " + organizationId.getIdPart());
            throw new InternalErrorException("No organization details found for organization ID: " + organizationId.getIdPart(), operationalOutcome);
        }

        return organizaitonDetailsToOrganizationResourceConverter(organizationDetails);
    }

    @Search
    public List<Organization> getOrganizationsByODSCode(@RequiredParam(name = Organization.SP_IDENTIFIER) TokenParam tokenParam) {
        if (StringUtils.isBlank(tokenParam.getSystem()) || StringUtils.isBlank(tokenParam.getValue())) {
            throw OperationOutcomeFactory.buildOperationOutcomeException(
                    new InvalidRequestException("Missing identifier token"),
                    SystemCode.INVALID_PARAMETER, IssueTypeEnum.INVALID_CONTENT);
        }

        switch (tokenParam.getSystem()) {
            case SystemURL.ID_ODS_ORGANIZATION_CODE:
                return organizationSearch.findOrganizationDetailsByOrgODSCode(tokenParam.getValue())
                        .stream()
                        .map(this::organizaitonDetailsToOrganizationResourceConverter)
                        .collect(Collectors.toList());

            case SystemURL.ID_ODS_SITE_CODE:
                return organizationSearch.findOrganizationDetailsBySiteODSCode(tokenParam.getValue())
                        .stream()
                        .map(this::organizaitonDetailsToOrganizationResourceConverter)
                        .collect(Collectors.toList());

            default:
                throw OperationOutcomeFactory.buildOperationOutcomeException(
                    new InvalidRequestException("Invalid system code"),
                    SystemCode.INVALID_PARAMETER, IssueTypeEnum.INVALID_CONTENT);
        }
    }

    @Operation(name = "$gpc.getschedule")
    public Bundle getSchedule(@IdParam IdDt organizationId, @ResourceParam Parameters params) {
        Bundle bundle = new Bundle();
        bundle.setType(BundleTypeEnum.DOCUMENT);
        OperationOutcome operationOutcome = new OperationOutcome();

        // params
        List<Parameter> parameters = params.getParameter();
        String planningHorizonStart = null;
        String planningHorizonEnd = null;

        for (int p = 0; p < parameters.size(); p++) {
            Parameter parameter = parameters.get(p);
            switch (parameter.getName()) {
                case "timePeriod":
                    PeriodDt timePeriod = (PeriodDt) parameter.getValue();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    planningHorizonStart = dateFormat.format(timePeriod.getStart());
                    planningHorizonEnd = dateFormat.format(timePeriod.getEnd());
                    break;
            }
        }

        if (organizationId != null && planningHorizonStart != null && planningHorizonEnd != null) {
            getScheduleOperation.populateBundle(bundle, operationOutcome, organizationId, planningHorizonStart, planningHorizonEnd);
        } else {
            String msg = String.format("Not all of the mandatory parameters were provided - orgId - %s planningHorizonStart - %s planningHorizonEnd - %s", organizationId, planningHorizonStart, planningHorizonEnd);
            operationOutcome.addIssue().setSeverity(IssueSeverityEnum.INFORMATION).setDetails(msg);

            Entry operationOutcomeEntry = new Entry();
            operationOutcomeEntry.setResource(operationOutcome);
            bundle.addEntry(operationOutcomeEntry);
        }

        return bundle;
    }

    private Organization organizaitonDetailsToOrganizationResourceConverter(OrganizationDetails organizationDetails) {
        Organization organization = new Organization()
                .setName(organizationDetails.getOrgName())
                .addIdentifier(new IdentifierDt(SystemURL.ID_ODS_ORGANIZATION_CODE, organizationDetails.getOrgCode()))
                .addIdentifier(new IdentifierDt(SystemURL.ID_ODS_SITE_CODE, organizationDetails.getSiteCode()));

        organization.setId(String.valueOf(organizationDetails.getId()));

        organization.getMeta()
                .addProfile(SystemURL.SD_GPC_ORGANIZATION)
                .setLastUpdated(organizationDetails.getLastUpdated())
                .setVersionId(String.valueOf(organizationDetails.getLastUpdated().getTime()));

        return organization;
    }
}
