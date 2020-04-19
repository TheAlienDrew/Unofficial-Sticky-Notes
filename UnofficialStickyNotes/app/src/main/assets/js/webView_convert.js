javascript:(function() {

        // wait for loading animation to disappear before making webView visible (if on sticky notes page)
        var currentUrl = document.location.host + document.location.pathname;
        if (currentUrl == 'www.onenote.com/stickynotes') {
            var checkLoading = setInterval(function() {
                var sidePane = document.querySelector('.n-side-pane-content');
                var noteListContainer = document.querySelector('.n-noteList-Container');
                var noteList = document.querySelector('.n-noteList');
                if(typeof(noteList) != 'undefined' && noteList != null && sidePane.childElementCount == 2) {
                    clearInterval(checkLoading);

                    // let page control refresher
                    noteListContainer.addEventListener('scroll', function() {
                        window.CallToAnAndroidFunction.setSwipeRefresher(noteListContainer.scrollTop);
                    }, false);
                    window.CallToAnAndroidFunction.setSwipeRefresher(noteListContainer.scrollTop);

                    // set webView to visible
                    window.CallToAnAndroidFunction.webViewSetVisible();
                }
            }, 100);
        } else {
            var checkLoading = setInterval(function() {
                if (typeof(document.activeElement) != 'undefined' && document.activeElement != null) {
                    clearInterval(checkLoading);

                    window.CallToAnAndroidFunction.webViewSetVisible();
                }
            }, 100);
        }


        // continue with website related fixes
        var node = document.createElement('style');

        node.type = 'text/css';
        // uses only app_conversion.css for mobile view
        node.innerHTML = '*{-webkit-tap-highlight-color:transparent}:focus{outline:0}html{position:fixed;height:100%;width:100%}#O365_HeaderLeftRegion{display:none}.n-Phone .n-newNoteButtonContainer{width:auto!important;height:auto!important;border-radius:initial!important;-webkit-box-shadow:none!important;box-shadow:none!important;color:auto!important;background:0 0!important;font-weight:400!important;position:static!important;z-index:auto!important;-ms-flex-item-align:auto!important;align-self:auto!important;right:auto!important;bottom:auto!important;margin:0 8px!important}.n-Phone .n-newNoteButtonIcon{display:block!important}.n-Phone .n-newNoteButtonIcon .newNote path{fill:var(--n-ui-icon)!important}';

        document.head.appendChild(node);

    })()