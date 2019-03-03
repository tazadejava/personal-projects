# Standard Score

Play Store listing:

```
Check school grades, get notified for updates, track your academic progression!
```

Android application built for use in LWSD School District to automatically download, update, and notify for updated school grades during the school year.

Play Store listing description:

```
With Standard Score, check your school grades on an intuitive app interface rather than go through the tedious mobile process to view them online!

Features include:
    - Automatically updates grades in the background and notifies the student when assignments have been updated
    - Calculates the “if I get X out of Y on this assignment, what will my term grade be?”
    - Calculates the “I need at least X out of Y on this assignment to get a specified percentage for this class”
    - Tracks your grade percentage history for each period through a progression graph
    - Saves previously recorded (through the app) grades and assignments for all years
    - Displays the student’s yearly schedule

DISCLAIMER: Currently, the app is built for use only in the Lake Washington School District. Standard Score is not guaranteed to work outside of the LWSD.
```

Example screenshots of the app (sample account used):

<img src="https://user-images.githubusercontent.com/47044879/53614349-8092a600-3b8e-11e9-8feb-963c3c6fd3c1.png" width="270" height="480" />
<img src="https://user-images.githubusercontent.com/47044879/53614350-8092a600-3b8e-11e9-982f-11be6701695c.png" width="270" height="480" />
<img src="https://user-images.githubusercontent.com/47044879/53614351-8092a600-3b8e-11e9-9041-9edd4883ad28.png" width="270" height="480" />
<img src="https://user-images.githubusercontent.com/47044879/53614352-8092a600-3b8e-11e9-8a0f-fcfb0b49e2c5.png" width="270" height="480" />

## Development

* The app is built with Google's Android platform. Consequently, most of the code is written in Java, with the UI formatting written in XML.
* To scrape the user's grades from the school's grading website, Skyward, I used a WebView interface system (via me/tazadejava/gradeupdates/GradesManager.class and me/tazadejava/gradeupdates/ScraperInterface.class). Consequently, I used JavaScript to scrape and pass the data back to Java to be saved. The JavaScript code can be found in the assets folder within the project.

## Deployment

The application is available for free on the Play Store. Otherwise, the source can be loaded on a gradle-supported IDE and built to run on any Android device with SDK 23 or higher.
