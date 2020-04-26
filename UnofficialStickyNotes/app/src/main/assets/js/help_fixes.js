javascript:(function() {

        // android related constants
        const LENGTH_LONG = 1;
        const LENGTH_SHORT = 0;
        // help website wasn't meant to be loaded in a normal window, so it needs fixes
        const slowDelay = 1000;
        const fastDelay = 100;
        const themeCss = '.ocSearchIFrameDiv{height: calc(100vh - 57px)}'
        const iframeFixCss = '.ocpArticleContent .ocpAlert{background-color:#686868}';
        const iframeId = 'ocSearchIFrame';
        // function for elements
        function elementExists(element) {
            return (typeof(element) != 'undefined' && element != null);
        }

        // continue with website related theming
        var node = document.createElement('style');

        node.type = 'text/css';
        node.innerHTML = themeCss;

        document.head.appendChild(node);

        // iframe needs dark theme fix if in dark mode
        if (window.Android.isDarkMode()) {
            checkForIFrame = setInterval(function() {
                var iframe = document.getElementById(iframeId);
                var iframeDoc = iframe.contentDocument;

                if (elementExists(iframe) && iframeDoc != null) {
                    clearInterval(checkForIFrame);

                    // must listen for page load to change style
                    iframe.onload = function() {
                        // display errors when the iframe can't load a page
                        try {
                            var doc = iframe.contentDocument || iframe.contentWindow.document;
                            var html = doc.body.innerHTML;

                            var innerDocument = frames[0].document;

                            var fixNode = document.createElement('style');
                            fixNode.type = 'text/css';
                            fixNode.innerHTML = iframeFixCss;
                            innerDocument.body.appendChild(fixNode);
                        } catch(err) {
                            // notify the user and return to the original page
                            alert('Sorry, you must visit that part of the help page online first before it can load offline.');
                            iframe.contentWindow.location.href = iframe.src;
                        }
                    };
                }
            }, fastDelay);
        }

    })()