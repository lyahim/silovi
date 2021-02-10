# SiLoVi - Simple Log Viewer
The goal of the project create a very simple but useful tool to access files (mainly logs) on servers in browser without difficult installation, great infrastucture and huge amount of configuration. <br><br>This system gives access to files in efficient way with cool user experience. It is ideal for non-production systems where need real time access for logs and other files during development.

## Concept
SiLoVi system has 2 main part to give access logs/files on a server. First named **[bridge](https://github.com/lyahim/silovi/tree/main/bridge)** which gives an entry point to a server which has log files. This entry point can be safe if it's need with basic authentication. **[viewer](https://github.com/lyahim/silovi/tree/main/viewer)** gives a fancy graphical UI on web browser to see file contents. The two components has 1-N relation, which means a **[viewer](https://github.com/lyahim/silovi/tree/main/viewer)** can show content from many **[bridges](https://github.com/lyahim/silovi/tree/main/bridge)** what configured and connected that. Components can be running on same server and separately from each other.
## Features
## Requirements
## Install
## Configuration
## Start
## Developer section
### Technology stack
### Build