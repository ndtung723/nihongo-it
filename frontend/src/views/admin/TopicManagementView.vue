<template>
  <v-container>
    <v-row class="mb-4">
      <v-col cols="12">
        <div class="header-section">
          <h1 class="text-h5 font-weight-bold mb-3 mb-md-0">Quản lý chủ đề</h1>

          <div class="filters-container">
            <v-text-field
              v-model="searchQuery.term"
              prepend-inner-icon="mdi-magnify"
              label="Tìm kiếm chủ đề"
              single-line
              hide-details
              class="search-field mb-2 mb-md-0 mr-md-2"
              density="compact"
            ></v-text-field>

            <v-select
              v-model="selectedCategoryId"
              :items="categories"
              item-title="name"
              item-value="categoryId"
              label="Lọc theo danh mục"
              class="filter-select mb-2 mb-md-0 mr-md-2"
              density="compact"
              hide-details
              @update:model-value="fetchTopicsByCategory"
              clearable
            >
            </v-select>

            <v-btn
              color="primary"
              prepend-icon="mdi-plus"
              @click="openAddDialog"
              class="add-btn"
            >
              Thêm chủ đề mới
            </v-btn>
          </div>
        </div>
      </v-col>
    </v-row>

    <v-card>
      <div class="table-responsive">
        <v-data-table
          :headers="headers"
          :items="topics"
          :loading="loading"
          class="elevation-1"
          :items-per-page="10"
          :items-per-page-options="[10, 20, 50]"
        >
          <!-- Name column -->
          <template v-slot:item.name="{ item }">
            <div class="font-weight-bold">{{ item.name }}</div>
          </template>

          <!-- Meaning column -->
          <template v-slot:item.meaning="{ item }">
            {{ item.meaning }}
          </template>

          <!-- Category column -->
          <template v-slot:item.categoryName="{ item }">
            <v-chip size="small" color="info" variant="tonal">
              {{ item.categoryName }}
            </v-chip>
          </template>

          <!-- Status column -->
          <template v-slot:item.isActive="{ item }">
            <v-chip
              size="small"
              :color="item.isActive ? 'success' : 'error'"
              :text="item.isActive ? 'Đang hoạt động' : 'Vô hiệu hóa'"
              :variant="item.isActive ? 'elevated' : 'tonal'"
            >
              {{ item.isActive ? "Đang hoạt động" : "Vô hiệu hóa" }}
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
                color="primary"
                variant="text"
                class="mr-1"
                @click="openEditDialog(item)"
              >
                <v-icon>mdi-pencil</v-icon>
                <v-tooltip activator="parent" location="top"
                  >Chỉnh sửa</v-tooltip
                >
              </v-btn>
              <v-btn
                icon
                size="small"
                :color="item.isActive ? 'error' : 'success'"
                variant="text"
                class="mr-1"
                @click="confirmToggleStatus(item)"
              >
                <v-icon>{{
                  item.isActive ? "mdi-close-circle" : "mdi-check-circle"
                }}</v-icon>
                <v-tooltip activator="parent" location="top">
                  {{ item.isActive ? "Vô hiệu hóa" : "Kích hoạt" }}
                </v-tooltip>
              </v-btn>
              <!-- <v-btn
                icon
                size="small"
                color="error"
                variant="text"
                @click="confirmDelete(item)"
              >
                <v-icon>mdi-delete</v-icon>
                <v-tooltip activator="parent" location="top">Xóa</v-tooltip>
              </v-btn> -->
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
              <div v-else class="text-body-1">Không tìm thấy chủ đề nào</div>
            </div>
          </template>
        </v-data-table>
      </div>
    </v-card>

    <!-- Edit/Add Dialog -->
    <v-dialog v-model="dialog" max-width="600px" persistent>
      <v-card>
        <v-card-title class="text-h5">
          {{ formTitle }}
        </v-card-title>
        <v-form ref="form" v-model="valid">
          <v-card-text>
            <v-row>
              <v-col cols="12">
                <v-text-field
                  v-model="editedItem.name"
                  label="Tên chủ đề (tiếng Nhật)"
                  :rules="[(v) => !!v || 'Tên chủ đề là bắt buộc']"
                  required
                ></v-text-field>
              </v-col>
              <v-col cols="12">
                <v-text-field
                  v-model="editedItem.meaning"
                  label="Ý nghĩa (tiếng Việt)"
                  :rules="[(v) => !!v || 'Ý nghĩa là bắt buộc']"
                  required
                ></v-text-field>
              </v-col>
              <v-col cols="12">
                <v-select
                  v-model="editedItem.categoryId"
                  :items="categories"
                  item-title="name"
                  item-value="categoryId"
                  label="Danh mục"
                  :rules="[(v) => !!v || 'Danh mục là bắt buộc']"
                  required
                ></v-select>
              </v-col>
              <v-col cols="6">
                <v-text-field
                  v-model.number="editedItem.displayOrder"
                  label="Thứ tự hiển thị"
                  type="number"
                ></v-text-field>
              </v-col>
              <v-col cols="6">
                <v-switch
                  v-model="editedItem.isActive"
                  label="Kích hoạt"
                  color="success"
                  hide-details
                ></v-switch>
              </v-col>
            </v-row>
          </v-card-text>
        </v-form>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="grey" variant="text" @click="closeDialog"> Hủy </v-btn>
          <v-btn
            color="primary"
            variant="elevated"
            @click="saveTopic"
            :disabled="!valid"
          >
            Lưu
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Status toggle confirmation dialog -->
    <v-dialog v-model="statusDialog" max-width="500px" persistent>
      <v-card>
        <v-card-title
          class="text-h5"
          :class="
            statusItem?.isActive
              ? 'bg-error text-white'
              : 'bg-success text-white'
          "
        >
          {{ statusItem?.isActive ? "Vô hiệu hóa chủ đề" : "Kích hoạt chủ đề" }}
        </v-card-title>
        <v-card-text class="pt-4">
          <p class="text-body-1">
            {{
              statusItem?.isActive
                ? `Bạn có chắc chắn muốn vô hiệu hóa chủ đề "${statusItem?.name}"?`
                : `Bạn có chắc chắn muốn kích hoạt lại chủ đề "${statusItem?.name}"?`
            }}
          </p>
          <div v-if="statusItem?.isActive" class="mt-2">
            Chủ đề vô hiệu hóa sẽ không hiển thị cho người dùng nhưng vẫn được
            lưu trong hệ thống.
          </div>
          <div v-else class="mt-2">
            Kích hoạt lại chủ đề sẽ cho phép người dùng xem và sử dụng chủ đề
            này.
          </div>
          <div
            v-if="statusItem?.vocabularyCount && statusItem.vocabularyCount > 0"
            class="mt-2 font-weight-bold bg-warning-lighten-5 pa-2 rounded"
          >
            <v-icon color="warning" class="mr-1">mdi-alert</v-icon>
            Lưu ý: Chủ đề này chứa
            <strong>{{ statusItem.vocabularyCount }}</strong> từ vựng.
          </div>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="grey" variant="text" @click="statusDialog = false"
            >Hủy</v-btn
          >
          <v-btn
            :color="statusItem?.isActive ? 'error' : 'success'"
            variant="elevated"
            @click="toggleTopicStatus"
          >
            {{ statusItem?.isActive ? "Vô hiệu hóa" : "Kích hoạt" }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Delete confirmation dialog -->
    <v-dialog v-model="deleteDialog" max-width="500px" persistent>
      <v-card>
        <v-card-title class="text-h5 bg-error text-white">
          Xóa chủ đề
        </v-card-title>
        <v-card-text class="pt-4">
          <p class="text-body-1">
            Bạn có chắc chắn muốn xóa chủ đề "{{ deleteItem?.name }}"?
          </p>
          <div class="mt-2 font-weight-bold bg-error-lighten-5 pa-2 rounded">
            <v-icon color="error" class="mr-1">mdi-alert</v-icon>
            Cảnh báo: Hành động này không thể hoàn tác và sẽ xóa tất cả dữ liệu
            liên quan!
          </div>
          <div
            v-if="deleteItem?.vocabularyCount && deleteItem.vocabularyCount > 0"
            class="mt-2 font-weight-bold bg-warning-lighten-5 pa-2 rounded"
          >
            <v-icon color="warning" class="mr-1">mdi-alert</v-icon>
            Lưu ý: Việc xóa chủ đề này sẽ xóa
            <strong>{{ deleteItem.vocabularyCount }}</strong> từ vựng.
          </div>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="grey" variant="text" @click="deleteDialog = false"
            >Hủy</v-btn
          >
          <v-btn color="error" variant="elevated" @click="deleteTopic">
            Xác nhận xóa
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from "vue";
import topicService from "@/services/topic.service";
import categoryService from "@/services/category.service";
import type {
  Topic,
  CreateTopicRequest,
  UpdateTopicRequest,
  Category,
} from "@/types/learning.types";
import { useAppToast } from "@/composables/useAppToast";
import { useDebounce } from "@/composables/useDebounce";
import { format, parseISO } from "date-fns";
import { vi } from "date-fns/locale";

const $toast = useAppToast();

const searchTerm = computed(() => searchQuery.value.term);
const debouncedSearchTerm = useDebounce(searchTerm, 400);
watch(debouncedSearchTerm, () => searchTopics());

// Data
const topics = ref<Topic[]>([]);
const categories = ref<Category[]>([]);
const selectedCategoryId = ref<string | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);
const searchQuery = ref({
  term: "",
});
const valid = ref(false);
const form = ref(null);

// Dialog state
const dialog = ref(false);
const statusDialog = ref(false);
const deleteDialog = ref(false);
const editedIndex = ref(-1);
const editedItem = ref<CreateTopicRequest | UpdateTopicRequest>({
  name: "",
  meaning: "",
  displayOrder: 0,
  isActive: true,
  categoryId: "",
});
const defaultItem: CreateTopicRequest = {
  name: "",
  meaning: "",
  displayOrder: 0,
  isActive: true,
  categoryId: "",
};
const statusItem = ref<Topic | null>(null);
const deleteItem = ref<Topic | null>(null);

// Table headers
const headers = [
  { title: "Tên chủ đề", key: "name", align: "start" as const, sortable: true },
  { title: "Ý nghĩa", key: "meaning", align: "start" as const, sortable: true },
  {
    title: "Danh mục",
    key: "categoryName",
    align: "start" as const,
    sortable: true,
  },
  {
    title: "Từ vựng",
    key: "vocabularyCount",
    align: "center" as const,
    sortable: true,
  },
  {
    title: "Trạng thái",
    key: "isActive",
    align: "center" as const,
    sortable: true,
  },
  {
    title: "Cập nhật",
    key: "updatedAt",
    align: "center" as const,
    sortable: true,
  },
  {
    title: "Thao tác",
    key: "actions",
    align: "center" as const,
    sortable: false,
  },
];

// Format dates with Vietnamese locale
const formatDate = (dateString: string | undefined) => {
  if (!dateString) return "N/A";
  try {
    return format(parseISO(dateString), "dd/MM/yyyy", { locale: vi });
  } catch {
    return "Ngày không hợp lệ";
  }
};

// Computed properties
const formTitle = computed(() => {
  return editedIndex.value === -1 ? "Thêm chủ đề mới" : "Chỉnh sửa chủ đề";
});

// Methods
async function fetchTopics() {
  try {
    loading.value = true;
    error.value = null;

    topics.value = await topicService.adminGetAllTopics();
  } catch (err: unknown) {
    error.value =
      (err as { response?: { data?: { message?: string } } }).response?.data
        ?.message || "Không thể tải danh sách chủ đề";
    $toast.error(error.value || "Không thể tải danh sách chủ đề");
    topics.value = [];
  } finally {
    loading.value = false;
  }
}

async function fetchCategories() {
  try {
    categories.value = await categoryService.adminGetAllCategories();
  } catch {
    $toast.error("Không thể tải danh sách danh mục");
    categories.value = [];
  }
}

async function fetchTopicsByCategory() {
  try {
    loading.value = true;
    error.value = null;

    if (selectedCategoryId.value) {
      topics.value = await topicService.adminGetTopicsByCategoryId(
        selectedCategoryId.value,
      );
    } else {
      // If no category is selected, fetch all topics
      await fetchTopics();
    }
  } catch (err: unknown) {
    error.value =
      (err as { response?: { data?: { message?: string } } }).response?.data
        ?.message || "Không thể tải danh sách chủ đề";
    $toast.error(error.value || "Không thể tải danh sách chủ đề");
  } finally {
    loading.value = false;
  }
}

async function searchTopics() {
  try {
    loading.value = true;
    error.value = null;

    if (!searchQuery.value.term) {
      if (selectedCategoryId.value) {
        await fetchTopicsByCategory();
      } else {
        await fetchTopics();
      }
      return;
    }

    if (selectedCategoryId.value) {
      topics.value = await topicService.adminSearchTopics(
        selectedCategoryId.value,
        searchQuery.value.term,
      );
    } else {
      // If searching without a category, filter client-side
      const allTopics = await topicService.adminGetAllTopics();
      const term = searchQuery.value.term.toLowerCase();
      topics.value = allTopics.filter(
        (topic) =>
          topic.name.toLowerCase().includes(term) ||
          (topic.meaning ?? "").toLowerCase().includes(term),
      );
    }
  } catch (err: unknown) {
    error.value =
      (err as { response?: { data?: { message?: string } } }).response?.data
        ?.message || "Không thể tìm kiếm chủ đề";
    $toast.error(error.value || "Không thể tìm kiếm chủ đề");
  } finally {
    loading.value = false;
  }
}

function openAddDialog() {
  editedIndex.value = -1;
  editedItem.value = { ...defaultItem };
  dialog.value = true;
}

function openEditDialog(item: Topic) {
  editedIndex.value = topics.value.indexOf(item);
  editedItem.value = {
    name: item.name,
    meaning: item.meaning,
    displayOrder: item.displayOrder,
    isActive: item.isActive,
    categoryId: item.categoryId,
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

async function saveTopic() {
  if (!form.value) return;

  try {
    if (editedIndex.value > -1) {
      // Update existing topic
      const topicId = topics.value[editedIndex.value].topicId;
      const updateData: UpdateTopicRequest = {
        name: editedItem.value.name,
        meaning: editedItem.value.meaning,
        displayOrder: editedItem.value.displayOrder as number,
        isActive: editedItem.value.isActive as boolean,
        categoryId: editedItem.value.categoryId,
      };

      await topicService.adminUpdateTopic(topicId, updateData);
      $toast.success("Cập nhật chủ đề thành công");
    } else {
      // Create new topic
      await topicService.adminCreateTopic(
        editedItem.value as CreateTopicRequest,
      );
      $toast.success("Thêm chủ đề mới thành công");
    }

    closeDialog();
    if (selectedCategoryId.value) {
      fetchTopicsByCategory();
    } else {
      fetchTopics();
    }
  } catch (err: unknown) {
    error.value =
      (err as { response?: { data?: { message?: string } } }).response?.data
        ?.message || "Không thể lưu chủ đề";
    $toast.error(error.value || "Không thể lưu chủ đề");
  }
}

function confirmToggleStatus(item: Topic) {
  statusItem.value = item;
  statusDialog.value = true;
}

async function toggleTopicStatus() {
  if (!statusItem.value) return;

  try {
    await topicService.adminToggleTopicStatus(statusItem.value.topicId);
    $toast.success(
      statusItem.value.isActive
        ? "Đã vô hiệu hóa chủ đề thành công"
        : "Đã kích hoạt chủ đề thành công",
    );
    statusDialog.value = false;
    if (selectedCategoryId.value) {
      fetchTopicsByCategory();
    } else {
      fetchTopics();
    }
  } catch (err: unknown) {
    error.value =
      (err as { response?: { data?: { message?: string } } }).response?.data
        ?.message || "Không thể thay đổi trạng thái chủ đề";
    $toast.error(error.value || "Không thể thay đổi trạng thái chủ đề");
  }
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function confirmDelete(item: Topic) {
  deleteItem.value = item;
  deleteDialog.value = true;
}

async function deleteTopic() {
  if (!deleteItem.value) return;

  try {
    await topicService.adminDeleteTopic(deleteItem.value.topicId);
    $toast.success("Đã xóa chủ đề thành công");
    deleteDialog.value = false;
    if (selectedCategoryId.value) {
      fetchTopicsByCategory();
    } else {
      fetchTopics();
    }
  } catch (err: unknown) {
    error.value =
      (err as { response?: { data?: { message?: string } } }).response?.data
        ?.message || "Không thể xóa chủ đề";
    $toast.error(error.value || "Không thể xóa chủ đề");
  }
}

// Initialize data
onMounted(async () => {
  await Promise.all([fetchTopics(), fetchCategories()]);
});
</script>

<style scoped>
.v-data-table :deep(th) {
  font-weight: bold;
  background-color: #f5f5f5;
}

.header-section {
  display: flex;
  flex-direction: column;
}

.filters-container {
  display: flex;
  flex-direction: column;
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.table-responsive :deep(.v-data-table) {
  min-width: 800px;
  width: 100%;
}

/* Responsive styles */
@media (min-width: 960px) {
  .header-section {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .filters-container {
    flex-direction: row;
    align-items: center;
  }

  .search-field {
    max-width: 333px;
  }

  .filter-select {
    max-width: 200px;
  }
}
</style>
