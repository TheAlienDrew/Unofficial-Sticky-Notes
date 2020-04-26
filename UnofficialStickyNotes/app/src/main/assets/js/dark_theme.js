javascript:(function() {

        // constants
        const stickyNotesURL = 'www.onenote.com/stickynotes';
        // theme changes based on url
        var currentUrl = document.location.host + document.location.pathname;
        // see the the text file that contains the link to the dark theme
        var themeCss = 'html{background:#202020}.n-loaderTitle{filter:invert(100%)}.n-loader{border-color:#7719aa #262626 #262626}.n-loadingAnimationContainer{background:#202020;border-color:rgba(32,32,32,.1)}.n-imageAltInputWrapper textarea,.n-imageAltWrapper,.n-lightboxOverflow-container{background-color:var(--n-overflow-menu-background)}.n-imageAltInputWrapper textarea,.n-imageAltTextCancel,.n-imageAltWrapper,.n-lightboxOverflow-container button{color:#fff}.n-imageAltWrapper{border-color:#fff}.n-imageAltInputWrapper textarea::placeholder{color:#959493}.n-imageAltTextCancel{border:1px solid #fff!important;padding:0;background-color:transparent}.n-imageAltButtonWrapper button:hover{background-color:#fff;color:#000;opacity:1!important}#O365_MainLink_NavMenu:focus,.o365cs-base .o365sx-activeButton,.o365cs-base .o365sx-activeButton:focus,.o365cs-base .o365sx-activeButton:hover{background-color:#202020!important;color:#fff!important}.ms-Dialog-button--close:hover,.o365cs-base .o365sx-neutral-lighterAlt-background,.o365sx-neutral-lighter-hover-background:hover{background:#333!important}#appLauncherTop .o365sx-neutral-dark-font,#appsModule h2.o365sx-neutral-dark-font,#flexPaneScrollRegion .o365sx-neutral-dark-font,.ms-Dialog{color:#fff}#allView .o365sx-neutral-dark-font,#appsModule div.o365sx-neutral-dark-font{filter:hue-rotate(180deg) invert(100%)}#FlexPane_MeControl .o365sx-neutral-foreground-background,.o365cs-base .o365sx-neutral-foreground-background{background:#494949}#flexPaneCloseButton,.ms-Icon--BingLogo,.ms-Icon--Calendar,.ms-Icon--MSNLogo,.ms-Icon--MicrosoftFlowLogo,.ms-Icon--OfficeLogo,.ms-Icon--People,.ms-Icon--SwayLogo32,.ms-Icon--TaskLogo,i[data-icon-name=Cancel]{filter:hue-rotate(180deg) invert(100%)}#O365fpcontainerid{border-color:#000}#FlexPane_MeControl a,#appLauncherMainView a,#appsModule button.o365sx-neutral-accent-font,#flexPaneScrollRegion a,button#allViewBackButton.o365sx-neutral-accent-font{color:#93cff7}.root-77{background-color:rgba(0,0,0,.4)}#Dialog0-title{color:#ccc}.ms-Dialog-content.innerContent-83{color:#fff}.ms-Dialog-main{background:#494949}';
        if (currentUrl != stickyNotesURL)
            themeCss = 'html{background-color:#202020!important;color:#fff}body{background-color:#000!important}#signup,a:link{color:#5ebaff}a:visted{color:#5ebaff}#signup:hover,a:hover{color:#c3bfb6}.text-title{color:#d1cec7!important}.lightbox-cover{background-color:#181818}.inner{background-color:#202020}.identityBanner{background-color:#202020}.backButton{background-color:#202020}body.cb{color:#dad8d2}input[type=email],input[type=password]{background-color:transparent!important;color:#e8e6e3!important}input[type=email],input[type=password]{border-color:#999!important;border-color:rgba(255,255,255,.6)!important}input[type=email]:hover,input[type=password]:hover{border-color:#cdcdcd!important;border-color:rgba(255,255,255,.8)!important}input[type=email]:focus,input[type=password]:focus{border-color:rgba(0,122,218,.6)!important}input[type=checkbox]{filter:invert(100%)}input::placeholder{color:#959493!important}.btn:not(.btn-primary){color:#dad8d2!important}.debug-details-banner{background-color:#464646!important}@media (max-width:600px),(max-height:366px){.footer.default{background-color:#202020!important}.footer.default div.footerNode a,.footer.default div.footerNode span{color:#fff!important}.moreOptions{filter:invert(100%)!important}}.debug-details-banner{color:#dad8d2}.dialog-outer .dialog-middle .dialog-inner{background-color:#202020}#errorBannerCloseLink,#fidoDialogTitle img,#gitHubDialogTitle img,.help-button,img.tile-img{filter:invert(100%)!important}.main{background-color:#181a1b}';

        // continue with website related theming
        var node = document.createElement('style');

        node.type = 'text/css';
        node.innerHTML = themeCss;

        document.head.appendChild(node);

        // this enables Microsoft's dark mode (normally turned on from outlook.com)
        document.body.classList.add('n-darkMode');

    })()