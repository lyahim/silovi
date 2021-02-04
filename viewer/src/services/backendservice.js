import axios from 'axios';

function loadFileTree() {
    return axios
        .get(process.env.VUE_APP_BACKEND_URL + '/file-tree')
        .then(response => {
            return response.data;
        })
        .catch(e => {
            console.error(e);
            return [];
        });
}
function refreshFileTree() {
    return axios.get(process.env.VUE_APP_BACKEND_URL + '/refresh-file-tree')
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
        .get(process.env.VUE_APP_BACKEND_URL + '/file-end/' + bridgeName + "/" + fileId)
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
