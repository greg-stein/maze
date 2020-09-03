![Maze](https://github.com/greg-stein/maze/blob/master/maze-logo.png?raw=true)

Maze is currently in MVP state. Minimum Viable Product. It has the minimum functionality to map and navigate inside buildings. Currently it uses following methods:
 - **WiFi-fingerprinting** - this method is used for estimating initial location (when you open the app), and to fix the dead reckoning when it collects too much drift.
 - **Dead reconing** - inertial navigation in mobile phones is useless. It is so erratic. So what we do instead is estimating direction of movement and detecting steps. We have a calibration service that measures average length of step of the user when he walks outside. This is done without user interaction.
 
 From the experience we can say that this method works very well in most situations.

We are currently working on documentation and refactoring to allow easy development of other methods for estimating indoor location. 

### Maturity
Maze is developed with MVP (=Model-View-Presenter) approach. It has high level of modularity and mostly the code is clean. We have unit tests that run locally on host machine and also on the device. Map engine is based on OpenGl rendering.

### How to use Maze App ###
1. Create building and floor in App UI
2. Take a picture of an emergency evacuation plan. The picture will be vectorized and a map will be generated.
3. Start mapping. Simply walk around, Maze will record WiFi fingerprints.
4. Tap on Upload button

### Contributions
**Feel free to contribute.**
We need Mobile, Fullstack, UX, DB... Everyone! The tasks will be populated to make it easy grabbing one and working on it.
For any questions please contact me: gregory.stein@maze.world
