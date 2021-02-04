<template>
  <div>
    <p
      class="text-content"
      :class="index%2?'even':'odd'"
      v-for="(line, index) in contentWithGuid"
      :key="line.key"
    >{{line.line}}</p>
  </div>
</template>

<script>
import { backendService } from "../services";

export default {
  name: "FileContent",
  props: ["file"],
  data() {
    return {
      content: [],
      inProgress: false
    };
  },
  watch: {
    file(newVal) {
      this.stopContentLoading();
      if (newVal) {
        this.scrollTo(0, "auto");
        let bridgeAndId = newVal.split("::");
        backendService
          .loadFileEnd(bridgeAndId[0], bridgeAndId[1])
          .then(response => {
            this.content = response;
          });
      } else {
        this.clearContent();
      }
    }
  },
  computed: {
    contentWithGuid: function() {
      return this.content.map(line => {
        return { line, key: line + this.guid() };
      });
    }
  },
  created() {
    this.$socket.$subscribe("content", this.appendFileContent);
    window.addEventListener("beforeunload", this.stopContentLoading);
  },
  updated() {
    this.scrollTo(this.$el.clientHeight, "smooth");
  },
  methods: {
    stopContentLoading() {
      this.inProgress = false;
      this.$socket.client.emit("file-stop");
    },
    guid() {
      return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
        (
          c ^
          (crypto.getRandomValues(new Uint8Array(1))[0] & (15 >> (c / 4)))
        ).toString(16)
      );
    },
    scrollTo(height, behavior) {
      window.scrollTo({
        top: height,
        left: 0,
        behavior: behavior
      });
    },
    clearContent() {
      this.content = [];
    },
    search(searchKey) {
      this.clearContent();
      this.inProgress = true;
      let bridgeAndId = this.file.split("::");

      this.$socket.client.emit("file", {
        bridge: bridgeAndId[0],
        fileId: bridgeAndId[1],
        searchKey: searchKey
      });
    },
    watch(searchKey) {
      this.inProgress = true;
      let bridgeAndId = this.file.split("::");

      this.$socket.client.emit("file-tail", {
        bridge: bridgeAndId[0],
        fileId: bridgeAndId[1],
        searchKey: searchKey
      });
    },
    appendFileContent(newContent) {
      if (this.inProgress) {
        this.content.push.apply(this.content, JSON.parse(newContent));
      }
    }
  }
};
</script>
<style scoped>
.v-application p {
  margin: 0;
}
.text-content {
  white-space: pre-wrap;
}
</style>