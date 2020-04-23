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

                    // scrolling note list checks swipe
                    noteListContainer.addEventListener('touchmove', function(e) {
                        window.Android.setSwipeRefresher(noteListContainer.scrollTop);
                    }, false);
                    // pressing in note list checks swipe
                    noteListContainer.addEventListener('touchstart', function(e) {
                        window.Android.setSwipeRefresher(noteListContainer.scrollTop);
                    }, false);
                    // must stop the scroll-to-refresh when opening up a note/image
                    var clickableListNotes = document.getElementsByClassName('n-noteList-notePreviewWrapper');
                    var currentNote = document.querySelector('.n-noteEditViewContainer');
                    // after clicking a note in list, disable swipe
                    setInterval(function() {
                        var i;
                        for (i = 0; i < clickableListNotes.length; ++i) {
                            clickableListNotes[i].onclick = function () {
                                window.Android.setSwipeRefresher(1); // disables swipe
                            };
                        }
                    }, 100);
                    // disable swipe while editing a note
                    currentNote.onclick = function () {
                        // check classlist for inactivity
                        if (!currentNote.classList.contains('inactive')) {
                            window.Android.setSwipeRefresher(1);
                        };
                    }
                    // execute once to determine swipe at page load
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