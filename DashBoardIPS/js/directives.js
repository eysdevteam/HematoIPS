	app.directive('mejoresPeores', function () {
      return {
        restrict: 'E',
        templateUrl: 'mejores-peores.html'
      };
    })

    app.directive('scatter', function () {
      return {
        restrict: 'E',
        templateUrl: 'scatter.html'
      };
     })

     app.directive('bars', function () {
      return {
        restrict: 'E',
        templateUrl: 'bars.html'
      };
     })

    app.directive('topTable', function () {
      return {
        restrict: 'E',
        templateUrl: 'top-table.html'
      };
     });