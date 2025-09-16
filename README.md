This app was developed for a project in the course Mobile Computing in Biomedic Engineering at the University of Porto, during my exchange semeseter there in the autumn of 2024. The task was to create an Android app with some kind of medical purpose and which used data from an external sensor through Bluetooth. This app is intended for elderly people or others who have a risk of falling and needing help. It connects to an accelerometer which the user wears around their chest and continuously analyses the data from it. When it detects movement indicating a fall, it alerts the listed emergency contacts through SMS unless the user says it was a false alarm, and logs the fall for later analysis. Due a tight schedule and initial hardware problems there was not time to make a complicated design, implement a more sophisticated fall detecetion algorithm or add all the planned features. Also some of the code, primarily in MainActivity, would have needed conisderable refactoring to improve quality and readability, but the most important thing was that the main functionality was succesfully implemented amd got working.

Key features:
- Bluetooth connection to accelerometer, with handling and analysis of raw data.
- Background listener service and broadcast receivers for continuous signal monitoring.
- Notifications with action buttons allowing quick and convenient user responses.
- Usage of device GPS and cellular network for recording fall location and sending SMS alerts.
- Persistent storage of user data, contacts and fall history in local database.
