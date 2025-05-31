<template>
  <div class="users-management">
    <h1 class="text-h5 mb-3">User Management</h1>

    <div class="header-controls mb-4">
      <v-text-field
        v-model="searchQuery"
        label="Search users"
        prepend-inner-icon="mdi-magnify"
        single-line
        hide-details
        class="search-field mb-3 mb-md-0"
        @keyup.enter="loadUsers"
        density="compact"
      ></v-text-field>

      <v-btn
        color="primary"
        prepend-icon="mdi-account-plus"
        class="add-user-btn"
        @click="openCreateDialog"
      >
        Add User
      </v-btn>
    </div>

    <v-card elevation="2">
      <div class="table-responsive">
        <v-data-table
          :headers="headers"
          :items="users"
          :items-per-page="10"
          :loading="loading"
          :sort-by="[{ key: sortBy, order: sortDirection }]"
          class="elevation-1"
          item-value="userId"
          @update:sort-by="handleSort"
        >
          <template v-slot:item.profilePicture="{ item }">
            <v-avatar size="36">
              <v-img
                :src="item.profilePicture || '/img/default-avatar.png'"
                :alt="`${item.fullName}'s avatar`"
              ></v-img>
            </v-avatar>
          </template>

          <template v-slot:item.roleId="{ item }">
            <v-chip
              :color="item.roleId === 1 ? 'error' : 'primary'"
              size="small"
              variant="outlined"
            >
              {{ item.roleId === 1 ? 'Admin' : 'User' }}
            </v-chip>
          </template>

          <template v-slot:item.status="{ item }">
            <v-chip
              :color="item.isActive ? 'success' : 'error'"
              size="small"
              :text="item.isActive ? 'Active' : 'Inactive'"
              variant="outlined"
            ></v-chip>
          </template>

          <template v-slot:item.lastLogin="{ item }">
            {{ formatDate(item.lastLogin) }}
          </template>

          <template v-slot:item.actions="{ item }">
            <div class="d-flex">
              <v-tooltip text="Edit User">
                <template v-slot:activator="{ props }">
                  <v-btn
                    v-bind="props"
                    icon
                    size="small"
                    variant="text"
                    color="primary"
                    @click="editUser(item)"
                  >
                    <v-icon>mdi-pencil</v-icon>
                  </v-btn>
                </template>
              </v-tooltip>

              <v-tooltip :text="item.isActive ? 'Deactivate User' : 'Activate User'">
                <template v-slot:activator="{ props }">
                  <v-btn
                    v-bind="props"
                    icon
                    size="small"
                    variant="text"
                    :color="item.isActive ? 'error' : 'success'"
                    @click="toggleUserStatus(item)"
                  >
                    <v-icon>{{ item.isActive ? 'mdi-account-off' : 'mdi-account-check' }}</v-icon>
                  </v-btn>
                </template>
              </v-tooltip>

              <v-tooltip text="Change Role">
                <template v-slot:activator="{ props }">
                  <v-btn
                    v-bind="props"
                    icon
                    size="small"
                    variant="text"
                    color="warning"
                    @click="changeRole(item)"
                  >
                    <v-icon>mdi-shield-account</v-icon>
                  </v-btn>
                </template>
              </v-tooltip>
            </div>
          </template>
        </v-data-table>
      </div>
    </v-card>

    <!-- User Create/Edit Dialog -->
    <v-dialog
      v-model="dialog.visible"
      max-width="600px"
      persistent
    >
      <v-card>
        <v-card-title>
          <span class="text-h5">{{ dialog.isEdit ? 'Edit User' : 'Create New User' }}</span>
        </v-card-title>

        <v-card-text>
          <v-form ref="form" v-model="formValid">
            <v-text-field
              v-model="dialog.user.email"
              label="Email"
              prepend-inner-icon="mdi-email"
              :rules="[v => !!v || 'Email is required', v => /.+@.+\..+/.test(v) || 'Email must be valid']"
              required
            ></v-text-field>

            <v-text-field
              v-model="dialog.user.fullName"
              label="Full Name"
              prepend-inner-icon="mdi-account"
              :rules="[v => !!v || 'Full name is required']"
              required
            ></v-text-field>

            <v-text-field
              v-if="!dialog.isEdit"
              v-model="dialog.user.password"
              label="Password"
              prepend-inner-icon="mdi-lock"
              :append-inner-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
              :type="showPassword ? 'text' : 'password'"
              :rules="[v => !!v || 'Password is required', v => v.length >= 6 || 'Password must be at least 6 characters']"
              required
              @click:append-inner="showPassword = !showPassword"
            ></v-text-field>

            <v-select
              v-model="dialog.user.currentLevel"
              label="Current Level"
              prepend-inner-icon="mdi-star"
              :items="jlptLevels"
            ></v-select>

            <v-select
              v-model="dialog.user.jlptGoal"
              label="JLPT Goal"
              prepend-inner-icon="mdi-star-circle"
              :items="jlptLevels"
            ></v-select>

            <v-select
              v-model="dialog.user.roleId"
              label="Role"
              prepend-inner-icon="mdi-shield-account"
              :items="roles"
              item-title="name"
              item-value="id"
            ></v-select>

            <div v-if="dialog.isEdit">
              <!-- Removed the active switch, now handled only via action buttons -->
            </div>
          </v-form>
        </v-card-text>

        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            color="grey"
            variant="text"
            @click="dialog.visible = false"
          >
            Cancel
          </v-btn>
          <v-btn
            color="primary"
            variant="elevated"
            :disabled="!formValid"
            @click="saveUser"
          >
            {{ dialog.isEdit ? 'Update' : 'Create' }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Role Change Dialog -->
    <v-dialog
      v-model="roleDialog.visible"
      max-width="400px"
    >
      <v-card>
        <v-card-title>Change User Role</v-card-title>
        <v-card-text>
          <p>Change role for <strong>{{ roleDialog.user?.email }}</strong>?</p>
          <v-select
            v-model="roleDialog.selectedRole"
            label="Select new role"
            :items="roles"
            item-title="name"
            item-value="id"
            class="mt-4"
          ></v-select>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            color="grey"
            variant="text"
            @click="roleDialog.visible = false"
          >
            Cancel
          </v-btn>
          <v-btn
            color="warning"
            variant="elevated"
            @click="updateUserRole"
          >
            Change Role
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Confirmation Dialog -->
    <v-dialog
      v-model="confirmDialog.visible"
      max-width="400px"
    >
      <v-card>
        <v-card-title>{{ confirmDialog.title }}</v-card-title>
        <v-card-text>{{ confirmDialog.message }}</v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            color="grey"
            variant="text"
            @click="confirmDialog.visible = false"
          >
            Cancel
          </v-btn>
          <v-btn
            :color="confirmDialog.color"
            variant="elevated"
            @click="confirmDialog.action"
          >
            {{ confirmDialog.actionText }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Snackbar for notifications -->
    <v-snackbar
      v-model="snackbar.visible"
      :color="snackbar.color"
      :timeout="3000"
    >
      {{ snackbar.text }}
      <template v-slot:actions>
        <v-btn
          variant="text"
          icon="mdi-close"
          @click="snackbar.visible = false"
        ></v-btn>
      </template>
    </v-snackbar>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { format, parseISO } from 'date-fns';
import adminService from '@/services/admin.service';
import type { UserInfo, UserCreateRequest, UserUpdateRequest } from '@/services/admin.service';

// Table data and pagination
const users = ref<UserInfo[]>([]);
const loading = ref(true);
const page = ref(1);
const totalPages = ref(1);
const totalItems = ref(0);
const searchQuery = ref('');
const sortBy = ref('email');
const sortDirection = ref<'asc' | 'desc'>('asc');

// Table headers
const headers = ref([
  { title: 'Avatar', key: 'profilePicture', sortable: false },
  { title: 'Email', key: 'email', sortable: true },
  { title: 'Name', key: 'fullName', sortable: true },
  { title: 'Role', key: 'roleId', sortable: true },
  { title: 'Status', key: 'status', sortable: true },
  { title: 'Last Login', key: 'lastLogin', sortable: true },
  { title: 'Actions', key: 'actions', sortable: false }
]);

// Form state
const formValid = ref(false);
const form = ref(null);
const showPassword = ref(false);

// Dialogs
const dialog = ref({
  visible: false,
  isEdit: false,
  user: {} as any
});

const roleDialog = ref({
  visible: false,
  user: null as UserInfo | null,
  selectedRole: 2
});

const confirmDialog = ref({
  visible: false,
  title: '',
  message: '',
  action: () => {},
  actionText: 'Confirm',
  color: 'primary'
});

const snackbar = ref({
  visible: false,
  text: '',
  color: 'success'
});

// Form options
const jlptLevels = ['N5', 'N4', 'N3', 'N2', 'N1'];
const roles = [
  { id: 1, name: 'Admin' },
  { id: 2, name: 'User' }
];

// Format dates
const formatDate = (dateString: string | null | undefined) => {
  if (!dateString) return 'Never';
  try {
    return format(parseISO(dateString), 'MMM dd, yyyy HH:mm');
  } catch (e) {
    return 'Invalid date';
  }
};

// Load users from API
const loadUsers = async () => {
  loading.value = true;
  try {
    const response = await adminService.getUsers(
      page.value - 1,
      10,
      searchQuery.value,
      sortBy.value,
      sortDirection.value
    );
    users.value = response.users;
    totalPages.value = response.totalPages;
    totalItems.value = response.totalItems;
  } catch (error) {
    showSnackbar('Failed to load users', 'error');
  } finally {
    loading.value = false;
  }
};

// Handle sort change
const handleSort = (newSort: any) => {
  if (newSort.length > 0) {
    sortBy.value = newSort[0].key;
    sortDirection.value = newSort[0].order as 'asc' | 'desc';
  } else {
    sortBy.value = 'email';
    sortDirection.value = 'asc';
  }
  loadUsers();
};

// Open create user dialog
const openCreateDialog = () => {
  dialog.value = {
    visible: true,
    isEdit: false,
    user: {
      email: '',
      password: '',
      fullName: '',
      currentLevel: 'N5',
      jlptGoal: 'N3',
      roleId: 2
    }
  };
};

// Open edit user dialog
const editUser = (user: UserInfo) => {
  dialog.value = {
    visible: true,
    isEdit: true,
    user: { ...user }
  };
};

// Save user (create or update)
const saveUser = async () => {
  try {
    if (dialog.value.isEdit) {
      // Update existing user
      const userId = dialog.value.user.userId;
      const userData: UserUpdateRequest = {
        email: dialog.value.user.email,
        fullName: dialog.value.user.fullName,
        currentLevel: dialog.value.user.currentLevel,
        jlptGoal: dialog.value.user.jlptGoal,
        roleId: dialog.value.user.roleId
      };

      if (dialog.value.user.password) {
        userData.password = dialog.value.user.password;
      }

      await adminService.updateUser(userId, userData);
      showSnackbar('User updated successfully');
    } else {
      // Create new user
      const userData: UserCreateRequest = {
        email: dialog.value.user.email,
        password: dialog.value.user.password,
        fullName: dialog.value.user.fullName,
        currentLevel: dialog.value.user.currentLevel,
        jlptGoal: dialog.value.user.jlptGoal,
        roleId: dialog.value.user.roleId
      };

      await adminService.createUser(userData);
      showSnackbar('User created successfully');
    }

    dialog.value.visible = false;
    loadUsers();
  } catch (error: any) {
    showSnackbar(error.response?.data?.message || 'Failed to save user', 'error');
  }
};

// Toggle user active status
const toggleUserStatus = (user: UserInfo) => {
  // Explicitly check if isActive is true or undefined/null (meaning it's active)
  const isCurrentlyActive = user.isActive !== false;

  confirmDialog.value = {
    visible: true,
    title: isCurrentlyActive ? 'Deactivate User?' : 'Activate User?',
    message: `Are you sure you want to ${isCurrentlyActive ? 'deactivate' : 'activate'} ${user.email}?`,
    action: async () => {
      try {
        if (isCurrentlyActive) {
          await adminService.deactivateUser(user.userId);
          showSnackbar('User deactivated successfully');
        } else {
          await adminService.activateUser(user.userId);
          showSnackbar('User activated successfully');
        }
        confirmDialog.value.visible = false;
        loadUsers();
      } catch (error: any) {
        showSnackbar(error.response?.data?.message || 'Failed to update user status', 'error');
      }
    },
    actionText: isCurrentlyActive ? 'Deactivate' : 'Activate',
    color: isCurrentlyActive ? 'error' : 'success'
  };
};

// Open role change dialog
const changeRole = (user: UserInfo) => {
  roleDialog.value = {
    visible: true,
    user: user,
    selectedRole: user.roleId
  };
};

// Update user role
const updateUserRole = async () => {
  if (!roleDialog.value.user) return;

  try {
    await adminService.changeUserRole(
      roleDialog.value.user.userId,
      roleDialog.value.selectedRole
    );

    roleDialog.value.visible = false;
    showSnackbar('User role updated successfully');
    loadUsers();
  } catch (error: any) {
    showSnackbar(error.response?.data?.message || 'Failed to change user role', 'error');
  }
};

// Helper to show snackbar messages
const showSnackbar = (text: string, color = 'success') => {
  snackbar.value = {
    visible: true,
    text,
    color
  };
};

// Load users on component mount
onMounted(() => {
  loadUsers();
});
</script>

<style scoped>
.users-management {
  padding: 24px;
  padding-top: 0px;
}

.header-controls {
  display: flex;
  flex-direction: column;
}

.search-field {
  width: 100%;
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

@media (min-width: 768px) {
  .header-controls {
    flex-direction: row;
    align-items: center;
  }

  .search-field {
    flex-grow: 1;
    margin-right: 16px;
  }

  .add-user-btn {
    margin-left: auto;
  }
}

@media (max-width: 600px) {
  .users-management {
    padding: 16px;
    padding-top: 0px;
  }
}
</style>
