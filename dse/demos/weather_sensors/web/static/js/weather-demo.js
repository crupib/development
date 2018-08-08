
var setErrorMessage = function (msg) {
    if (msg)
        $("#status").prepend("<div id='error-message' class='alert alert-danger'>" + msg + "</div>");
};

var setElapsedTime = function (time, records) {
    if (time && records)
        $("#status").append("<div id='elapsed-time'><h4>Time spent loading: " + time +
                            " seconds for " + records + " records</h4></div>");
};

var buildTable = function (json) {
    var keys = [];
    for (header in json[0]) {
        keys.push(header);
    }

    // build header row
    var $tr =$('<tr>');
    for (i in keys) {
        $tr.append($('<th>').append(keys[i]));
    }
    $tr.appendTo('table#data');

    // build data rows
    for (var i = 0; i < json.length; i++) {
        var $tr = $('<tr>');
        for (j in keys) {
            var key = keys[j];
            var value = json[i][key];
            $tr.append($('<td>').append(value));
        }
        $tr.appendTo('table#data');
    }
};
