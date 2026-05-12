<template>
  <v-container>
    <v-row class="mb-4">
      <v-col cols="12">
        <div class="d-flex align-center">
          <h1 class="text-h5 font-weight-bold mr-4">Quản lý hội thoại</h1>
          <v-spacer></v-spacer>
          <v-text-field
            v-model="searchQuery"
            prepend-inner-icon="mdi-magnify"
            label="Tìm kiếm hội thoại"
            single-line
            hide-details
            class="mr-2"
            density="compact"
            style="max-width: 333px"
            @input="debouncedSearch"
          ></v-text-field>
          <v-select
            v-model="selectedJlptLevel"
            :items="jlptLevels"
            item-title="text"
            item-value="value"
            label="Lọc theo trình độ JLPT"
            class="mr-2"
            density="compact"
            style="max-width: 200px"
            hide-details
            @update:model-value="fetchConversationsByLevel"
            clearable
          >
          </v-select>
          <v-btn color="primary" prepend-icon="mdi-plus" @click="openAddDialog">
            Thêm hội thoại mới
          </v-btn>
        </div>
      </v-col>
    </v-row>

    <v-card>
      <v-data-table
        :headers="headers"
        :items="conversations"
        :loading="loading"
        class="elevation-1"
        :items-per-page="10"
        :items-per-page-options="[10, 20, 50]"
      >
        <!-- Title column -->
        <template v-slot:item.title="{ item }">
          <div class="font-weight-bold">{{ item.title }}</div>
        </template>

        <!-- Description column -->
        <template v-slot:item.description="{ item }">
          <div class="text-truncate" style="max-width: 250px">
            {{ item.description }}
          </div>
        </template>

        <!-- JLPT Level column -->
        <template v-slot:item.jlptLevel="{ item }">
          <v-chip size="small" color="primary" variant="tonal">
            {{ item.jlptLevel }}
          </v-chip>
        </template>

        <!-- Unit column -->
        <template v-slot:item.unit="{ item }">
          {{ item.unit }}
        </template>

        <!-- Line count column -->
        <template v-slot:item.lineCount="{ item }">
          <v-chip size="small" color="info" variant="tonal">
            {{ item.lines?.length || 0 }} dòng
          </v-chip>
        </template>

        <!-- Updated at column -->
        <template v-slot:item.updatedAt="{ item }">
          {{ formatDate(item.updatedAt) }}
        </template>

        <!-- Actions column -->
        <template v-slot:item.actions="{ item }">
          <div class="d-flex">
            <v-btn
              icon
              size="small"
              color="info"
              variant="text"
              class="mr-1"
              @click="viewDetail(item)"
            >
              <v-icon>mdi-eye</v-icon>
              <v-tooltip activator="parent" location="top"
                >Xem chi tiết</v-tooltip
              >
            </v-btn>
            <v-btn
              icon
              size="small"
              color="primary"
              variant="text"
              class="mr-1"
              @click="openEditDialog(item)"
            >
              <v-icon>mdi-pencil</v-icon>
              <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
            </v-btn>
            <v-btn
              icon
              size="small"
              color="error"
              variant="text"
              @click="confirmDelete(item)"
            >
              <v-icon>mdi-delete</v-icon>
              <v-tooltip activator="parent" location="top">Xóa</v-tooltip>
            </v-btn>
          </div>
        </template>

        <!-- No data template -->
        <template v-slot:no-data>
          <div class="text-center pa-5">
            <v-icon
              size="large"
              icon="mdi-text-box-search-outline"
              class="mb-2"
            ></v-icon>
            <div v-if="error" class="text-body-1 text-error">{{ error }}</div>
            <div v-else-if="loading" class="text-body-1">
              Đang tải dữ liệu...
            </div>
            <div v-else class="text-body-1">Không tìm thấy hội thoại nào</div>
          </div>
        </template>
      </v-data-table>
    </v-card>

    <!-- Edit/Add Dialog -->
    <v-dialog v-model="dialog" max-width="800px" persistent scrollable>
      <v-card>
        <v-card-title class="text-h5 bg-primary text-white">
          {{ formTitle }}
        </v-card-title>
        <v-divider></v-divider>
        <v-card-text class="pt-4">
          <v-container>
            <v-form ref="form" v-model="valid">
              <v-row>
                <v-col cols="12">
                  <v-text-field
                    v-model="editedItem.title"
                    label="Tiêu đề hội thoại"
                    prepend-icon="mdi-format-title"
                    :rules="[(v) => !!v || 'Tiêu đề là bắt buộc']"
                    required
                    autofocus
                  ></v-text-field>
                </v-col>
                <v-col cols="12">
                  <v-textarea
                    v-model="editedItem.description"
                    label="Mô tả hội thoại"
                    prepend-icon="mdi-text-box-outline"
                    auto-grow
                    rows="2"
                  ></v-textarea>
                </v-col>
                <v-col cols="6">
                  <v-select
                    v-model="editedItem.jlptLevel"
                    :items="jlptLevels"
                    item-title="text"
                    item-value="value"
                    label="Trình độ JLPT"
                    prepend-icon="mdi-certificate-outline"
                    :rules="[(v) => !!v || 'Trình độ JLPT là bắt buộc']"
                    required
                  ></v-select>
                </v-col>
                <v-col cols="6">
                  <v-text-field
                    v-model.number="editedItem.unit"
                    label="Bài học (Unit)"
                    prepend-icon="mdi-book-open-variant"
                    type="number"
                    :rules="[(v) => !!v || 'Bài học là bắt buộc']"
                    required
                  ></v-text-field>
                </v-col>
              </v-row>
            </v-form>
          </v-container>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="grey" variant="text" @click="closeDialog">Hủy</v-btn>
          <v-btn
            color="primary"
            variant="elevated"
            @click="saveConversation"
            :disabled="!valid"
          >
            Lưu
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Delete confirmation dialog -->
    <v-dialog v-model="deleteDialog" max-width="500px" persistent>
      <v-card>
        <v-card-title class="text-h5 bg-error text-white">
          Xóa hội thoại
        </v-card-title>
        <v-card-text class="pt-4">
          <p class="text-body-1">
            Bạn có chắc chắn muốn xóa hội thoại "{{ deleteItem?.title }}"?
          </p>
          <div class="mt-2">
            Dữ liệu sẽ bị xóa vĩnh viễn và không thể khôi phục.
          </div>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="grey" variant="text" @click="deleteDialog = false"
            >Hủy</v-btn
          >
          <v-btn color="error" variant="elevated" @click="deleteConversation">
            Xóa
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useAppToast } from "@/composables/useAppToast";
import conversationService from "@/services/conversation.service";
import type { Conversation } from "@/services/conversation.service";
import { format, parseISO } from "date-fns";
import { vi } from "date-fns/locale";
import type { VForm } from "vuetify/components";

// Tạo global declaration cho lodash/debounce
const debounce = (fn: (...args: unknown[]) => unknown, delay: number) => {
  let timer: number | null = null;
  return function (this: unknown, ...args: unknown[]) {
    if (timer) clearTimeout(timer);
    timer = window.setTimeout(() => {
      fn.apply(this, args);
      timer = null;
    }, delay);
  };
};

// Khai báo toast và router
const router = useRouter();
const toast = useAppToast();

// Data
const conversations = ref<Conversation[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const valid = ref(false);
const dialog = ref(false);
const deleteDialog = ref(false);
const form = ref<unknown>(null);
const searchQuery = ref("");
const selectedJlptLevel = ref<string | null>(null);
const jlptLevels = conversationService.getJlptLevels();
const expandedLines = ref<Record<string, boolean>>({});

// Table headers
const headers = ref([
  { title: "Tiêu đề", key: "title", sortable: true },
  { title: "Mô tả", key: "description", sortable: false },
  { title: "Trình độ JLPT", key: "jlptLevel", sortable: true },
  { title: "Bài học", key: "unit", sortable: true },
  { title: "Số dòng", key: "lineCount", sortable: false },
  { title: "Cập nhật lúc", key: "updatedAt", sortable: true },
  { title: "Thao tác", key: "actions", sortable: false },
]);

// Edit/Add dialog
const editedIndex = ref(-1);
const editedItem = ref<Conversation>({
  title: "",
  description: "",
  jlptLevel: "N5",
  unit: 1,
  lines: [],
});
const defaultItem: Conversation = {
  title: "",
  description: "",
  jlptLevel: "N5",
  unit: 1,
  lines: [],
};

// Delete dialog
const deleteItem = ref<Conversation | null>(null);

// Fetch data
onMounted(() => {
  fetchConversations();
});

// Computed
const formTitle = computed(() => {
  return editedIndex.value === -1
    ? "Thêm hội thoại mới"
    : "Chỉnh sửa hội thoại";
});

// Methods
const fetchConversations = async () => {
  loading.value = true;
  error.value = null;
  try {
    // Call API to get conversations with pagination
    const response = await conversationService.adminGetConversations();
    conversations.value = response.data.content || [];
  } catch {
    error.value = "Không thể tải dữ liệu, vui lòng thử lại sau.";
  } finally {
    loading.value = false;
  }
};

const debouncedSearch = debounce(async () => {
  loading.value = true;
  try {
    const response = await conversationService.adminGetConversations(
      0,
      10,
      searchQuery.value,
    );
    conversations.value = response.data.content || [];
  } catch {
    toast.error("Lỗi tìm kiếm hội thoại");
  } finally {
    loading.value = false;
  }
}, 500);

const fetchConversationsByLevel = async () => {
  loading.value = true;
  try {
    if (selectedJlptLevel.value) {
      const response =
        await conversationService.adminGetConversationsByJlptLevel(
          selectedJlptLevel.value,
          0,
          10,
        );
      conversations.value = response.data.content || [];
    } else {
      fetchConversations();
    }
  } catch {
    toast.error("Lỗi lọc hội thoại theo trình độ");
  } finally {
    loading.value = false;
  }
};

const formatDate = (dateString?: string) => {
  if (!dateString) return "N/A";
  try {
    return format(parseISO(dateString), "dd/MM/yyyy HH:mm", { locale: vi });
  } catch {
    return dateString;
  }
};

// Dialog methods
const openAddDialog = () => {
  editedIndex.value = -1;
  editedItem.value = JSON.parse(JSON.stringify(defaultItem));
  expandedLines.value = {}; // Reset expanded lines
  dialog.value = true;
};

const openEditDialog = (item: Conversation) => {
  if (item.conversationId) {
    router.push(`/admin/conversations/${item.conversationId}/edit`);
  }
};

const closeDialog = () => {
  dialog.value = false;
  setTimeout(() => {
    editedItem.value = JSON.parse(JSON.stringify(defaultItem));
    editedIndex.value = -1;
    expandedLines.value = {};
  }, 300);
};

const saveConversation = async () => {
  try {
    // Tạo một object mới chỉ chứa các thông tin cơ bản cần thiết
    const basicConversation = {
      title: editedItem.value.title,
      description: editedItem.value.description || "",
      jlptLevel: editedItem.value.jlptLevel,
      unit: editedItem.value.unit,
      lines: [], // Luôn gửi mảng rỗng
    };

    loading.value = true;

    if (editedIndex.value > -1) {
      // Update existing conversation
      if (editedItem.value.conversationId) {
        await conversationService.adminUpdateConversation(
          editedItem.value.conversationId,
          {
            ...basicConversation,
            conversationId: editedItem.value.conversationId,
          },
        );
        toast.success("Hội thoại đã được cập nhật thành công");
      }
    } else {
      // Create new conversation
      const response =
        await conversationService.adminCreateConversation(basicConversation);
      const newConversation = response.data;
      toast.success("Đã thêm hội thoại mới thành công");

      // Nếu thành công và có conversationId, chuyển đến trang chỉnh sửa
      if (newConversation && newConversation.conversationId) {
        closeDialog();
        router.push(
          `/admin/conversations/${newConversation.conversationId}/edit`,
        );
        return; // Thoát sớm để không gọi closeDialog() lần nữa
      }
    }
    closeDialog();
    fetchConversations();
  } catch (err: unknown) {
    error.value =
      (err as { response?: { data?: { message?: string } } }).response?.data
        ?.message || "Không thể lưu hội thoại";
    toast.error(error.value || "Không thể lưu hội thoại");
  } finally {
    loading.value = false;
  }
};

const confirmDelete = (item: Conversation) => {
  deleteItem.value = item;
  deleteDialog.value = true;
};

const deleteConversation = async () => {
  if (!deleteItem.value || !deleteItem.value.conversationId) {
    toast.error("Không thể xóa: ID hội thoại không tồn tại");
    return;
  }

  const conversationId = deleteItem.value.conversationId; // Lưu ID để tránh TypeScript null check

  loading.value = true;

  try {
    // Gọi qua service
    await conversationService.adminDeleteConversation(conversationId);

    toast.success("Đã xóa hội thoại thành công");
    deleteDialog.value = false;
    fetchConversations();
  } catch (err: unknown) {
    error.value =
      (err as { response?: { data?: { message?: string }; message?: string } })
        .response?.data?.message ||
      (err as { message?: string }).message ||
      "Không thể xóa hội thoại";
    toast.error(error.value || "Không thể xóa hội thoại");
  } finally {
    loading.value = false;
  }
};

const viewDetail = (item: Conversation) => {
  if (item.conversationId) {
    router.push(`/admin/conversations/${item.conversationId}`);
  }
};
</script>

<style scoped>
.conversation-lines-container {
  margin-top: 12px;
}

.conversation-line-card {
  border-left: 4px solid var(--v-primary-base);
  transition: all 0.2s ease;
}

.conversation-line-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.drag-handle {
  cursor: move;
  color: #aaa;
  transition: color 0.2s ease;
}

.drag-handle:hover {
  color: var(--v-primary-base);
}

.cursor-move {
  cursor: move;
}

.line-number {
  font-weight: bold;
  min-width: 30px;
}
</style>
