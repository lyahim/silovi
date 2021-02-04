<template>
  <v-app>
    <v-system-bar app color="primary" height="60" dark>
      <div class="d-flex align-center">
        <v-img
          alt="Silovi Logo"
          class="shrink mt-1 hidden-sm-and-down"
          contain
          :src="require('@/assets/silovi.svg')"
          min-width="100"
          transition="scale-transition"
          width="100"
        />
      </div>

      <v-spacer></v-spacer>

      <v-btn href="https://github.com/lyahim/silovi" target="_blank" text>
        <span class="mr-2">Documentation</span>
        <v-icon>mdi-open-in-new</v-icon>
      </v-btn>
    </v-system-bar>

    <v-navigation-drawer app permanent width="20%">
      <FileTree @fileSelect="onFileSelect"/>
    </v-navigation-drawer>

    <v-app-bar app color="secondary" dark flat v-if="selectedFile">
      <v-text-field
        class="search-field"
        v-model="searchKey"
        label="Search"
        dark
        flat
        solo-inverted
        hide-details
        clearable
        clear-icon="mdi-close-circle-outline"
      ></v-text-field>
      <v-progress-circular class="ml-2" indeterminate size="30" v-if="inProgress"></v-progress-circular>
      <v-btn @click="stop" class="ml-2" color="error" v-if="inProgress">Stop</v-btn>
      <v-btn @click="search" class="ml-2" v-if="!inProgress && searchKey">Search</v-btn>
      <v-btn @click="search" class="ml-2" v-if="!inProgress && !searchKey">Full content</v-btn>
      <span class="ml-2" v-if="!inProgress">OR</span>
      <v-btn @click="watch" class="ml-2" v-if="!inProgress" color="success">
        <v-icon>mdi-play</v-icon>
      </v-btn>
      <v-spacer></v-spacer>
      <v-toolbar-title>{{fileTitle}}</v-toolbar-title>
      <v-spacer></v-spacer>
      <v-btn @click="clearContent">Clear</v-btn>
    </v-app-bar>

    <v-main>
      <v-container fluid>
        <FileContent ref="contentHolder" :file="selectedFile"/>
      </v-container>
    </v-main>

    <v-footer app>
      <v-col class="text-center" cols="12">
        {{ new Date().getFullYear() }} —
        <strong>Lyahim</strong>
      </v-col>
    </v-footer>
  </v-app>
</template>

<script>
import FileContent from "./components/FileContent";
import FileTree from "./components/FileTree";

export default {
  name: "App",

  components: {
    FileContent,
    FileTree
  },
  data() {
    return {
      selectedFile: null,
      searchKey: null,
      inProgress: false
    };
  },
  created() {
    this.$socket.$subscribe("content-end", this.onContentEnd);
  },
  computed: {
    fileTitle() {
      if (this.selectedFile) {
        let fileData = this.selectedFile.split("::");
        return fileData[0] + " - " + fileData[2];
      }
      return "";
    }
  },
  methods: {
    onFileSelect(selectedFile) {
      this.inProgress = false;
      this.selectedFile = selectedFile;
    },
    clearContent() {
      this.$refs.contentHolder.clearContent();
    },
    search() {
      this.inProgress = true;
      this.$refs.contentHolder.search(this.searchKey);
    },
    watch() {
      this.inProgress = true;
      this.$refs.contentHolder.watch(this.searchKey);
    },
    stop() {
      this.inProgress = false;
      this.$refs.contentHolder.stopContentLoading(this.searchKey);
    },
    onContentEnd() {
      this.inProgress = false;
    }
  }
};
</script>
<style scoped>
.search-field.v-text-field.v-text-field--solo /deep/ .v-input__control {
  min-height: 32px;
}
.search-field.v-text-field {
  width: 20%;
  flex: 0 1 auto;
}
</style>