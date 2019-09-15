javascript:(function() {

        var node = document.createElement('style');

        node.type = 'text/css';
        // uses only app_conversion.css
        node.innerHTML = '*{-webkit-tap-highlight-color:transparent}*:focus{outline:none}html{position:fixed;height:100%;width:100%}#Control_10_container,#Control_11_container,#O365_HeaderLeftRegion,a[href="https://account.microsoft.com/"],a[href="https://account.microsoft.com/profile/"],a[href="https://privacy.microsoft.com/en-US/privacystatement"],a[href="https://www.microsoft.com/en-US/servicesagreement/"]{display:none!important}';

        document.head.appendChild(node);

    })()