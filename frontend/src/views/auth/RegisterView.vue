<template>
  <v-container class="register-container" fluid>
    <v-row justify="center" align="center">
      <v-col cols="12" sm="8" md="6" lg="5">
        <v-card class="register-card">
          <v-card-title class="text-center">
            <h1 class="title">Register</h1>
          </v-card-title>

          <v-card-text>
            <v-form ref="form" @submit.prevent="handleRegister">
              <v-row>
                <v-col cols="12">
                  <v-text-field
                    v-model="formData.email"
                    label="Email"
                    type="email"
                    prepend-icon="mdi-email"
                    required
                  ></v-text-field>
                </v-col>

                <v-col cols="12">
                  <v-text-field
                    v-model="formData.fullName"
                    label="Full Name"
                    prepend-icon="mdi-account"
                    required
                  ></v-text-field>
                </v-col>

                <v-col cols="12">
                  <v-text-field
                    v-model="formData.password"
                    label="Password"
                    type="password"
                    prepend-icon="mdi-lock"
                    required
                  ></v-text-field>
                </v-col>

                <v-col cols="12">
                  <v-text-field
                    v-model="confirmPassword"
                    label="Confirm Password"
                    type="password"
                    prepend-icon="mdi-lock-check"
                    required
                  ></v-text-field>
                </v-col>

                <v-col cols="6">
                  <v-select
                    v-model="formData.currentLevel"
                    :items="japaneseLevel"
                    label="Current Japanese Level"
                    prepend-icon="mdi-book-open-variant"
                  ></v-select>
                </v-col>

                <v-col cols="6">
                  <v-select
                    v-model="formData.jlptGoal"
                    :items="jlptLevels"
                    label="JLPT Goal"
                    prepend-icon="mdi-flag"
                  ></v-select>
                </v-col>
              </v-row>

              <v-btn
                type="submit"
                color="primary"
                block
                class="mt-4"
                :loading="loading"
                :disabled="!isFormValid"
              >
                Register
              </v-btn>

              <div class="text-center mt-4">
                <router-link
                  :to="{ name: 'login' }"
                  class="text-decoration-none"
                >
                  Already have an account? Login
                </router-link>
              </div>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores";
import { useAppToast } from "@/composables/useAppToast";

const authStore = useAuthStore();
const router = useRouter();
const toast = useAppToast();

const form = ref<unknown>(null);
const formData = reactive({
  email: "",
  fullName: "",
  password: "",
  currentLevel: "N5",
  jlptGoal: "N3",
  profilePicture: "",
});
const confirmPassword = ref("");

const loading = computed(() => authStore.loading);
const isFormValid = computed(
  () =>
    !!formData.email &&
    !!formData.fullName &&
    !!formData.password &&
    formData.password === confirmPassword.value,
);

const japaneseLevel = [
  { value: "BEGINNER", title: "Beginner" },
  { value: "N5", title: "N5" },
  { value: "N4", title: "N4" },
  { value: "N3", title: "N3" },
  { value: "N2", title: "N2" },
  { value: "N1", title: "N1" },
];

const jlptLevels = [
  { value: "N5", title: "N5" },
  { value: "N4", title: "N4" },
  { value: "N3", title: "N3" },
  { value: "N2", title: "N2" },
  { value: "N1", title: "N1" },
];

function validateForm(): boolean {
  const errors: string[] = [];

  if (!formData.email) {
    errors.push("Email is required");
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
    errors.push("Email format is invalid");
  }

  if (!formData.fullName) {
    errors.push("Full name is required");
  } else if (formData.fullName.length < 2) {
    errors.push("Full name must be at least 2 characters");
  }

  if (!formData.password) {
    errors.push("Password is required");
  } else if (formData.password.length < 6) {
    errors.push("Password must be at least 6 characters");
  }

  if (formData.password !== confirmPassword.value) {
    errors.push("Passwords do not match");
  }

  errors.forEach((error) => toast.error(error));
  return errors.length === 0;
}

async function handleRegister() {
  if (!validateForm()) return;

  try {
    const success = await authStore.register(formData);
    if (success) {
      toast.success("Registration successful! Please login.");
      router.push({ name: "login" });
    } else if (authStore.error) {
      toast.error(authStore.error);
    }
  } catch {
    toast.error("Registration failed. Please try again.");
  }
}
</script>

<style lang="sass" scoped>
.register-container
  min-height: 100vh
  display: flex
  align-items: center

.register-card
  width: 100%
  padding: 1rem

.title
  color: #333
  margin-bottom: 1rem
  width: 100%

::v-deep .v-text-field
  margin-bottom: 0.5rem

// Add styles to reduce column height and hide message details
::v-deep .v-col
  padding-top: 4px
  padding-bottom: 4px

::v-deep .v-input__details
  display: none !important
  margin: 0
  padding: 0
  min-height: 0

::v-deep .v-messages
  min-height: 0
</style>
