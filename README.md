<p align="center">
  <img width="80%" src="https://raw.githubusercontent.com/lyahim/silovi/7b2c5e138730fe9c78008cdbfe13bb3b96d086f2/images/silovi_bg.svg">
</p>

# SiLoVi - Simple Log Viewer
| Status | Bridge | Viewer |
| --- | --- | --- |
| Build | [![Build Status](https://travis-ci.com/lyahim/silovi.svg?branch=main)](https://travis-ci.com/lyahim/silovi) | [![Build Status](https://travis-ci.com/lyahim/silovi.svg?branch=main)](https://travis-ci.com/lyahim/silovi) |
| Sonar check | [![Bridge Quality Status](https://sonarcloud.io/api/project_badges/measure?project=lyahim_silovi&metric=alert_status)](https://sonarcloud.io/dashboard?id=lyahim_silovi) | [![Viewer Quality Status](https://sonarcloud.io/api/project_badges/measure?project=silovi-viewer&metric=alert_status)](https://sonarcloud.io/dashboard?id=silovi-viewer) |
The goal of the project create a very simple but useful tool to access files (mainly logs) on servers in browser without difficult installation, great infrastucture and huge amount of configuration. <br><br>This system gives access to files in efficient way with cool user experience. It is ideal for non-production systems where need real time access for logs and other files during development.
### [Demo will available here](#)

## Concept
SiLoVi system has 2 main part to give access logs/files on a server. First named bridge which gives an entry point to a server which has log files. This entry point can be safe with basic authentication. viewer gives a fancy graphical UI on web browser to see file contents. The two components has 1-N relation, which means a viewer can show content from many bridges what configured and connected that. Components can be running on same server and separately from each other.
## Features
- **Listing files** - suprisingly
- **Seaching** - both files by name and content
    - Simple match in lines
    - Regular expression what match the lines
    - Filtering in Live Polling mode
    - Browser search in loaded content
- **Full content** loading in efficient way. - **Important!** It is feasible to load very large files because the system allows **but!** the viewer is run in browser on users machine what possibly can't handle those amount of data.
- **Live polling** (aka tail -f) - with this mode currently selected file updated if it was been changed line the popular linux program
- **Content clearing** - it is useful when live polling enabled
- **Secure** - original file system cant be accessed directly by users. Listed files on browser accessed through 2 more layers. Firstly the **[viewer](https://github.com/lyahim/silovi/tree/main/viewer)** backend what serves data what given by **[bridges](https://github.com/lyahim/silovi/tree/main/bridge)**. **[Bridges](https://github.com/lyahim/silovi/tree/main/bridge)** has a fine grained configuration to give only necessary information about the original file system.
- **No need public internet** access, it can run on intranet
## Install - step-by-step
1. Install Java if not exists - [Java download page](https://www.java.com/en/download/manual.jsp)
2. Install Node.js 12 if not exists and want to use *Single version* - [Node.js download page](https://nodejs.org/en/download/)
3. Download and unzip [silovi-bridge.zip](http://silovi.nhely.hu/latest/silovi-bridge.zip)
4. Download and unzip viewer - [Bundle version](http://silovi.nhely.hu/latest/silovi-viewer-bundle.zip) or [Single version](http://silovi.nhely.hu/latest/silovi-viewer-bundle.zip)
5. Set *root_log_folder* property in bridge **config.properties**
6. On Linux distribution set file permissions - !Later...
7. Start bridge with start command in root folder
8. Start viewer with start command in root folder
## Install - detailed
### Requirements
- Bridge
    - JRE 11
- Viewer
    - Node.js 12
    - OR no requirement for bundled version
### Download
Download and unzip one bridge archive and one viewer. Links below points to latest version. Viewer has two versions but needed to download one from it. Bundle version has no requirements, it perfect when isn't installed Node.js. Single version can run on installed Node.js.
- Bridge - [silovi-bridge.zip](http://silovi.nhely.hu/latest/silovi-bridge.zip)
- Viewer
    - [Bundle version](http://silovi.nhely.hu/latest/silovi-viewer-bundle.zip)
    - [Single version](http://silovi.nhely.hu/latest/silovi-viewer-bundle.zip)
### Configuration
- Bridge - In unzipped folder is existed config.properties what contains all necessary parameters to run bridge application. This is a 'key=value' configuration file. Details of configuration you can see [here](https://github.com/lyahim/silovi/tree/main/bridge).
    - **Before first run root_log_folder is had to set.**  This configuration set the base folder whom files will be read.
- Viewer - In unzipped folder is existed config.json what contains a json structure for bridge connections with name and url properties. Name is displayed as root in browser, url is the root url where bridge serves data. The file contains a preset for one local bridge on port 8080. Details of configuration you can see [here](https://github.com/lyahim/silovi/tree/main/viewer).
<br>!TODO Apache/nginx config / ws enabled
### Start
## DEVELOPER SECTION
### Technology stack / requirements
#### Bridge
- JDK 11
#### Viewer
- Node.js 12
### Build
#### Windows
Simplest way to use build.cmd in root. 
#### Linux
!! Later
#### Mac
!! Later