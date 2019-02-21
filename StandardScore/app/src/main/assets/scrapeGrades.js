javascript: (function() {
	var table = document.getElementsByClassName('shrinkMe')[0];
	var classID;
	for (i = 0; i < table.rows.length; i++) {
		if (i == 0) {
			var heading = table.rows[i].childNodes[0];
			var className, period, teacher, termDates, term, letterGrade, percentage;
			var comment = '';

			if(document.getElementById('showCommentInfo') != null) {
			    var commentsTr = document.getElementById('grid_commentInfoGrid').childNodes[0].childNodes;
			    var length = heading.childNodes.length;
			    for (r = 1; r < length; r += 2) {
			        var child = commentsTr[r];
			        if(child == null) {
			            break;
			        }
			        comment += child.childNodes[0].innerText;
			        if(length - 2 != r) {
			            comment += ';';
			        }
			    }
			}

			for (r = 0; r < heading.childNodes.length; r++) {
				var child = heading.childNodes[r];
				switch (r) {
					case 0:
						className = child.childNodes[0].childNodes[0].childNodes[0].innerText;
						period = child.childNodes[0].childNodes[0].childNodes[2].innerText;
						teacher = child.childNodes[0].childNodes[0].childNodes[4].innerText;
						break;
					case 2:
						var innerTableRows = child.childNodes[1].childNodes[0].rows;
						term = innerTableRows[0].childNodes[0].childNodes[0].data;
						termDates = innerTableRows[0].childNodes[0].childNodes[2].innerText;
						letterGrade = innerTableRows[1].childNodes[0].childNodes[0].innerText;
						percentage = innerTableRows[1].childNodes[1].innerText;
						break;
				}
			}
			classID = className + term + termDates;
			scrape.setLastDialog(classID);
			scrape.registerClassTerm(classID, className, period, teacher, term, termDates, letterGrade, percentage, comment);
		} else if (i == 1) { /*IS THE INDIVIDUAL GRADES AND POINTS*/
			var gradestable = table.rows[i].childNodes[0].childNodes[0].getElementsByTagName('table');
			var gradesbody = gradestable[0].getElementsByTagName('tbody')[0];
			for (r = 0; r < gradesbody.rows.length; r++) {
				var row = gradesbody.rows[r];
				if (row.className == 'odd' || row.className == 'even') {
					var gradeDate = '',
						gradedName = '',
						pointsOutOf = '',
						percentageGrade = '-1',
						letterGrade = '-';
					var isMissing = false,
						isNoCount = false,
						absentNote = '',
						comment = '';
					for (c = 0; c < row.cells.length; c++) {
						var cell = row.cells[c];
						switch (c) {
							case 0:
								gradeDate = cell.childNodes[0].data;
								break;
							case 1:
								gradedName = cell.childNodes[0].innerText;
								break;
							case 2:
							    if(cell.childNodes[0].data == null) {
							        letterGrade = 'XC';
							    } else {
                                    letterGrade = cell.childNodes[0].data.replace('  ', '');
                                    if (cell.childNodes.length > 1) {
                                        comment = cell.childNodes[1].getAttribute('data-info');
                                    }
								}
								break;
							case 3:
								percentageGrade = cell.childNodes[0].data;
								break;
							case 4:
								pointsOutOf = cell.innerText.replace(/\t/g, '');
								break;
							case 5:
								if (cell.childNodes[0].data !== ' ') {
									isMissing = true;
								}
								break;
							case 6:
								if (cell.childNodes[0].data !== ' ') {
									isNoCount = true;
								}
								break;
							case 7:
								if (cell.childNodes[0].data !== ' ') {
									absentNote = cell.innerText;
								}
								break;
						}
					}
					scrape.addGradedItem(classID, gradeDate, gradedName, pointsOutOf, percentageGrade, letterGrade, comment, isMissing, isNoCount, absentNote);
				} else if (row.className == 'sf_Section cat') {
					var sectionName, weight, outOf, percentage, letterGrade;
					for (c = 0; c < row.cells.length; c++) {
						var cell = row.cells[c];
						switch (c) {
							case 1:
								sectionName = cell.childNodes[0].data;
								if (cell.hasChildNodes() && cell.childNodes.length > 2) {
									weight = cell.childNodes[2].innerText;
								} else {
								    weight = 'weighted at 100%';
								}
								break;
							case 2:
								letterGrade = cell.childNodes[0].data;
								break;
							case 3:
								percentage = cell.childNodes[0].data;
								break;
							case 4:
								outOf = cell.innerText.replace(/\t/g, '');
								break;
						}
					}
					scrape.addGradeHeader(classID, sectionName, weight, outOf, percentage, letterGrade);
				}
			}
		}
	}
	if (document.getElementsByClassName('sf_dialogClose').length > 0) {
		document.getElementsByClassName('sf_dialogClose')[0].click();
	}
})()