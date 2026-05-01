<template>
  <div class="conversation-detail-container">
    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
      <span class="mt-4 text-body-1">Đang tải dữ liệu hội thoại...</span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <v-alert type="error" class="mb-4">{{ error }}</v-alert>
      <v-btn @click="goBack" color="primary" variant="outlined">
        <v-icon start>mdi-arrow-left</v-icon>
        Quay lại danh sách hội thoại
      </v-btn>
    </div>

    <!-- Main Content -->
    <template v-else-if="conversation">
      <!-- Conversation Header -->
      <div class="conversation-header mb-1">
        <div class="d-flex align-center">
          <v-btn icon @click="goBack" class="mr-1" size="small" color="secondary" variant="text">
            <v-icon>mdi-arrow-left</v-icon>
          </v-btn>
          <span class="text-subtitle-1 font-weight-medium">Chi tiết hội thoại</span>
          <v-spacer></v-spacer>
          <v-btn
            color="primary"
            prepend-icon="mdi-pencil"
            variant="text"
            size="small"
            @click="goToEdit"
          >
            Chỉnh sửa
          </v-btn>
        </div>
      </div>

      <!-- Conversation Card -->
      <v-card class="main-conversation-card mb-6" variant="elevated">
        <v-divider></v-divider>

        <!-- Conversation Details Section -->
        <v-card-text class="py-3 px-3">
          <!-- Thiết kế gọn hơn cho phần chi tiết thông tin -->
          <v-card class="mb-3" variant="outlined" density="compact">
            <v-list lines="two" density="compact" class="py-0">
              <!-- Tiêu đề trên một hàng riêng -->
              <v-list-item density="compact" class="py-1">
                <template v-slot:prepend>
                  <v-icon color="primary" size="small" class="mr-1">mdi-format-title</v-icon>
                </template>
                <v-list-item-title class="text-body-2 font-weight-medium">Tiêu đề</v-list-item-title>
                <v-list-item-subtitle class="text-caption">{{ conversation.title }}</v-list-item-subtitle>
              </v-list-item>

              <v-divider inset></v-divider>

              <!-- Mô tả trên một hàng riêng -->
              <v-list-item density="compact" class="py-1">
                <template v-slot:prepend>
                  <v-icon color="primary" size="small" class="mr-1">mdi-text-box-outline</v-icon>
                </template>
                <v-list-item-title class="text-body-2 font-weight-medium">Mô tả</v-list-item-title>
                <v-list-item-subtitle class="text-caption">{{ conversation.description }}</v-list-item-subtitle>
              </v-list-item>

              <v-divider inset></v-divider>

              <!-- Các thông tin khác gộp trên một hàng -->
              <v-row class="ma-0 pa-0">
                <v-col cols="3" class="pa-0">
                  <v-list-item density="compact" class="py-1">
                    <template v-slot:prepend>
                      <v-icon color="primary" size="small" class="mr-1">mdi-certificate-outline</v-icon>
                    </template>
                    <v-list-item-title class="text-body-2 font-weight-medium">JLPT</v-list-item-title>
                    <v-list-item-subtitle>
                      <v-chip :color="getJlptColor(conversation.jlptLevel)" size="x-small" class="mt-1">{{ conversation.jlptLevel }}</v-chip>
                    </v-list-item-subtitle>
                  </v-list-item>
                </v-col>

                <v-col cols="3" class="pa-0">
                  <v-list-item density="compact" class="py-1">
                    <template v-slot:prepend>
                      <v-icon color="primary" size="small" class="mr-1">mdi-book-open-variant</v-icon>
                    </template>
                    <v-list-item-title class="text-body-2 font-weight-medium">Bài học</v-list-item-title>
                    <v-list-item-subtitle class="text-caption">{{ conversation.unit }}</v-list-item-subtitle>
                  </v-list-item>
                </v-col>

                <v-col cols="3" class="pa-0">
                  <v-list-item density="compact" class="py-1">
                    <template v-slot:prepend>
                      <v-icon color="primary" size="small" class="mr-1">mdi-chat-outline</v-icon>
                    </template>
                    <v-list-item-title class="text-body-2 font-weight-medium">Số dòng</v-list-item-title>
                    <v-list-item-subtitle class="text-caption">{{ conversation.lines?.length || 0 }}</v-list-item-subtitle>
                  </v-list-item>
                </v-col>

                <v-col cols="3" class="pa-0">
                  <v-list-item density="compact" class="py-1">
                    <template v-slot:prepend>
                      <v-icon color="primary" size="small" class="mr-1">mdi-clock-outline</v-icon>
                    </template>
                    <v-list-item-title class="text-body-2 font-weight-medium">Cập nhật</v-list-item-title>
                    <v-list-item-subtitle class="text-caption">{{ formatDate(conversation.updatedAt) }}</v-list-item-subtitle>
                  </v-list-item>
                </v-col>
              </v-row>
            </v-list>
          </v-card>

          <!-- Conversation Content -->
          <div class="d-flex mb-2">
            <v-chip color="primary" label size="small">
              <v-icon start size="small">mdi-chat-outline</v-icon>
              Nội dung hội thoại
            </v-chip>
          </div>

          <template v-if="!conversation.lines || conversation.lines.length === 0">
            <div class="text-center pa-6 bg-grey-lighten-4 rounded-lg">
              <v-icon icon="mdi-chat-outline" size="large" class="mb-2 text-grey"></v-icon>
              <div class="text-body-2 text-grey-darken-1">Hội thoại này chưa có nội dung.</div>
            </div>
          </template>

          <template v-else>
            <!-- Chat Messages Container -->
            <div class="chat-container">
              <template v-for="(line, i) in sortedLines" :key="line.lineId || i">
                <div
                  class="message-row mb-2"
                  :class="[
                    line.speaker === 'user' ? 'justify-end' : 'justify-start'
                  ]"
                >
                  <!-- Avatar for Nihongo IT -->
                  <div v-if="line.speaker !== 'user'" class="avatar-container mr-2">
                    <v-avatar size="36" color="info">
                      <v-icon color="white">mdi-robot</v-icon>
                    </v-avatar>
                  </div>

                  <!-- Message Bubble -->
                  <div
                    class="message-container"
                    :class="{
                      'user-message': line.speaker === 'user',
                      'bot-message': line.speaker !== 'user'
                    }"
                    :style="{
                      maxWidth: '80%',
                      minWidth: '270px'
                    }"
                  >
                    <!-- Message Content -->
                    <div class="message-content pa-2">
                      <div class="d-flex justify-space-between align-center mb-1">
                        <div class="japanese-text text-subtitle-1 font-weight-medium word-break-wrap">
                          {{ line.japaneseText }}
                        </div>
                        <!-- Audio Button - Dummy for design -->
                        <v-btn
                          icon
                          disabled
                          density="comfortable"
                          size="x-small"
                          color="info"
                          variant="text"
                          title="Nghe phát âm mẫu"
                          class="ms-2 flex-shrink-0"
                        >
                          <v-icon size="small">mdi-volume-high</v-icon>
                        </v-btn>
                      </div>
                      <div class="text-body-2 text-medium-emphasis word-break-wrap">
                        {{ line.vietnameseTranslation }}
                      </div>

                      <!-- Notes and Vocabulary -->
                      <div v-if="line.notes" class="mt-2 text-caption notes-section word-break-wrap">
                        <span class="font-weight-medium">Ghi chú:</span> {{ line.notes }}
                      </div>

                      <div v-if="line.importantVocab" class="mt-2 vocab-section">
                        <div class="text-caption font-weight-medium mb-1">Từ vựng quan trọng:</div>
                        <div class="d-flex flex-wrap">
                          <v-chip
                            v-for="(vocab, idx) in line.importantVocab.split(',')"
                            :key="idx"
                            size="x-small"
                            :color="line.speaker === 'user' ? 'primary' : 'info'"
                            variant="outlined"
                            class="mr-1 mb-1"
                          >
                            {{ vocab.trim() }}
                          </v-chip>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Avatar for User -->
                  <div v-if="line.speaker === 'user'" class="avatar-container ml-2">
                    <v-avatar size="36" color="primary">
                      <v-icon>mdi-account</v-icon>
                    </v-avatar>
                  </div>
                </div>
              </template>
            </div>
          </template>
        </v-card-text>
      </v-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import conversationService from '@/services/conversation.service';
import type { Conversation } from '@/services/conversation.service';
import { format, parseISO } from 'date-fns';
import { vi } from 'date-fns/locale';

// Utilities
const route = useRoute();
const router = useRouter();

// Data
const conversation = ref<Conversation | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);

// Computed
const sortedLines = computed(() => {
  if (!conversation.value || !conversation.value.lines) return [];
  return [...conversation.value.lines].sort((a, b) => a.orderIndex - b.orderIndex);
});

// Lifecycle hooks
onMounted(async () => {
  const conversationId = route.params.id as string;
  if (conversationId) {
    await fetchConversation(conversationId);
  } else {
    error.value = 'Không tìm thấy ID hội thoại';
    router.push({ name: 'adminConversations' });
  }
});

// Methods
const fetchConversation = async (id: string) => {
  loading.value = true;
  error.value = null;

  try {
    const response = await conversationService.adminGetConversationById(id);
    conversation.value = response.data;
  } catch (err) {
    error.value = 'Không thể tải dữ liệu hội thoại. Vui lòng thử lại sau.';
  } finally {
    loading.value = false;
  }
};

const formatDate = (dateString?: string) => {
  if (!dateString) return 'N/A';
  try {
    return format(parseISO(dateString), 'dd/MM/yyyy HH:mm', { locale: vi });
  } catch (error) {
    return dateString;
  }
};

const getJlptColor = (level: string | undefined): string => {
  if (!level) return 'grey';
  const colors: Record<string, string> = {
    'N1': 'red',
    'N2': 'orange',
    'N3': 'amber',
    'N4': 'light-green',
    'N5': 'green'
  }
  return colors[level] || 'grey'
}

const getSpeakerName = (speaker: string) => {
  return speaker === 'bot' ? 'Nihongo IT' : 'Người dùng';
};

const goBack = () => {
  router.push({ name: 'adminConversations' });
};

const goToEdit = () => {
  if (conversation.value?.conversationId) {
    router.push(`/admin/conversations/${conversation.value.conversationId}/edit`);
  }
};
</script>

<style scoped lang="scss">
.conversation-detail-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 16px;

  .loading-container,
  .error-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 60vh;
  }

  .details-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 16px;
    margin-bottom: 24px;
  }

  .detail-item {
    display: flex;
    flex-direction: column;
    padding: 8px;
    background-color: rgba(0, 0, 0, 0.02);
    border-radius: 8px;
  }

  .detail-label {
    font-size: 0.85rem;
    font-weight: 500;
    color: var(--v-medium-emphasis-color);
    margin-bottom: 4px;
  }

  .detail-value {
    font-size: 0.95rem;
  }

  .japanese-text {
    font-family: 'Noto Sans JP', sans-serif;
  }

  .chat-container {
    display: flex;
    flex-direction: column;
    padding: 4px 0;
    min-height: 50vh;
    position: relative;
    overflow: visible;
    margin-bottom: 16px;
  }

  .message-row {
    display: flex;
    align-items: flex-start;
    width: 100%;
    margin-bottom: 16px;
  }

  .message-container {
    position: relative;
    border-radius: 18px;
    transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    min-width: 270px;

    &:hover {
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
      transform: translateY(-2px);
    }

    &.user-message {
      background-color: #E3F2FD; // Màu xanh nhạt cho tin nhắn người dùng
      border-top-right-radius: 4px;
      align-self: flex-end;
    }

    &.bot-message {
      background-color: #E8F5E9; // Màu xanh lá nhạt cho tin nhắn của Nihongo IT
      border-top-left-radius: 4px;
      align-self: flex-start;
    }
  }

  .notes-section {
    background-color: rgba(0, 0, 0, 0.03);
    padding: 4px 8px;
    border-radius: 4px;
    font-style: italic;
  }

  .vocab-section {
    padding: 4px 0;
  }

  .word-break-wrap {
    overflow-wrap: break-word;
    word-wrap: break-word;
    word-break: break-word;
    hyphens: auto;
    white-space: normal;
    max-width: 100%;
  }
}
</style>
