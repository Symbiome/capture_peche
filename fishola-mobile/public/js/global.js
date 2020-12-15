/* onScroll function
----------------------------------------*/
function onScroll(event){
  var scrollPosition = $(document).scrollTop();
  $('nav li a').each(function () {
    var currentLink = $(this);
    var refElement = $(currentLink.attr("href"));
    if (refElement && refElement.position && refElement.position()) {
      if (refElement.position().top <= scrollPosition && refElement.position().top + refElement.height() > scrollPosition) {
        $('nav ul li').removeClass("active");
        currentLink.parent().addClass("active");
      }
      else{
        currentLink.parent().removeClass("active");
      }
    }
  });
}

// $(document).on("scroll", onScroll);
