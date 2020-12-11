/*

November Template

http://www.templatemo.com/tm-473-november

*/

/* Google Maps
------------------------------------------------*/
var center;

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

/* HTML Document is loaded and DOM is ready.
--------------------------------------------*/
$(document).ready(function(){
  //slider
//   var sudoSlider = $("#slider").sudoSlider({
//    effect: "fade",
//    pause: 3000,
//    auto:true,
//    continuous:true
//  });

  //mobilemenu
  $('.mobile').click(function(){
    var $self = $(this);
    $('.menumobile').slideToggle( function(){
      $self.toggleClass('closed');
    });
  });

  //navigation script
  $('.Navigation ul li a').click(function(){
    $('.menumobile').removeAttr("style");
    $('#mobile_sec .mobile').removeClass("closed");
  });

  $('a.slicknav_btn').click(function(){
    $(".mobilemenu ul").css({"display":"block"});
  });

  //tab
  $(".tabLink").each(function(){
    $(this).click(function(){
      tabeId = $(this).attr('id');
      $(".tabLink").removeClass("activeLink");
      $(this).addClass("activeLink");
      $(".tabcontent").addClass("hide");
      $("#"+tabeId+"-1").removeClass("hide");
      return false;
    });
  });
  $('a[href*=#]:not([href=#])').click(function() {
    if (location.pathname.replace(/^\//,'') == this.pathname.replace(/^\//,'')
      || location.hostname == this.hostname)
    {
      var target = $(this.hash),
      headerHeight = $(".primary-header").height() + 5; // Get fixed header height
      target = target.length ? target : $('[name=' + this.hash.slice(1) +']');
      if (target.length)
      {
        $('html,body').animate({
          scrollTop: target.offset().top + 2
        }, 600);
        return false;
      }
    }
  });	

}); 

$(document).on("scroll", onScroll);
