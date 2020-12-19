javascript:(function(){
    var table = document.getElementsByClassName('scrollRows')[0].childNodes[0];
    var row = SEARCH_ROW;

    var currentRow = 0;
    var finalRowIndex = 0;
    for(var i = 0; i < table.rows.length; i += 1) {
        if(table.rows[i].style.display === '') {
            if(row == currentRow) {
                finalRowIndex = i;
                break;
            }

            currentRow += 1;
        }
    }

    if(table.rows[finalRowIndex].hasAttribute('group-parent')) {
        var slot = table.rows[finalRowIndex].childNodes[SEARCH_COLUMN];
        if((typeof slot !== 'undefined') && slot.hasChildNodes() && (typeof slot.childNodes[0].childNodes[0].click) === 'function') {
            slot.childNodes[0].childNodes[0].click();
            return finalRowIndex;
        } else {
            return -1;
        }
    }
    return false;
})()
