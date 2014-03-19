API = {
  registerName: function(name, onSuccess) {
    $.ajax({
      type: "POST",
      url: "/api/v1/hostnames",
      data: JSON.stringify({ basename: name }),
      success: function(data) {
        onSuccess(data.name);
      },
      contentType: "application/json"
    });
  }
}

IndexPage = {
  initialize: function() {
    $("#boxname-info-list").on("click", ".register", function(e) {
      var li = $(this).closest("li.boxname-info");
      var boxname = li.data("boxname");
      API.registerName(boxname, function(hostname) {
        alert("Registered name: " + hostname);
        // TODO: Use javascript to update these numbers periodically rather than reloading the entire page.
        window.location.reload();
      });
      return false;
    });
  }
}

$(function() { IndexPage.initialize(); });
