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
        console.log("convert_stickyNotesPicker.js started");

        // constants
        const neverCached = 'chromewebdata'
        const slowDelay = 1000;
        const fastDelay = 100;
        // constant selectors
        const notesLoadingSelector = '.n-loadingAnimationContainer';
        const mainLinkNavMenuSelector = '#O365_MainLink_NavMenu';
        const appNameSpanSelector = '#O365_AppName > span';
        const sidePaneContentSelector = '.n-side-pane-content';
        const noteEditContainerSelector = '.n-detailViewContainer';
        const noteCloseButtonSelector = '.n-closeDetailViewButton';
        const msListPageSelector = '.ms-List-page';
        // constant classes
        const msListCellClass = 'ms-List-cell';
        const noteIdAttribute = 'data-flip-id';
        // notes dark mode constants
        const darkModeClassName = 'n-darkMode';
        const uiContainerSelector = '#n-ui-container';
        // loading gif constants
        const loadingGifDark = 'https://npwuscdn-onenote.azureedge.net/ondcnotesintegration/img/loading-dark.gif';
        const loadingGifSelector = '#n-side-pane > div.n-side-pane-content > div > div > div > img';
        // constant database info
        const DATABASE = 'notes.sdk';
        const STORE = 'notes';
        // theme constants
        const themeBaseCss = '*{-webkit-tap-highlight-color:transparent}:focus{outline:0!important}html{position:fixed;height:100%;width:100%}'; // see app_conversion.css
        const fadeInCss = '.fade-in-Unofficial{animation:fadeInUnofficial ease .2s;-webkit-animation:fadeInUnofficial ease .2s}@keyframes fadeInUnofficial{0%{opacity:0}100%{opacity:1}}@-webkit-keyframes fadeInUnofficial{0%{opacity:0}100%{opacity:1}}';
        const darkModeCss = 'html{background:#202020}.n-loaderTitle{filter:invert(100%)}.n-loader{border-color:#7719aa #262626 #262626}.n-loadingAnimationContainer{background:#202020;border-color:rgba(32,32,32,.1)}.n-imageAltInputWrapper textarea,.n-imageAltWrapper,.n-lightboxOverflow-container{background-color:var(--n-overflow-menu-background)}.n-imageAltInputWrapper textarea,.n-imageAltTextCancel,.n-imageAltWrapper,.n-lightboxOverflow-container button{color:#fff}.n-imageAltWrapper{border-color:#fff}.n-imageAltInputWrapper textarea::placeholder{color:#959493}.n-imageAltTextCancel{border:1px solid #fff!important;padding:0;background-color:transparent}.n-imageAltButtonWrapper button:hover{background-color:#fff;color:#000;opacity:1!important}#O365_MainLink_NavMenu:focus,.o365cs-base .o365sx-activeButton,.o365cs-base .o365sx-activeButton:focus,.o365cs-base .o365sx-activeButton:hover{background-color:#202020!important;color:#fff!important}.ms-Dialog-button--close:hover,.o365cs-base .o365sx-neutral-lighterAlt-background,.o365sx-neutral-lighter-hover-background:hover{background:#333!important}#appLauncherTop .o365sx-neutral-dark-font,#appsModule h2.o365sx-neutral-dark-font,#flexPaneScrollRegion .o365sx-neutral-dark-font,.ms-Dialog{color:#fff}#allView .o365sx-neutral-dark-font,#appsModule div.o365sx-neutral-dark-font{filter:hue-rotate(180deg) invert(100%)}#FlexPane_MeControl .o365sx-neutral-foreground-background,#appLauncher,.o365cs-base .o365sx-neutral-foreground-background{background:#494949!important}#flexPaneCloseButton,.ms-Icon--BingLogo,.ms-Icon--MSNLogo,.ms-Icon--People,.ms-Icon--PrivacyLogo,.ms-Icon--RewardsLogo,i[data-icon-name=Cancel]{filter:hue-rotate(180deg) invert(100%)}#O365fpcontainerid{border-color:#000}#FlexPane_MeControl a,#appLauncherMainView a,#appsModule button.o365sx-neutral-accent-font,#flexPaneScrollRegion a,button#allViewBackButton.o365sx-neutral-accent-font{color:#93cff7}.ms-Dialog>:not(.ms-Overlay--dark) .ms-Overlay{background-color:rgba(0,0,0,.4)}.ms-Dialog>.ms-Overlay--dark{background-color:rgba(255,255,255,.4)}.ms-Dialog-title{color:#ccc}.ms-Dialog-content [class^=innerContent-]{color:#fff}:not(.n-lightboxModal)>.ms-Dialog-main{background:#494949}';
        const widgetPickCss = '#topLevelRegion{pointer-events:none}#HeaderButtonRegion,#O365_HeaderLeftRegion,#O365_HeaderRightRegion,#centerRegion,.n-newNoteButtonContainer{display:none}'; // see app_conversion.css

        // function for elements
        var elementExists = function(element) {
            return (typeof(element) != 'undefined' && element != null);
        };
        
        // function to get note data from database by id
        var getNoteById = function(id) {
            return new Promise (function(resolve) {
                let open = indexedDB.open(DATABASE);
                open.onerror = function(event) {
                    console.error('getNoteById error: ' + open.errorCode);
                    return resolve(false);
                };
                open.onsuccess = function(event) {
                    let db = open.result;
                    let tx = db.transaction(STORE, 'readonly');
                    let store = tx.objectStore(STORE);
                    let get = store.get(id);
                    get.onerror = function(event) {
                        console.error('getNoteById error: ' + get.errorCode);
                        return resolve(false);
                    };
                    get.onsuccess = function(event) {
                        return resolve(get.result);
                    };
                };
            });
        };
        
        // check for links with linkify, and create html anchor tags
        // if using linkPositionsArray, please make sure it is an empty array
        // - the following will be the output format for each element
        /* {
         *   value             // the link that got linkified
         *   oldIndex          // from in the originalContent
         *   addedLength       // the length of characters added on to value to create link
         *   afterAddedLength // the length of characters added after the value to create link
         *   beforeAddedLength // the length of characters added before the value to create link
         * }
         */
        var linkUp = function(originalContent, linkPositionsArray) {
            let newContent = originalContent;
            let links = linkify.find(newContent);
            // link up
            let original_index = 0;
            let new_index = 0;
            for (let i = 0; i < links.length; i++) {
                let linkified = links[i];
                let link = linkified.value;

                // create the `a` link
                let linkNode = document.createElement('a');
                linkNode.href = linkified.href;
                linkNode.innerText = link;
                let linkifiedHTML = linkNode.outerHTML;
                let oldIndex = originalContent.indexOf(link, original_index);
                let newIndex = newContent.indexOf(link, new_index);
                let addedLength = linkifiedHTML.length - link.length;
                let afterAddedLength = linkifiedHTML.length - linkifiedHTML.lastIndexOf('</');
                let beforeAddedLength = addedLength - afterAddedLength;

                // save positional information
                if (linkPositionsArray) {
                    let linkInfo = {};
                    linkInfo.value = link;
                    linkInfo.oldIndex = oldIndex;
                    linkInfo.addedLength = addedLength;
                    linkInfo.afterAddedLength = afterAddedLength;
                    linkInfo.beforeAddedLength = beforeAddedLength;
                    linkPositionsArray.push(linkInfo);
                }
                // replace occurrence at location, and set new indices
                newContent = newContent.slice(0, newIndex)
                           + linkifiedHTML
                           + newContent.slice(newIndex + link.length);
                original_index = oldIndex + link.length;
                new_index = newIndex + linkifiedHTML.length;
            }

            return newContent;
        };

        // parse note data into html string
        var parseBlockInsideHTML = function(noteContentBlock) {
            // for when I need to include RTL support
            //let noteContentStyle = noteContentBlock ? noteContentBlock.blockStyle : null;
            //let noteContentTextDirection = noteContentStyle.textDirection || 'ltf';

            // parse the actual text with tags only TextView can use (style not supported)

            // check if block is empty, and then try setting text
            let newNoteContent = '';
            if (noteContentBlock && noteContentBlock.content && noteContentBlock.content.text) {
                // start processing string like an array
                let noteText = noteContentBlock.content.text;
                let inlineStyles = noteContentBlock.content.inlineStyles;

                // check if we are dealing with plain or formatted text
                if (inlineStyles.length > 0) { // not plain text
                    /* inlineStyles is an array of objects with the following members
                     * {
                     *   bold:          [boolean]
                     *   italic:        [boolean]
                     *   length:        [integer]
                     *   offset:        [integer]
                     *   strikethrough: [boolean]
                     *   underlined:    [boolean]
                     * }
                     */
                    // need to check for links first to address styling
                    let linkifiedInfo = [];
                    newNoteContent = linkUp(noteText, linkifiedInfo);

                    // copy original array of inlineStyles to alter and match new indicies
                    let newInlineStyles = JSON.parse(JSON.stringify(inlineStyles));
                    // start manipulating the styles
                    let extenderOffset = 0; // this is the amount that needs to increase subsequent offsets
                    let inlineStylesIndex = 0;
                    for (let i = 0; i < linkifiedInfo.length; i++) {
                        // current link
                        let linkedInfo = linkifiedInfo[i];
                        let linkStartIndex = linkedInfo.oldIndex;
                        let linkEndIndex = linkStartIndex + linkedInfo.value.length;
                        // peek link (there may not be another link after)
                        let peekLinkedInfo = linkifiedInfo[i+1];
                        let peekLinkStartIndex = peekLinkedInfo ? peekLinkedInfo.oldIndex : null;

                        // start checking styles against links
                        let style = inlineStyles[inlineStylesIndex];
                        let styleStartIndex = style ? style.offset : null;
                        let started = false;
                        let ended = false;
                        while ((inlineStylesIndex < inlineStyles.length) && (peekLinkedInfo ? (styleStartIndex < peekLinkStartIndex) : true)) {
                            // if style is before the link (skip)
                            /* if (styleStartIndex < linkStartIndex); */

                            // if style is inside the link
                            if (!started && (styleStartIndex >= linkStartIndex) && (peekLinkedInfo ? (styleStartIndex < peekLinkStartIndex) : true)) {
                                extenderOffset += linkedInfo.beforeAddedLength;
                                started = true;
                            }
                            // if style is after link, but not at/after the start of the next link
                            if (!ended && (styleStartIndex >= linkEndIndex) && (peekLinkedInfo ? (styleStartIndex < peekLinkStartIndex) : true)) {
                                extenderOffset += linkedInfo.afterAddedLength;
                                ended = true;
                            }

                            newInlineStyles[inlineStylesIndex].offset += extenderOffset;

                            inlineStylesIndex++;
                            style = inlineStyles[inlineStylesIndex];
                            if (style) styleStartIndex = style.offset;
                        }

                        // if we didn't find any styles in or after the link, extend before the next link starts
                        extenderOffset += ((started ? 0 : linkedInfo.beforeAddedLength) + (ended ? 0 : linkedInfo.afterAddedLength));
                    }
                    // now we can use the newInlineStyles on the lineContent

                    // start from end of newNoteContent to add on styles
                    for (let j = (newInlineStyles.length-1); j >= 0; j--) {
                        let newInlineStyle = newInlineStyles[j];
                        let stringToStyle = newNoteContent.substr(newInlineStyle.offset, newInlineStyle.length);

                        // compose the styled span node
                        let styleHtmlStart = '';
                        let styleHtmlEnd = '';
                        if (newInlineStyle.bold) {
                            styleHtmlStart = styleHtmlStart + '<b>';
                            styleHtmlEnd = '</b>' + styleHtmlEnd;
                        }
                        if (newInlineStyle.italic) {
                            styleHtmlStart = styleHtmlStart + '<i>';
                            styleHtmlEnd = '</i>' + styleHtmlEnd;
                        }
                        if (newInlineStyle.underlined) {
                            styleHtmlStart = styleHtmlStart + '<u>';
                            styleHtmlEnd = '</u>' + styleHtmlEnd;
                        }
                        if (newInlineStyle.strikethrough) {
                            styleHtmlStart = styleHtmlStart + '<s>';
                            styleHtmlEnd = '</s>' + styleHtmlEnd;
                        }
                        stringToStyle = styleHtmlStart + stringToStyle + styleHtmlEnd;

                        // replace original string with styled version
                        newNoteContent = newNoteContent.slice(0, newInlineStyle.offset)
                                        + stringToStyle
                                        + newNoteContent.slice(newInlineStyle.offset + newInlineStyle.length);
                    }
                } else newNoteContent = linkUp(noteText); // just plain text, maybe with some links
            } else newNoteContent = '<br>'; // needed or else empty lines aren't shown

            return newNoteContent;
        };

        // takes the selected note, and converts it to a format useable for a widget
        var setNoteWidgetFunction = function(element) {
            element.onclick = async function(event) {
                event.preventDefault();
                event.stopImmediatePropagation();
                let noteId = element.getAttribute(noteIdAttribute);
                await getNoteById(noteId).then(noteData => {
                    let blocks = noteData.document.content;

                    // get basic note data
                    let note = {};
                    note.color = noteData.color;
                    note.content = [];
                    for (let i = 0; i < blocks.length; i++) {
                        let noteBlock = blocks[i];
                        let block = {};
                        block.blockStyle = JSON.parse(JSON.stringify(noteBlock.blockStyle));
                        block.content = JSON.parse(JSON.stringify(noteBlock.content));
                        //block.id = noteBlock.id;
                        //block.type = noteBlock.type;
                        note.content.push(block);
                    }
                    // remove empty ending lines from content
                    while (note.content[note.content.length-1].content.text == '') {
                        note.content.splice(note.content.length-1, 1);
                    }
                    note.modifiedAt = noteData.documentModifiedAt;
                    note.id = noteData.id;
                    //note.remoteId = noteDate.remoteId; // required to get media online
                    //note.media = ...;

                    // if I ever need media (images), I can just use the following
                    /*
                    let ImplicitAuth = JSON.parse(localStorage.getItem("ImplicitAuth"));
                    let UserInfo = JSON.parse(localStorage.getItem("UserInfo"));
                    let bearer = ImplicitAuth.cachedObjects["ImplicitAuth-https://substrate.office.com/Notes-Internal.ReadWrite"].value.accessToken;
                    let userCID = UserInfo.cachedObjects.UserInfo.value.userCid;
                    await fetch("https://substrate.office.com/NotesClient/api/v1.1/me/notes/"+noteRemoteId+"/media/"+imageRemoteId, {
                        "headers": {
                            "accept": "application/json",
                            "authorization": "Bearer "+bearer,
                            "blob": "true",
                            "content-type": "application/json",
                            "request-priority": "foreground",
                            "x-anchormailbox": "CID:"+userCID
                        },
                        "method": "GET",
                        "mode": "cors"
                    }).then(response => response.blob());
                    */

                    // node to contain all data
                    let widgetContent = document.createElement('div');
                    // parse the note content into html
                    let noteContentHTML = '';
                    for (let i = 0; i < note.content.length; i++) {
                        let currentBlock = note.content[i];
                        let currentStyle = currentBlock ? currentBlock.blockStyle : null;
                        let isBullet = currentStyle ? currentStyle.bullet : false;

                        // conditionally create bullet list
                        if (isBullet) {
                            let bulletParaNode = document.createElement('p');
                            let bulletListNode = document.createElement('ul');
                            // add bullets to list
                            while (isBullet) {
                                let bulletNode = document.createElement('li');
                                bulletNode.innerHTML = parseBlockInsideHTML(currentBlock);
                                bulletListNode.appendChild(bulletNode);
                                i++;
                                currentBlock = note.content[i];
                                currentStyle = currentBlock ? currentBlock.blockStyle : null;
                                isBullet = currentStyle ? currentStyle.bullet : false;
                            }
                            bulletParaNode.appendChild(bulletListNode);
                            noteContentHTML += bulletParaNode.outerHTML;
                        }
                        // only gets here when element isn't a bullet
                        let lineParaNode = document.createElement('p');
                        lineParaNode.innerHTML = parseBlockInsideHTML(currentBlock);
                        noteContentHTML += lineParaNode.outerHTML;
                    }
                    widgetContent.innerHTML = noteContentHTML;

                    // create object containing data for Android to parse
                    let widgetNoteData = {};
                    widgetNoteData.id = note.id;
                    widgetNoteData.color = note.color;
                    widgetNoteData.timestamp = (new Date(note.modifiedAt)).toLocaleDateString();
                    widgetNoteData.content = widgetContent.innerHTML;
                    window.Android.createNoteWidget(JSON.stringify(widgetNoteData));
                });
            };
        };

        // variables
        var isDarkMode = window.Android.isDarkMode();
        var themeCss = themeBaseCss + widgetPickCss + fadeInCss;
        // add onto the theme in case of dark mode
        if (isDarkMode) themeCss += darkModeCss;

        // wait for loading animation to appear then disappear before making webView visible
        //   (if on sticky notes page)
        var editingActive = 0;
        var theScrollTop = 0;
        var noteListContainer = null;
        var noteListCell = null;
        document.body.style.display = 'none'; // hide the page to force it into phone UI
        var checkLoading = setInterval(function() {
            var mainLinkNavMenu = document.querySelector(mainLinkNavMenuSelector);
            var sidePaneContent = document.querySelector(sidePaneContentSelector);
            var appNameSpan = document.querySelector(appNameSpanSelector);

            // sidePaneContent.lastElementChild.innerText != "Loading your notes..." was a good way
            //   to test, but not when locales change
            // trying to see if the loading animation appeared and disappeared won't always work
            //   (can be too fast)
            // so, just need to test if the side pane has at least loaded, and then query select for
            //  the loading element
            if(elementExists(mainLinkNavMenu)
               && elementExists(sidePaneContent)
               && !elementExists(sidePaneContent.querySelector(notesLoadingSelector))
               && elementExists(appNameSpan) ) {
                clearInterval(checkLoading);

                // show the page after loaded (after hidden to force Phone mode)
                document.body.style.display = '';
                // change link text for app name to simple instructions
                appNameSpan.innerText = appNameSpan.innerText + ' > Select note for widget';

                let listLoaded = false;
                // need to watch for everytime the list gets loaded in to assign onclick function
                setInterval(function() {
                    let msListPage = document.querySelector(msListPageSelector);
                    if (msListPage && !listLoaded) {
                        listLoaded = true;
                        // assign onclick function to existing notes
                        for (let i = 0; i < msListPage.childElementCount; i++) {
                            let child = msListPage.children[i];
                            setNoteWidgetFunction(child);
                        }

                        // listen to when new notes get created
                        let obs = new MutationObserver(function(mutations, observer) {
                            // look through all mutations that just occured
                            for(var i=0; i < mutations.length; ++i) {
                                // look through all added nodes of this mutation
                                for(var j=0; j < mutations[i].addedNodes.length; ++j) {
                                    // check current added node
                                    let node = mutations[i].addedNodes[j];
                                    if(node.classList.contains(msListCellClass)) {
                                        setNoteWidgetFunction(node);
                                    }
                                }
                            }
                        });
                        obs.observe(msListPage, { childList: true });
                    } else if (!msListPage) listLoaded = false;
                }, fastDelay);

                // if page was loaded while hidden, note will show, which needs to be closed out
                let noteClose = setInterval(function() {
                    let noteCloseButton = document.querySelector(noteCloseButtonSelector);
                    if (noteCloseButton) {
                        clearInterval(noteClose);
                        noteCloseButton.click();
                    }
                }, fastDelay);

                // set webView to visible after a small delay (so the loading gif on page disappears a bit more)
                document.body.classList.add('fade-in-Unofficial'); // animate opacity as fade before showing page
                setTimeout(function() {
                    window.Android.webViewSetVisible(true);
                }, slowDelay);
            }
        }, fastDelay);

        // continue with website related fixes
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