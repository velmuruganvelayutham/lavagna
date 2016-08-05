(function () {

	'use strict';

	var module = angular.module('lavagna.controllers');

	module.controller('ProjectMilestonesCtrl', function ($stateParams, $rootScope, $scope, Card, User, Label, Notification, StompClient, project) {

		$scope.sidebarOpen = true;
		$scope.project = project;
        $scope.currentPage = 1;

		var orderByStatus = function (milestone) {
			var insertStatusIfExists = function (milestone, source, target, status) {
				if (source[status] != undefined) {
					target[target.length] = {status: status, count: source[status]};
					milestone.totalCards += source[status];
				}
			};

			milestone.totalCards = 0;
			var sorted = [];
			insertStatusIfExists(milestone, milestone.cardsCountByStatus, sorted, "BACKLOG");
			insertStatusIfExists(milestone, milestone.cardsCountByStatus, sorted, "OPEN");
			insertStatusIfExists(milestone, milestone.cardsCountByStatus, sorted, "DEFERRED");
			insertStatusIfExists(milestone, milestone.cardsCountByStatus, sorted, "CLOSED");
			$scope.cardsCountByStatus[milestone.labelListValue.value] = sorted;
		};

		var loadMilestonesInProject = function () {
			User.hasPermission('READ', $stateParams.projectName).then(function () {
				return Card.findCardsByMilestone($stateParams.projectName);
			}).then(function (response) {
				$scope.cardsByMilestone = response.milestones;
				$scope.cardsCountByStatus = [];
				for (var index in response.milestones) {
					var milestone = response.milestones[index];
					orderByStatus(milestone);
				}
				$scope.statusColors = response.statusColors;
			});
		};

		loadMilestonesInProject();

		StompClient.subscribe($scope, '/event/project/' + $stateParams.projectName + '/label-value', loadMilestonesInProject);

		StompClient.subscribe($scope, '/event/project/' + $stateParams.projectName + '/label', loadMilestonesInProject);

	});
})();
