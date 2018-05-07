app.controller('Mejoraron' ,function($scope,$http) {
	$http.get("web/donut1/donut.json").then(function(data){
		$scope.dataset=data.data
		donut($scope.dataset, "#IPSmejoraron");		
	});
});

app.controller('Empeoraron' ,function($scope, $http) {
	$http.get("web/donut2/donut.json").then(function(data){
                $scope.dataset=data.data
                donut($scope.dataset, "#IPSempeoraron");
   });
});

app.controller("scatter", function($scope, $http) {
    $http.get("web/scatter/scatter.json").then(function(data){
        $scope.data=data.data
        scatreeheat($scope.data, "#scatter")         
    });
});

app.controller("TopIPS", function($scope, $http){
    tabla("#tablaips");
});

app.controller("TablaVar", function($scope, $http){
    $http.get("web/name-var/name.json").then(function(data){
        $scope.data=data.data
        tablavar($scope.data,"#tablavar");
    });

});

app.controller("bargraph", function($scope, $http) {
    $http.get("web/bars-whisker/table.json").then(function(data){
        $scope.data=data.data
        bargraph($scope.data, "#contenedor", "#svg")         
    });
});
