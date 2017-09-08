var ViewManager = {

    loadTemplate: function(templateUrl) {
        var template = "";
        $.ajax({
            url: templateUrl,
            async: false,
            dataType: "html",
            success: function(data) {
                template = data;
            }
        });
        return template;
    },

    render: function(data, component, templateName) {
        var template = "";
        if (templateName == null) {
            for (var pageType in data) {
                template = this.loadTemplate(ctx + "/templates/" + pageType + ".mst.html");
                break;
            }
        } else {
            template = this.loadTemplate(ctx + "/templates/" + templateName + ".mst.html");
        }

        var html = "";
        if (template != "") {
            html = Mustache.render(template, data);
        }

        $(component).empty();
        $(component).html(html);
    }

}