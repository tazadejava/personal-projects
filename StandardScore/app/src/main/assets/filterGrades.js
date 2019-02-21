javascript:(function(){
    var table=document.getElementsByClassName('scrollRows')[0].childNodes[0];
    var currentRow = 0;
    for(i = 0; i < table.rows.length; i++) {
        if(table.rows[i].hasAttribute('group-parent')) {
            if(currentRow == SEARCH_ROW) {
                for(c = 0; c < table.rows[i].childNodes.length; c++) {
                    if(c == SEARCH_COLUMN) {
                        var slot = table.rows[i].childNodes[c];
                        if(slot.hasChildNodes() && typeof slot.childNodes[0].click == 'function') {
                            slot.childNodes[0].click();
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            }
            currentRow++;
        }
    }
    return false;
})()
