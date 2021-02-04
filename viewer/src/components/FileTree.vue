<template>
  <div>
    <v-app-bar dark flat color="secondary" elevate-on-scroll>
      <v-text-field
        class="search-field"
        v-model="search"
        label="Search"
        dark
        flat
        solo-inverted
        hide-details
        clearable
        clear-icon="mdi-close-circle-outline"
      ></v-text-field>
      <v-btn @click="refresh" class="ml-2">
        <v-icon>mdi-refresh</v-icon>
      </v-btn>
    </v-app-bar>
    <v-container class="overflow-y-auto">
      <v-row>
        <v-treeview
          v-model="tree"
          :items="items"
          :search="search"
          :filter="filter"
          activatable
          item-key="key"
          open-on-click
          @update:active="onSelect"
        >
          <template v-slot:prepend="{ item, open }">
            <v-icon v-if="item.bridge">{{ 'mdi-home-variant' }}</v-icon>
            <v-icon v-else-if="item.children">{{ open ? 'mdi-folder-open' : 'mdi-folder' }}</v-icon>
            <v-icon color="primary" v-else>{{ 'mdi-file' }}</v-icon>
          </template>
          <template v-slot:label="{ item }">
            {{item.name}}
            <div class="text-caption" v-if="item.size">{{fileSize(item.size)}}</div>
          </template>
        </v-treeview>
      </v-row>
    </v-container>
  </div>
</template>

<script>
import filesize from "filesize";
import { backendService } from "../services";

export default {
  name: "FileTree",
  data() {
    return {
      search: null,
      tree: [],
      items: []
    };
  },
  created() {
    backendService.loadFileTree().then(result => {
      this.items = result;
    });
  },
  computed: {
    filter() {
      return (item, search, textKey) => item[textKey].indexOf(search) > -1;
    }
  },
  methods: {
    onSelect(array) {
      this.$emit("fileSelect", array[0]);
    },
    refresh() {
      backendService.refreshFileTree().then(result => {
        this.items = result;
      });
    },
    fileSize(bytes) {
      return filesize(bytes);
    }
  }
};
</script>
<style scoped>
.search-field.v-text-field.v-text-field--solo /deep/ .v-input__control {
  min-height: 32px;
}
</style>
