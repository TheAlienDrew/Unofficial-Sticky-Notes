javascript:(function() {

        // constants
        const stickyNotesURL = 'www.onenote.com/stickynotes'
        const signUpURL = 'https://signup.live.com/';
        const loginAuthURL = 'login.microsoftonline.com/common/oauth2/authorize';
        const neverCached = 'chromewebdata'
        const disableSwipe = 1;
        const slowDelay = 1000;
        const fastDelay = 100;
        const helpPageTimeout = 10*slowDelay;
        // constant selectors
        const mainLinkNavMenuSelector = '#O365_MainLink_NavMenu';
        const mainLinkNavMenuCloseButtonSelector = '#appLauncherTop > button';
        const appLauncherMainViewSelector = '#appLauncherMainView';
        const appsModuleSelector = '#appsModule';
        const headerLeftTitleSelector = appsModuleSelector + ' > h2';
        const primaryLinksSelector = appsModuleSelector + ' > div:nth-child(2)';
        const officeLinkSelector = appLauncherMainViewSelector + ' > a';
        const allAppsLinkSpacerSelector = appsModuleSelector + ' > div:nth-child(3)';
        const allAppsLinkSelector = '#allAppsLink';
        const sidePaneContentSelector = '.n-side-pane-content';
        const flexPaneCloseButtonSelector = '#flexPaneCloseButton';
        const lightBoxCloseSelector = '.n-lightbox-close';
        const imageAltTextCancelSelector = '.n-imageAltTextCancel';
        const deleteCloseButtonSelector = '.n-cancelButton'; // close button for delete note/image
        const noteColorPickerButtonSelector = '.n-calloutMenuButton';
        const noteEditCloseButtonSelector = '.n-closeDetailViewButton';
        const dialogOverlaySelector = '.ms-Overlay';
        const dialogTitleSelector = '.ms-Dialog-title';
        const dialogConfirmButtonSelector = '.n-confirmButton';
        const searchCloseButtonSelector = '.searchBar-closeButton > div > button';
        const noteEditContainerSelector = '.n-detailViewContainer';
        const noteSelector = '.n-note';
        const noteListContainerSelector = '.n-noteListContainer';
        const noteListCellClassName = 'ms-List-cell';
        const helpButtonSelector = '#O365_MainLink_Help';
        const helpPaneSelector = '#helpPaneFull'; // TODO: Required for fall back of help page
        // theme changes based on url
        var currentURL = document.location.host + document.location.pathname;
        var themeCss = '*{-webkit-tap-highlight-color:transparent}:focus{outline:0!important}html{position:fixed;height:100%;width:100%}'; // see app_conversion.css
        var fadeInCss = '.fade-in-Unofficial{animation:fadeInUnofficial ease .2s;-webkit-animation:fadeInUnofficial ease .2s}@keyframes fadeInUnofficial{0%{opacity:0}100%{opacity:1}}@-webkit-keyframes fadeInUnofficial{0%{opacity:0}100%{opacity:1}}';
        themeCss += fadeInCss;

        // function for elements
        var elementExists = function(element) {
            return (typeof(element) != 'undefined' && element != null);
        };
        // function for figuring out if edit note is active on phone view
        var isEditing = function() {
            var noteEditContainer = document.querySelector(noteEditContainerSelector);
            if (elementExists(noteEditContainer) && !noteEditContainer.classList.contains('inactive') && !noteEditContainer.classList.contains('n-noteSelected')) return true;
            else return false;
        };
        // function for getting scrollTop to enable swipe
        var getScrollTop = function() {
            // when someone has no notes, the noteListContainer and noteList don't load in, so it has to be accounted for
            if (elementExists(noteListContainer)) return noteListContainer.scrollTop;
            else return 0;
        };

        // wait for loading animation to appear then disappear before making webView visible (if on sticky notes page)
        var editingActive = 0;
        var theScrollTop = 0;
        var noteListContainer = null;
        var noteListCell = null;
        if (currentURL == stickyNotesURL) {
            // include modified menu icons
            themeCss += '.o365cs-base .ms-Icon--MoonBlackLogo:before{content:""}.ms-svg-Icon.ms-Icon--MoonBlackLogo:before{background:url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiBpZD0ic3ZnNiIgcHJlc2VydmVBc3BlY3RSYXRpbz0ieE1pZFlNaWQgbWVldCIgdmlld0JveD0iMCAwIDQ4IDQ4IiBoZWlnaHQ9IjQ4IiB3aWR0aD0iNDgiPgogIDxnCiAgICAgaWQ9Imc0IgogICAgIHN0cm9rZT0ibm9uZSIKICAgICBmaWxsPSIjMDAwMDAwIgogICAgIHRyYW5zZm9ybT0ibWF0cml4KDAuMDA0OTAxMTYsMCwwLC0wLjAwNDkwMzM2LC0wLjAwNTIwMTIsNDguMDI5MDE4KSI+CiAgICA8cGF0aAogICAgICAgaWQ9InBhdGgyIgogICAgICAgZD0iTSAzNDU1LDk3NDUgQyAyMzUwLDkzNzggMTQzNSw4Njc3IDgwNyw3NzE5IC03MSw2Mzc4IC0yNDMsNDY4MyAzNDcsMzE3NSA0MTUsMzAwMiA2MDQsMjYxNCA2OTcsMjQ2MCAxMDQwLDE4ODggMTQ5NiwxMzgwIDIwMTUsOTk1IDI1MzcsNjA3IDMxMzAsMzIyIDM3NTAsMTYxIDQxODIsNTAgNDUzNSw2IDUwMDUsNiBjIDMxNCwtMSA0MzUsNyA3MDQsNDUgMTI5NywxODEgMjQ3OCw4ODIgMzI3NSwxOTQzIDMwMSw0MDEgNTQzLDg0OSA3MTQsMTMyMSA1NCwxNTEgMTAzLDMwNSA5NiwzMDUgLTIsMCAtNjEsLTMxIC0xMzEsLTY4IC01NzUsLTMwNyAtMTE1NSwtNDgwIC0xNzkwLC01MzMgLTE4NSwtMTUgLTY2MSwtNyAtODQzLDE1IC05MTcsMTExIC0xNzM5LDQ2OSAtMjQzNSwxMDYxIC0xMzAsMTEwIC0zODEsMzYxIC00OTgsNDk5IC01MDQsNTg4IC04NDMsMTI4MCAtMTAwMSwyMDQxIC05Myw0NDkgLTExNCwxMDAxIC01NSwxNDYwIDY4LDUzMiAyNTEsMTA5MyA1MTUsMTU3OCAzNiw2NSA2NCwxMjAgNjIsMTIyIC0yLDIgLTc1LC0yMCAtMTYzLC01MCB6IiAvPgogIDwvZz4KPC9zdmc+Cg==) 50% no-repeat;color:transparent}.o365cs-base .ms-Icon--MoonWhiteLogo:before{content:""}.ms-svg-Icon.ms-Icon--MoonWhiteLogo:before{background:url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiBpZD0ic3ZnNiIgcHJlc2VydmVBc3BlY3RSYXRpbz0ieE1pZFlNaWQgbWVldCIgdmlld0JveD0iMCAwIDQ4IDQ4IiBoZWlnaHQ9IjQ4IiB3aWR0aD0iNDgiPgogIDxnCiAgICAgaWQ9Imc0IgogICAgIHN0cm9rZT0ibm9uZSIKICAgICBmaWxsPSIjRkZGRkZGIgogICAgIHRyYW5zZm9ybT0ibWF0cml4KDAuMDA0OTAxMTYsMCwwLC0wLjAwNDkwMzM2LC0wLjAwNTIwMTIsNDguMDI5MDE4KSI+CiAgICA8cGF0aAogICAgICAgaWQ9InBhdGgyIgogICAgICAgZD0iTSAzNDU1LDk3NDUgQyAyMzUwLDkzNzggMTQzNSw4Njc3IDgwNyw3NzE5IC03MSw2Mzc4IC0yNDMsNDY4MyAzNDcsMzE3NSA0MTUsMzAwMiA2MDQsMjYxNCA2OTcsMjQ2MCAxMDQwLDE4ODggMTQ5NiwxMzgwIDIwMTUsOTk1IDI1MzcsNjA3IDMxMzAsMzIyIDM3NTAsMTYxIDQxODIsNTAgNDUzNSw2IDUwMDUsNiBjIDMxNCwtMSA0MzUsNyA3MDQsNDUgMTI5NywxODEgMjQ3OCw4ODIgMzI3NSwxOTQzIDMwMSw0MDEgNTQzLDg0OSA3MTQsMTMyMSA1NCwxNTEgMTAzLDMwNSA5NiwzMDUgLTIsMCAtNjEsLTMxIC0xMzEsLTY4IC01NzUsLTMwNyAtMTE1NSwtNDgwIC0xNzkwLC01MzMgLTE4NSwtMTUgLTY2MSwtNyAtODQzLDE1IC05MTcsMTExIC0xNzM5LDQ2OSAtMjQzNSwxMDYxIC0xMzAsMTEwIC0zODEsMzYxIC00OTgsNDk5IC01MDQsNTg4IC04NDMsMTI4MCAtMTAwMSwyMDQxIC05Myw0NDkgLTExNCwxMDAxIC01NSwxNDYwIDY4LDUzMiAyNTEsMTA5MyA1MTUsMTU3OCAzNiw2NSA2NCwxMjAgNjIsMTIyIC0yLDIgLTc1LC0yMCAtMTYzLC01MCB6IiAvPgogIDwvZz4KPC9zdmc+Cg==) 50% no-repeat;color:transparent}.o365cs-base .ms-Icon--ToolTipBlackLogo:before{content:""}.ms-svg-Icon.ms-Icon--ToolTipBlackLogo:before{background:url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczpzb2RpcG9kaT0iaHR0cDovL3NvZGlwb2RpLnNvdXJjZWZvcmdlLm5ldC9EVEQvc29kaXBvZGktMC5kdGQiIGhlaWdodD0iNDgiIHdpZHRoPSI0OCIgdmlld0JveD0iMCAwIDQ4IDQ4Ij4KICA8ZwogICAgIGlkPSJnOCIKICAgICBmaWxsLXJ1bGU9ImV2ZW5vZGQiCiAgICAgZmlsbD0ibm9uZSIKICAgICBzdHJva2Utd2lkdGg9IjEiCiAgICAgc3Ryb2tlPSJub25lIgogICAgIHRyYW5zZm9ybT0ic2NhbGUoMC4zMikiPgogICAgPHBhdGgKICAgICAgIHN0eWxlPSJzdHJva2Utd2lkdGg6MS41IgogICAgICAgc29kaXBvZGk6bm9kZXR5cGVzPSJzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3NzY2Njc3Nzc3Nzc3Nzc3NzY2Njc3NzcyIKICAgICAgIGlkPSJwYXRoNiIKICAgICAgIGZpbGw9IiMwMDAwMDAiCiAgICAgICBkPSJtIDQyLjEzMzUsNzkuMTE2IGggNjUuNzMzIGMgMi41NTksMCA0LjYzMzUsMi4wNzQ1IDQuNjMzNSw0LjYzMzUgMCwyLjU1OSAtMi4wNzQ1LDQuNjMzNSAtNC42MzM1LDQuNjMzNSBoIC02NS43MzMgYyAtMi41NTksMCAtNC42MzM1LC0yLjA3NDUgLTQuNjMzNSwtNC42MzM1IDAsLTIuNTU5IDIuMDc0NSwtNC42MzM1IDQuNjMzNSwtNC42MzM1IHogTSAzNy41LDYyLjI1MyB2IDAgYyAwLC0yLjU1OSAyLjA3NDUsLTQuNjMzNSA0LjYzMzUsLTQuNjMzNSBoIDY1LjczMyBjIDIuNTU5LDAgNC42MzM1LDIuMDc0NSA0LjYzMzUsNC42MzM1IDAsMi41NTkgLTIuMDc0NSw0LjYzMzUgLTQuNjMzNSw0LjYzMzUgSCA0Mi4xMzM1IEMgMzkuNTc0NSw2Ni44ODY1IDM3LjUsNjQuODEyIDM3LjUsNjIuMjUzIFogbSAwLC0yMS40OTY1IHYgMCBjIDAsLTIuNTU5IDIuMDc0NSwtNC42MzM1IDQuNjMzNSwtNC42MzM1IGggNjUuNzMzIGMgMi41NTksMCA0LjYzMzUsMi4wNzQ1IDQuNjMzNSw0LjYzMzUgMCwyLjU1OSAtMi4wNzQ1LDQuNjMzNSAtNC42MzM1LDQuNjMzNSBIIDQyLjEzMzUgQyAzOS41NzQ1LDQ1LjM5IDM3LjUsNDMuMzE1NSAzNy41LDQwLjc1NjUgWiBNIDEzMy4yMzMsOTYgYyAwLDMuMTYwNSAtMi41NzI1LDUuNzMzIC01LjczMyw1LjczMyBIIDEwMS41OTM1IEwgNzUsMTI0LjMzNjUgNDguNDA2NSwxMDEuNzMzIEggMjIuNSBjIC0zLjE2MDUsMCAtNS43MzMsLTIuNTcyNSAtNS43MzMsLTUuNzMzIFYgMjguNSBjIDAsLTMuMTYyIDIuNTcyNSwtNS43MzMgNS43MzMsLTUuNzMzIGggMTA1IGMgMy4xNjA1LDAgNS43MzMsMi41NzEgNS43MzMsNS43MzMgeiBNIDEyNy41LDEzLjUgaCAtMTA1IGMgLTguMjg0NSwwIC0xNSw2LjcxNTUgLTE1LDE1IFYgOTYgYyAwLDguMjg0NSA2LjcxNTUsMTUgMTUsMTUgSCA0NSBsIDMwLDI1LjUgMzAsLTI1LjUgaCAyMi41IGMgOC4yODQ1LDAgMTUsLTYuNzE1NSAxNSwtMTUgViAyOC41IGMgMCwtOC4yODQ1IC02LjcxNTUsLTE1IC0xNSwtMTUgeiIgLz4KICA8L2c+Cjwvc3ZnPgo=) 50% no-repeat;color:transparent}.o365cs-base .ms-Icon--ToolTipWhiteLogo:before{content:""}.ms-svg-Icon.ms-Icon--ToolTipWhiteLogo:before{background:url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczpzb2RpcG9kaT0iaHR0cDovL3NvZGlwb2RpLnNvdXJjZWZvcmdlLm5ldC9EVEQvc29kaXBvZGktMC5kdGQiIGhlaWdodD0iNDgiIHdpZHRoPSI0OCIgdmlld0JveD0iMCAwIDQ4IDQ4Ij4KICA8ZwogICAgIGlkPSJnOCIKICAgICBmaWxsLXJ1bGU9ImV2ZW5vZGQiCiAgICAgZmlsbD0ibm9uZSIKICAgICBzdHJva2Utd2lkdGg9IjEiCiAgICAgc3Ryb2tlPSJub25lIgogICAgIHRyYW5zZm9ybT0ic2NhbGUoMC4zMikiPgogICAgPHBhdGgKICAgICAgIHN0eWxlPSJzdHJva2Utd2lkdGg6MS41IgogICAgICAgc29kaXBvZGk6bm9kZXR5cGVzPSJzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3NzY2Njc3Nzc3Nzc3Nzc3NzY2Njc3NzcyIKICAgICAgIGlkPSJwYXRoNiIKICAgICAgIGZpbGw9IiNGRkZGRkYiCiAgICAgICBkPSJtIDQyLjEzMzUsNzkuMTE2IGggNjUuNzMzIGMgMi41NTksMCA0LjYzMzUsMi4wNzQ1IDQuNjMzNSw0LjYzMzUgMCwyLjU1OSAtMi4wNzQ1LDQuNjMzNSAtNC42MzM1LDQuNjMzNSBoIC02NS43MzMgYyAtMi41NTksMCAtNC42MzM1LC0yLjA3NDUgLTQuNjMzNSwtNC42MzM1IDAsLTIuNTU5IDIuMDc0NSwtNC42MzM1IDQuNjMzNSwtNC42MzM1IHogTSAzNy41LDYyLjI1MyB2IDAgYyAwLC0yLjU1OSAyLjA3NDUsLTQuNjMzNSA0LjYzMzUsLTQuNjMzNSBoIDY1LjczMyBjIDIuNTU5LDAgNC42MzM1LDIuMDc0NSA0LjYzMzUsNC42MzM1IDAsMi41NTkgLTIuMDc0NSw0LjYzMzUgLTQuNjMzNSw0LjYzMzUgSCA0Mi4xMzM1IEMgMzkuNTc0NSw2Ni44ODY1IDM3LjUsNjQuODEyIDM3LjUsNjIuMjUzIFogbSAwLC0yMS40OTY1IHYgMCBjIDAsLTIuNTU5IDIuMDc0NSwtNC42MzM1IDQuNjMzNSwtNC42MzM1IGggNjUuNzMzIGMgMi41NTksMCA0LjYzMzUsMi4wNzQ1IDQuNjMzNSw0LjYzMzUgMCwyLjU1OSAtMi4wNzQ1LDQuNjMzNSAtNC42MzM1LDQuNjMzNSBIIDQyLjEzMzUgQyAzOS41NzQ1LDQ1LjM5IDM3LjUsNDMuMzE1NSAzNy41LDQwLjc1NjUgWiBNIDEzMy4yMzMsOTYgYyAwLDMuMTYwNSAtMi41NzI1LDUuNzMzIC01LjczMyw1LjczMyBIIDEwMS41OTM1IEwgNzUsMTI0LjMzNjUgNDguNDA2NSwxMDEuNzMzIEggMjIuNSBjIC0zLjE2MDUsMCAtNS43MzMsLTIuNTcyNSAtNS43MzMsLTUuNzMzIFYgMjguNSBjIDAsLTMuMTYyIDIuNTcyNSwtNS43MzMgNS43MzMsLTUuNzMzIGggMTA1IGMgMy4xNjA1LDAgNS43MzMsMi41NzEgNS43MzMsNS43MzMgeiBNIDEyNy41LDEzLjUgaCAtMTA1IGMgLTguMjg0NSwwIC0xNSw2LjcxNTUgLTE1LDE1IFYgOTYgYyAwLDguMjg0NSA2LjcxNTUsMTUgMTUsMTUgSCA0NSBsIDMwLDI1LjUgMzAsLTI1LjUgaCAyMi41IGMgOC4yODQ1LDAgMTUsLTYuNzE1NSAxNSwtMTUgViAyOC41IGMgMCwtOC4yODQ1IC02LjcxNTUsLTE1IC0xNSwtMTUgeiIgLz4KICA8L2c+Cjwvc3ZnPgo=) 50% no-repeat;color:transparent}.o365cs-base .XosamnChUoOcR_dGIrHqq{height:76px!important}.o365cs-base ._1NTwdglUKLpHkf8sgkCoVl{font-size:52px!important}.o365cs-base ._1HJV45WSd5AgKYxKiK4-Fc{font:20px SegoeUI-Regular-final,Segoe UI,"Segoe UI Web (West European)",Segoe,-apple-system,BlinkMacSystemFont,Roboto,Helvetica Neue,Tahoma,Helvetica,Arial,sans-serif!important;margin-top:20px!important}';

            var checkLoading = setInterval(function() {
                var mainLinkNavMenu = document.querySelector(mainLinkNavMenuSelector);
                var sidePaneContent = document.querySelector(sidePaneContentSelector);

                // sidePaneContent.lastElementChild.innerText is the best way to determine that notes are loaded
                // trying to see if the loading animation appeared and disappeared won't always work (can be too fast)
                // TODO: Might need to come back to fix this yet again if different locales change the loading notes text
                if(elementExists(mainLinkNavMenu) && elementExists(sidePaneContent) && sidePaneContent.lastElementChild.innerText != 'Loading your notes...') {
                    clearInterval(checkLoading);

                    var helpPaneExists = false;
                    var closeButtonActive = false;
                    var editingNote = false;

                    // modify the menu for app actions
                    var isDarkMode = window.Android.isDarkMode();
                    mainLinkNavMenu.onclick = function() {
                        // launcher needs to be hidden while removing Microsoft stuff
                        var hideAppLauncher = document.createElement('style')
                        hideAppLauncher.innerHTML = '#appLauncherMainView {visibility: hidden}';
                        document.head.appendChild(hideAppLauncher);

                        var checkLauncher = setInterval(function () {
                            var appLauncher = document.querySelector(appLauncherMainViewSelector);
                            if (elementExists(appLauncher)) {
                                clearInterval(checkLauncher);

                                var checkModule = setInterval(function() {
                                    var appsModule = document.querySelector(appsModuleSelector);
                                    if (elementExists(appsModule)) {
                                        clearInterval(checkModule);

                                        var headerLeftTitle = document.querySelector(headerLeftTitleSelector);
                                        var primaryLinks = document.querySelector(primaryLinksSelector);
                                        var officeLink = document.querySelector(officeLinkSelector);
                                        var allAppsLinkSpacer = document.querySelector(allAppsLinkSpacerSelector);
                                        var allAppsLink = document.querySelector(allAppsLinkSelector);
                                        // remove things not being used
                                        officeLink.remove();
                                        allAppsLinkSpacer.remove();
                                        allAppsLink.remove();
                                        // change some things
                                        headerLeftTitle.innerText = window.Android.getAndroidString('options');
                                        // create template link
                                        var outlookAction = primaryLinks.children[0];
                                        var templateAction = outlookAction.cloneNode(true);
                                        var templateLink = templateAction.firstChild;
                                        var templateLogo = templateLink.firstChild.firstChild;
                                        var templateText = templateLink.lastChild.firstChild;
                                        templateAction.style.width = (outlookAction.clientWidth * 2) + 'px';
                                        templateLink.id = templateLink.ariaLabel = templateText.innerText = 'Template';
                                        templateLink.href = 'javascript:(function(){ alert("insert JS function here"); })()';
                                        templateLink.removeAttribute('target');
                                        templateLink.style.color = '#000';
                                        templateLogo.classList.remove('ms-Icon--OutlookLogo'); // update me when needed
                                        //templateLogo.className = 'ms-Icon--TemplateLogo ' + templateLogo.className;

                                        primaryLinks.innerHTML = '';

                                        // include my own links
                                        var themeModeAction = templateAction.cloneNode(true);
                                        var themeModeLink = themeModeAction.firstChild;
                                        var themeModeLogo = themeModeLink.firstChild.firstChild;
                                        var themeModeText = themeModeLink.lastChild.firstChild;
                                        themeModeLink.href = 'javascript:(function(){ window.Android.promptSwitchTheme() })()';
                                        themeModeLink.id = 'SwitchTheme';
                                        themeModeLink.ariaLabel = themeModeText.innerText = window.Android.getAndroidString('switchTheme');
                                        if (isDarkMode) {
                                            themeModeLogo.className = 'ms-Icon--MoonWhiteLogo ' + themeModeLogo.className;
                                        } else {
                                            themeModeLogo.className = 'ms-Icon--MoonBlackLogo ' + themeModeLogo.className;
                                        }
                                        primaryLinks.appendChild(themeModeAction);

                                        var toggleToolTipsAction = templateAction.cloneNode(true);
                                        var toggleToolTipsLink = toggleToolTipsAction.firstChild;
                                        var toggleToolTipsLogo = toggleToolTipsLink.firstChild.firstChild;
                                        var toggleToolTipsText = toggleToolTipsLink.lastChild.firstChild;
                                        toggleToolTipsLink.href = 'javascript:(function(){ window.Android.promptToggleToolTips() })()';
                                        toggleToolTipsLink.id = 'ToggleToolTips';
                                        toggleToolTipsLink.ariaLabel = toggleToolTipsText.innerText = window.Android.getAndroidString('toggleToolTips');
                                        if (isDarkMode) {
                                            toggleToolTipsLogo.className = 'ms-Icon--ToolTipWhiteLogo ' + toggleToolTipsLogo.className;
                                        } else {
                                            toggleToolTipsLogo.className = 'ms-Icon--ToolTipBlackLogo ' + toggleToolTipsLogo.className;
                                        }
                                        primaryLinks.appendChild(toggleToolTipsAction);

                                        // make launcher visible again
                                        setTimeout(function() {hideAppLauncher.remove()}, fastDelay);
                                    }
                                }, fastDelay);
                            }
                        }, fastDelay);
                    };
                    // scrolling note list checks swipe
                    var waitForNoteListContainer = setInterval(function() {
                        noteListContainer = document.querySelector(noteListContainerSelector);
                        if (elementExists(noteListContainer)) {
                            clearInterval(waitForNoteListContainer);

                            noteListContainer.addEventListener('touchmove', function(e) {
                                editingActive = isEditing();
                                theScrollTop = getScrollTop();
                                window.Android.setSwipeRefresher(theScrollTop, editingActive);
                            }, false);
                            // pressing in note list checks swipe
                            noteListContainer.addEventListener('touchstart', function(e) {
                                editingActive = isEditing();
                                theScrollTop = getScrollTop();
                                window.Android.setSwipeRefresher(theScrollTop, editingActive);
                            }, false);
                        }
                    }, fastDelay);
                    // after clicking a note in list, disable swipe
                    var waitForNoteListCell = setInterval(function() {
                        noteListCell = document.getElementsByClassName(noteListCellClassName);
                        if (elementExists(noteListCell)) clearInterval(waitForNoteListCell);
                    }, fastDelay);
                    setInterval(function() {
                        if (elementExists(noteListCell)) {
                            var i;
                            for (i = 0; i < noteListCell.length; ++i) {
                                // needs to always check

                                noteListCell[i].onclick = function () {
                                    window.Android.setSwipeRefresher(disableSwipe, disableSwipe);
                                    editingNote = false; // needed to copy note data from another note when in tablet mode
                                };
                            }
                        }
                    }, slowDelay);
                    // change help to open in the webView
                    var waitForHelp = setInterval(function() {
                        var helpButton = document.querySelector(helpButtonSelector);
                        if (elementExists(helpButton)) {
                            clearInterval(waitForHelp);

                            helpButton.onclick = function () {
                                window.Android.webViewSetVisible(false);

                                var helpIFrameSelector = helpPaneSelector + ' iframe';
                                var helpIFrameLoaded = false;
                                var checkForHelp = setInterval(function () {
                                    var helpIFrame = document.querySelector(helpIFrameSelector);

                                    var helpPageTimed = 0;
                                    if (elementExists(helpIFrame) || helpPageTimed == helpPageTimeout) {
                                        clearInterval(checkForHelp);

                                        if (helpPageTimed == helpPageTimeout) {
                                            var helpNotCached = window.Android.getAndroidString('helpNotCached');
                                            alert(helpNotCached);
                                        } else {
                                            helpIFrame.onload = function() {
                                                window.Android.loadStickiesHelp();
                                            };

                                            // change to the new URL
                                            helpIFrame.src = window.Android.getHelpUrl();
                                        }

                                        helpPageTimed += slowDelay;
                                    }
                                }, slowDelay);
                            };
                        }
                    }, fastDelay);
                    // disable swipe if help pane is open // TODO: This is a fall back if the help page doesn't load into the webView
                    setInterval(function() {
                        var helpPane = document.querySelector(helpPaneSelector);
                        if (elementExists(helpPane) && !helpPaneExists) {
                            // needs to always check

                            window.Android.setSwipeRefresher(disableSwipe, disableSwipe);
                            if (window.Android.isDarkMode()) helpPane.style = 'filter:invert(100%)hue-rotate(180deg)';
                            helpPaneExists = true;
                        } else helpPaneExists = false;
                    }, slowDelay);
                    // disable swipe while editing a note
                    var waitForEditNote = setInterval(function() {
                        var note = document.querySelector(noteSelector);
                        if (elementExists(note)) {
                            clearInterval(note);

                            note.onclick = function () {
                                // check classlist for inactivity
                                if (!note.classList.contains('inactive')) {
                                    window.Android.setSwipeRefresher(disableSwipe, disableSwipe);
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
                                editingActive = isEditing();
                                theScrollTop = getScrollTop();
                                window.Android.setSwipeRefresher(theScrollTop, editingActive);
                            };
                        }
                    }, slowDelay);

                    // allow back button to close any current element that can be closed out
                    setInterval(function() {
                        var currentCloseElement = '';
                        // first, see if description cancel button exists
                        var tempCloseButton = document.querySelector(imageAltTextCancelSelector);
                        if (elementExists(tempCloseButton)) currentCloseElement = imageAltTextCancelSelector;
                        else {
                            // second, see if delete note/image close button exists
                            tempCloseButton = document.querySelector(deleteCloseButtonSelector);
                            if (elementExists(tempCloseButton)) currentCloseElement = deleteCloseButtonSelector;
                            else {
                                // third, see if image close button exists
                                tempCloseButton = document.querySelector(lightBoxCloseSelector);
                                if (elementExists(tempCloseButton)) currentCloseElement = lightBoxCloseSelector;
                                else {
                                    // fourth, see if edit note close button exists and is visible
                                    tempCloseButton = document.querySelector(noteEditCloseButtonSelector);
                                    if (elementExists(tempCloseButton) && window.getComputedStyle(document.querySelector(noteEditCloseButtonSelector)).visibility == 'visible') currentCloseElement = noteEditCloseButtonSelector;
                                    else {
                                        // fifth, see if the menu pane close action is available
                                        tempCloseButton = document.querySelector(mainLinkNavMenuCloseButtonSelector);
                                        if (elementExists(tempCloseButton)) currentCloseElement = mainLinkNavMenuCloseButtonSelector;
                                        else {
                                            // sixth, see if any flex pane close button exists (settings/help/account)
                                            tempCloseButton = document.querySelector(flexPaneCloseButtonSelector);
                                            if (elementExists(tempCloseButton)) {
                                                // sixth and a half, check if export notes dialog is open
                                                var dialogTitleExport = document.querySelector(dialogTitleSelector);
                                                var dialogOverlay = document.querySelector(dialogOverlaySelector);
                                                if (elementExists(dialogTitleExport) && (dialogTitleExport.innerText.match(/export/i) != '') && elementExists(dialogOverlay)) currentCloseElement = dialogOverlaySelector;
                                                else currentCloseElement = flexPaneCloseButtonSelector;
                                            } else {
                                                // seventh, see if the offline ok button exists
                                                var dialogTitleOffline = document.querySelector(dialogTitleSelector);
                                                tempCloseButton = document.querySelector(dialogConfirmButtonSelector);
                                                if (elementExists(dialogTitleOffline) && (dialogTitleOffline.innerText.match(/offline/i) != '') && elementExists(dialogConfirmButtonSelector)) currentCloseElement = dialogConfirmButtonSelector;
                                                else {
                                                    // lastly, see if the search close button exists and is visible
                                                    tempCloseButton = document.querySelector(searchCloseButtonSelector);
                                                    if (elementExists(tempCloseButton) && window.getComputedStyle(document.querySelector(searchCloseButtonSelector)).visibility == 'visible') currentCloseElement = searchCloseButtonSelector;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // finalize
                        window.Android.setCloseAvailable(elementExists(tempCloseButton), currentCloseElement);
                    }, slowDelay);

                    /* var helpIFrameSelector = helpPaneSelector + ' iframe'; // TODO: DISABLED BECAUSE WEBVIEW DOESN'T ACTIVATE DYNAMIC JAVASCRIPT/CSS BEYOND THE FIRST IFRAME
                    var helpIFrameLoaded = false;
                    function checkForHelp() {
                        setTimeout(function() {
                            var helpIFrame = document.querySelector(helpIFrameSelector);
                            var helpIFrameExists = elementExists(helpIFrame);
                            if (helpIFrameExists && !helpIFrameLoaded) {
                                helpIFrameLoaded = true;

                                // change to the new URL
                                var newURL = window.Android.getHelpUrl();

                                // what to execute when the iFrame loads
                                helpIFrame.onload = function () {
                                    var checkForHelpIFrame = setInterval(function() {
                                        var helpIFrameDoc = helpIFrame.contentDocument;

                                        if (helpIFrameDoc != null) {
                                            clearInterval(checkForHelpIFrame);

                                            var iframeID = 'ocSearchIFrame'; // TODO: const

                                            // set the style fixes
                                            var checkForIFrame = setInterval(function() {
                                                var iframe = helpIFrameDoc.getElementById(iframeID);
                                                var iframeDoc = iframe.contentDocument;

                                                if (elementExists(iframe) && iframeDoc != null) {
                                                    clearInterval(checkForIFrame);

                                                    // must listen for page load to change style
                                                    iframe.onload = function () {
                                                        var iDocument = frames[0].document;

                                                        // activate the MS Support Modern CSS (improves light theme, and fixes dark theme)
                                                        var msSupportModernStyle = document.createElement('link');
                                                        msSupportModernStyle.type = 'text/css';
                                                        msSupportModernStyle.rel = 'stylesheet';
                                                        msSupportModernStyle.href = cssSupportModernMS;
                                                        iDocument.head.appendChild(msSupportModernStyle);
                                                    }
                                                }
                                            }, fastDelay);
                                        }
                                    }, fastDelay);
                                }

                                helpIFrame.src = newURL;
                            } else if (!helpIFrameExists) helpIFrameLoaded = false;
                            checkForHelp();
                        }, fastDelay);
                    }
                    checkForHelp(); */

                    // execute once to determine swipe at page load
                    editingActive = isEditing();
                    theScrollTop = getScrollTop();
                    window.Android.setSwipeRefresher(theScrollTop, editingActive);
                    // set webView to visible after a small delay (so the loading gif on page disappears a bit more)
                    document.body.classList.add('fade-in-Unofficial'); // animate opacity as fade before showing page
                    setTimeout(function() {
                        window.Android.webViewSetVisible(true);
                    }, slowDelay);
                }
            }, fastDelay);
        } else {
            var checkLoading = setInterval(function() {
                if (typeof(document.activeElement) != 'undefined' && document.activeElement != null) {
                    clearInterval(checkLoading);

                    // the create account link is broken, and needs to be changed
                    if (currentURL == loginAuthURL) {
                        var signUpSelector = 'signup';
                        var signUp = document.getElementById(signUpSelector);

                        var newSignUp = signUp.cloneNode(true);
                        newSignUp.href = signUpURL;

                        signUp.remove();
                        document.getElementsByClassName('form-group')[1].appendChild(newSignUp);
                    }

                    window.Android.setSwipeRefresher(disableSwipe, disableSwipe);

                    // let login pages not fade in (just appear)
                    var helpLink = window.Android.getHelpUrl();
                    if (document.location.host != neverCached && currentURL != helpLink) {
                        window.Android.webViewSetVisible(true);
                    }
                }
            }, fastDelay);
        }

        // continue with website related fixes
        var node = document.createElement('style');

        node.type = 'text/css';
        node.innerHTML = themeCss;

        document.head.appendChild(node);

    })()