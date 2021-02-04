const app = require('express')();
const serveStatic = require("serve-static")
const path = require('path');
const fs = require('fs');
const axios = require('axios');
const cors = require('cors');
const fetch = require("node-fetch");
const AbortController = require("abort-controller")

const fileErrorMessage = ['File content cannot be displayed'];

const http = require('http').Server(app);
const io = require('socket.io')(http, {
    cors: {
        origin: '*'
    }
});

app.use(serveStatic(path.join(__dirname, 'dist')));
app.use(cors());

const port = process.env.PORT || 80;
const logFileTree = [];
let configFileJson = {};

http.listen(port, () => {
    configFileJson = getConfigFileContent();
    loadFileTree();
    console.log("SiLoVi viewer started");
});

app.get('/file-tree', (req, res) => {
    if (logFileTree) {
        res.json(logFileTree);
    } else {
        res.json({});
    }
});

app.get('/refresh-file-tree', (req, res) => {
    loadFileTree(() => {
        if (logFileTree) {
            res.json(logFileTree);
        } else {
            res.json({});
        }
    });
});

app.get('/file-end/:bridge/:fileId', (req, res) => {
    let bridge = req.params.bridge;
    if (bridge) {
        let bridgeConfig = configFileJson.find(c => c.name === bridge);
        if (bridgeConfig) {
            axios.get(bridgeConfig.url + '/file-end/' + req.params.fileId).then((response) => {
                res.send(response.data);
            }, (error) => {
                console.error(error);
                res.send(fileErrorMessage);
            });
        } else {
            res.send(fileErrorMessage);
        }
    } else {
        res.send(fileErrorMessage);
    }
});

io.on('connection', (socket) => {
    /*socket.on('disconnect', () => {
        console.log('user disconnected');
    });*/
    let controller;
    socket.on('file', (params) => {
        let bridge = params.bridge;
        if (bridge) {
            controller = new AbortController();
            const signal = controller.signal;

            let bridgeConfig = configFileJson.find(c => c.name === bridge);
            if (bridgeConfig) {
                fetch(bridgeConfig.url + '/file/' + params.fileId + (params.searchKey ? '?searchKey=' + params.searchKey : ''), { signal })
                    .then(response => response.body)
                    .then(response => {
                        response.on('data', (chunk) => {
                            socket.emit("content", chunk.toString())
                        });
                        response.on('end', () => {
                            socket.emit("content-end", "");
                        });
                    }).catch(error => {
                        console.error(error);
                        socket.emit("content-end", "");
                    });
            }
        }
    });
    socket.on('file-tail', (params) => {
        let bridge = params.bridge;
        if (bridge) {
            controller = new AbortController();
            const signal = controller.signal;

            let bridgeConfig = configFileJson.find(c => c.name === bridge);
            if (bridgeConfig) {
                fetch(bridgeConfig.url + '/file-tail/' + params.fileId + (params.searchKey ? '?searchKey=' + params.searchKey : ''), { signal })
                    .then(response => response.body)
                    .then(response => {
                        response.on('data', (chunk) => {
                            socket.emit("content", chunk.toString())
                        });
                        response.on('end', () => {
                            socket.emit("content-end", "");
                        });
                    }).catch(error => {
                        console.error(error);
                        socket.emit("content-end", "");
                    });
            }
        }
    });
    socket.on('file-stop', () => {
        if (controller) {
            controller.abort();
        }
    });
});

function getConfigFileContent() {
    let rawdata = fs.readFileSync('config.json');
    return JSON.parse(rawdata);
}

function loadFileTree(callback) {
    getBridgeFiles((result) => {
        logFileTree.splice(0, logFileTree.length)
        logFileTree.push.apply(logFileTree, result);
        if (callback) {
            callback();
        }
    })
}

function getBridgeFiles(callback) {
    if (configFileJson) {
        let requests = [];
        configFileJson.forEach(element => {
            requests.push(axios.get(element.url + '/files', { params: { siloviname: element.name } }));
        });
        let result = [];
        axios.all(requests).then(axios.spread((...responses) => {
            responses.forEach((response) => {
                result.push(processBridgeResult(response));
            });
            callback(result);
        })).catch(errors => {
            console.log(errors);
            callback([]);
        })
    } else {
        callback([]);
    }
}

function processBridgeResult(response) {
    let fileList = response.data;
    fileList.sort((o1, o2) => {
        const pathResult = o1.path.localeCompare(o2.path);
        if (pathResult === 0) {
            return o1.name.localeCompare(o2.name);
        }
        return pathResult;
    });

    return createJsonFromFileList(fileList, response.config.params.siloviname);
}

function createJsonFromFileList(fileList, bridgeName) {
    let paths = [];
    let pathFileMap = {};
    fileList.forEach(f => {
        paths.push(f.path)
        if (!pathFileMap[f.path]) {
            pathFileMap[f.path] = [];
        }
        f.key = bridgeName + '::' + f.id + '::' + f.name;
        pathFileMap[f.path].push(f);
    });

    let result = paths.reduce((r, p) => {
        let names = p.split('/');
        if (names.every(e => ["", ""].includes(e))) {
            names = [""];
        }
        names.reduce((q, name) => {
            let temp = q.find(o => o.name === name);
            if (!temp) q.push(temp = { name, children: pathFileMap[p], key: bridgeName + name });
            return temp.children;
        }, r);
        return r;
    }, []);

    result = result[0];
    result.name = bridgeName;
    result.key = bridgeName;
    result.bridge = true;

    return result;
}
