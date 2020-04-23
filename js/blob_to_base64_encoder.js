// allows for converting blobs to data uris
var reader = new window.FileReader();
const convertBlob = function(aBlobLinkElement) {
    if (aBlobLinkElement != null && aBlobLinkElement.href != null && aBlobLinkElement.href.startsWith('blob')) {
        var url = aBlobLinkElement.href;
        fetch(url)
          .then(res => res.blob()) // Gets the response and returns it as a blob
          .then(blob => {
             reader.readAsDataURL(blob);
             reader.onload = function(e) {
                 var base64data = e.target.result;
                 
                 aBlobLinkElement.href = base64data;
                 aBlobLinkElement.click();
             }
        });
    }
};