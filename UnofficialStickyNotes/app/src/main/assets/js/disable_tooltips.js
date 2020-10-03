javascript:(function() {

        var disableCustomToolTips = document.createElement('style');
        disableCustomToolTips.type = 'text/css';
        disableCustomToolTips.innerHTML = '.ms-Tooltip{display:none!important}';
        document.head.appendChild(disableCustomToolTips);

        setInterval(function() {
            var elementsWithTitle = document.querySelectorAll('[title]');
            if (typeof(elementsWithTitle) != 'undefined' && elementsWithTitle != null && elementsWithTitle.length > 0) {
                 elementsWithTitle.forEach((element) => {
                     element.title = ' ';
                 });
            }
        }, 100);

    })()