<template>
  <div class="user-detail">
    <div class="d-flex align-center mb-4">
      <v-btn
        prepend-icon="mdi-arrow-left"
        variant="text"
        :to="{ name: 'adminUsers' }"
        class="mr-4"
      >
        Back to Users
      </v-btn>
      <h1 class="text-h4">User Details</h1>
    </div>

    <v-alert
      v-if="error"
      type="error"
      class="mb-4"
    >
      {{ error }}
    </v-alert>

    <v-card v-if="loading" elevation="2" class="pa-6 d-flex justify-center align-center" height="400">
      <v-progress-circular
        indeterminate
        color="primary"
        size="64"
      ></v-progress-circular>
    </v-card>

    <template v-else-if="user">
      <v-row>
        <v-col cols="12" md="4">
          <v-card class="mb-4" elevation="2">
            <div class="d-flex flex-column align-center pa-4">
              <v-avatar size="120" class="mb-4">
                <v-img
                  :src="user.profilePicture || '/img/default-avatar.png'"
                  :alt="`${user.fullName}'s avatar`"
                ></v-img>
              </v-avatar>

              <h2 class="text-h5 mb-1">{{ user.fullName }}</h2>
              <p class="text-subtitle-1 mb-2 text-grey-darken-1">{{ user.email }}</p>

              <v-chip
                :color="user.roleId === ROLES.ADMIN ? 'error' : 'primary'"
                size="small"
                class="mb-4"
              >
                {{ user.roleId === ROLES.ADMIN ? 'Admin' : 'User' }}
              </v-chip>

              <v-chip
                :color="user.isActive ? 'success' : 'error'"
                size="small"
                class="mb-4"
              >
                {{ user.isActive ? 'Active' : 'Inactive' }}
              </v-chip>
            </div>

            <v-divider></v-divider>

            <v-list>
              <v-list-item>
                <template v-slot:prepend>
                  <v-icon icon="mdi-email"></v-icon>
                </template>
                <v-list-item-title>Email</v-list-item-title>
                <v-list-item-subtitle>{{ user.email }}</v-list-item-subtitle>
              </v-list-item>

              <v-list-item>
                <template v-slot:prepend>
                  <v-icon icon="mdi-clock"></v-icon>
                </template>
                <v-list-item-title>Last Login</v-list-item-title>
                <v-list-item-subtitle>{{ formatDate(user.lastLogin) }}</v-list-item-subtitle>
              </v-list-item>

              <v-list-item>
                <template v-slot:prepend>
                  <v-icon icon="mdi-calendar"></v-icon>
                </template>
                <v-list-item-title>Account Created</v-list-item-title>
                <v-list-item-subtitle>{{ formatDate(user.createdAt) }}</v-list-item-subtitle>
              </v-list-item>
            </v-list>

            <v-card-actions class="pa-4">
              <v-btn
                block
                color="primary"
                prepend-icon="mdi-pencil"
                @click="openEditDialog"
              >
                Edit User
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-col>

        <v-col cols="12" md="8">
          <v-card class="mb-4" elevation="2">
            <v-card-title>
              <v-icon class="mr-2">mdi-school</v-icon>
              Learning Information
            </v-card-title>
            <v-card-text>
              <v-row>
                <v-col cols="12" sm="6">
                  <div class="mb-3">
                    <div class="text-caption text-grey-darken-1">Current JLPT Level</div>
                    <div class="text-h6">{{ user.currentLevel || 'Not set' }}</div>
                  </div>
                </v-col>

                <v-col cols="12" sm="6">
                  <div class="mb-3">
                    <div class="text-caption text-grey-darken-1">JLPT Goal</div>
                    <div class="text-h6">{{ user.jlptGoal || 'Not set' }}</div>
                  </div>
                </v-col>

                <v-col cols="12" sm="6">
                  <div class="mb-3">
                    <div class="text-caption text-grey-darken-1">Streak Count</div>
                    <div class="text-h6">{{ user.streakCount || 0 }} days</div>
                  </div>
                </v-col>

                <v-col cols="12" sm="6">
                  <div class="mb-3">
                    <div class="text-caption text-grey-darken-1">Points</div>
                    <div class="text-h6">{{ user.points || 0 }} pts</div>
                  </div>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <v-card class="mb-4" elevation="2">
            <v-card-title>
              <v-icon class="mr-2">mdi-bell</v-icon>
              Notification Settings
            </v-card-title>
            <v-card-text>
              <v-row>
                <v-col cols="12" sm="6">
                  <div class="mb-3">
                    <div class="text-caption text-grey-darken-1">Reminders Enabled</div>
                    <div class="text-h6">
                      <v-chip
                        :color="user.reminderEnabled ? 'success' : 'error'"
                        size="small"
                      >
                        {{ user.reminderEnabled ? 'Enabled' : 'Disabled' }}
                      </v-chip>
                    </div>
                  </div>
                </v-col>

                <v-col cols="12" sm="6">
                  <div class="mb-3">
                    <div class="text-caption text-grey-darken-1">Reminder Time</div>
                    <div class="text-h6">{{ user.reminderTime || 'Not set' }}</div>
                  </div>
                </v-col>

                <v-col cols="12" sm="6">
                  <div class="mb-3">
                    <div class="text-caption text-grey-darken-1">Min Card Threshold</div>
                    <div class="text-h6">{{ user.minCardThreshold || 'Not set' }}</div>
                  </div>
                </v-col>

                <v-col cols="12" sm="6">
                  <div class="mb-3">
                    <div class="text-caption text-grey-darken-1">Notification Preferences</div>
                    <div class="text-h6">
                      <template v-if="user.notificationPreferences">
                        <v-chip
                          v-for="pref in user.notificationPreferences.split(',')"
                          :key="pref"
                          size="small"
                          class="mr-1"
                        >
                          {{ pref }}
                        </v-chip>
                      </template>
                      <span v-else>Not set</span>
                    </div>
                  </div>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <v-card elevation="2">
            <v-card-title>
              <v-icon class="mr-2">mdi-card-text</v-icon>
              Flashcards
            </v-card-title>
            <v-card-text>
              <div class="text-h6">{{ user.flashcardCount || 0 }} flashcards created</div>

              <v-row class="mt-4">
                <v-col cols="12" sm="6" md="4">
                  <v-card variant="outlined">
                    <v-card-text>
                      <div class="text-center">
                        <div class="text-h4 mb-1">{{ user.newCards || 0 }}</div>
                        <div>New</div>
                      </div>
                    </v-card-text>
                  </v-card>
                </v-col>

                <v-col cols="12" sm="6" md="4">
                  <v-card variant="outlined">
                    <v-card-text>
                      <div class="text-center">
                        <div class="text-h4 mb-1">{{ user.learningCards || 0 }}</div>
                        <div>Learning</div>
                      </div>
                    </v-card-text>
                  </v-card>
                </v-col>

                <v-col cols="12" sm="6" md="4">
                  <v-card variant="outlined">
                    <v-card-text>
                      <div class="text-center">
                        <div class="text-h4 mb-1">{{ user.masteredCards || 0 }}</div>
                        <div>Mastered</div>
                      </div>
                    </v-card-text>
                  </v-card>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </template>

    <!-- User Edit Dialog -->
    <v-dialog
      v-model="editDialog.visible"
      max-width="600px"
    >
      <v-card>
        <v-card-title>
          <span class="text-h5">Edit User</span>
        </v-card-title>

        <v-card-text>
          <v-form ref="form" v-model="formValid">
            <v-text-field
              v-model="editDialog.user.email"
              label="Email"
              prepend-inner-icon="mdi-email"
              :rules="[v => !!v || 'Email is required', v => /.+@.+\..+/.test(v) || 'Email must be valid']"
              required
            ></v-text-field>

            <v-text-field
              v-model="editDialog.user.fullName"
              label="Full Name"
              prepend-inner-icon="mdi-account"
              :rules="[v => !!v || 'Full name is required']"
              required
            ></v-text-field>

            <v-text-field
              v-model="editDialog.user.password"
              label="New Password (leave blank to keep current)"
              prepend-inner-icon="mdi-lock"
              :append-inner-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
              :type="showPassword ? 'text' : 'password'"
              :rules="[v => !v || v.length >= 6 || 'Password must be at least 6 characters']"
              @click:append-inner="showPassword = !showPassword"
            ></v-text-field>

            <v-select
              v-model="editDialog.user.currentLevel"
              label="Current Level"
              prepend-inner-icon="mdi-star"
              :items="jlptLevels"
            ></v-select>

            <v-select
              v-model="editDialog.user.jlptGoal"
              label="JLPT Goal"
              prepend-inner-icon="mdi-star-circle"
              :items="jlptLevels"
            ></v-select>

            <v-select
              v-model="editDialog.user.roleId"
              label="Role"
              prepend-inner-icon="mdi-shield-account"
              :items="roles"
              item-title="name"
              item-value="id"
            ></v-select>

            <v-switch
              v-model="editDialog.user.isActive"
              label="Account active"
              color="success"
              hide-details
              class="mt-2"
            ></v-switch>

            <v-switch
              v-model="editDialog.user.isEmailVerified"
              label="Email verified"
              color="info"
              hide-details
              class="mt-2"
            ></v-switch>

            <v-switch
              v-model="editDialog.user.reminderEnabled"
              label="Reminders enabled"
              color="purple"
              hide-details
              class="mt-2"
            ></v-switch>

            <v-text-field
              v-model="editDialog.user.reminderTime"
              label="Reminder Time (HH:MM)"
              prepend-inner-icon="mdi-clock"
              class="mt-4"
            ></v-text-field>

            <v-text-field
              v-model="editDialog.user.minCardThreshold"
              label="Min Card Threshold"
              prepend-inner-icon="mdi-card-multiple"
              type="number"
            ></v-text-field>
          </v-form>
        </v-card-text>

        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            color="grey"
            variant="text"
            @click="editDialog.visible = false"
          >
            Cancel
          </v-btn>
          <v-btn
            color="primary"
            variant="elevated"
            :disabled="!formValid"
            @click="saveUser"
          >
            Save
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
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { format, parseISO } from 'date-fns';
import adminService from '@/services/admin.service';
import type { UserInfo, UserUpdateRequest } from '@/services/admin.service';
import { ROLES } from '@/types/roles';

const route = useRoute();
const router = useRouter();
const userId = route.params.id as string;

// User data
const user = ref<any>(null);
const loading = ref(true);
const error = ref('');

// Form state
const formValid = ref(false);
const form = ref(null);
const showPassword = ref(false);

const editDialog = ref({
  visible: false,
  user: {} as any
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

// Load user data
const loadUser = async () => {
  loading.value = true;
  error.value = '';
  try {
    const response = await adminService.getUserById(userId);
    user.value = response;

    // Add some mock data for demonstration
    user.value = {
      ...response,
      createdAt: response.createdAt || '2023-01-01T00:00:00Z',
      streakCount: 12,
      points: 500,
      flashcardCount: 65,
      newCards: 5,
      learningCards: 45,
      masteredCards: 15,
      notificationPreferences: response.notificationPreferences || 'email,app'
    };
  } catch (error: any) {
    error.value = error.response?.data?.message || 'Failed to load user details';
  } finally {
    loading.value = false;
  }
};

// Open edit dialog
const openEditDialog = () => {
  if (!user.value) return;

  editDialog.value = {
    visible: true,
    user: { ...user.value }
  };
};

// Save user
const saveUser = async () => {
  try {
    const userData: UserUpdateRequest = {
      email: editDialog.value.user.email,
      fullName: editDialog.value.user.fullName,
      currentLevel: editDialog.value.user.currentLevel,
      jlptGoal: editDialog.value.user.jlptGoal,
      roleId: editDialog.value.user.roleId,
      isActive: editDialog.value.user.isActive,
      isEmailVerified: editDialog.value.user.isEmailVerified,
      reminderEnabled: editDialog.value.user.reminderEnabled,
      reminderTime: editDialog.value.user.reminderTime,
      minCardThreshold: editDialog.value.user.minCardThreshold,
      notificationPreferences: editDialog.value.user.notificationPreferences
    };

    if (editDialog.value.user.password) {
      userData.password = editDialog.value.user.password;
    }

    await adminService.updateUser(userId, userData);
    showSnackbar('User updated successfully');
    editDialog.value.visible = false;
    loadUser(); // Reload user data
  } catch (error: any) {
    showSnackbar(error.response?.data?.message || 'Failed to update user', 'error');
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

// Load user on component mount
onMounted(() => {
  loadUser();
});
</script>

<style scoped>
.user-detail {
  padding: 24px;
}
</style>
