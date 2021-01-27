javascript:(function() {

        // constants
        const stickyNotesURL = 'www.onenote.com/stickynotes'
        const signUpURL = 'https://signup.live.com/';
        const loginAuthURL = 'login.microsoftonline.com/common/oauth2/authorize';
        const neverCached = 'chromewebdata'
        const slowDelay = 1000;
        const fastDelay = 100;
        // constant selectors
        const mainLinkNavMenuSelector = '#O365_MainLink_NavMenu';
        const appNameSpanSelector = '#O365_AppName > span';
        const sidePaneContentSelector = '.n-side-pane-content';
        const noteEditContainerSelector = '.n-detailViewContainer';
        const noteCloseButtonSelector = '.n-closeDetailViewButton';
        const msListPageSelector = '.ms-List-page';
        // constant classes
        const msListCellClass = 'ms-List-cell';
        const noteIdAttribute = 'data-flip-id';
        // constant database info
        const DATABASE = 'notes.sdk';
        const STORE = 'notes';
        // theme changes based on url
        var currentURL = document.location.host + document.location.pathname;
        var themeCss = '*{-webkit-tap-highlight-color:transparent}:focus{outline:0!important}html{position:fixed;height:100%;width:100%}#topLevelRegion{pointer-events:none}#HeaderButtonRegion,#O365_HeaderLeftRegion,#O365_HeaderRightRegion,#centerRegion,.n-newNoteButtonContainer{display:none}'; // see app_conversion.css
        var fadeInCss = '.fade-in-Unofficial{animation:fadeInUnofficial ease .2s;-webkit-animation:fadeInUnofficial ease .2s}@keyframes fadeInUnofficial{0%{opacity:0}100%{opacity:1}}@-webkit-keyframes fadeInUnofficial{0%{opacity:0}100%{opacity:1}}';
        themeCss += fadeInCss;

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
                    //note.remoteId = noteDate.remoteId;
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

        // wait for loading animation to appear then disappear before making webView visible (if on sticky notes page)
        var editingActive = 0;
        var theScrollTop = 0;
        var noteListContainer = null;
        var noteListCell = null;
        if (currentURL == stickyNotesURL) {
            // hide the page to force it into phone UI
            document.body.style.display = 'none';
            
            var checkLoading = setInterval(function() {
                var mainLinkNavMenu = document.querySelector(mainLinkNavMenuSelector);
                var sidePaneContent = document.querySelector(sidePaneContentSelector);

                // sidePaneContent.lastElementChild.innerText is the best way to determine that notes are loaded
                // trying to see if the loading animation appeared and disappeared won't always work (can be too fast)
                // TODO: Might need to come back to fix this yet again if different locales change the loading notes text
                if(elementExists(mainLinkNavMenu) && elementExists(sidePaneContent) && sidePaneContent.lastElementChild.innerText != 'Loading your notes...') {
                    clearInterval(checkLoading);

                    // show the page after loaded (after hidden to force Phone mode)
                    document.body.style.display = '';
                    // change link text for app name to simple instructions
                    let appNameSpan = document.querySelector(appNameSpanSelector);
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
        } else {
            var checkLoading = setInterval(function() {
                if (typeof(document.activeElement) != 'undefined' && document.activeElement != null) {
                    clearInterval(checkLoading);

                    // the create account link is broken, and needs to be changed
                    if (currentURL == loginAuthURL) {
                        var signUpSelector = 'signup';
                        var signUp = document.getElementById(signUpSelector);

                        var newSignUp = signUp.cloneNode(true);
                        newSignUp.href = signUpURL;

                        signUp.remove();
                        document.getElementsByClassName('form-group')[1].appendChild(newSignUp);
                    }

                    // let login pages not fade in (just appear)
                    if (document.location.host != neverCached) {
                        window.Android.webViewSetVisible(true);
                    }
                }
            }, fastDelay);
        }

        // continue with website related fixes
        var node = document.createElement('style');

        node.type = 'text/css';
        node.innerHTML = themeCss;

        document.head.appendChild(node);

    })()