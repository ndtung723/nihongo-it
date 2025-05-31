<template>
  <div class="conversation-edit-container">
    <!-- Header -->
    <div class="conversation-header mb-1">
      <div class="d-flex align-center">
        <v-btn icon @click="goBack" class="mr-1" size="small" color="secondary" variant="text">
          <v-icon>mdi-arrow-left</v-icon>
        </v-btn>
        <span class="text-subtitle-1 font-weight-medium">Chỉnh sửa hội thoại</span>
        <v-spacer></v-spacer>
        <v-btn color="success" prepend-icon="mdi-content-save" @click="saveConversation" :loading="saving"
          :disabled="!dirty" size="small">
          Lưu thay đổi
        </v-btn>
      </div>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="loading-container">
      <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
      <span class="mt-4 text-body-1">Đang tải dữ liệu hội thoại...</span>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="error-container">
      <v-alert type="error" class="mb-4">{{ error }}</v-alert>
      <v-btn @click="fetchConversation" color="primary" variant="outlined">
        <v-icon start>mdi-refresh</v-icon>
        Thử lại
      </v-btn>
    </div>

    <!-- Main Content -->
    <template v-else-if="conversation">
      <!-- Main Conversation Container -->
      <v-card class="main-conversation-container" variant="elevated" color="#fff">
        <!-- Conversation Info -->
        <v-card-text class="py-4 px-4" variant="outlined">
          <!-- Thông tin hội thoại -->

          <v-card variant="outlined">
            <v-list lines="two" density="compact" class="py-0">
              <!-- Tiêu đề trên một hàng riêng -->

              <v-list-item density="compact" class="py-1">
                <template v-slot:prepend>
                  <v-icon color="primary" size="small" class="mr-1">mdi-format-title</v-icon>
                </template>
                <v-list-item-title class="text-body-2 font-weight-medium">Tiêu đề</v-list-item-title>
                <v-list-item-subtitle>
                  <v-text-field
                    v-model="conversation.title"
                    hide-details="auto"
                    density="compact"
                    variant="outlined"
                    placeholder="Nhập tiêu đề hội thoại"
                    class="mt-1"
                    @input="markAsDirty"
                    bg-color="white"
                  ></v-text-field>
                </v-list-item-subtitle>
              </v-list-item>


              <v-divider inset></v-divider>

              <!-- Mô tả trên một hàng riêng -->
              <v-list-item density="compact" class="py-1">
                <template v-slot:prepend>
                  <v-icon color="primary" size="small" class="mr-1">mdi-text-box-outline</v-icon>
                </template>
                <v-list-item-title class="text-body-2 font-weight-medium">Mô tả</v-list-item-title>
                <v-list-item-subtitle>
                  <v-textarea
                    v-model="conversation.description"
                    hide-details="auto"
                    density="compact"
                    variant="outlined"
                    placeholder="Nhập mô tả hội thoại"
                    class="mt-1"
                    @input="markAsDirty"
                    rows="1"
                    auto-grow
                    bg-color="white"
                  ></v-textarea>
                </v-list-item-subtitle>
              </v-list-item>

              <v-divider inset></v-divider>

              <!-- Các thông tin khác gộp trên một hàng -->
              <v-row class="ma-0 pa-0" align="center">
                <v-col cols="3" class="pa-0">
                  <v-list-item density="compact" class="py-1 d-flex align-center">
                    <template v-slot:prepend>
                      <v-icon color="primary" size="small" class="mr-1">mdi-certificate-outline</v-icon>
                    </template>
                    <v-list-item-title class="text-body-2 font-weight-medium d-flex align-center">JLPT</v-list-item-title>
                    <v-list-item-subtitle>
                      <v-select
                        v-model="conversation.jlptLevel"
                        :items="jlptLevels"
                        hide-details="auto"
                        density="compact"
                        variant="outlined"
                        class="mt-1"
                        @update:model-value="markAsDirty"
                        bg-color="white"
                      ></v-select>
                    </v-list-item-subtitle>
                  </v-list-item>
                </v-col>

                <v-col cols="3" class="pa-0">
                  <v-list-item density="compact" class="py-1 d-flex align-center">
                    <template v-slot:prepend>
                      <v-icon color="primary" size="small" class="mr-1">mdi-book-open-variant</v-icon>
                    </template>
                    <v-list-item-title class="text-body-2 font-weight-medium d-flex align-center">Bài học</v-list-item-title>
                    <v-list-item-subtitle>
                      <v-text-field
                        v-model.number="conversation.unit"
                        hide-details="auto"
                        density="compact"
                        variant="outlined"
                        type="number"
                        min="1"
                        style="max-width: 80px;"
                        class="mt-1"
                        @input="markAsDirty"
                        bg-color="white"
                      ></v-text-field>
                    </v-list-item-subtitle>
                  </v-list-item>
                </v-col>

                <v-col cols="3" class="pa-0">
                  <v-list-item density="compact" class="py-1 d-flex align-center">
                    <template v-slot:prepend>
                      <v-icon color="primary" size="small" class="mr-1">mdi-chat-outline</v-icon>
                    </template>
                    <v-list-item-title class="text-body-2 font-weight-medium d-flex align-center">Số dòng</v-list-item-title>
                    <v-list-item-subtitle class="text-caption d-flex align-center">{{ conversation.lines?.length || 0
                      }}</v-list-item-subtitle>
                  </v-list-item>
                </v-col>

                <v-col cols="3" class="pa-0">
                  <v-list-item density="compact" class="py-1 d-flex align-center">
                    <template v-slot:prepend>
                      <v-icon color="primary" size="small" class="mr-1">mdi-drag-variant</v-icon>
                    </template>
                    <v-list-item-title class="text-body-2 font-weight-medium d-flex align-center">Sắp xếp</v-list-item-title>
                    <v-list-item-subtitle class="text-caption d-flex align-center">Kéo thả để sắp xếp</v-list-item-subtitle>
                  </v-list-item>
                </v-col>
              </v-row>
            </v-list>
          </v-card>


          <!-- Divider -->
          <v-divider class="mb-4"></v-divider>

          <!-- Conversation Lines Section -->
          <div>
            <div class="d-flex align-center mb-4">
              <v-chip color="primary" label size="small">
                <v-icon start size="small">mdi-chat-outline</v-icon>
                Nội dung hội thoại
              </v-chip>
              <v-spacer></v-spacer>
              <v-btn color="primary" prepend-icon="mdi-plus" @click="addLine" size="small" variant="text">
                Thêm dòng
              </v-btn>
            </div>

            <!-- Empty state -->
            <div v-if="!conversation.lines || conversation.lines.length === 0"
              class="text-center pa-4 bg-grey-lighten-4 rounded-lg mb-4">
              <v-icon icon="mdi-chat-outline" size="large" class="mb-2 text-grey"></v-icon>
              <div class="text-body-2 text-grey-darken-1">Chưa có dòng hội thoại nào. Hãy thêm dòng hội thoại đầu tiên.
              </div>
            </div>

            <!-- Lines list -->
            <draggable v-else v-model="conversation.lines" handle=".drag-handle" item-key="tempId"
              class="chat-container" @change="onLinesReordered">
              <template #item="{ element, index }">
                <div class="message-row mb-4" :class="element.speaker === 'user' ? 'justify-end' : 'justify-start'"
                  :data-temp-id="element.tempId">
                  <!-- Avatar for Nihongo IT -->
                  <div v-if="element.speaker !== 'user'" class="avatar-container mr-2">
                    <v-avatar size="36" color="info" class="drag-handle cursor-move">
                      <v-icon color="white">mdi-robot</v-icon>
                    </v-avatar>
                  </div>

                  <!-- Message Bubble -->
                  <div class="message-container" :class="{
                    'user-message': element.speaker === 'user',
                    'bot-message': element.speaker !== 'user'
                  }">
                    <!-- Message Header Controls -->
                    <div class="message-header px-2 pt-1 d-flex align-center">
                      <div class="line-number me-2 text-caption">#{{ index + 1 }}</div>
                      <v-chip size="x-small" :color="element.speaker === 'user' ? 'primary' : 'info'" variant="tonal">
                        {{ getSpeakerName(element.speaker) }}
                      </v-chip>
                      <v-spacer></v-spacer>
                      <div class="drag-controls d-flex align-center me-2">
                        <v-btn size="x-small" icon variant="text" color="grey-darken-1"
                          class="drag-handle cursor-move mr-2" title="Di chuyển lên" :disabled="index === 0"
                          @click.stop="moveLineUp(index)">
                          <v-icon size="small">mdi-arrow-up</v-icon>
                        </v-btn>
                        <v-btn size="x-small" icon variant="text" color="grey-darken-1" class="drag-handle cursor-move"
                          title="Di chuyển xuống" :disabled="index === conversation.lines.length - 1"
                          @click.stop="moveLineDown(index)">
                          <v-icon size="small">mdi-arrow-down</v-icon>
                        </v-btn>
                      </div>
                      <v-btn icon size="x-small" color="error" variant="text" @click="confirmDeleteLine(index)"
                        class="ml-1">
                        <v-icon size="small">mdi-delete</v-icon>
                      </v-btn>
                      <v-btn icon size="x-small" :color="expandedLines[element.tempId] ? 'primary' : 'default'"
                        variant="text" @click="toggleExpand(element.tempId)" class="ml-1">
                        <v-icon size="small">{{ expandedLines[element.tempId] ? 'mdi-chevron-up' : 'mdi-chevron-down'
                          }}</v-icon>
                      </v-btn>
                    </div>

                    <!-- Message Preview (when collapsed) -->
                    <div v-if="!expandedLines[element.tempId]" class="message-preview px-2 pb-2">
                      <div class="japanese-text text-body-1 font-weight-medium word-break-wrap">
                        {{ element.japaneseText || 'Chưa có nội dung' }}
                      </div>
                      <div class="text-body-2 text-medium-emphasis word-break-wrap">
                        {{ element.vietnameseTranslation || 'Chưa có bản dịch' }}
                      </div>
                    </div>

                    <!-- Message Edit Form (when expanded) -->
                    <v-expand-transition>
                      <div v-if="expandedLines[element.tempId]" class="message-edit pa-2">
                        <v-textarea v-model="element.japaneseText" label="Đoạn hội thoại (tiếng Nhật)" rows="2"
                          auto-grow hide-details="auto" density="compact" variant="outlined" bg-color="white"
                          class="mb-2" :rules="[v => !!v || 'Đoạn hội thoại tiếng Nhật là bắt buộc']" required
                          @input="markAsDirty"></v-textarea>

                        <v-textarea v-model="element.vietnameseTranslation" label="Bản dịch (tiếng Việt)" rows="2"
                          auto-grow hide-details="auto" density="compact" variant="outlined" bg-color="white"
                          class="mb-2" :rules="[v => !!v || 'Bản dịch tiếng Việt là bắt buộc']" required
                          @input="markAsDirty"></v-textarea>

                        <v-textarea v-model="element.notes" label="Ghi chú" rows="2" auto-grow hide-details="auto"
                          density="compact" variant="outlined" bg-color="white" class="mb-2"
                          @input="markAsDirty"></v-textarea>

                        <v-text-field v-model="element.importantVocab" label="Từ vựng quan trọng"
                          hint="Các từ vựng phân cách bởi dấu phẩy" hide-details="auto" density="compact"
                          variant="outlined" bg-color="white" style="min-height: 50px;"
                          @input="markAsDirty"></v-text-field>
                      </div>
                    </v-expand-transition>
                  </div>

                  <!-- Avatar for User with profile picture -->
                  <div v-if="element.speaker === 'user'" class="avatar-container ml-2">
                    <v-avatar size="36" color="primary" class="drag-handle cursor-move">
                      <v-img v-if="userProfilePicture" :src="userProfilePicture" alt="Ảnh đại diện của bạn"></v-img>
                      <span v-else class="text-subtitle-2 text-white">{{ avatarInitials }}</span>
                    </v-avatar>
                  </div>
                </div>
              </template>
            </draggable>
          </div>
        </v-card-text>
      </v-card>
    </template>

    <!-- Delete Line Confirmation Dialog -->
    <v-dialog v-model="deleteLineDialog" max-width="500px">
      <v-card>
        <v-card-title class="text-h5 bg-error text-white">
          Xóa dòng hội thoại
        </v-card-title>
        <v-card-text class="pt-4">
          <p>Bạn có chắc chắn muốn xóa dòng hội thoại này không?</p>
          <div class="mt-2 pa-2 bg-grey-lighten-4 rounded">
            {{ lineToDelete !== null && conversation?.lines && conversation.lines[lineToDelete]?.japaneseText }}
          </div>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="grey" variant="text" @click="deleteLineDialog = false">
            Hủy
          </v-btn>
          <v-btn color="error" variant="elevated" @click="deleteLine">
            Xóa
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Unsaved Changes Dialog -->
    <v-dialog v-model="unsavedChangesDialog" max-width="500px">
      <v-card>
        <v-card-title class="text-h5 bg-warning">
          Thay đổi chưa được lưu
        </v-card-title>
        <v-card-text class="pt-4">
          <p>Bạn có thay đổi chưa được lưu. Bạn muốn làm gì?</p>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="grey" variant="text" @click="discardChanges">
            Hủy thay đổi
          </v-btn>
          <v-btn color="primary" variant="elevated" @click="saveAndNavigate">
            Lưu và tiếp tục
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useToast } from 'vue-toast-notification';
import conversationService from '@/services/conversation.service';
import type { Conversation, ConversationLine } from '@/services/conversation.service';
import draggable from 'vuedraggable';
import { useAuthStore } from '@/stores';

// Utilities
const route = useRoute();
const router = useRouter();
const toast = useToast();
const authStore = useAuthStore();

// Data
const conversation = ref<Conversation | null>(null);
const originalConversation = ref<string>('');
const loading = ref(false);
const saving = ref(false);
const error = ref<string | null>(null);
const dirty = ref(false);
const expandedLines = ref<Record<string, boolean>>({});
const deleteLineDialog = ref(false);
const lineToDelete = ref<number | null>(null);
const unsavedChangesDialog = ref(false);
const pendingNavigation = ref<string | null>(null);
const jlptLevels = ref(['N1', 'N2', 'N3', 'N4', 'N5']);

// Computed cho thông tin user
const userProfilePicture = computed(() => authStore.user?.profilePicture || null)
const avatarInitials = computed(() => {
  if (!authStore.user?.fullName) return 'U'
  return authStore.user.fullName
    .split(' ')
    .map(name => name.charAt(0))
    .join('')
    .toUpperCase()
})

// Lifecycle hooks
onMounted(async () => {
  const conversationId = route.params.id as string;
  if (conversationId) {
    await fetchConversation(conversationId);
  } else {
    error.value = 'Không tìm thấy ID hội thoại';
    router.push({ name: 'admin-conversations' });
  }

  // Add event listener for beforeunload
  window.addEventListener('beforeunload', handleBeforeUnload);
});

onBeforeUnmount(() => {
  // Remove event listener
  window.removeEventListener('beforeunload', handleBeforeUnload);
});

// Methods
const fetchConversation = async (id: string) => {
  loading.value = true;
  error.value = null;

  try {
    const response = await conversationService.adminGetConversationById(id);
    const conversationData = response.data;

    if (conversationData.lines) {
      // Sort lines by orderIndex
      conversationData.lines.sort((a, b) => a.orderIndex - b.orderIndex);

      // Add tempId for draggable
      conversationData.lines.forEach((line, idx) => {
        line.tempId = line.lineId || `temp-${idx}-${Date.now()}`;
      });

      // Expand first line by default
      if (conversationData.lines.length > 0) {
        expandedLines.value[conversationData.lines[0].tempId as string] = true;
      }
    }

    conversation.value = conversationData;
    originalConversation.value = JSON.stringify(conversationData);
    dirty.value = false;
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Không thể tải dữ liệu hội thoại. Vui lòng thử lại sau.';
  } finally {
    loading.value = false;
  }
};

const saveConversation = async () => {
  if (!conversation.value || !conversation.value.conversationId) return;

  saving.value = true;

  try {
    // Prepare data for saving: update orderIndex for all lines
    updateOrderIndices();

    // Remove tempId before saving
    const conversationToSave = JSON.parse(JSON.stringify(conversation.value));
    if (conversationToSave.lines) {
      conversationToSave.lines.forEach((line: any) => {
        delete line.tempId;
      });
    }

    await conversationService.adminUpdateConversation(
      conversationToSave.conversationId,
      conversationToSave
    );

    toast.success('Hội thoại đã được cập nhật thành công');
    originalConversation.value = JSON.stringify(conversationToSave);
    dirty.value = false;

    // If there's a pending navigation, do it now
    if (pendingNavigation.value) {
      router.push(pendingNavigation.value);
      pendingNavigation.value = null;
    }
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Có lỗi xảy ra khi lưu hội thoại';
    toast.error(error.value || 'Có lỗi xảy ra khi lưu hội thoại');
  } finally {
    saving.value = false;
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

const addLine = () => {
  if (!conversation.value) return;

  if (!conversation.value.lines) {
    conversation.value.lines = [];
  }

  const orderIndex = conversation.value.lines.length > 0
    ? Math.max(...conversation.value.lines.map(line => line.orderIndex)) + 1
    : 1;

  // Xác định speaker dựa trên orderIndex
  const speaker = orderIndex % 2 === 1 ? 'bot' : 'user';

  const newLine: ConversationLine & { tempId?: string } = {
    speaker,
    japaneseText: '',
    vietnameseTranslation: '',
    notes: '',
    importantVocab: '',
    orderIndex,
    tempId: `temp-${Date.now()}`
  };

  conversation.value.lines.push(newLine);

  // Expand the new line
  expandedLines.value[newLine.tempId as string] = true;

  // Collapse other lines
  for (const id in expandedLines.value) {
    if (id !== newLine.tempId) {
      expandedLines.value[id] = false;
    }
  }

  markAsDirty();

  // Thêm hiệu ứng cuộn đến dòng mới sau khi DOM cập nhật
  setTimeout(() => {
    scrollToElement(`.message-row:last-child`);
  }, 50);
};

const confirmDeleteLine = (index: number) => {
  lineToDelete.value = index;
  deleteLineDialog.value = true;
};

const deleteLine = () => {
  if (lineToDelete.value === null || !conversation.value || !conversation.value.lines) return;

  const deletedLine = conversation.value.lines[lineToDelete.value];
  conversation.value.lines.splice(lineToDelete.value, 1);

  // Remove expanded status
  if (deletedLine.tempId) {
    delete expandedLines.value[deletedLine.tempId];
  }

  // Update orderIndex for remaining lines
  updateOrderIndices();

  deleteLineDialog.value = false;
  lineToDelete.value = null;

  markAsDirty();
};

const toggleExpand = (tempId: string) => {
  expandedLines.value[tempId] = !expandedLines.value[tempId];

  // Nếu đang mở, cuộn đến phần tử đó sau khi hiệu ứng expand hoàn tất
  if (expandedLines.value[tempId]) {
    setTimeout(() => {
      const element = document.querySelector(`[data-temp-id="${tempId}"]`);
      if (element) {
        scrollToElement(`[data-temp-id="${tempId}"]`);
      }
    }, 300);
  }
};

const updateOrderIndices = () => {
  if (!conversation.value || !conversation.value.lines) return;

  conversation.value.lines.forEach((line, idx) => {
    const newOrderIndex = idx + 1;
    line.orderIndex = newOrderIndex;
    // Update speaker based on orderIndex
    line.speaker = newOrderIndex % 2 === 1 ? 'bot' : 'user';
  });
};

const onLinesReordered = () => {
  updateOrderIndices();
  markAsDirty();
};

const markAsDirty = () => {
  dirty.value = true;
};

const goBack = () => {
  if (dirty.value) {
    pendingNavigation.value = '/admin/conversations';
    unsavedChangesDialog.value = true;
  } else {
    router.push({ name: 'admin-conversations' });
  }
};

const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  if (dirty.value) {
    // Standard browsers
    e.preventDefault();
    // Some older browsers
    e.returnValue = '';
    return '';
  }
};

const discardChanges = () => {
  dirty.value = false;
  unsavedChangesDialog.value = false;

  if (pendingNavigation.value) {
    router.push(pendingNavigation.value);
    pendingNavigation.value = null;
  }
};

const saveAndNavigate = async () => {
  unsavedChangesDialog.value = false;
  await saveConversation();
};

const moveLineUp = (index: number) => {
  if (!conversation.value || !conversation.value.lines || index <= 0) return;

  const line = conversation.value.lines[index];
  conversation.value.lines.splice(index, 1);
  conversation.value.lines.splice(index - 1, 0, line);
  updateOrderIndices();
  markAsDirty();
};

const moveLineDown = (index: number) => {
  if (!conversation.value || !conversation.value.lines || index >= conversation.value.lines.length - 1) return;

  const line = conversation.value.lines[index];
  conversation.value.lines.splice(index, 1);
  conversation.value.lines.splice(index + 1, 0, line);
  updateOrderIndices();
  markAsDirty();
};

// Thêm hiệu ứng cuộn mượt
const scrollToElement = (selector: string) => {
  const element = document.querySelector(selector);
  if (!element) return;

  const rect = element.getBoundingClientRect();
  const targetPosition = window.scrollY + rect.top - 150;

  // Sử dụng smooth scrolling
  window.scrollTo({
    top: targetPosition,
    behavior: 'smooth'
  });
};
</script>

<style scoped lang="scss">
.conversation-edit-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 16px;

  .main-conversation-container {
    border-radius: 8px;
    overflow: hidden;
  }

  .loading-container,
  .error-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 60vh;
  }

  versation-edit-container .japanese-text {
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
    will-change: contents;
  }

  .message-row {
    display: flex;
    align-items: flex-start;
    width: 100%;
    margin-bottom: 16px;
    animation: fadeInUp 0.4s cubic-bezier(0.25, 0.1, 0.25, 1) forwards;
    will-change: transform, opacity;
    transform: translateZ(0);
  }

  @keyframes fadeInUp {
    from {
      opacity: 0;
      transform: translate3d(0, 20px, 0);
    }

    to {
      opacity: 1;
      transform: translate3d(0, 0, 0);
    }
  }

  .message-container {
    position: relative;
    border-radius: 18px;
    transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    width: 300px;
    min-width: 390px;
    max-width: 600px;
    width: fit-content;
    transform-origin: center center;
    will-change: transform, box-shadow;
    backface-visibility: hidden;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      transform: translateY(-2px) translateZ(0);
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

  .message-header {
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    background-color: rgba(0, 0, 0, 0.02);
    border-radius: 18px 18px 0 0;
    transition: background-color 0.2s ease;

    &:hover {
      background-color: rgba(0, 0, 0, 0.05);
    }
  }

  .message-preview,
  .message-edit {
    padding: 8px 12px;
  }

  .message-edit {
    width: 100%;

    :deep(.v-field__input) {
      min-height: unset;
      padding-top: 4px;
      padding-bottom: 4px;
    }

    :deep(.v-textarea__input) {
      min-height: 24px;
      line-height: 1.2;
      transition: all 0.2s ease;
    }

    :deep(.v-field--variant-outlined .v-field__outline) {
      margin-bottom: 0;
      transition: border-color 0.2s ease;
    }

    :deep(.v-field) {
      transition: all 0.2s ease;

      &:focus-within {
        transform: translateY(-1px);
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
      }
    }
  }

  .cursor-move {
    cursor: move;
  }

  .line-number {
    font-weight: bold;
    min-width: 24px;
    font-size: 0.85rem;
  }

  .word-break-wrap {
    overflow-wrap: break-word;
    word-wrap: break-word;
    word-break: break-word;
    hyphens: auto;
    white-space: normal;
    max-width: 100%;
  }

  .drag-controls {
    background-color: rgba(0, 0, 0, 0.04);
    padding: 2px 6px;
    border-radius: 12px;
    transition: all 0.2s ease;

    &:hover {
      background-color: rgba(0, 0, 0, 0.08);
    }

    .v-btn {
      transition: all 0.2s cubic-bezier(0.25, 0.8, 0.5, 1);

      &:hover {
        transform: translateY(-1px);
        background-color: rgba(0, 0, 0, 0.05);
      }

      &:active {
        transform: translateY(0);
      }
    }
  }

  .avatar-container {
    .v-avatar {
      transition: transform 0.2s ease, box-shadow 0.2s ease;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

      &:hover {
        transform: scale(1.05);
        box-shadow: 0 3px 8px rgba(0, 0, 0, 0.2);
      }
    }
  }
}

// Sửa cách viết transition để tránh lỗi với :deep
.v-expand-transition {

  &:deep(.v-expand-transition-enter-active),
  &:deep(.v-expand-transition-leave-active) {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
    overflow: hidden;
  }

  &:deep(.v-expand-transition-enter-from),
  &:deep(.v-expand-transition-leave-to) {
    opacity: 0;
    transform: translateY(-5px);
  }
}

// Sửa các selector cho sortable
.sortable-ghost:deep() {
  opacity: 0.5;
  background-color: #f5f5f5;
  border-radius: 18px;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1) !important;

  * {
    opacity: 0.5;
  }
}

.sortable-drag:deep() {
  opacity: 1 !important;
  z-index: 10;
  transform: rotate(1deg) !important;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1) !important;
}
</style>
