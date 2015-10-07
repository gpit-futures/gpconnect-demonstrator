'use strict';

angular.module('rippleDemonstrator')
  .controller('headerController', function ($scope, $rootScope, $state, $stateParams, PatientService) {

    var role = $stateParams.role;
    var email = $stateParams.email;
    $scope.searchExpression = '';
    $scope.reportTypes = [];

    // Set current user
    PatientService.setCurrentUser(role, email);

    // Get current user
    $scope.currentUser = PatientService.getCurrentUser();

    // Temporary default user
    if (!$scope.currentUser.role) {
      $scope.currentUser.role = 'idcr';
      $scope.currentUser.email = 'example@email.com';
    }

    // Direct different roles to different pages at login
    switch ($scope.currentUser.role) {
    case 'idcr':
      $state.go('patients-charts');
      break;
    case 'phr':
      $state.go('patients-summary', {
        patientId: 10
      }); // id is hard coded
      break;
    default:
      $state.go('patients-summary', {
        patientId: 10
      }); // id is hard coded
    }

    $rootScope.$on('$stateChangeSuccess', function (event, toState) {

      var previousState = '';
      var pageHeader = '';
      var previousPage = '';

      var mainWidth = 0;
      var detailWidth = 0;

      switch (toState.name) {
      case 'patients-list':
        previousState = 'patients-charts';
        pageHeader = 'Patient Lists';
        previousPage = 'Patient Dashboard';
        mainWidth = 12;
        detailWidth = 0;
        break;
      case 'patients-charts':
        previousState = '';
        pageHeader = 'Patient Dashboard';
        previousPage = '';
        mainWidth = 12;
        detailWidth = 0;
        break;
      case 'patients-summary':
        previousState = 'patients-list';
        pageHeader = 'Patient Summary';
        previousPage = 'Patient Lists';
        mainWidth = 12;
        detailWidth = 0;
        break;
      case 'patients-lookup':
        previousState = '';
        pageHeader = '';
        previousPage = '';
        mainWidth = 6;
        detailWidth = 6;
        break;
      case 'search-report':
        previousState = '';
        pageHeader = '';
        previousPage = '';
        mainWidth = 12;
        detailWidth = 0;
        break;
      default:
        previousState = 'patients-list';
        pageHeader = 'Patients Details';
        previousPage = 'Patient Lists';
        mainWidth = 6;
        detailWidth = 6;
        break;
      }

      $scope.containsReportString = function () {
        return $scope.searchExpression.indexOf('rp ') == 0 ? true : false;
      }

      $scope.reportMode = false;
      $scope.checkExpression = function () {
        if ($scope.reportMode) {
          $scope.reportTypes = [
      'Diagnosis: ',
      'Orders: '
    ];
        } else {
          $scope.reportMode = $scope.containsReportString();
          $scope.processReportMode();
        }
      }

      $scope.cancelReportMode = function () {
        $scope.reportMode = false;
        $scope.searchExpression = '';
      }

      $scope.searchReport = function () {
        if ($scope.reportMode && $scope.searchExpression != '') {
          var tempExpression = $scope.searchExpression;
          $scope.searchExpression = '';
          $scope.reportMode = false;
          $scope.chartPage = true;
          $state.go('search-report', {searchString: tempExpression});
        }
      }

      $scope.processReportMode = function () {
        if ($scope.searchExpression === 'rp ') {
          $scope.searchExpression = '';
        }
      }

      $scope.pageHeader = pageHeader;
      $scope.previousState = previousState;
      $scope.previousPage = previousPage;

      // Set bootstrap column sizes
      $scope.mainWidth = mainWidth;
      $scope.detailWidth = detailWidth;

      $scope.goBack = function () {
        $state.go(previousState);
      };

      $scope.userContextViewExists = ('user-context' in $state.current.views);
      $scope.actionsExists = ('actions' in $state.current.views);

      $scope.go = function (patient) {
        $state.go('patients-summary', {
          patientId: patient.id
        });
      };

      // Set title depending on user
      if ($scope.currentUser.role === 'idcr') {
        $scope.title = 'IDCR POC';
      }
      if ($scope.currentUser.role === 'phr') {
        $scope.title = 'PHR';
      }

      // Set home url depending on user
      $scope.goHome = function () {
        if ($scope.currentUser.role === 'idcr') {
          $state.go('patients-charts');
        }
        if ($scope.currentUser.role === 'phr') {
          $state.go('patients-summary', {
            patientId: 10
          }); // Id is hardcoded
        }
      };
    });

  });
