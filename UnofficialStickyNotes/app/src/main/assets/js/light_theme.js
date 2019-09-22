javascript:(function() {

        var node = document.createElement('style');

        node.type = 'text/css';
        // uses only app_conversion.css
        node.innerHTML = '*{-webkit-tap-highlight-color:transparent}:focus{outline:0}html{position:fixed;height:100%;width:100%}';

        document.head.appendChild(node);

    })()