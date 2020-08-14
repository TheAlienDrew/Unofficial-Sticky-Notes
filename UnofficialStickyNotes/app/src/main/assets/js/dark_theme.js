javascript:(function() {

        // constants
        const stickyNotesURL = 'www.onenote.com/stickynotes';
        // theme changes based on url
        var currentUrl = document.location.host + document.location.pathname;
        // see the the text file that contains the link to the dark theme
        var themeCss = 'html{background:#202020}.n-loaderTitle{filter:invert(100%)}.n-loader{border-color:#7719aa #262626 #262626}.n-loadingAnimationContainer{background:#202020;border-color:rgba(32,32,32,.1)}.n-imageAltInputWrapper textarea,.n-imageAltWrapper,.n-lightboxOverflow-container{background-color:var(--n-overflow-menu-background)}.n-imageAltInputWrapper textarea,.n-imageAltTextCancel,.n-imageAltWrapper,.n-lightboxOverflow-container button{color:#fff}.n-imageAltWrapper{border-color:#fff}.n-imageAltInputWrapper textarea::placeholder{color:#959493}.n-imageAltTextCancel{border:1px solid #fff!important;padding:0;background-color:transparent}.n-imageAltButtonWrapper button:hover{background-color:#fff;color:#000;opacity:1!important}#O365_MainLink_NavMenu:focus,.o365cs-base .o365sx-activeButton,.o365cs-base .o365sx-activeButton:focus,.o365cs-base .o365sx-activeButton:hover{background-color:#202020!important;color:#fff!important}.ms-Dialog-button--close:hover,.o365cs-base .o365sx-neutral-lighterAlt-background,.o365sx-neutral-lighter-hover-background:hover{background:#333!important}#appLauncherTop .o365sx-neutral-dark-font,#appsModule h2.o365sx-neutral-dark-font,#flexPaneScrollRegion .o365sx-neutral-dark-font,.ms-Dialog{color:#fff}#allView .o365sx-neutral-dark-font,#appsModule div.o365sx-neutral-dark-font{filter:hue-rotate(180deg) invert(100%)}#FlexPane_MeControl .o365sx-neutral-foreground-background,.o365cs-base .o365sx-neutral-foreground-background{background:#494949}#flexPaneCloseButton,.ms-Icon--BingLogo,.ms-Icon--Calendar,.ms-Icon--MSNLogo,.ms-Icon--MicrosoftFlowLogo,.ms-Icon--OfficeLogo,.ms-Icon--People,.ms-Icon--SwayLogo32,.ms-Icon--TaskLogo,i[data-icon-name=Cancel]{filter:hue-rotate(180deg) invert(100%)}#O365fpcontainerid{border-color:#000}#FlexPane_MeControl a,#appLauncherMainView a,#appsModule button.o365sx-neutral-accent-font,#flexPaneScrollRegion a,button#allViewBackButton.o365sx-neutral-accent-font{color:#93cff7}.root-77{background-color:rgba(0,0,0,.4)}#Dialog0-title{color:#ccc}.ms-Dialog-content.innerContent-83{color:#fff}.ms-Dialog-main{background:#494949}';
        if (currentUrl != stickyNotesURL)
            themeCss = ':root{--special-background-material-dark:#202020;--special-background-material-dark-sub:#2d2d2d;--normally-white:#000;--normally-white-alpha0-650:rgba(0,0,0,0.65);--normally-white-alpha0-400:rgba(0,0,0,0.4);--normally-lighter-gray95:#0a0a0a;--normally-gray95:#0d0d0d;--normally-gray95-alpha0-200:rgba(13,13,13,0.2);--normally-darker-gray95:#111;--normally-lighter-gray90:#191919;--normally-gray90:#1a1a1a;--normally-gray80:#333;--normally-darker-gray80:#3f3f3f;--normally-gray70:#4c4c4c;--normally-darker-gray70:#4d4d4d;--normally-gray60:#666;--normally-darker-gray60:#888;--normally-between-gray60-gray40:#898989;--normally-lighter-gray40:#8b8b8b;--normally-gray40:#999;--normally-darker-gray40:#aaa;--normally-gray20:#ccc;--normally-darker-gray20:#cdcdcd;--normally-gray15:#d9d9d9;--normally-gray12:#e0e0e0;--normally-darker-gray12:#e4e4e4;--normally-black:#fff;--normally-black-alpha0-800:rgba(255,255,255,0.8);--normally-black-alpha0-700:rgba(255,255,255,0.7);--normally-black-alpha0-600:rgba(255,255,255,0.6);--normally-black-alpha0-550:rgba(255,255,255,0.55);--normally-black-alpha0-400:rgba(255,255,255,0.4);--normally-black-alpha0-300:rgba(255,255,255,0.3);--normally-black-alpha0-200:rgba(255,255,255,0.2);--normally-black-alpha0-175:rgba(255,255,255,0.175);--normally-black-alpha0-150:rgba(255,255,255,0.15);--normally-black-alpha0-100:rgba(255,255,255,0.1);--normally-black-alpha0-050:rgba(255,255,255,0.05);--normally-coal-shade1:#e0dcdd;--normally-red-shade1:#ff8a80;--normally-yellow100:#f4ff81;--normally-green-shade1:#84f084;--normally-paleblue-shade1:#0f1b24;--normally-blue-shade1:#357ebd;--normally-blue-shade2:#29a0ff;--normally-blue-shade2-alpha0-100:rgba(41,160,255,0.1);--normally-blue-shade3:#47aeff;--normally-blue-shade4:#59b6ff;--normally-blue-shade5:#73c1ff;--normally-lavendar-shade1:#6a757d;--normally-lavendar-shade2:#4d72b0}mark{background:var(--normally-yellow100);color:var(--normally-black)}body{background-color:var(--special-background-material-dark);color:var(--normally-black)}a{color:var(--normally-gray80)}a:active{color:var(--normally-gray60)}.text-input,input[type=color],input[type=date],input[type=datetime-local],input[type=datetime],input[type=email],input[type=month],input[type=number],input[type=password],input[type=search],input[type=tel],input[type=text],input[type=time],input[type=url],input[type=week],textarea{background-color:var(--normally-white-alpha0-400);border-color:var(--normally-black-alpha0-400);background-color:transparent}.text-input-focus,input[type=color]:focus,input[type=date]:focus,input[type=datetime-local]:focus,input[type=datetime]:focus,input[type=email]:focus,input[type=month]:focus,input[type=number]:focus,input[type=password]:focus,input[type=search]:focus,input[type=tel]:focus,input[type=text]:focus,input[type=time]:focus,input[type=url]:focus,input[type=week]:focus,textarea:focus{background-color:var(--special-background-material-dark);border-color:var(--normally-blue-shade3)}.text-input-disabled,fieldset[disabled] input[type=color],fieldset[disabled] input[type=date],fieldset[disabled] input[type=datetime-local],fieldset[disabled] input[type=datetime],fieldset[disabled] input[type=email],fieldset[disabled] input[type=month],fieldset[disabled] input[type=number],fieldset[disabled] input[type=password],fieldset[disabled] input[type=search],fieldset[disabled] input[type=tel],fieldset[disabled] input[type=text],fieldset[disabled] input[type=time],fieldset[disabled] input[type=url],fieldset[disabled] input[type=week],fieldset[disabled] textarea,input[type=color][disabled],input[type=color][readonly],input[type=date][disabled],input[type=date][readonly],input[type=datetime-local][disabled],input[type=datetime-local][readonly],input[type=datetime][disabled],input[type=datetime][readonly],input[type=email][disabled],input[type=email][readonly],input[type=month][disabled],input[type=month][readonly],input[type=number][disabled],input[type=number][readonly],input[type=password][disabled],input[type=password][readonly],input[type=search][disabled],input[type=search][readonly],input[type=tel][disabled],input[type=tel][readonly],input[type=text][disabled],input[type=text][readonly],input[type=time][disabled],input[type=time][readonly],input[type=url][disabled],input[type=url][readonly],input[type=week][disabled],input[type=week][readonly],textarea[disabled],textarea[readonly]{background-color:var(--normally-black-alpha0-200)!important;border-color:var(--normally-gray80)!important;color:var(--normally-black-alpha0-200)!important}input::-ms-clear:active,input::-ms-reveal:active{background-color:var(--normally-blue-shade3);color:var(--normally-white)}.form-group.has-error input::-ms-clear:active,.form-group.has-error input::-ms-reveal:active,input.has-error::-ms-clear:active,input.has-error::-ms-reveal:active{background-color:var(--normally-red-shade1);color:var(--normally-white)}input[type=radio]::-ms-check{background-color:var(--special-background-material-dark);border-color:var(--normally-black-alpha0-600);color:var(--normally-black)}input[type=radio]:checked::-ms-check{border-color:var(--normally-blue-shade3);color:var(--normally-black)}input[type=radio]:active::-ms-check{border-color:var(--normally-black-alpha0-600);color:var(--normally-black-alpha0-600)}fieldset[disabled] input[type=radio]::-ms-check,input[type=radio][disabled]::-ms-check{background-color:var(--special-background-material-dark)!important;border-color:var(--normally-black-alpha0-200)!important;color:var(--normally-black-alpha0-200)!important}fieldset[disabled] input[type=radio]:checked::-ms-check,input[type=radio][disabled]:checked::-ms-check{color:var(--normally-black-alpha0-200)!important}input[type=checkbox]::-ms-check{border-color:var(--normally-black-alpha0-800);color:var(--normally-black)}input[type=checkbox]:checked::-ms-check{background-color:var(--normally-blue-shade3);border-color:var(--normally-blue-shade3)}input[type=checkbox]:active::-ms-check{background-color:var(--normally-black-alpha0-600)}fieldset[disabled] input[type=checkbox]::-ms-check,input[type=checkbox][disabled]::-ms-check{border-color:var(--normally-black-alpha0-200)!important;color:var(--normally-black-alpha0-200)!important}progress{background-color:var(--normally-gray80);color:var(--normally-blue-shade3)}input[type=range]:hover::-ms-thumb{background-color:var(--normally-gray12)}input[type=range]:disabled::-ms-thumb{background-color:var(--normally-gray80)!important}fieldset[disabled] input[type=checkbox]+span,fieldset[disabled] input[type=radio]+span,input[type=checkbox].disabled+span,input[type=checkbox][disabled]+span,input[type=radio].disabled+span,input[type=radio][disabled]+span{color:var(--normally-black-alpha0-200)}select{border:2px solid var(--normally-black-alpha0-400);color:var(--normally-black)}fieldset[disabled] select,select.disabled,select[disabled]{background-color:var(--normally-black-alpha0-200)!important;border-color:var(--normally-black-alpha0-200)!important;color:var(--normally-black-alpha0-600)!important}.btn,button,input[type=button],input[type=reset],input[type=submit]{background-color:var(--normally-black-alpha0-200);color:var(--normally-black)}.btn:focus,.btn:hover,button:focus,button:hover,input[type=button]:focus,input[type=button]:hover,input[type=reset]:focus,input[type=reset]:hover,input[type=submit]:focus,input[type=submit]:hover{border-color:var(--normally-black-alpha0-400)}.btn:hover,button:hover,input[type=button]:hover,input[type=reset]:hover,input[type=submit]:hover{cursor:pointer}.btn.btn-primary,button.btn-primary,input[type=button].btn-primary,input[type=reset].btn-primary,input[type=submit].btn-primary{background-color:var(--normally-blue-shade3);background-color:var(--normally-blue-shade3);border-color:var(--normally-blue-shade3);border-color:var(--normally-blue-shade3);color:var(--normally-white)}.btn.btn-primary:focus,.btn.btn-primary:hover,button.btn-primary:focus,button.btn-primary:hover,input[type=button].btn-primary:focus,input[type=button].btn-primary:hover,input[type=reset].btn-primary:focus,input[type=reset].btn-primary:hover,input[type=submit].btn-primary:focus,input[type=submit].btn-primary:hover{border-color:--normally-blue-shade5}.btn.disabled,.btn[disabled],button.disabled,button[disabled],fieldset[disabled] .btn,fieldset[disabled] button,fieldset[disabled] input[type=button],fieldset[disabled] input[type=reset],fieldset[disabled] input[type=submit],input[type=button].disabled,input[type=button][disabled],input[type=reset].disabled,input[type=reset][disabled],input[type=submit].disabled,input[type=submit][disabled]{background-color:var(--normally-black-alpha0-200)!important;color:var(--normally-black-alpha0-200)!important}.section .section-header{border-bottom:1px solid var(--normally-lighter-gray90)}.section .section-subtitle{color:var(--normally-between-gray60-gray40)}.dropdown-menu{background-color:var(--special-background-material-dark);border:1px solid var(--normally-gray80);border:1px solid var(--normally-black-alpha0-150)}.dropdown-menu .divider{background-color:var(--normally-gray90)}.dropdown-menu>li>a{color:var(--normally-gray20)}.dropdown-menu>li>a:focus,.dropdown-menu>li>a:hover{background-color:var(--normally-lighter-gray95);color:var(--normally-gray15)}.dropdown-menu>.active>a,.dropdown-menu>.active>a:focus,.dropdown-menu>.active>a:hover{background-color:--normally-blue-shade1;color:var(--normally-white)}.input-group-addon{background-color:var(--normally-darker-gray95);border:1px solid var(--normally-gray80);color:var(--normally-darker-gray40)}.modal .modal-dialog{border:2px solid var(--normally-blue-shade3)}.tooltip .tooltip-inner{background:var(--normally-gray95);border:1px solid var(--normally-gray80);color:var(--normally-black)}.text-secondary{color:--normally-black-alpha0-700}body.cb .modalDialogContainer{background-color:var(--special-background-material-dark);border:1px solid var(--normally-blue-shade3)}body.cb{color:var(--normally-darker-gray12)}.background,.background-image-holder{background:var(--normally-gray95)}.footer{background-color:var(--normally-black-alpha0-200)}.debug-details-banner{background-color:var(--special-background-material-dark);color:var(--normally-darker-gray12)}.debug-details-banner .debug-details-notification{color:var(--normally-green-shade1)}.text-input,input[type=color],input[type=date],input[type=datetime-local],input[type=datetime],input[type=email],input[type=month],input[type=number],input[type=password],input[type=search],input[type=tel],input[type=text],input[type=time],input[type=url],input[type=week],select,textarea{border-color:var(--normally-gray40);border-color:var(--normally-black-alpha0-600)}.text-input-hover,input[type=color]:hover,input[type=date]:hover,input[type=datetime-local]:hover,input[type=datetime]:hover,input[type=email]:hover,input[type=month]:hover,input[type=number]:hover,input[type=password]:hover,input[type=search]:hover,input[type=tel]:hover,input[type=text]:hover,input[type=time]:hover,input[type=url]:hover,input[type=week]:hover,select:hover,textarea:hover{border-color:var(--normally-darker-gray20);border-color:var(--normally-black-alpha0-800)}.btn.btn-primary-focus,.btn.btn-primary:focus,button.btn-primary:focus,input[type=button].btn-primary:focus,input[type=reset].btn-primary:focus,input[type=submit].btn-primary:focus{background-color:var(--normally-blue-shade4);outline:2px solid var(--normally-black)}.button.secondary{background-color:var(--normally-gray80);background-color:var(--normally-black-alpha0-200);color:var(--normally-black)}.button.primary{background-color:var(--normally-blue-shade3);border-color:var(--normally-blue-shade3);color:var(--normally-white)}.button.primary:focus{background-color:var(--normally-blue-shade4);outline:2px solid var(--normally-black)}.boilerplate-text.transparent-lightbox{background-color:var(--normally-gray95-alpha0-200)}.row.tile .table:focus{background:var(--normally-gray80);background:var(--normally-black-alpha0-100);outline:var(--normally-black) dashed 1px}.menu-dots>div:focus{outline:var(--normally-black) dashed 1px}.menu{background-color:var(--special-background-material-dark);border:1px solid var(--normally-lighter-gray90);border:1px solid var(--normally-black-alpha0-100)}.menu li a{background-color:var(--normally-gray95);background-color:var(--normally-black-alpha0-050)}.menu li a:focus{background-color:var(--normally-lighter-gray90);background-color:var(--normally-black-alpha0-100);outline:var(--normally-black) dashed 1px}.dialog-outer .dialog-middle .dialog-inner{background-color:var(--special-background-material-dark);border:2px var(--normally-lavendar-shade2) solid}.appInfoPopOver{background-color:var(--special-background-material-dark);border:2px solid var(--normally-lighter-gray90)}a:focus,a:visited{color:var(--normally-blue-shade3)}a:hover{color:var(--normally-gray40)}input::-ms-clear:hover,input::-ms-reveal:hover,progress::-ms-fill{color:var(--normally-blue-shade3)}.appInfoVerifiedPublisherStatus{color:var(--normally-blue-shade3)}body.cb div.placeholder{color:var(--normally-gray40)}.text-input-moz-placeholder,input[type=color]::-moz-placeholder,input[type=date]::-moz-placeholder,input[type=datetime-local]::-moz-placeholder,input[type=datetime]::-moz-placeholder,input[type=email]::-moz-placeholder,input[type=month]::-moz-placeholder,input[type=number]::-moz-placeholder,input[type=password]::-moz-placeholder,input[type=search]::-moz-placeholder,input[type=tel]::-moz-placeholder,input[type=text]::-moz-placeholder,input[type=time]::-moz-placeholder,input[type=url]::-moz-placeholder,input[type=week]::-moz-placeholder,textarea::-moz-placeholder{color:var(--normally-black-alpha0-600)}.text-input-webkit-placeholder,input[type=color]::-webkit-input-placeholder,input[type=date]::-webkit-input-placeholder,input[type=datetime-local]::-webkit-input-placeholder,input[type=datetime]::-webkit-input-placeholder,input[type=email]::-webkit-input-placeholder,input[type=month]::-webkit-input-placeholder,input[type=number]::-webkit-input-placeholder,input[type=password]::-webkit-input-placeholder,input[type=search]::-webkit-input-placeholder,input[type=tel]::-webkit-input-placeholder,input[type=text]::-webkit-input-placeholder,input[type=time]::-webkit-input-placeholder,input[type=url]::-webkit-input-placeholder,input[type=week]::-webkit-input-placeholder,textarea::-webkit-input-placeholder{color:var(--normally-black-alpha0-600)}.text-input-ms-placeholder,input[type=color]:-ms-input-placeholder,input[type=date]:-ms-input-placeholder,input[type=datetime-local]:-ms-input-placeholder,input[type=datetime]:-ms-input-placeholder,input[type=email]:-ms-input-placeholder,input[type=month]:-ms-input-placeholder,input[type=number]:-ms-input-placeholder,input[type=password]:-ms-input-placeholder,input[type=search]:-ms-input-placeholder,input[type=tel]:-ms-input-placeholder,input[type=text]:-ms-input-placeholder,input[type=time]:-ms-input-placeholder,input[type=url]:-ms-input-placeholder,input[type=week]:-ms-input-placeholder,textarea:-ms-input-placeholder{color:var(--normally-black-alpha0-600)}.form-group.has-error input[type=color],.form-group.has-error input[type=date],.form-group.has-error input[type=datetime-local],.form-group.has-error input[type=datetime],.form-group.has-error input[type=email],.form-group.has-error input[type=month],.form-group.has-error input[type=number],.form-group.has-error input[type=password],.form-group.has-error input[type=search],.form-group.has-error input[type=tel],.form-group.has-error input[type=text],.form-group.has-error input[type=time],.form-group.has-error input[type=url],.form-group.has-error input[type=week],.form-group.has-error textarea,.text-input-has-error,input[type=color].has-error,input[type=date].has-error,input[type=datetime-local].has-error,input[type=datetime].has-error,input[type=email].has-error,input[type=month].has-error,input[type=number].has-error,input[type=password].has-error,input[type=search].has-error,input[type=tel].has-error,input[type=text].has-error,input[type=time].has-error,input[type=url].has-error,input[type=week].has-error,label.focus-border-color.input-group-addon.has-error,label.input-group-addon.has-error,textarea.has-error{border-color:var(--normally-red-shade1)!important}.text-input-has-error-focus,input[type=color].has-error:focus,input[type=date].has-error:focus,input[type=datetime-local].has-error:focus,input[type=datetime].has-error:focus,input[type=email].has-error:focus,input[type=month].has-error:focus,input[type=number].has-error:focus,input[type=password].has-error:focus,input[type=search].has-error:focus,input[type=tel].has-error:focus,input[type=text].has-error:focus,input[type=time].has-error:focus,input[type=url].has-error:focus,input[type=week].has-error:focus,select.has-error:focus,textarea.has-error:focus{border-color:var(--normally-red-shade1)}.form-group.has-error input::-ms-clear:hover,.form-group.has-error input::-ms-reveal:hover,input.has-error::-ms-clear:hover,input.has-error::-ms-reveal:hover{color:var(--normally-red-shade1)}.alert-error{color:var(--normally-red-shade1)}input[type=checkbox]:hover::-ms-check,input[type=radio]:hover::-ms-check{border-color:var(--normally-black)}.person:focus{border-color:var(--normally-black)}input[type=radio]:hover:checked::-ms-check{border-color:var(--normally-blue-shade3)}.text-input-focus,input[type=color]:focus,input[type=date]:focus,input[type=datetime-local]:focus,input[type=datetime]:focus,input[type=email]:focus,input[type=month]:focus,input[type=number]:focus,input[type=password]:focus,input[type=search]:focus,input[type=tel]:focus,input[type=text]:focus,input[type=time]:focus,input[type=url]:focus,input[type=week]:focus,select:focus,textarea:focus{border-color:var(--normally-blue-shade3)}input[type=radio]:active:checked::-ms-check,select:hover{border-color:var(--normally-black-alpha0-600)}progress::-webkit-progress-value{background-color:var(--normally-blue-shade3)}progress::-moz-progress-bar{background-color:var(--normally-blue-shade3)}.progress>div{background-color:var(--normally-blue-shade3)}input[type=range]::-ms-fill-lower,input[type=range]::-ms-thumb{background-color:var(--normally-blue-shade3)}progress::-webkit-progress-bar{background-color:var(--normally-gray80)}input[type=range]:active::-ms-thumb{background-color:var(--normally-gray80)}.btn.btn-primary:active,.btn:active,button.btn-primary:active,button:active,input[type=button].btn-primary:active,input[type=button]:active,input[type=reset].btn-primary:active,input[type=reset]:active,input[type=submit].btn-primary:active,input[type=submit]:active{background-color:var(--normally-black-alpha0-400)}input[type=range]::-ms-fill-upper{background-color:var(--normally-black-alpha0-400)}label.disabled{background-color:var(--normally-black-alpha0-200)!important}input[type=range]:disabled::-ms-fill-lower,input[type=range]:disabled::-ms-fill-upper{background-color:var(--normally-black-alpha0-200)!important}.backButton,.inner,.modal-content,.new-session-popup-v2sso,.promoted-fed-cred-content,.row.tile:active .progress>div,.row.tile:focus .progress>div,.row.tile:focus:hover .progress>div,.sign-in-box,select:active,select:focus option,select[multiple]:focus{background-color:var(--special-background-material-dark)}.footer.new-background-image div.footerNode span,.section .section-title,.section.item-section .section-title,a.btn:link,a.btn:visited{color:var(--normally-black)}.footer.new-background-image div.footerNode a{color:var(--normally-black)}a.btn.btn-primary:link,a.btn.btn-primary:visited,div.footerNode a,div.footerNode span{color:var(--normally-white)}.boilerplate-text,.table>tbody>tr:nth-child(odd){background-color:var(--normally-gray95)}.dropdown-header,.dropdown-menu>.disabled>a,.dropdown-menu>.disabled>a:focus,.dropdown-menu>.disabled>a:hover{color:var(--normally-darker-gray60)}.modal-backdrop,body.cb .modalDialogOverlay{background-color:var(--normally-black)}.background-overlay,.dialog-outer{background:--normally-black-alpha0-550}.inner.transparent-lightbox,.new-session-popup-v2sso.transparent-lightbox,.promoted-fed-cred-content.transparent-lightbox,.sign-in-box.transparent-lightbox{background-color:var(--normally-white-alpha0-650)}.backButton:hover:focus,.btn,button,input[type=button],input[type=reset],input[type=submit]{background-color:var(--normally-gray80);background-color:var(--normally-black-alpha0-200)}.btn-hover,.btn:hover,.button.secondary:hover,button:hover,input[type=button]:hover,input[type=reset]:hover,input[type=submit]:hover{background-color:var(--normally-darker-gray70);background-color:var(--normally-black-alpha0-300)}.btn-focus,.btn:focus,.button.secondary:focus,button:focus,input[type=button]:focus,input[type=reset]:focus,input[type=submit]:focus{background-color:var(--normally-darker-gray70);background-color:var(--normally-black-alpha0-300);outline:2px solid var(--normally-black)}.btn.btn-primary-hover,.btn.btn-primary:hover,.button.primary:hover,button.btn-primary:hover,input[type=button].btn-primary:hover,input[type=reset].btn-primary:hover,input[type=submit].btn-primary:hover{background-color:var(--normally-blue-shade4)}.backButton:focus,.backButton:hover,.menu li a:hover,.row.tile:not(.no-pick):hover{background-color:var(--normally-lighter-gray90);background-color:var(--normally-black-alpha0-100)}.backButton:active,.menu li a:active,.row.tile:not(.no-pick):active{background-color:var(--normally-gray70);background-color:var(--normally-black-alpha0-300)}@media (max-width:600px),(max-height:366px){.footer.default{background:var(--special-background-material-dark)}.footer.default div.footerNode a,.footer.default div.footerNode span{color:var(--normally-lighter-gray40)!important}.debug-details-banner{background-color:var(--special-background-material-dark-sub)}.appInfoPopOver{background-color:var(--special-background-material-dark)}.footerSignout,.footerSignout>a{color:var(--normally-gray15)!important}.inner.app,.promoted-fed-cred-content,.promoted-fed-cred-content.app,.sign-in-box.app{border:1px solid var(--normally-lavendar-shade1);border:1px solid var(--normally-black-alpha0-400)}}.stack-trace{color:var(--normally-black)}.stack-trace fieldset{border-top:1px solid var(--normally-white);color:var(--normally-black)}.stack-trace hr{border-top:solid 1px var(--normally-white)}.input.text-box{background-color:var(--normally-white-alpha0-400);border-color:var(--normally-black-alpha0-400);border-color:var(--normally-gray40);border-color:var(--normally-black-alpha0-600);background-color:transparent}.input.text-box:focus{background-color:var(--special-background-material-dark);border-color:var(--normally-blue-shade3)}.input.text-box:hover{border-color:var(--normally-darker-gray20);border-color:var(--normally-black-alpha0-800)}[disabled].input.text-box,[readonly].input.text-box,fieldset[disabled] .input.text-box{background-color:var(--normally-black-alpha0-200)!important;border-color:var(--normally-gray80)!important;color:var(--normally-black-alpha0-200)!important}select:focus{background:var(--normally-darker-gray95)}.error{color:var(--normally-red-shade1)}.dropdown-toggle.membernamePrefillSelect{border-color:var(--normally-gray40)}.dropdown-toggle.membernamePrefillSelect:active{border:1px solid var(--normally-blue-shade2)}.phoneCountryCode{border-color:var(--normally-gray40);border-color:var(--normally-black-alpha0-600)}.phoneCountryCode.hasFocus{background-color:var(--normally-darker-gray95);border:1px solid var(--normally-darker-gray95);border-bottom-color:var(--normally-blue-shade3)}.lightbox-cover{background-color:var(--special-background-material-dark)}.cc-banner{background:var(--normally-gray95);color:var(--normally-coal-shade1)}.cc-banner .cc-link{color:var(--normally-blue-shade3)}.cc-banner .cc-link:focus{background:var(--normally-paleblue-shade1);background:content-box var(--normally-blue-shade2-alpha0-100)}.env-banner{background:var(--normally-blue-shade3);color:var(--normally-white)}.env-banner-link:active,.env-banner-link:hover,.env-banner-link:link,.env-banner-link:link:active,.env-banner-link:link:hover,.env-banner-link:visited,.env-banner-link:visited:active,.env-banner-link:visited:hover{color:var(--normally-white)}fieldset{border:1px solid var(--normally-darker-gray80)}.dropdown-menu{-webkit-box-shadow:0 6px 12px var(--normally-black-alpha0-175);box-shadow:0 6px 12px var(--normally-black-alpha0-175)}.input.text-box::-moz-placeholder{color:var(--normally-black-alpha0-600)}.input.text-box::-webkit-input-placeholder{color:var(--normally-black-alpha0-600)}.input.text-box:-ms-input-placeholder{color:var(--normally-black-alpha0-600)}.dropdown-toggle.membernamePrefillSelect.has-error,.input.text-box.has-error,.phoneCountryCode.has-error{border-color:var(--normally-red-shade1)}.input.text-box.has-error:focus{border-color:var(--normally-red-shade1)}.dropdown-toggle.membernamePrefillSelect.has-error:hover{border-color:var(--normally-red-shade1)}.app-name,.text-title,.title{color:var(--normally-darker-gray12)}.dropdown-toggle.membernamePrefillSelect:focus,.dropdown-toggle.membernamePrefillSelect:hover,.open .dropdown-toggle.membernamePrefillSelect{background-color:var(--normally-darker-gray95)!important;border:1px solid var(--normally-blue-shade2)}.appInfoPopOver,.new-session-popup-v2sso{-moz-box-shadow:0 2px 6px var(--normally-black-alpha0-200);-webkit-box-shadow:0 2px 6px var(--normally-black-alpha0-200);box-shadow:0 2px 6px var(--normally-black-alpha0-200)}@media (max-width:600px),(max-height:366px){.appInfoPopOver{-moz-box-shadow:0 2px 6px var(--normally-black-alpha0-200);-webkit-box-shadow:0 2px 6px var(--normally-black-alpha0-200);box-shadow:0 2px 6px var(--normally-black-alpha0-200)}}.template-header{box-shadow:0 2px 6px var(--normally-black-alpha0-200)}#debugDetailsCopyMessage img,#errorBannerCloseLink img,#fidoDialogTitle img,#gitHubDialogTitle img,.backButton img,.help-button img,img.tile-img,img[src="https://aadcdn.msftauth.net/ests/2.1/content/images/ellipsis_635a63d500a92a0b8497cdc58d0f66b1.svg"].desktopMode,input[type=checkbox]{filter:invert(100%) hue-rotate(180deg)}div[style="background: rgb(242, 242, 242);"].background-image-holder{filter:brightness(10%)}a:focus,a:visited,div:not(#footerLinks):not(.footerNode)>a:link{color:var(--normally-blue-shade3)}.footerNode>a{color:var(--normally-black)!important}.identityBanner{background-color:var(--special-background-material-dark)}#CredentialsInputPane fieldset{border:none!important}';

        // continue with website related theming
        var node = document.createElement('style');

        node.type = 'text/css';
        node.innerHTML = themeCss;

        document.head.appendChild(node);

        // this enables Microsoft's dark mode (normally turned on from outlook.com)
        document.body.classList.add('n-darkMode');

    })()