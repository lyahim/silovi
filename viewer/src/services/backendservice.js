import axios from 'axios';

const currentHost = window.location.protocol + "//" + window.location.host;
const backendUrl = process.env.NODE_ENV === "development" ? process.env.VUE_APP_DEV_BACKEND_URL : currentHost;

function loadFileTree() {
    return axios
        .get(backendUrl + '/file-tree')
        .then(response => {
            return response.data;
        })
        .catch(e => {
            console.error(e);
            return [];
        });
}
function refreshFileTree() {
    return axios.get(backendUrl + '/refresh-file-tree')
        .then(response => {
            return response.data;
        })
        .catch(e => {
            console.error(e);
            return [];
        });
}

function loadFileEnd(bridgeName, fileId) {
    return axios
        .get(backendUrl + '/file-end/' + bridgeName + "/" + fileId)
        .then(response => {
            return this.content = response.data;
        })
        .catch(e => {
            console.error(e);
            return [];
        });
}

export const backendService = {
    refreshFileTree,
    loadFileTree,
    loadFileEnd
};
