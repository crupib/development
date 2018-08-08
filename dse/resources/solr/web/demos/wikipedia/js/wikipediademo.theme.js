(function ($) {

AjaxSolr.theme.prototype.result = function (doc) {

    var body = doc.body;

    try
    {
	body = InstaView.convert(doc.body);
    }catch(e){
	
    }

    var output = '<div><h2>'+doc.title+'</h2><a href="http://wikipedia.org/wiki/' + doc.title + '">(wikipedia article)</a>';
    output += '<p><div style="max-height:200px;overflow:hidden">'+doc.date + ' ' + body+'</div></p></div>';
    return output;
};

AjaxSolr.theme.prototype.tag = function (value, weight, handler) {
  return $('<a href="#" class="tagcloud_item"/>').text(value).addClass('tagcloud_size_' + weight).click(handler);
};

AjaxSolr.theme.prototype.facet_link = function (value, handler) {
  return $('<a href="#"/>').text(value).click(handler);
};

AjaxSolr.theme.prototype.no_items_found = function () {
  return 'no items found in current selection';
};

})(jQuery);
