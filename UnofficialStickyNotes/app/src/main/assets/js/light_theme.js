javascript:(function() {

        var node = document.createElement('style');

        node.type = 'text/css';
        // uses only app_conversion.css for mobile view
        node.innerHTML = '*{-webkit-tap-highlight-color:transparent}:focus{outline:0}html{position:fixed;height:100%;width:100%}#O365_HeaderLeftRegion{display:none}';

        document.head.appendChild(node);

    })()