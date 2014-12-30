# Google Glass Lecturing App

This application allows the user to see the notes for each slide of a Microsoft
Powerpoint presentation. The app also supports LaTeX equations that are displayed as
images to the user.

## MainActivity.java
This is the starting activity, it only allows the user to start the presentation and establish
the permanent connection with the server. It does this by starting the PresentationModeActivity.java activity.

## PresentationModeActivity.java
This class does all the work of handling the presentation. 
- While this application is running, it prevents the app from going to sleep.
- It has gesture listeners to change the slides and to swipe between the notes of the presentation.
- It allows the user to terminate the presentation in the options menu and return to the MainActivity.java
- The SendPostTask and SendGetTask classes execute AsyncTasks to perform GET and POST requests respectively. These tasks change the UI as needed, depending on the request.

In terms of the structure of the notes, these are organized as a 2D ArrayList. The first index (_slideIndex) represents
the current slide and the second index (_currentIndex) is the note displayed within a slide

The types of GET requests supported are:
- IMAGE: Which return the image for the equation sent.
- INDEX: Which requests the current slide index of the Powerpoint presentation

The types of POST requests are:
- Action Notes: This is the first request sent when the activity starts (in the onCreate() method). It requests the notes of the powerpoint presentation and when they are received, the notes are structured so that they can be presented individually to the glass. This action also triggers the sync between the [Desktop App](https://github.com/crro/GoogleGlassDesktopApp) and the [Heroku Server](https://github.com/crro/GoogleGlassServerHeroku).
- Action Previous: This request makes the ongoing presentation to go to the next slide. This is sent when the user swipes forward on the Glass device
- Action Next: This request makes the ongoing presentation to go to the previous slide.This is sent when the user swipes backward on the Glass device

## GlassConstants.java
This class is used to store the constants used throughout the application.

## The SendPostTask and the SendGetTask classes
These classes perform GET and POST requests respectively.