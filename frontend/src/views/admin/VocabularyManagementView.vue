<template>
  <v-container>
    <v-row class="mb-4">
      <v-col cols="12">
        <div class="filters-container pa-3 rounded">
          <div class="d-flex flex-wrap align-center">
            <h1 class="text-h5 font-weight-bold mr-4">Quản lý từ vựng</h1>
            <v-spacer></v-spacer>

            <div class="search-filters d-flex flex-wrap">
              <v-text-field
                v-model="searchQuery"
                prepend-inner-icon="mdi-magnify"
                label="Tìm kiếm từ vựng"
                single-line
                hide-details
                class="mr-2 search-field"
                density="compact"
                variant="outlined"
                bg-color="surface"
                style="max-width: 300px; width: 300px"
                @input="debouncedSearch"
              ></v-text-field>

              <v-select
                v-model="selectedTopicId"
                :items="topics"
                item-title="name"
                item-value="topicId"
                label="Lọc theo chủ đề"
                class="mr-2 topic-select"
                density="compact"
                variant="outlined"
                bg-color="surface"
                style="max-width: 250px ; width: 250px"
                hide-details
                @update:model-value="fetchVocabularyByTopic"
                clearable
                prepend-inner-icon="mdi-folder-outline"
              >
              </v-select>

              <v-select
                v-model="selectedJlptLevel"
                :items="jlptLevels"
                label="JLPT Level"
                class="mr-2 jlpt-select"
                density="compact"
                variant="outlined"
                bg-color="surface"
                style="max-width: 150px; width: 150px"
                hide-details
                @update:model-value="fetchVocabularyByTopic"
                clearable
                prepend-inner-icon="mdi-certificate-outline"
              >
              </v-select>

              <v-btn
                color="primary"
                prepend-icon="mdi-plus"
                @click="openAddDialog"
                variant="elevated"
                density="default"
                class="add-btn"
              >
                Thêm từ vựng
              </v-btn>
            </div>
          </div>
        </div>
      </v-col>
    </v-row>

    <v-card>
      <div class="table-responsive">
        <v-data-table
          :headers="headers"
          :items="vocabulary"
          :loading="loading"
          class="elevation-1"
          :items-per-page="10"
          :items-per-page-options="[10, 20, 50]"
          @update:options="handleTableOptions"
        >
          <!-- Term column -->
          <template v-slot:item.term="{ item }">
            <div class="d-flex flex-column">
              <div class="font-weight-bold mb-1 text-body-1">{{ item.term }}</div>
              <div class="text-body-2 text-grey">{{ item.pronunciation }}</div>
            </div>
          </template>

          <!-- Meaning column -->
          <template v-slot:item.meaning="{ item }">
            <div class="text-body-2">{{ item.meaning }}</div>
          </template>

          <!-- Example column -->
          <template v-slot:item.example="{ item }">
            <div v-if="item.example" class="example-container">
              <div class="mb-1 font-italic text-body-2">{{ item.example }}</div>
              <div class="text-grey text-caption">{{ item.exampleMeaning }}</div>
            </div>
            <div v-else class="text-caption text-grey text-center font-italic">
              <v-icon size="small" color="grey" class="mr-1">mdi-minus-circle</v-icon>
              Không có ví dụ
            </div>
          </template>

          <!-- Topic column -->
          <template v-slot:item.topicName="{ item }">
            <v-chip
              size="small"
              color="info"
              variant="tonal"
              class="topic-chip"
            >
              <v-icon size="x-small" class="mr-1">mdi-tag</v-icon>
              {{ item.topicName }}
            </v-chip>
          </template>

          <!-- JLPT Level column -->
          <template v-slot:item.jlptLevel="{ item }">
            <v-chip
              size="small"
              :color="getJlptLevelColor(item.jlptLevel)"
              variant="tonal"
              class="jlpt-chip"
            >
              {{ item.jlptLevel }}
            </v-chip>
          </template>

          <!-- Actions column -->
          <template v-slot:item.actions="{ item }">
            <div class="d-flex justify-center">
              <v-btn
                icon
                size="small"
                color="primary"
                variant="text"
                class="mr-2"
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
              <v-icon size="large" icon="mdi-text-box-search-outline" class="mb-2"></v-icon>
              <div v-if="error" class="text-body-1 text-error">{{ error }}</div>
              <div v-else-if="loading" class="text-body-1">Đang tải dữ liệu...</div>
              <div v-else class="text-body-1">Không tìm thấy từ vựng nào</div>
            </div>
          </template>
        </v-data-table>
      </div>
    </v-card>

    <!-- Edit/Add Dialog -->
    <v-dialog v-model="dialog" max-width="700px" persistent>
      <v-card class="vocabulary-edit-dialog">
        <v-card-title class="text-h5 px-4 py-3 primary-gradient text-white">
          {{ formTitle }}
        </v-card-title>
        <v-form ref="form" v-model="valid">
          <v-card-text class="px-4 py-3">
            <v-row dense>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model="editedItem.term"
                  label="Từ vựng (tiếng Nhật)"
                  :rules="[v => !!v || 'Từ vựng là bắt buộc']"
                  density="compact"
                  variant="outlined"
                  bg-color="surface"
                  class="mb-2"
                  required
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model="editedItem.pronunciation"
                  label="Cách đọc (furigana)"
                  density="compact"
                  variant="outlined"
                  bg-color="surface"
                  class="mb-2"
                ></v-text-field>
              </v-col>
              <v-col cols="12">
                <v-text-field
                  v-model="editedItem.meaning"
                  label="Ý nghĩa (tiếng Việt)"
                  :rules="[v => !!v || 'Ý nghĩa là bắt buộc']"
                  density="compact"
                  variant="outlined"
                  bg-color="surface"
                  class="mb-2"
                  required
                ></v-text-field>
              </v-col>
              <v-col cols="12">
                <v-textarea
                  v-model="editedItem.example"
                  label="Ví dụ (tiếng Nhật)"
                  auto-grow
                  rows="2"
                  density="compact"
                  variant="outlined"
                  bg-color="surface"
                  class="mb-2"
                ></v-textarea>
              </v-col>
              <v-col cols="12">
                <v-textarea
                  v-model="editedItem.exampleMeaning"
                  label="Ý nghĩa ví dụ (tiếng Việt)"
                  auto-grow
                  rows="2"
                  density="compact"
                  variant="outlined"
                  bg-color="surface"
                  class="mb-2"
                ></v-textarea>
              </v-col>
              <v-col cols="12" md="6">
                <v-select
                  v-model="editedItem.topicName"
                  :items="topics"
                  item-title="name"
                  item-value="name"
                  label="Chủ đề"
                  :rules="[v => !!v || 'Chủ đề là bắt buộc']"
                  density="compact"
                  variant="outlined"
                  bg-color="surface"
                  class="mb-2"
                  required
                ></v-select>
              </v-col>
              <v-col cols="12" md="6">
                <v-select
                  v-model="editedItem.jlptLevel"
                  :items="jlptLevels"
                  label="JLPT Level"
                  :rules="[v => !!v || 'JLPT Level là bắt buộc']"
                  density="compact"
                  variant="outlined"
                  bg-color="surface"
                  class="mb-2"
                  required
                ></v-select>
              </v-col>
            </v-row>
          </v-card-text>
        </v-form>
        <v-divider></v-divider>
        <v-card-actions class="px-4 py-3">
          <v-spacer></v-spacer>
          <v-btn color="grey" variant="text" @click="closeDialog">
            Hủy
          </v-btn>
          <v-btn color="primary" variant="elevated" @click="saveVocabulary" :disabled="!valid">
            <v-icon left>mdi-content-save</v-icon>
            Lưu
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Delete confirmation dialog -->
    <v-dialog v-model="deleteDialog" max-width="500px" persistent>
      <v-card class="vocabulary-delete-dialog">
        <v-card-title class="text-h5 px-4 py-3 error-gradient text-white">
          <v-icon left color="white" class="mr-2">mdi-delete-alert</v-icon>
          Xóa từ vựng
        </v-card-title>
        <v-card-text class="pt-4 px-4">
          <p class="text-body-1 mb-3">
            Bạn có chắc chắn muốn xóa từ vựng <span class="font-weight-bold">"{{ deleteItem?.term }}"</span>?
          </p>
          <div class="mt-2 font-weight-bold bg-error-lighten-5 pa-3 rounded">
            <v-icon color="error" class="mr-2">mdi-alert</v-icon>
            Cảnh báo: Hành động này không thể hoàn tác và sẽ xóa tất cả dữ liệu liên quan!
          </div>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions class="px-4 py-3">
          <v-spacer></v-spacer>
          <v-btn color="grey" variant="text" @click="deleteDialog = false">
            Hủy
          </v-btn>
          <v-btn
            color="error"
            variant="elevated"
            @click="deleteVocabulary"
          >
            <v-icon left>mdi-delete</v-icon>
            Xác nhận xóa
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import vocabularyService from '@/services/vocabularyService';
import topicService from '@/services/topicService';
import type { Vocabulary, CreateVocabularyRequest, UpdateVocabularyRequest } from '@/services/vocabularyService';
import type { Topic } from '@/services/topicService';
import { useToast } from 'vue-toast-notification';
import { format, parseISO } from 'date-fns';
import { vi } from 'date-fns/locale';

const $toast = useToast();

// Data
const vocabulary = ref<Vocabulary[]>([]);
const topics = ref<Topic[]>([]);
const jlptLevels = ref<string[]>(['N1', 'N2', 'N3', 'N4', 'N5']);
const selectedTopicId = ref<string | null>(null);
const selectedJlptLevel = ref<string | null>(null);
const searchQuery = ref<string>('');
const loading = ref(false);
const error = ref<string | null>(null);
const valid = ref(false);
const form = ref(null);
const pagination = ref({
  page: 1,
  itemsPerPage: 10,
  pageCount: 0,
  itemsLength: 0
});

// Dialog state
const dialog = ref(false);
const deleteDialog = ref(false);
const editedIndex = ref(-1);
const editedItem = ref<CreateVocabularyRequest | UpdateVocabularyRequest>({
  term: '',
  meaning: '',
  pronunciation: '',
  example: '',
  exampleMeaning: '',
  topicName: '',
  jlptLevel: ''
});
const defaultItem: CreateVocabularyRequest = {
  term: '',
  meaning: '',
  pronunciation: '',
  example: '',
  exampleMeaning: '',
  topicName: '',
  jlptLevel: 'N3'
};
const deleteItem = ref<Vocabulary | null>(null);

// Table headers
const headers = [
  { title: 'Từ vựng', key: 'term', align: 'start' as const, sortable: true },
  { title: 'Ý nghĩa', key: 'meaning', align: 'start' as const, sortable: true },
  { title: 'Ví dụ', key: 'example', align: 'start' as const, sortable: false },
  { title: 'Chủ đề', key: 'topicName', align: 'center' as const, sortable: true },
  { title: 'JLPT', key: 'jlptLevel', align: 'center' as const, sortable: true },
  { title: 'Thao tác', key: 'actions', align: 'center' as const, sortable: false }
];

// Format dates with Vietnamese locale
const formatDate = (dateString: string | undefined) => {
  if (!dateString) return 'N/A';
  try {
    return format(parseISO(dateString), 'dd/MM/yyyy', { locale: vi });
  } catch (e) {
    return 'Ngày không hợp lệ';
  }
};

// Computed properties
const formTitle = computed(() => {
  return editedIndex.value === -1 ? 'Thêm từ vựng mới' : 'Chỉnh sửa từ vựng';
});

// JLPT Level color
const getJlptLevelColor = (level: string): string => {
  switch (level) {
    case 'N1': return 'red';
    case 'N2': return 'deep-orange';
    case 'N3': return 'amber';
    case 'N4': return 'light-green';
    case 'N5': return 'green';
    default: return 'grey';
  }
};

// Methods
async function fetchVocabulary(page: number = 0, size: number = 10) {
  try {
    loading.value = true;
    error.value = null;

    let response;

    if (searchQuery.value) {
      response = await vocabularyService.adminSearchVocabulary(
        searchQuery.value,
        selectedTopicId.value || undefined,
        selectedJlptLevel.value || undefined,
        page,
        size
      );
    } else if (selectedTopicId.value && selectedJlptLevel.value) {
      // Khi cả topic và JLPT level đều được chọn, sử dụng tham số jlptLevel trong API
      response = await vocabularyService.adminGetVocabularyByTopicId(
        selectedTopicId.value,
        page,
        size
      );
      // JLPT level sẽ được lọc bởi backend theo logic đã cập nhật
    } else if (selectedJlptLevel.value) {
      // Khi chỉ chọn JLPT level
      response = await vocabularyService.adminGetVocabularyByJlptLevel(
        selectedJlptLevel.value,
        page,
        size
      );
    } else if (selectedTopicId.value) {
      // Khi chỉ chọn topic
      response = await vocabularyService.adminGetVocabularyByTopicId(
        selectedTopicId.value,
        page,
        size
      );
    } else {
      // Khi không có điều kiện lọc
      response = await vocabularyService.adminGetAllVocabulary(page, size);
    }

    vocabulary.value = response.data.content;
    pagination.value = {
      page: response.data.page + 1, // Convert from 0-based to 1-based for display
      itemsPerPage: response.data.size,
      pageCount: response.data.totalPages,
      itemsLength: response.data.totalElements
    };
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Không thể tải danh sách từ vựng';
    $toast.error(error.value || 'Không thể tải danh sách từ vựng');
    vocabulary.value = [];
  } finally {
    loading.value = false;
  }
}

async function fetchTopics() {
  try {
    const response = await topicService.adminGetAllTopics();
    topics.value = response.data;
  } catch (err: any) {
    $toast.error('Không thể tải danh sách chủ đề');
    topics.value = [];
  }
}

function fetchVocabularyByTopic() {
  fetchVocabulary(0, pagination.value.itemsPerPage);
}

// Debounce function for search
let debounceTimeout: number | null = null;
function debouncedSearch() {
  if (debounceTimeout) {
    clearTimeout(debounceTimeout);
  }
  debounceTimeout = setTimeout(() => {
    fetchVocabulary(0, pagination.value.itemsPerPage);
  }, 500) as unknown as number;
}

function handleTableOptions(options: any) {
  const { page, itemsPerPage } = options;
  if (itemsPerPage !== pagination.value.itemsPerPage) {
    pagination.value.itemsPerPage = itemsPerPage;
  }
  fetchVocabulary(page - 1, itemsPerPage); // Convert to 0-based for API
}

function openAddDialog() {
  editedIndex.value = -1;
  editedItem.value = { ...defaultItem };
  dialog.value = true;
}

function openEditDialog(item: Vocabulary) {
  editedIndex.value = vocabulary.value.indexOf(item);
  editedItem.value = {
    term: item.term,
    meaning: item.meaning,
    pronunciation: item.pronunciation || '',
    example: item.example || '',
    exampleMeaning: item.exampleMeaning || '',
    topicName: item.topicName || '',
    jlptLevel: item.jlptLevel
  };
  dialog.value = true;
}

function closeDialog() {
  dialog.value = false;
  setTimeout(() => {
    editedItem.value = { ...defaultItem };
    editedIndex.value = -1;
  }, 300);
}

async function saveVocabulary() {
  if (!form.value) return;

  try {
    if (editedIndex.value > -1) {
      // Update existing vocabulary
      const vocabId = vocabulary.value[editedIndex.value].vocabId;
      const updateData = { ...editedItem.value } as UpdateVocabularyRequest;

      await vocabularyService.adminUpdateVocabulary(vocabId, updateData);
      $toast.success('Cập nhật từ vựng thành công');
    } else {
      // Create new vocabulary
      await vocabularyService.adminCreateVocabulary(editedItem.value as CreateVocabularyRequest);
      $toast.success('Thêm từ vựng mới thành công');
    }

    closeDialog();
    fetchVocabulary(pagination.value.page - 1, pagination.value.itemsPerPage);
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Không thể lưu từ vựng';
    $toast.error(error.value || 'Không thể lưu từ vựng');
  }
}

function confirmDelete(item: Vocabulary) {
  deleteItem.value = item;
  deleteDialog.value = true;
}

async function deleteVocabulary() {
  if (!deleteItem.value) return;

  try {
    await vocabularyService.adminDeleteVocabulary(deleteItem.value.vocabId);
    $toast.success('Đã xóa từ vựng thành công');
    deleteDialog.value = false;
    fetchVocabulary(pagination.value.page - 1, pagination.value.itemsPerPage);
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Không thể xóa từ vựng';
    $toast.error(error.value || 'Không thể xóa từ vựng');
  }
}

// Initialize data
onMounted(async () => {
  await Promise.all([fetchVocabulary(), fetchTopics()]);
});
</script>

<style scoped>
.v-data-table :deep(th) {
  font-weight: bold;
  background-color: #f5f5f5;
}

.vocabulary-edit-dialog :deep(.v-field__field) {
  min-height: 40px;
}

.primary-gradient {
  background: linear-gradient(to right, #1976d2, #2196f3);
}

.vocabulary-edit-dialog {
  border-radius: 8px;
  overflow: hidden;
}

.vocabulary-edit-dialog :deep(.v-card-title) {
  font-weight: 500;
  letter-spacing: 0.5px;
}

.vocabulary-edit-dialog :deep(.v-btn) {
  text-transform: none;
  letter-spacing: 0.5px;
}

.error-gradient {
  background: linear-gradient(to right, #d32f2f, #f44336);
}

.vocabulary-delete-dialog {
  border-radius: 8px;
  overflow: hidden;
}

.vocabulary-delete-dialog :deep(.v-card-title) {
  font-weight: 500;
  letter-spacing: 0.5px;
}

.example-container {
  padding: 4px;
  border-left: 2px solid #e0e0e0;
}

.topic-chip {
  font-size: 0.75rem;
  font-weight: 500;
}

.jlpt-chip {
  font-size: 0.75rem;
  font-weight: 600;
}

/* Thêm hiệu ứng hover cho hàng trong bảng */
.v-data-table :deep(tbody tr) {
  transition: background-color 0.2s;
}

.v-data-table :deep(tbody tr:hover) {
  background-color: rgba(25, 118, 210, 0.05);
}

.filters-container {
  background: white;
  border: 1px solid #eeeeee;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.search-field :deep(.v-field__outline__start),
.topic-select :deep(.v-field__outline__start),
.jlpt-select :deep(.v-field__outline__start) {
  border-radius: 6px 0 0 6px;
}

.search-field :deep(.v-field__outline__end),
.topic-select :deep(.v-field__outline__end),
.jlpt-select :deep(.v-field__outline__end) {
  border-radius: 0 6px 6px 0;
}

.add-btn {
  height: 40px;
  text-transform: none;
  letter-spacing: 0.5px;
  font-weight: 500;
}

@media (max-width: 960px) {
  .search-filters {
    margin-top: 12px;
    width: 100%;
  }

  .search-field, .topic-select, .jlpt-select {
    margin-bottom: 8px;
  }
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

/* Ensure table takes full width inside the scrollable container */
.table-responsive :deep(.v-data-table) {
  min-width: 800px;
  width: 100%;
}
</style>
