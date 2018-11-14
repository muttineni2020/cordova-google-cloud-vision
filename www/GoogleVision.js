// JS interface
var exec = require('cordova/exec');

var GoogleVision = {
	serviceName: "GoogleVision",
	
	ScanImage: function(success, error, base64Image, scanKey) {
		exec(success, error, this.serviceName, "ScanImage", [base64Image, scanKey]);
	},

};

module.exports = GoogleVision;