javascript:(function() {
        /* Copyright (C) 2020  Andrew Larson (thealiendrew@gmail.com)
         *
         * This program is free software: you can redistribute it and/or modify
         * it under the terms of the GNU General Public License as published by
         * the Free Software Foundation, either version 3 of the License, or
         * (at your option) any later version.
         *
         * This program is distributed in the hope that it will be useful,
         * but WITHOUT ANY WARRANTY; without even the implied warranty of
         * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
         * GNU General Public License for more details.
         *
         * You should have received a copy of the GNU General Public License
         * along with this program.  If not, see <https://www.gnu.org/licenses/>.
         */
        console.log("convert_stickyNotesMain.js started");

        // constants
        const neverCached = 'chromewebdata'
        const DISABLE_SWIPE = 1;
        const slowDelay = 1000;
        const fastDelay = 100;
        const helpPageTimeout = 10*slowDelay;
        // constant selectors
        const notesLoadingSelector = '.n-loadingAnimationContainer';
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
        // notes dark mode constants
        const darkModeClassName = 'n-darkMode';
        const uiContainerSelector = '#n-ui-container';
        // loading gif constants
        const loadingGifDark = 'https://npwuscdn-onenote.azureedge.net/ondcnotesintegration/img/loading-dark.gif';
        const loadingGifSelector = '#n-side-pane > div.n-side-pane-content > div > div > div > img';
        // theme constants
        const themeBaseCss = '*{-webkit-tap-highlight-color:transparent}:focus{outline:0!important}html{position:fixed;height:100%;width:100%}'; // see app_conversion.css
        const fadeInCss = '.fade-in-Unofficial{animation:fadeInUnofficial ease .2s;-webkit-animation:fadeInUnofficial ease .2s}@keyframes fadeInUnofficial{0%{opacity:0}100%{opacity:1}}@-webkit-keyframes fadeInUnofficial{0%{opacity:0}100%{opacity:1}}';
        const darkModeCss = 'html{background:#202020}.n-loaderTitle{filter:invert(100%)}.n-loader{border-color:#7719aa #262626 #262626}.n-loadingAnimationContainer{background:#202020;border-color:rgba(32,32,32,.1)}.n-imageAltInputWrapper textarea,.n-imageAltWrapper,.n-lightboxOverflow-container{background-color:var(--n-overflow-menu-background)}.n-imageAltInputWrapper textarea,.n-imageAltTextCancel,.n-imageAltWrapper,.n-lightboxOverflow-container button{color:#fff}.n-imageAltWrapper{border-color:#fff}.n-imageAltInputWrapper textarea::placeholder{color:#959493}.n-imageAltTextCancel{border:1px solid #fff!important;padding:0;background-color:transparent}.n-imageAltButtonWrapper button:hover{background-color:#fff;color:#000;opacity:1!important}#O365_MainLink_NavMenu:focus,.o365cs-base .o365sx-activeButton,.o365cs-base .o365sx-activeButton:focus,.o365cs-base .o365sx-activeButton:hover{background-color:#202020!important;color:#fff!important}.ms-Dialog-button--close:hover,.o365cs-base .o365sx-neutral-lighterAlt-background,.o365sx-neutral-lighter-hover-background:hover{background:#333!important}#appLauncherTop .o365sx-neutral-dark-font,#appsModule h2.o365sx-neutral-dark-font,#flexPaneScrollRegion .o365sx-neutral-dark-font,.ms-Dialog{color:#fff}#allView .o365sx-neutral-dark-font,#appsModule div.o365sx-neutral-dark-font{filter:hue-rotate(180deg) invert(100%)}#FlexPane_MeControl .o365sx-neutral-foreground-background,#appLauncher,.o365cs-base .o365sx-neutral-foreground-background{background:#494949!important}#flexPaneCloseButton,.ms-Icon--BingLogo,.ms-Icon--MSNLogo,.ms-Icon--People,.ms-Icon--PrivacyLogo,.ms-Icon--RewardsLogo,i[data-icon-name=Cancel]{filter:hue-rotate(180deg) invert(100%)}#O365fpcontainerid{border-color:#000}#FlexPane_MeControl a,#appLauncherMainView a,#appsModule button.o365sx-neutral-accent-font,#flexPaneScrollRegion a,button#allViewBackButton.o365sx-neutral-accent-font{color:#93cff7}.ms-Dialog>:not(.ms-Overlay--dark) .ms-Overlay{background-color:rgba(0,0,0,.4)}.ms-Dialog>.ms-Overlay--dark{background-color:rgba(255,255,255,.4)}.ms-Dialog-title{color:#ccc}.ms-Dialog-content [class^=innerContent-]{color:#fff}:not(.n-lightboxModal)>.ms-Dialog-main{background:#494949}';
        const menuIconsCss = '.o365cs-base .ms-Icon--MoonBlackLogo:before{content:""}.ms-svg-Icon.ms-Icon--MoonBlackLogo:before{background:url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiBpZD0ic3ZnNiIgcHJlc2VydmVBc3BlY3RSYXRpbz0ieE1pZFlNaWQgbWVldCIgdmlld0JveD0iMCAwIDQ4IDQ4IiBoZWlnaHQ9IjQ4IiB3aWR0aD0iNDgiPgogIDxnCiAgICAgaWQ9Imc0IgogICAgIHN0cm9rZT0ibm9uZSIKICAgICBmaWxsPSIjMDAwMDAwIgogICAgIHRyYW5zZm9ybT0ibWF0cml4KDAuMDA0OTAxMTYsMCwwLC0wLjAwNDkwMzM2LC0wLjAwNTIwMTIsNDguMDI5MDE4KSI+CiAgICA8cGF0aAogICAgICAgaWQ9InBhdGgyIgogICAgICAgZD0iTSAzNDU1LDk3NDUgQyAyMzUwLDkzNzggMTQzNSw4Njc3IDgwNyw3NzE5IC03MSw2Mzc4IC0yNDMsNDY4MyAzNDcsMzE3NSA0MTUsMzAwMiA2MDQsMjYxNCA2OTcsMjQ2MCAxMDQwLDE4ODggMTQ5NiwxMzgwIDIwMTUsOTk1IDI1MzcsNjA3IDMxMzAsMzIyIDM3NTAsMTYxIDQxODIsNTAgNDUzNSw2IDUwMDUsNiBjIDMxNCwtMSA0MzUsNyA3MDQsNDUgMTI5NywxODEgMjQ3OCw4ODIgMzI3NSwxOTQzIDMwMSw0MDEgNTQzLDg0OSA3MTQsMTMyMSA1NCwxNTEgMTAzLDMwNSA5NiwzMDUgLTIsMCAtNjEsLTMxIC0xMzEsLTY4IC01NzUsLTMwNyAtMTE1NSwtNDgwIC0xNzkwLC01MzMgLTE4NSwtMTUgLTY2MSwtNyAtODQzLDE1IC05MTcsMTExIC0xNzM5LDQ2OSAtMjQzNSwxMDYxIC0xMzAsMTEwIC0zODEsMzYxIC00OTgsNDk5IC01MDQsNTg4IC04NDMsMTI4MCAtMTAwMSwyMDQxIC05Myw0NDkgLTExNCwxMDAxIC01NSwxNDYwIDY4LDUzMiAyNTEsMTA5MyA1MTUsMTU3OCAzNiw2NSA2NCwxMjAgNjIsMTIyIC0yLDIgLTc1LC0yMCAtMTYzLC01MCB6IiAvPgogIDwvZz4KPC9zdmc+Cg==) 50% no-repeat;color:transparent}.o365cs-base .ms-Icon--MoonWhiteLogo:before{content:""}.ms-svg-Icon.ms-Icon--MoonWhiteLogo:before{background:url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiBpZD0ic3ZnNiIgcHJlc2VydmVBc3BlY3RSYXRpbz0ieE1pZFlNaWQgbWVldCIgdmlld0JveD0iMCAwIDQ4IDQ4IiBoZWlnaHQ9IjQ4IiB3aWR0aD0iNDgiPgogIDxnCiAgICAgaWQ9Imc0IgogICAgIHN0cm9rZT0ibm9uZSIKICAgICBmaWxsPSIjRkZGRkZGIgogICAgIHRyYW5zZm9ybT0ibWF0cml4KDAuMDA0OTAxMTYsMCwwLC0wLjAwNDkwMzM2LC0wLjAwNTIwMTIsNDguMDI5MDE4KSI+CiAgICA8cGF0aAogICAgICAgaWQ9InBhdGgyIgogICAgICAgZD0iTSAzNDU1LDk3NDUgQyAyMzUwLDkzNzggMTQzNSw4Njc3IDgwNyw3NzE5IC03MSw2Mzc4IC0yNDMsNDY4MyAzNDcsMzE3NSA0MTUsMzAwMiA2MDQsMjYxNCA2OTcsMjQ2MCAxMDQwLDE4ODggMTQ5NiwxMzgwIDIwMTUsOTk1IDI1MzcsNjA3IDMxMzAsMzIyIDM3NTAsMTYxIDQxODIsNTAgNDUzNSw2IDUwMDUsNiBjIDMxNCwtMSA0MzUsNyA3MDQsNDUgMTI5NywxODEgMjQ3OCw4ODIgMzI3NSwxOTQzIDMwMSw0MDEgNTQzLDg0OSA3MTQsMTMyMSA1NCwxNTEgMTAzLDMwNSA5NiwzMDUgLTIsMCAtNjEsLTMxIC0xMzEsLTY4IC01NzUsLTMwNyAtMTE1NSwtNDgwIC0xNzkwLC01MzMgLTE4NSwtMTUgLTY2MSwtNyAtODQzLDE1IC05MTcsMTExIC0xNzM5LDQ2OSAtMjQzNSwxMDYxIC0xMzAsMTEwIC0zODEsMzYxIC00OTgsNDk5IC01MDQsNTg4IC04NDMsMTI4MCAtMTAwMSwyMDQxIC05Myw0NDkgLTExNCwxMDAxIC01NSwxNDYwIDY4LDUzMiAyNTEsMTA5MyA1MTUsMTU3OCAzNiw2NSA2NCwxMjAgNjIsMTIyIC0yLDIgLTc1LC0yMCAtMTYzLC01MCB6IiAvPgogIDwvZz4KPC9zdmc+Cg==) 50% no-repeat;color:transparent}.o365cs-base .ms-Icon--ToolTipBlackLogo:before{content:""}.ms-svg-Icon.ms-Icon--ToolTipBlackLogo:before{background:url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczpzb2RpcG9kaT0iaHR0cDovL3NvZGlwb2RpLnNvdXJjZWZvcmdlLm5ldC9EVEQvc29kaXBvZGktMC5kdGQiIGhlaWdodD0iNDgiIHdpZHRoPSI0OCIgdmlld0JveD0iMCAwIDQ4IDQ4Ij4KICA8ZwogICAgIGlkPSJnOCIKICAgICBmaWxsLXJ1bGU9ImV2ZW5vZGQiCiAgICAgZmlsbD0ibm9uZSIKICAgICBzdHJva2Utd2lkdGg9IjEiCiAgICAgc3Ryb2tlPSJub25lIgogICAgIHRyYW5zZm9ybT0ic2NhbGUoMC4zMikiPgogICAgPHBhdGgKICAgICAgIHN0eWxlPSJzdHJva2Utd2lkdGg6MS41IgogICAgICAgc29kaXBvZGk6bm9kZXR5cGVzPSJzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3NzY2Njc3Nzc3Nzc3Nzc3NzY2Njc3NzcyIKICAgICAgIGlkPSJwYXRoNiIKICAgICAgIGZpbGw9IiMwMDAwMDAiCiAgICAgICBkPSJtIDQyLjEzMzUsNzkuMTE2IGggNjUuNzMzIGMgMi41NTksMCA0LjYzMzUsMi4wNzQ1IDQuNjMzNSw0LjYzMzUgMCwyLjU1OSAtMi4wNzQ1LDQuNjMzNSAtNC42MzM1LDQuNjMzNSBoIC02NS43MzMgYyAtMi41NTksMCAtNC42MzM1LC0yLjA3NDUgLTQuNjMzNSwtNC42MzM1IDAsLTIuNTU5IDIuMDc0NSwtNC42MzM1IDQuNjMzNSwtNC42MzM1IHogTSAzNy41LDYyLjI1MyB2IDAgYyAwLC0yLjU1OSAyLjA3NDUsLTQuNjMzNSA0LjYzMzUsLTQuNjMzNSBoIDY1LjczMyBjIDIuNTU5LDAgNC42MzM1LDIuMDc0NSA0LjYzMzUsNC42MzM1IDAsMi41NTkgLTIuMDc0NSw0LjYzMzUgLTQuNjMzNSw0LjYzMzUgSCA0Mi4xMzM1IEMgMzkuNTc0NSw2Ni44ODY1IDM3LjUsNjQuODEyIDM3LjUsNjIuMjUzIFogbSAwLC0yMS40OTY1IHYgMCBjIDAsLTIuNTU5IDIuMDc0NSwtNC42MzM1IDQuNjMzNSwtNC42MzM1IGggNjUuNzMzIGMgMi41NTksMCA0LjYzMzUsMi4wNzQ1IDQuNjMzNSw0LjYzMzUgMCwyLjU1OSAtMi4wNzQ1LDQuNjMzNSAtNC42MzM1LDQuNjMzNSBIIDQyLjEzMzUgQyAzOS41NzQ1LDQ1LjM5IDM3LjUsNDMuMzE1NSAzNy41LDQwLjc1NjUgWiBNIDEzMy4yMzMsOTYgYyAwLDMuMTYwNSAtMi41NzI1LDUuNzMzIC01LjczMyw1LjczMyBIIDEwMS41OTM1IEwgNzUsMTI0LjMzNjUgNDguNDA2NSwxMDEuNzMzIEggMjIuNSBjIC0zLjE2MDUsMCAtNS43MzMsLTIuNTcyNSAtNS43MzMsLTUuNzMzIFYgMjguNSBjIDAsLTMuMTYyIDIuNTcyNSwtNS43MzMgNS43MzMsLTUuNzMzIGggMTA1IGMgMy4xNjA1LDAgNS43MzMsMi41NzEgNS43MzMsNS43MzMgeiBNIDEyNy41LDEzLjUgaCAtMTA1IGMgLTguMjg0NSwwIC0xNSw2LjcxNTUgLTE1LDE1IFYgOTYgYyAwLDguMjg0NSA2LjcxNTUsMTUgMTUsMTUgSCA0NSBsIDMwLDI1LjUgMzAsLTI1LjUgaCAyMi41IGMgOC4yODQ1LDAgMTUsLTYuNzE1NSAxNSwtMTUgViAyOC41IGMgMCwtOC4yODQ1IC02LjcxNTUsLTE1IC0xNSwtMTUgeiIgLz4KICA8L2c+Cjwvc3ZnPgo=) 50% no-repeat;color:transparent}.o365cs-base .ms-Icon--ToolTipWhiteLogo:before{content:""}.ms-svg-Icon.ms-Icon--ToolTipWhiteLogo:before{background:url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczpzb2RpcG9kaT0iaHR0cDovL3NvZGlwb2RpLnNvdXJjZWZvcmdlLm5ldC9EVEQvc29kaXBvZGktMC5kdGQiIGhlaWdodD0iNDgiIHdpZHRoPSI0OCIgdmlld0JveD0iMCAwIDQ4IDQ4Ij4KICA8ZwogICAgIGlkPSJnOCIKICAgICBmaWxsLXJ1bGU9ImV2ZW5vZGQiCiAgICAgZmlsbD0ibm9uZSIKICAgICBzdHJva2Utd2lkdGg9IjEiCiAgICAgc3Ryb2tlPSJub25lIgogICAgIHRyYW5zZm9ybT0ic2NhbGUoMC4zMikiPgogICAgPHBhdGgKICAgICAgIHN0eWxlPSJzdHJva2Utd2lkdGg6MS41IgogICAgICAgc29kaXBvZGk6bm9kZXR5cGVzPSJzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3NzY2Njc3Nzc3Nzc3Nzc3NzY2Njc3NzcyIKICAgICAgIGlkPSJwYXRoNiIKICAgICAgIGZpbGw9IiNGRkZGRkYiCiAgICAgICBkPSJtIDQyLjEzMzUsNzkuMTE2IGggNjUuNzMzIGMgMi41NTksMCA0LjYzMzUsMi4wNzQ1IDQuNjMzNSw0LjYzMzUgMCwyLjU1OSAtMi4wNzQ1LDQuNjMzNSAtNC42MzM1LDQuNjMzNSBoIC02NS43MzMgYyAtMi41NTksMCAtNC42MzM1LC0yLjA3NDUgLTQuNjMzNSwtNC42MzM1IDAsLTIuNTU5IDIuMDc0NSwtNC42MzM1IDQuNjMzNSwtNC42MzM1IHogTSAzNy41LDYyLjI1MyB2IDAgYyAwLC0yLjU1OSAyLjA3NDUsLTQuNjMzNSA0LjYzMzUsLTQuNjMzNSBoIDY1LjczMyBjIDIuNTU5LDAgNC42MzM1LDIuMDc0NSA0LjYzMzUsNC42MzM1IDAsMi41NTkgLTIuMDc0NSw0LjYzMzUgLTQuNjMzNSw0LjYzMzUgSCA0Mi4xMzM1IEMgMzkuNTc0NSw2Ni44ODY1IDM3LjUsNjQuODEyIDM3LjUsNjIuMjUzIFogbSAwLC0yMS40OTY1IHYgMCBjIDAsLTIuNTU5IDIuMDc0NSwtNC42MzM1IDQuNjMzNSwtNC42MzM1IGggNjUuNzMzIGMgMi41NTksMCA0LjYzMzUsMi4wNzQ1IDQuNjMzNSw0LjYzMzUgMCwyLjU1OSAtMi4wNzQ1LDQuNjMzNSAtNC42MzM1LDQuNjMzNSBIIDQyLjEzMzUgQyAzOS41NzQ1LDQ1LjM5IDM3LjUsNDMuMzE1NSAzNy41LDQwLjc1NjUgWiBNIDEzMy4yMzMsOTYgYyAwLDMuMTYwNSAtMi41NzI1LDUuNzMzIC01LjczMyw1LjczMyBIIDEwMS41OTM1IEwgNzUsMTI0LjMzNjUgNDguNDA2NSwxMDEuNzMzIEggMjIuNSBjIC0zLjE2MDUsMCAtNS43MzMsLTIuNTcyNSAtNS43MzMsLTUuNzMzIFYgMjguNSBjIDAsLTMuMTYyIDIuNTcyNSwtNS43MzMgNS43MzMsLTUuNzMzIGggMTA1IGMgMy4xNjA1LDAgNS43MzMsMi41NzEgNS43MzMsNS43MzMgeiBNIDEyNy41LDEzLjUgaCAtMTA1IGMgLTguMjg0NSwwIC0xNSw2LjcxNTUgLTE1LDE1IFYgOTYgYyAwLDguMjg0NSA2LjcxNTUsMTUgMTUsMTUgSCA0NSBsIDMwLDI1LjUgMzAsLTI1LjUgaCAyMi41IGMgOC4yODQ1LDAgMTUsLTYuNzE1NSAxNSwtMTUgViAyOC41IGMgMCwtOC4yODQ1IC02LjcxNTUsLTE1IC0xNSwtMTUgeiIgLz4KICA8L2c+Cjwvc3ZnPgo=) 50% no-repeat;color:transparent}.o365cs-base .XosamnChUoOcR_dGIrHqq{height:76px!important}.o365cs-base ._1NTwdglUKLpHkf8sgkCoVl{font-size:52px!important}.o365cs-base ._1HJV45WSd5AgKYxKiK4-Fc{font:20px SegoeUI-Regular-final,Segoe UI,"Segoe UI Web (West European)",Segoe,-apple-system,BlinkMacSystemFont,Roboto,Helvetica Neue,Tahoma,Helvetica,Arial,sans-serif!important;margin-top:20px!important}';

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

        // variables
        var isDarkMode = window.Android.isDarkMode();
        var themeCss = themeBaseCss + menuIconsCss + fadeInCss;
        // add onto the theme in case of dark mode
        if (isDarkMode) themeCss += darkModeCss;

        // wait for loading animation to appear then disappear before making webView visible
        var editingActive = 0;
        var theScrollTop = 0;
        var noteListContainer = null;
        var noteListCell = null;
        var checkLoading = setInterval(function() {
            var mainLinkNavMenu = document.querySelector(mainLinkNavMenuSelector);
            var sidePaneContent = document.querySelector(sidePaneContentSelector);

            // sidePaneContent.lastElementChild.innerText != "Loading your notes..." was a good way
            //   to test, but not when locales change
            // trying to see if the loading animation appeared and disappeared won't always work
            //   (can be too fast)
            // so, just need to test if the side pane has at least loaded, and then query select for
            //  the loading element
            if(elementExists(mainLinkNavMenu)
               && elementExists(sidePaneContent)
               && !elementExists(sidePaneContent.querySelector(notesLoadingSelector)) ) {
                clearInterval(checkLoading);

                var helpPaneExists = false;
                var closeButtonActive = false;
                var editingNote = false;

                // modify the menu for app actions
                mainLinkNavMenu.onclick = function() {
                    // launcher needs to be hidden while removing Microsoft stuff
                    var hideAppLauncher = document.createElement('style')
                    hideAppLauncher.innerHTML = appLauncherMainViewSelector + ' {visibility: hidden}';
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
                                window.Android.setSwipeRefresher(DISABLE_SWIPE, DISABLE_SWIPE);
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

                        window.Android.setSwipeRefresher(DISABLE_SWIPE, DISABLE_SWIPE);
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
                                window.Android.setSwipeRefresher(DISABLE_SWIPE, DISABLE_SWIPE);
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

        // continue with website related theme fixes
        var node = document.createElement('style');
        node.type = 'text/css';
        node.innerHTML = themeCss;
        document.head.appendChild(node);

        // additional fixes for the sticky notes page
        if (isDarkMode) {
            // this enables Microsoft's dark mode (normally turned on from outlook.com)
            document.body.classList.add(darkModeClassName);
            // fix issues with loading gif being light
            var loadingGif = null;
            var fixLoadingGif = setInterval(function() {
                loadingGif = document.querySelector(loadingGifSelector);
                if (elementExists(loadingGif)) {
                    clearInterval(fixLoadingGif);
                    loadingGif.src = loadingGifDark;
                }
            }, 10);
            // fix issues with Phone note view not being darkened like it should
            setInterval(function() {
                var uiContainer = document.querySelector(uiContainerSelector);
                if (elementExists(uiContainer) && !uiContainer.classList.contains(darkModeClassName)) uiContainer.classList.add(darkModeClassName);
            }, fastDelay);
        }

    })()