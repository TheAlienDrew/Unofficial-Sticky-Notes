javascript:(function() {

        // theme changes based on url
        var stickyNotesURL = 'www.onenote.com/stickynotes';
        var currentUrl = document.location.host + document.location.pathname;
        var themeCss = '*{-webkit-tap-highlight-color:transparent}:focus{outline:0}html{position:fixed;height:100%;width:100%}';

        // wait for loading animation to disappear before making webView visible (if on sticky notes page)
        if (currentUrl == stickyNotesURL) {
            themeCss += '#O365_HeaderLeftRegion{display:none}';
            var checkLoading = setInterval(function() {
                var sidePane = document.querySelector('.n-side-pane-content');
                var noteListContainer = document.querySelector('.n-noteList-Container');
                var noteList = document.querySelector('.n-noteList');
                if(typeof(noteList) != 'undefined' && noteList != null && sidePane.childElementCount == 2) {
                    clearInterval(checkLoading);

                    // let page control refresher
                    noteListContainer.addEventListener('scroll', function() {
                        window.Android.setSwipeRefresher(noteListContainer.scrollTop);
                    }, false);
                    window.Android.setSwipeRefresher(noteListContainer.scrollTop);

                    // set webView to visible
                    window.Android.webViewSetVisible();
                }
            }, 100);
        } else {
            var checkLoading = setInterval(function() {
                if (typeof(document.activeElement) != 'undefined' && document.activeElement != null) {
                    clearInterval(checkLoading);

                    // the create account link is broken, and needs to be changed
                    if (currentUrl == 'login.microsoftonline.com/common/oauth2/authorize') {
                        var signupUrl = 'https://signup.live.com/'
                        var signupSelector = 'signup';
                        var signup = document.getElementById(signupSelector);

                        var newSignup = signup.cloneNode(true);
                        newSignup.href = signupUrl;

                        signup.remove();
                        document.getElementsByClassName('form-group')[1].appendChild(newSignup);
                    }

                    window.Android.webViewSetVisible();
                }
            }, 100);
        }

        // continue with website related fixes
        var node = document.createElement('style');

        node.type = 'text/css';
        // uses only app_conversion.css for mobile view
        node.innerHTML = themeCss;

        document.head.appendChild(node);

    })()