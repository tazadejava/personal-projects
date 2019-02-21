javascript: (function() {
    var periodRows = document.getElementsByClassName("scrollWrap jspScrollable")[0].childNodes[0].childNodes[0].childNodes[1].childNodes[0].childNodes[0].childNodes;
    var periodNumberRows = document.getElementsByClassName("fixedRows")[0].childNodes[0].childNodes[0].childNodes;

    for(i = 0; i < periodRows.length; i++) {
        var periodClasses = periodRows[i].childNodes;

        var periodNumber = periodNumberRows[i].childNodes[0].childNodes[0].innerText.replace("Period ", "");

        for(j = 0; j < periodClasses.length; j++) {
            var periodData = periodClasses[j].childNodes[1].childNodes[1].childNodes[0].childNodes[0].childNodes[1].childNodes[0].childNodes;
            var className = periodData[0].innerText;
            var teacherName = periodData[2].innerText;
            var roomName = "";
            if(periodData.length >= 6) {
                roomName = periodData[5].data.replace("   ", "");
            }

            scrape.savePeriodTerm("Term " + (j + 1), periodNumber, className, teacherName, roomName);
        }
    }

    scrape.completeScheduleScrape();
})()
