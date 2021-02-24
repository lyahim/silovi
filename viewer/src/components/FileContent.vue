<template>
  <div>
    <div v-if="fileLoading" class="content-loader">
      <v-progress-circular indeterminate size="100"></v-progress-circular>Loading
    </div>
    <div
      class="line-data"
      :class="index%2?'even':'odd'"
      v-for="(line, index) in content"
      :key="line.i"
      @mouseover="changeLoadMoreState(line.i, true, index)"
      @mouseleave="changeLoadMoreState(line.i, false, index)"
    >
      <v-btn
        x-small
        color="grey"
        :style="{ minWidth: lineCounterWidth + 'px' }"
        :ref="'idx_prev'+ line.i"
        class="load-more v-btn default prev"
        @click="loadMoreLines(line.i,'PREV')"
      >
        <v-icon>mdi-chevron-up</v-icon>
      </v-btn>
      <v-btn
        x-small
        color="grey"
        :style="{ minWidth: lineCounterWidth + 'px' }"
        :ref="'idx_next'+ line.i"
        class="load-more v-btn default next"
        @click="loadMoreLines(line.i,'NEXT')"
      >
        <v-icon>mdi-chevron-down</v-icon>
      </v-btn>
      <span
        class="line-number"
        :style="{ minWidth: lineCounterWidth + 'px' }"
        @click="selectText($refs['line'+line.i])"
      >{{line.i}}</span>
      <span :ref="'line'+ line.i" class="ml-2 line-content">{{line.c}}</span>
    </div>
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
      inProgress: false,
      fileLoading: false,
      lineCounterWidth: 30,
      lineAdding: false
    };
  },
  watch: {
    file(newVal) {
      this.stopContentLoading();
      this.clearContent();
      if (newVal) {
        this.fileLoading = true;
        let bridgeAndId = newVal.split("::");
        backendService
          .loadFileEnd(bridgeAndId[0], bridgeAndId[1])
          .then(response => {
            if (Array.isArray(response)) {
              this.updateLineCounterWidth(response);
              this.content = response;
            } else {
              this.content = [{ i: "", c: "File content cannot be displayed" }];
            }
            this.fileLoading = false;
          });
      }
    }
  },
  created() {
    this.$socket.$subscribe("content", this.appendFileContent);
    window.addEventListener("beforeunload", this.stopContentLoading);
  },
  updated() {
    if (!this.lineAdding) {
      this.scrollTo(this.$el.clientHeight, "smooth");
      this.lineAdding = false;
    }
  },
  methods: {
    updateLineCounterWidth(newData) {
      let lastElement = newData[newData.length - 1];
      if (lastElement) {
        let largestCount = lastElement.i.toString();
        let newWidth = 10 + largestCount.length * 9;
        if (newWidth > this.lineCounterWidth) {
          this.lineCounterWidth = newWidth;
        }
      }
    },
    changeLoadMoreState(lineIdx, state, index) {
      let prevNum = this.$refs["idx_prev" + lineIdx][0].$el;
      let nextNum = this.$refs["idx_next" + lineIdx][0].$el;
      let line = this.$refs["line" + lineIdx][0];

      if (
        (index === 0 && this.content[0].i > 1) ||
        (this.content[index - 1] && this.content[index - 1].i !== lineIdx - 1)
      ) {
        if (state) {
          prevNum.style.display = "block";
          line.style.borderTop = "1px solid #ccc";
        } else {
          prevNum.style.display = "none";
          line.style.borderTop = "none";
        }
      }

      if (
        this.content[index + 1] &&
        this.content[index + 1].i !== lineIdx + 1
      ) {
        if (state) {
          nextNum.style.display = "block";
          line.style.borderBottom = "1px solid #ccc";
        } else {
          nextNum.style.display = "none";
          line.style.borderBottom = "none";
        }
      }
    },
    stopContentLoading() {
      this.inProgress = false;
      this.$socket.client.emit("file-stop");
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
      this.lineAdding = false;
    },
    loadMoreLines(startLine, direction) {
      let bridgeAndId = this.file.split("::");
      backendService
        .loadMoreLines(bridgeAndId[0], bridgeAndId[1], startLine, direction)
        .then(response => {
          if (Array.isArray(response)) {
            this.lineAdding = true;
            let index = this.content.findIndex(item => item.i === startLine);
            if (index >= 0) {
              response.forEach(item => {
                let cIndex = this.content.findIndex(
                  cItem => cItem.i === item.i
                );
                if (cIndex > 0) {
                  this.content.splice(cIndex, 1);
                }
              });
              let index = this.content.findIndex(item => item.i === startLine);
              this.changeLoadMoreState(startLine, false, index);
              if ("NEXT" === direction) {
                response.forEach(item => {
                  this.content.splice(++index, 0, item);
                });
              } else {
                response.forEach(item => {
                  this.content.splice(index++, 0, item);
                });
              }
            }
          }
        });
    },
    search(searchKey) {
      this.clearContent();
      this.inProgress = true;
      let bridgeAndId = this.file.split("::");

      this.$socket.client.emit("file", {
        bridge: bridgeAndId[0],
        fileId: bridgeAndId[1],
        searchKey
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
        let newData = JSON.parse(newContent);

        this.updateLineIndexes(newData);
        this.updateLineCounterWidth(newData);
        this.content.push.apply(this.content, newData);
      }
    },
    updateLineIndexes(newData) {
      let lastElement = this.content[this.content.length - 1];
      if (lastElement) {
        let lastLineNumber = lastElement.i;

        if (newData[0] && newData[0].i < 0) {
          newData.forEach(element => {
            element.i = ++lastLineNumber;
          });
        }
      }
    },
    selectText(container) {
      if (document.selection) {
        // IE
        let range = document.body.createTextRange();
        range.moveToElementText(container[0]);
        range.select();
      } else if (window.getSelection) {
        let range = document.createRange();
        range.selectNode(container[0]);
        window.getSelection().removeAllRanges();
        window.getSelection().addRange(range);
      }
    }
  }
};
</script>
<style scoped>
.v-application p {
  margin: 0;
}
.line-data {
  display: flex;
}
.line-number {
  background-color: #cccccc;
  padding: 2px 5px;
  cursor: pointer;
  text-align: center;
}
.line-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.content-loader {
  width: 150px;
  margin: 0 auto;
  text-align: center;
}
.load-more {
  position: absolute;
  display: none;
  z-index: 5;
}
.load-more.prev {
  margin-top: -17px;
}
.load-more.next {
  margin-top: 20px;
}
</style>