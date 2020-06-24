javascript:(function() {

        // constants
        const stickyNotesURL = 'www.onenote.com/stickynotes'
        const signUpURL = 'https://signup.live.com/';
        const loginURL = 'login.microsoftonline.com/common/oauth2/authorize';
        const disableSwipe = 1;
        const slowDelay = 1000;
        const fastDelay = 100;
        // constant selectors
        const sidePaneSelector = '.n-side-pane-content';
        const flexPaneCloseButtonSelector = '#flexPaneCloseButton';
        const lightBoxCloseSelector = '.n-lightbox-close';
        const warningCancelSelector = '.n-warningCancel';
        const imageAltTextCancelSelector = '.n-imageAltTextCancel';
        const noteEditCloseButtonSelector = '.n-noteEdit-closeButton';
        const noteSelector = '.n-note';
        const noteListContainerSelector = '.n-noteList-Container';
        const noteListSelector = '.n-noteList';
        const notePreviewSelector = 'n-noteList-notePreviewWrapper';
        const helpPaneSelector = '#helpPaneFull'; // TODO: Required for fall back of help page
        const editableTextSelector = 'div[contenteditable="true"].n-slateEditorRoot';
        const helpButtonSelector = '#O365_MainLink_Help_container';
        // theme changes based on url
        var currentURL = document.location.host + document.location.pathname;
        var tempImageFix = '.n-imageGalleryEdit-singleImage-FocusZone{padding-inline-start:0px}';
        var tempBulletFix = '.n-notePreviewContainer li{margin:0 0 0 -15px;list-style-type:initial}';
        var themeCss = tempImageFix + tempBulletFix + '*{-webkit-tap-highlight-color:transparent}:focus{outline:0!important}html{position:fixed;height:100%;width:100%}'; // see app_conversion.css
        // function for elements
        var elementExists = function(element) {
            return (typeof(element) != 'undefined' && element != null);
        };

        // wait for loading animation to disappear before making webView visible (if on sticky notes page)
        if (currentURL == stickyNotesURL) {
            themeCss += '#O365_HeaderLeftRegion{display:none}.n-Phone .n-noteFocus{margin:0 0}';
            var checkLoading = setInterval(function() {
                var sidePane = document.querySelector(sidePaneSelector);
                var noteListContainer = document.querySelector(noteListContainerSelector);
                var noteList = document.querySelector(noteListSelector);
                var notePreview = document.getElementsByClassName(notePreviewSelector);
                var helpPaneExists = false;
                var closeButtonActive = false;
                var editingNote = false;
                if(elementExists(noteList) && sidePane.childElementCount == 2) {
                    clearInterval(checkLoading);

                    // scrolling note list checks swipe
                    noteListContainer.addEventListener('touchmove', function(e) {
                        window.Android.setSwipeRefresher(noteListContainer.scrollTop);
                    }, false);
                    // pressing in note list checks swipe
                    noteListContainer.addEventListener('touchstart', function(e) {
                        window.Android.setSwipeRefresher(noteListContainer.scrollTop);
                    }, false);
                    // after clicking a note in list, disable swipe
                    setInterval(function() {
                        var i;
                        for (i = 0; i < notePreview.length; ++i) {
                            // needs to always check

                            notePreview[i].onclick = function () {
                                window.Android.setSwipeRefresher(disableSwipe);
                                editingNote = false; // needed to copy note data from another note when in tablet mode
                            };
                        }
                    }, slowDelay);
                    // change help to open in the webView
                    var waitForHelp = setInterval(function() {
                        var helpButton = document.querySelector(helpButtonSelector);
                        if (elementExists(helpButton)) {
                            clearInterval(waitForHelp);

                            helpButton.onclick = function () {
                                window.Android.loadStickiesHelp();
                            };
                        }
                    }, fastDelay);
                    // disable swipe if help pane is open // TODO: This is a fall back if the help page doesn't load into the webView
                    setInterval(function() {
                        var helpPane = document.querySelector(helpPaneSelector);
                        if (elementExists(helpPane) && !helpPaneExists) {
                            // needs to always check

                            window.Android.setSwipeRefresher(disableSwipe);
                            if (window.Android.isDarkMode()) helpPane.style = "filter:invert(100%)hue-rotate(180deg)";
                            helpPaneExists = true;
                        } else helpPaneExists = false;
                    }, slowDelay);
                    // disable swipe while editing a note (needed for tablet users)
                    var waitForEditNote = setInterval(function() {
                        var note = document.querySelector(noteSelector);
                        if (elementExists(note)) {
                            clearInterval(note);

                            note.onclick = function () {
                                // check classlist for inactivity
                                if (!note.classList.contains('inactive')) {
                                    window.Android.setSwipeRefresher(disableSwipe);
                                }
                            };
                        }
                    }, slowDelay);
                    // clicking the close button conditionally enables swipe
                    var waitForClose = setInterval(function() {
                        var noteEditCloseButton = document.querySelector(noteEditCloseButtonSelector);
                        if (elementExists(noteEditCloseButton)) {
                            clearInterval(waitForClose)

                            noteEditCloseButton.onclick = function () {
                                window.Android.setSwipeRefresher(noteListContainer.scrollTop);
                            };
                        }
                    }, slowDelay);

                    // allow back button to close current element
                    setInterval(function() {
                        var currentCloseButton = "";
                        // first, see if description cancel button exists
                        var tempCloseButton = document.querySelector(imageAltTextCancelSelector);
                        if (elementExists(tempCloseButton)) currentCloseButton = imageAltTextCancelSelector;
                        else {
                            // second, see if warning cancel button exists
                            tempCloseButton = document.querySelector(warningCancelSelector);
                            if (elementExists(tempCloseButton)) currentCloseButton = warningCancelSelector;
                            else {
                                // third, see if image close button exists
                                tempCloseButton = document.querySelector(lightBoxCloseSelector);
                                if (elementExists(tempCloseButton)) currentCloseButton = lightBoxCloseSelector;
                                else {
                                    // fourth, see if edit note close button exists and is visible
                                    tempCloseButton = document.querySelector(noteEditCloseButtonSelector);
                                    if (elementExists(tempCloseButton) && window.getComputedStyle(document.querySelector(noteEditCloseButtonSelector)).visibility == 'visible') currentCloseButton = noteEditCloseButtonSelector;
                                    else {
                                        // lastly, see if help close button exists
                                        tempCloseButton = document.querySelector(flexPaneCloseButtonSelector);
                                        if (elementExists(tempCloseButton)) currentCloseButton = flexPaneCloseButtonSelector;
                                    }
                                }
                            }
                        }

                        // finalize
                        window.Android.setCloseAvailable(elementExists(tempCloseButton), currentCloseButton);
                    }, slowDelay);

                    /*var helpIFrameSelector = helpPaneSelector + ' iframe'; // TODO: DISABLED BECAUSE WEBVIEW DOESN'T ACTIVATE DYNAMIC JAVASCRIPT/CSS BEYOND THE FIRST IFRAME
                    var helpIFrameLoaded = false;
                    function checkForHelp() {
                        setTimeout(function() {
                            var helpIFrame = document.querySelector(helpIFrameSelector);
                            var helpIFrameExists = elementExists(helpIFrame);
                            if (helpIFrameExists && !helpIFrameLoaded) {
                                var newURL = window.Android.getHelpUrl();

                                // change to the new URL
                                helpIFrame.src = newURL;
                                helpIFrameLoaded = true;
                            } else if (!helpIFrameExists) helpIFrameLoaded = false;
                            checkForHelp();
                        }, fastDelay);
                    }
                    checkForHelp();*/

                    // execute once to determine swipe at page load
                    window.Android.setSwipeRefresher(noteListContainer.scrollTop);
                    // set webView to visible
                    window.Android.webViewSetVisible();
                }
            }, fastDelay);
        } else {
            var checkLoading = setInterval(function() {
                if (typeof(document.activeElement) != 'undefined' && document.activeElement != null) {
                    clearInterval(checkLoading);

                    // the create account link is broken, and needs to be changed
                    if (currentURL == loginURL) {
                        var signUpSelector = 'signup';
                        var signUp = document.getElementById(signUpSelector);

                        var newSignUp = signUp.cloneNode(true);
                        newSignUp.href = signUpURL;

                        signUp.remove();
                        document.getElementsByClassName('form-group')[1].appendChild(newSignUp);
                    }

                    window.Android.setSwipeRefresher(disableSwipe);
                    window.Android.webViewSetVisible();
                }
            }, fastDelay);
        }

        // continue with website related fixes
        var node = document.createElement('style');

        node.type = 'text/css';
        node.innerHTML = themeCss;

        document.head.appendChild(node);

    })()