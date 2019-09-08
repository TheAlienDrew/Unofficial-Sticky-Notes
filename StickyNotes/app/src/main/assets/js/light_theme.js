javascript:(function() {

        var node = document.createElement('style');

        node.type = 'text/css';
        // uses only app_conversion.css
        node.innerHTML = '#Control_10_container,#Control_11_container,#O365_HeaderLeftRegion,a[href="https://account.microsoft.com/"],a[href="https://account.microsoft.com/profile/"],a[href="https://privacy.microsoft.com/en-US/privacystatement"],a[href="https://www.microsoft.com/en-US/servicesagreement/"]{display:none!important}';

        document.head.appendChild(node);

    })()