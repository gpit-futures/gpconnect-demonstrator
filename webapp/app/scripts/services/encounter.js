'use strict';

angular.module('gpConnect').factory('Encounter', function($rootScope, $http, FhirEndpointLookup, fhirJWTFactory) {
    var findAllHTMLTables = function(patientId, fromDate, toDate) {
        return FhirEndpointLookup.getEndpoint($rootScope.patientOdsCode,"urn:nhs:names:services:gpconnect:fhir:operation:gpc.getcarerecord-1").then(function(response) {
            var endpointLookupResult = response;
            
            return $http.post(
                    endpointLookupResult.restUrlPrefix+'/Patient/$gpc.getcarerecord',
                    '{"resourceType" : "Parameters","parameter" : [{"name" : "patientNHSNumber","valueIdentifier" : { "system": "https://fhir.nhs.uk/Id/nhs-number", "value" : "'+patientId+'" }},{"name" : "recordSection","valueCodeableConcept" :{"coding" : [{"system":"http://fhir.nhs.net/ValueSet/gpconnect-record-section-1","code":"ENC","display":"Encounters"}]}},{"name" : "timePeriod","valuePeriod" : { "start" : "'+fromDate+'", "end" : "'+toDate+'" }}]}',
                    {
                        headers: {
                            'Ssp-From': endpointLookupResult.fromASID,
                            'Ssp-To': endpointLookupResult.toASID,
                            'Ssp-InteractionID': "urn:nhs:names:services:gpconnect:fhir:operation:gpc.getcarerecord-1",
                            'Ssp-TraceID': fhirJWTFactory.guid(),
                            'Authorization': "Bearer " + fhirJWTFactory.getJWT("patient", "read", patientId),
                            'Accept': "application/json+fhir",
                            'Content-Type': "application/json+fhir"
                        }
                    }
            );
        });
    };

    return {
        findAllHTMLTables: findAllHTMLTables
    };
});