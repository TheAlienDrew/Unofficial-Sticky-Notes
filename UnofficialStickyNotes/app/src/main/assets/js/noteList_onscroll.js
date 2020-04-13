javascript:(function() {

        setTimeout(function() {
            var titleBarText = document.getElementById("O365_AppName");
            titleBarText.setAttribute("href", "https://www.onenote.com/stickynotes#");
            document.querySelector('.n-noteList-Container').setAttribute("onscroll", "var noteList = document.querySelector('.n-noteList-Container'); window.CallToAnAndroidFunction.setSwipeRefresher(noteList.scrollTop);");
        }, 1100);

    })()