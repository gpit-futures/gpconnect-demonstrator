'use strict';

angular.module('openehrPocApp')
  .factory('PatientService', function ($http, Patient) {

    var all = function () {
      return $http.get('/dummy-data/patients.json', { cache: true }).then(function (result) {
        var patients = [];

        angular.forEach(result.data, function (patient) {
          patient = new Patient(patient);
          patients.push(patient);
        });

        return patients;
      });
    };

    var get = function (id) {
      return all().then(function (patients) {
        return _.findWhere(patients, { id: parseInt(id) });
      });
    };

    var update = function (patient, updatedDiagnosis) {
      var diagnosis = _.findWhere(patient.diagnoses, { id: updatedDiagnosis.id });
      angular.extend(diagnosis, updatedDiagnosis);
    };

    var summaries = function () {
      return all().then(function (patients) {
        var summaries = {};
        var ageSummaries = _.countBy(patients, function (patient) { return patient.ageRange; });

        summaries.age = _.map(ageSummaries, function (value, key) {
          return { series: key, value: value };
        });

        summaries.age = _.sortBy(summaries.age, function (value) {
          return value.age;
        });

        var departmentSummaries = _.countBy(patients, function (patient) { return patient.department; });
        departmentSummaries.All = patients.length;

        summaries.department = _.map(departmentSummaries, function (value, key) {
          return { series: key, value: value };
        });

        summaries.department = _.sortBy(summaries.department, function (value) {
          return value.department;
        });

        return summaries;
      });
    };

    return {
      all: all,
      get: get,
      update: update,
      summaries: summaries
    };

  });