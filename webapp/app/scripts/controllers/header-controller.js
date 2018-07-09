'use strict';

angular.module('gpConnect')
  .controller('headerController', function ($scope, $rootScope, $state, usSpinnerService, $stateParams, UserService, $modal) {

    // Get current user
    UserService.setCurrentUserFromQueryString();
    $scope.currentUser = UserService.getCurrentUser();

    // Direct different roles to different pages at login
    switch ($scope.currentUser.role) {
    case 'gpconnect':
      $state.go('main-search');
      break;
    case 'phr':
      $state.go('patients-summary', {
        patientId: 9999999000
      }); // id is hard coded
      break;
    default:
      $state.go('patients-summary', {
        patientId: 9999999000
      }); // id is hard coded
    }

    $rootScope.$on('$stateChangeSuccess', function (event, toState) {
      var previousState = '';
      var pageHeader = '';
      var previousPage = '';

      var mainWidth = 0;
      var detailWidth = 0;

      switch (toState.name) {
        case 'main-search':
          previousState = '';
          pageHeader = 'Appointments Module';
          previousPage = '';
          mainWidth = 12;
          detailWidth = 0;
          break;
      case 'patients-list':
        previousState = 'main-search';
        pageHeader = 'Appointments: Patient Lists';
        previousPage = 'Patient Dashboard';
        mainWidth = 12;
        detailWidth = 0;
        break;
      case 'patients-summary':
        previousState = 'patients-list';
        pageHeader = 'Appointments: Patient Summary';
        previousPage = 'Patient Lists';
        mainWidth = 11;
        detailWidth = 0;
        break;
      case 'patients-lookup':
        previousState = '';
        pageHeader = 'Appointments: Patients lookup';
        previousPage = '';
        mainWidth = 6;
        detailWidth = 6;
        break;
      default:
        previousState = 'patients-list';
        pageHeader = 'Appointments: Patients Details';
        previousPage = 'Patient Lists';
        mainWidth = 11;
        detailWidth = 0;
        break;
      }

      $scope.pageHeader = pageHeader;
      $scope.previousState = previousState;
      $scope.previousPage = previousPage;

      $scope.mainWidth = mainWidth;
      $scope.detailWidth = detailWidth;

      $scope.goBack = function () {
        history.back();
      };

      $scope.userContextViewExists = ('user-context' in $state.current.views);
      $scope.actionsExists = ('actions' in $state.current.views);

      $scope.go = function (patient) {
        $state.go('patients-summary', {
          patientId: patient.id
        });
      };

      if ($scope.currentUser.role === 'gpconnect') {
        $scope.title = UserService.getContent('gpconnect_title');
        $scope.privacy = UserService.getContent('gpconnect_privacy');
      }
      if ($scope.currentUser.role === 'phr') {
        $scope.title = UserService.getContent('phr_title');
      }

      $scope.footer = UserService.getContent('gpconnect_footer');

      $scope.goHome = function () {
        if ($scope.currentUser.role === 'gpconnect') {
          $state.go('main-search');
        }
        if ($scope.currentUser.role === 'phr') {
          $state.go('patients-summary', {
            patientId: 9999999000
          }); // Id is hardcoded
        }
      };
      
      $scope.gpConnectInfo = function () {
        $modal.open({
            templateUrl: 'views/application/information-modal.html',
            size: 'lg',
            controller: 'InformationModalCtrl'
        });
      };
      
    });
  });
