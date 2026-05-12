<template>
  <div class="furigana-container">
    <div class="d-flex flex-column align-center text-center mb-3">
      <h1 class="text-h5 font-weight-bold mb-1">日本語ふりがな生成器</h1>
      <h2 class="text-caption text-medium-emphasis">
        Japanese Furigana Generator
      </h2>
    </div>

    <v-card class="mb-3 elevation-1 rounded-lg">
      <v-card-text class="py-2 px-3">
        <v-textarea
          v-model="japaneseText"
          placeholder="例: 日本語を勉強しています。"
          rows="2"
          auto-grow
          density="compact"
          hide-details
          variant="outlined"
          bg-color="grey-lighten-5"
          class="text-body-2 mb-2"
        ></v-textarea>

        <div class="d-flex align-items-center justify-space-between">
          <v-switch
            v-model="showFurigana"
            label="Show Furigana"
            color="primary"
            hide-details
            density="compact"
            class="mini-switch"
            inset
          ></v-switch>
          <v-btn
            color="primary"
            size="small"
            :loading="loading"
            @click="generateFurigana"
            rounded
            class="px-3"
          >
            <span class="text-caption mr-1">Generate</span>
            <v-icon size="small">mdi-translate</v-icon>
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <v-card
      v-if="tokens.length > 0"
      class="result-card mb-3 elevation-1 rounded-lg"
    >
      <v-card-item class="py-2">
        <template v-slot:prepend>
          <v-icon color="primary" size="small">mdi-alphabetical-variant</v-icon>
        </template>
        <v-card-title class="text-subtitle-2 px-0"
          >Result with Furigana</v-card-title
        >
      </v-card-item>

      <v-card-text class="pb-2 pt-0">
        <div class="furigana-text">
          <ruby v-for="(token, index) in tokens" :key="index" class="ruby-text">
            {{ token.text }}
            <rt v-if="token.reading && token.isKanji && showFurigana">{{
              token.reading
            }}</rt>
          </ruby>
        </div>

        <v-divider class="my-2"></v-divider>

        <div class="d-flex justify-end">
          <v-btn
            size="x-small"
            variant="text"
            @click="copyToClipboard"
            class="mr-2"
          >
            <v-icon size="small" class="mr-1">mdi-content-copy</v-icon>
            <span class="text-caption">HTML</span>
          </v-btn>
          <v-btn size="x-small" variant="text" @click="copyPlainText">
            <v-icon size="small" class="mr-1">mdi-clipboard-text</v-icon>
            <span class="text-caption">Text</span>
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <v-card
      v-if="tokens.length > 0"
      class="debug-card elevation-0 rounded-lg bg-grey-lighten-5"
    >
      <v-card-item class="py-1 px-3">
        <template v-slot:prepend>
          <v-icon size="small" color="grey-darken-1">mdi-code-json</v-icon>
        </template>
        <v-card-title class="text-caption text-medium-emphasis">
          Debug Information
        </v-card-title>
        <template v-slot:append>
          <v-btn
            variant="text"
            density="compact"
            size="x-small"
            color="primary"
            @click="showDebug = !showDebug"
          >
            {{ showDebug ? "Hide" : "Show" }}
          </v-btn>
        </template>
      </v-card-item>

      <v-expand-transition>
        <div v-if="showDebug">
          <v-card-text class="pt-0 pb-2 px-3">
            <pre class="debug-json">{{ JSON.stringify(tokens, null, 2) }}</pre>
          </v-card-text>
        </div>
      </v-expand-transition>
    </v-card>

    <v-snackbar
      v-model="snackbar.show"
      :color="snackbar.color"
      timeout="3000"
      location="top"
    >
      {{ snackbar.text }}
    </v-snackbar>
  </div>
</template>

<script setup>
import { ref } from "vue";
import api from "@/utils/api";

// State
const japaneseText = ref("日本語を勉強しています。");
const tokens = ref([]);
const loading = ref(false);
const showDebug = ref(false);
const showFurigana = ref(true);
const snackbar = ref({
  show: false,
  text: "",
  color: "success",
});

// Methods
async function generateFurigana() {
  if (!japaneseText.value.trim()) {
    showSnackbar("Please enter some Japanese text", "error");
    return;
  }

  loading.value = true;

  try {
    // Gọi API với instance api từ utils/api.ts đã cấu hình sẵn
    const response = await api.get("/api/v1/learning/furigana", {
      params: { text: japaneseText.value },
    });

    tokens.value = response.data;
    showSnackbar("Furigana generated successfully", "success");
  } catch {
    showSnackbar("Failed to generate furigana. Please try again.", "error");
  } finally {
    loading.value = false;
  }
}

function copyToClipboard() {
  if (tokens.value.length === 0) return;

  let html = "";
  tokens.value.forEach((token) => {
    if (token.isKanji && token.reading) {
      html += `<ruby>${token.text}<rt>${token.reading}</rt></ruby>`;
    } else {
      html += token.text;
    }
  });

  navigator.clipboard
    .writeText(html)
    .then(() => showSnackbar("HTML copied to clipboard", "success"))
    .catch(() => showSnackbar("Failed to copy HTML", "error"));
}

function copyPlainText() {
  if (tokens.value.length === 0) return;

  const text = tokens.value.map((token) => token.text).join("");

  navigator.clipboard
    .writeText(text)
    .then(() => showSnackbar("Text copied to clipboard", "success"))
    .catch(() => showSnackbar("Failed to copy text", "error"));
}

function showSnackbar(text, color = "success") {
  snackbar.value = {
    show: true,
    text,
    color,
  };
}

// Initialize with example text
generateFurigana();
</script>

<style scoped>
.furigana-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 12px;
}

.result-card {
  background-color: #fafafa;
}

.furigana-text {
  font-size: 1.25rem;
  line-height: 2;
  margin: 12px 0;
  padding: 8px;
  background-color: #fff;
  border-radius: 4px;
  word-wrap: break-word;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.ruby-text {
  margin: 0 1px;
  text-align: center;
  position: relative;
}

ruby {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
}

rt {
  position: absolute;
  top: -0.5em;
  font-size: 0.5rem;
  color: #2196f3;
  font-weight: 400;
  text-align: center;
  line-height: 1;
  white-space: nowrap;
  letter-spacing: 0.05em;
}

.debug-json {
  font-family: monospace;
  font-size: 0.75rem;
  line-height: 1.3;
  overflow-x: auto;
  padding: 6px;
  background-color: #f5f5f5;
  border-radius: 4px;
  max-height: 200px;
}

.mini-switch {
  margin-top: 0;
  margin-bottom: 0;
}

:deep(.mini-switch .v-switch__track) {
  opacity: 0.5;
  transform: scale(0.75);
}

:deep(.mini-switch .v-switch__thumb) {
  transform: scale(0.75);
}
</style>
