package uk.gov.hscic.common.filters;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.resource.OperationOutcome;
import ca.uhn.fhir.model.dstu2.valueset.IssueSeverityEnum;
import ca.uhn.fhir.model.dstu2.valueset.IssueTypeEnum;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import ca.uhn.fhir.rest.method.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import uk.gov.hscic.common.util.NhsCodeValidator;

@Component
public class FhirRequestAuthInterceptor extends AuthorizationInterceptor {

	static Logger authLog = Logger.getLogger("AuthLog");

	@Override
	public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {

		String authorizationStr = theRequestDetails.getHeader("Authorization");
		String[] jwtHeaderComponents = authorizationStr.split(" ");
		Base64.Decoder decoder = Base64.getDecoder();
		String[] tokenComponents = jwtHeaderComponents[1].split("\\.");

		try {
			decoder.decode(tokenComponents[1]);
		} catch (IllegalArgumentException iae) {
			throw new InvalidRequestException("Not Base 64");
		}

		if (jwtHeaderComponents.length == 2 && "Bearer".equalsIgnoreCase(jwtHeaderComponents[0])) {

			String claimsJsonString = new String(Base64.getDecoder().decode(tokenComponents[1]));

			JSONObject claimsJsonObject = new JSONObject(claimsJsonString);

			System.out.println(claimsJsonObject);

			try {

				String requestScopeType = claimsJsonObject.getJSONObject("requested_record").getString("resourceType");
				JSONArray requestIdentifiersArray = claimsJsonObject.getJSONObject("requested_record")
						.getJSONArray("identifier");
			
				String identifierValue = ((JSONObject) requestIdentifiersArray.get(0)).getString("value");
			
				String requestOperation = theRequestDetails.getRequestPath();
				String contentTypeAllCall = theRequestDetails.getHeader("Content-Type");
				String acceptHeaderAllCall = theRequestDetails.getHeader("Accept");
				//Different values are being passed, this section must be refactored 			
				if(contentTypeAllCall !=null){
				
				if ((contentTypeAllCall.equals("application/json+fhir") || contentTypeAllCall.equals("application/xml+fhir")
								|| contentTypeAllCall.equals("application/json+fhir;charset=utf-8")
								|| contentTypeAllCall.equals("application/xml+fhir;charset=utf-8")
						|| contentTypeAllCall.equals("application/json;charset=UTF-8"))
						&& (acceptHeaderAllCall.equals("application/xml+fhir")
								|| acceptHeaderAllCall.equals("application/json+fhir")
								|| acceptHeaderAllCall.equals("application/json+fhir;charset=utf-8")
								|| acceptHeaderAllCall.equals("application/xml+fhir;charset=utf-8") ||
								acceptHeaderAllCall.equals("application/json, text/plain, */*"))){
				} else {
					int theStatusCode = 415;
					String theMessage = "Unsupported media type";
					throw new UnclassifiedServerFailureException(theStatusCode, theMessage);
				}
				}

				if (requestOperation.equals("Patient/$gpc.getcarerecord")) {

					String contentType = theRequestDetails.getHeader("Content-Type");
					String acceptHeader = theRequestDetails.getHeader("Accept");

					

					if (contentType != null && (contentType.equals("application/json+fhir;charset=utf-8")
							|| contentType.equals("application/xml+fhir;charset=utf-8") || contentType.equals("application/json;charset=UTF-8")
									&& (acceptHeader.equals("application/xml+fhir;charset=utf-8")
											|| acceptHeader.equals("application/json+fhir;charset=utf-8") || acceptHeader.equals("application/json, text/plain, */*")))) {
					} else {
						int theStatusCode = 415;
						String theMessage = "Unsupported media type";
						throw new UnclassifiedServerFailureException(theStatusCode, theMessage);
					}

					if (!requestScopeType.equals("Patient")) {
						throw new InvalidRequestException("Bad Request Exception");
					}
					// Gets the NHS number in the request body to compare to
					// requested_record
					Map<String, List<String>> map = theRequestDetails.getUnqualifiedToQualifiedNames();

					for (String paramName : map.keySet()) {
						List<String> paramValues = map.get(paramName);

						// Get Values of Param Name
						for (String valueOfParam : paramValues) {
							// Output the Values
							if (valueOfParam.contains(identifierValue) != true) {
								throw new InvalidRequestException("Bad Request Exception1");
							}
							;
						}
					}

				}

				if (requestIdentifiersArray.length() > 0) {
					String identifierSystem = ((JSONObject) requestIdentifiersArray.get(0)).getString("system");

					if ("Patient".equalsIgnoreCase(requestScopeType)) {
						// If it is a patient orientated request
						if (!"http://fhir.nhs.net/Id/nhs-number".equalsIgnoreCase(identifierSystem)
								|| !NhsCodeValidator.nhsNumberValid(identifierValue)) {

							OperationOutcome operationOutcomes = new OperationOutcome();
							CodingDt errorCoding = new CodingDt()
									.setSystem("http://fhir.nhs.net/ValueSet/gpconnect-error-or-warning-code-1")
									.setCode("INVALID_NHS_NUMBER");
							CodeableConceptDt errorCodableConcept = new CodeableConceptDt().addCoding(errorCoding);
							errorCodableConcept.setText("Patient Record Not Found");
							operationOutcomes.addIssue().setSeverity(IssueSeverityEnum.ERROR)
									.setCode(IssueTypeEnum.NOT_FOUND).setDetails(errorCodableConcept);
							throw new InvalidRequestException("Dates are invalid: ", operationOutcomes);

						}

					} else {
						// If it is an organization oriantated request
						if (!"http://fhir.nhs.net/Id/ods-organization-code".equalsIgnoreCase(identifierSystem)) {
							throw new InvalidRequestException("Bad Request Exception");
						}
					}
				} else {
					throw new InvalidRequestException("Bad Request Exception");
				}

				// Checking the practionerId and the sub are equal in value
				JSONObject requestingPractitionerArray = claimsJsonObject.getJSONObject("requesting_practitioner");
				String practionerId = requestingPractitionerArray.getString("id");
				String sub = claimsJsonObject.getString("sub");
				if (!(practionerId.equals(sub))) {
					throw new InvalidRequestException("Bad Request Exception");

				}

				// Checking organization identifier is correct
				JSONArray organizationIdentifierArray = claimsJsonObject.getJSONObject("requesting_organization")
						.getJSONArray("identifier");
				if (organizationIdentifierArray.length() > 0) {
					for (int i = 0; i < organizationIdentifierArray.length(); i++) {
						String identifierSystem = ((JSONObject) organizationIdentifierArray.get(i)).getString("system");
						if (!"http://fhir.nhs.net/Id/ods-organization-code".equalsIgnoreCase(identifierSystem)) {
							throw new InvalidRequestException("Bad Request Exception");

						}
					}
				} else {
					throw new InvalidRequestException("Bad Request Exception");

				}
				// The method has been commented out to allow continuation of
				// work.
				// The method
				// fails the response due to the wrong identifier being in place
				/*
				 * JSONArray practitionerIdentifierArray =
				 * claimsJsonObject.getJSONObject("requesting_practitioner")
				 * .getJSONArray("identifier"); if
				 * (practitionerIdentifierArray.length() > 0) { for (int i = 0;
				 * i < practitionerIdentifierArray.length(); i++) { String
				 * identifierSystem = ((JSONObject)
				 * practitionerIdentifierArray.get(i)).getString("system"); if
				 * (!"http://fhir.nhs.net/sds-user-id".equalsIgnoreCase(
				 * identifierSystem)) { throw new
				 * InvalidRequestException("Bad Request Exception"); } } } else
				 * { return new throw new
				 * InvalidRequestException("Bad Request Exception");}
				 */

				// Checking practitioner identifier is correct
				JSONArray practitionerIdentifierArray = claimsJsonObject.getJSONObject("requesting_practitioner")
						.getJSONArray("identifier");
				if (practitionerIdentifierArray.length() > 0) {
					String identifierSystem = ((JSONObject) practitionerIdentifierArray.get(0)).getString("system");
					if (!"http://fhir.nhs.net/sds-user-id".equalsIgnoreCase(identifierSystem)) {
						throw new InvalidRequestException("Bad Request Exception");

					}
				} else {
					throw new InvalidRequestException("Bad Request Exception");

				}

				boolean hasRequestingDevice = claimsJsonObject.has("requesting_device");
				if (hasRequestingDevice == false) {
					throw new InvalidRequestException("Bad Request Exception");
				}

				// Checking the creation date is not in the future
				int timeValidationIdentifierInt = claimsJsonObject.getInt("iat");

				if (timeValidationIdentifierInt > (System.currentTimeMillis()) / 1000) {
					throw new InvalidRequestException("Bad Request Exception");

				}
				// Checking the reason for request is directcare
				String reasonForRequestValid = claimsJsonObject.getString("reason_for_request");
				if (!reasonForRequestValid.equals("directcare")) {
					throw new InvalidRequestException("Bad Request Exception");

				}

				if (claimsJsonObject.getJSONObject("requesting_device").getString("resourceType")
						.equals("InvalidResourceType")) {
					throw new InvalidRequestException("Bad Request Exception");
				}
				;
				if (claimsJsonObject.getJSONObject("requesting_organization").getString("resourceType")
						.equals("InvalidResourceType")) {
					throw new InvalidRequestException("Bad Request Exception");
				}
				;
				if (claimsJsonObject.getJSONObject("requesting_practitioner").getString("resourceType")
						.equals("InvalidResourceType")) {
					throw new InvalidRequestException("Bad Request Exception");
				}

				;

				if (claimsJsonObject.getJSONObject("requested_record").getString("resourceType").equals("Patient")
						&& claimsJsonObject.getString("requested_scope").equals("organization/*.write")) {
					throw new InvalidRequestException("Bad Request Exception");
				}
				if (claimsJsonObject.getJSONObject("requested_record").getString("resourceType").equals("Organization")
						&& claimsJsonObject.getString("requested_scope").equals("patient/*.read")) {
					throw new InvalidRequestException("Bad Request Exception");
				}
				// Checking the expiary time is 5 minutes after creation

				int timeValidationExpiryTime = claimsJsonObject.getInt("exp");
				int expiryTime = 300;

				if ((timeValidationExpiryTime) - (timeValidationIdentifierInt) != expiryTime) {
					throw new InvalidRequestException("Bad Request Exception");

				}

				FhirContext ctx = new FhirContext();
				IParser parser = ctx.newJsonParser();
				parser.setParserErrorHandler(new StrictErrorHandler());
				JSONObject requestingPracticionerObject = claimsJsonObject.getJSONObject("requesting_practitioner");
				JSONObject requestingDeviceObject = claimsJsonObject.getJSONObject("requesting_device");
				JSONObject requestingOrganizationObject = claimsJsonObject.getJSONObject("requesting_organization");
				//
				try {
					parser.parseResource(requestingPracticionerObject.toString()).getFormatCommentsPost();

				} catch (DataFormatException e) {
					throw new UnprocessableEntityException("Invalid Resource Present");
				}

				try {

					parser.parseResource(requestingDeviceObject.toString()).getFormatCommentsPost();

				} catch (DataFormatException e) {
					throw new UnprocessableEntityException("Invalid Resource Present");
				}
				try {

					parser.parseResource(requestingOrganizationObject.toString()).getFormatCommentsPost();

				} catch (DataFormatException e) {
					throw new UnprocessableEntityException("Invalid Resource Present");
				}

				String requestedScopeValue = claimsJsonObject.getString("requested_scope");
				boolean comparisonResultPR = requestedScopeValue.equals("patient/*.read");
				boolean comparisonResultPW = requestedScopeValue.equals("patient/*.write");
				boolean comparisonResultOR = requestedScopeValue.equals("organization/*.read");
				boolean comparisonResultOW = requestedScopeValue.equals("organization/*.write");

				if (comparisonResultPR == true || comparisonResultPW == true || comparisonResultOR == true
						|| comparisonResultOW == true) {
				} else {
					throw new InvalidRequestException("Bad Request Exception");

				}

				// Checks the aud is the correct link
				String aud = claimsJsonObject.getString("aud");
				if (!(aud.equals("https://authorize.fhir.nhs.net/token"))) {
					throw new InvalidRequestException("Bad Request Exception");
				}

				// Checks the JWT has the correct propertys
				boolean JWTHasCorrectJsonPropertys = true;
				JWTHasCorrectJsonPropertys = checkJWTJSONResponseRequestedRecordIsValidated(claimsJsonObject);
				JWTHasCorrectJsonPropertys = checkJWTJSONResponseRequestingDeviceIsValidated(claimsJsonObject);
				JWTHasCorrectJsonPropertys = checkJWTJSONResponseRequestingOrganizationIsValidated(claimsJsonObject);
				JWTHasCorrectJsonPropertys = checkJWTJSONResponseRequestingPractitionerIsValidated(claimsJsonObject);

				if (JWTHasCorrectJsonPropertys == false) {

					throw new InvalidRequestException("Bad Request Exception");
				}
			} catch (org.json.JSONException e) {
				throw new InvalidRequestException("Bad Request Exception");

			}

		}
		return new RuleBuilder().allowAll().build();
	}

	private boolean checkJWTJSONResponseRequestedRecordIsValidated(JSONObject claimsJsonObject) {
		// Done
		try {
			claimsJsonObject.getJSONObject("requested_record").getString("resourceType");
			JSONArray requestedRecordResults = claimsJsonObject.getJSONObject("requested_record")
					.getJSONArray("identifier");
			if (requestedRecordResults.length() > 0) {
				((JSONObject) requestedRecordResults.get(0)).getString("system");
				((JSONObject) requestedRecordResults.get(0)).getString("value");
			}
			return true;
		} catch (Exception e) {
			return false;

		}
	}

	private boolean checkJWTJSONResponseRequestingDeviceIsValidated(JSONObject claimsJsonObject) {
		try {
			claimsJsonObject.getJSONObject("requesting_device").getString("resourceType");
			claimsJsonObject.getJSONObject("requesting_device").getString("id");
			claimsJsonObject.getJSONObject("requesting_device").getString("model");
			claimsJsonObject.getJSONObject("requesting_device").getString("version");
			JSONArray requestingDeviceResults = claimsJsonObject.getJSONObject("requesting_device")
					.getJSONArray("identifier");
			if (requestingDeviceResults.length() > 0) {
				((JSONObject) requestingDeviceResults.get(0)).getString("system");
				((JSONObject) requestingDeviceResults.get(0)).getString("value");
			}
			return true;
		} catch (Exception e) {
			return false;

		}
	}

	private boolean checkJWTJSONResponseRequestingOrganizationIsValidated(JSONObject claimsJsonObject) {
		try {
			claimsJsonObject.getJSONObject("requesting_organization").getString("resourceType");
			claimsJsonObject.getJSONObject("requesting_organization").getString("id");
			claimsJsonObject.getJSONObject("requesting_organization").getString("name");
			JSONArray requestingOrganizationResults = claimsJsonObject.getJSONObject("requesting_organization")
					.getJSONArray("identifier");
			if (requestingOrganizationResults.length() > 0) {
				((JSONObject) requestingOrganizationResults.get(0)).getString("system");
				((JSONObject) requestingOrganizationResults.get(0)).getString("value");
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	 private boolean
	 checkJWTJSONResponseRequestingPractitionerIsValidated(JSONObject
	 claimsJsonObject) {
	 try {
	
	
	 claimsJsonObject.getJSONObject("requesting_practitioner").getString("resourceType");
	 claimsJsonObject.getJSONObject("requesting_practitioner").getString("id");
	 JSONArray requestingPractitionerResults =
	 claimsJsonObject.getJSONObject("requesting_practitioner")
	 .getJSONArray("identifier");
	 if (requestingPractitionerResults.length() > 0) {
	 ((JSONObject) requestingPractitionerResults.get(0)).getString("system");
	 ((JSONObject) requestingPractitionerResults.get(0)).getString("value");
	 }
	
	 JSONObject requestingPractitionerResultsName =
	 claimsJsonObject.getJSONObject("requesting_practitioner")
	 .getJSONObject("name");
	
	 if (requestingPractitionerResultsName.length() > 0) {
	 requestingPractitionerResultsName.get("family");
	 requestingPractitionerResultsName.get("given");
	 requestingPractitionerResultsName.get("prefix");
	 }
	// String requestingPractitionerPracRole = ((JSONObject)claimsJsonObject.getJSONObject("requesting_practitioner").getJSONArray("practitionerRole").getJSONObject(0).getJSONObject("role").getJSONArray("coding").get(0)).getString("code");
	 //The roles are currently unknown, this name has been hardcoded until
	/// they are found out
	// if (!requestingPractitionerPracRole.equals("AssuranceJobRole"))
	//  {
	//  return false;
	// }
	
	 return true;
	 } catch (Exception e) {
	 return false;
	 }
	 }

}
