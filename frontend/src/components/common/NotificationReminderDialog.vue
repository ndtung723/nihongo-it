<template>
  <v-dialog v-model="show" max-width="450">
    <v-card class="reminder-dialog">
      <v-card-title class="text-subtitle-1 d-flex align-center">
        <v-icon color="primary" size="small" class="mr-2">mdi-bell-ring</v-icon>
        Nhắc nhở ôn tập thẻ ghi nhớ
      </v-card-title>

      <v-divider></v-divider>

      <v-card-text class="py-4">
        <div class="d-flex">
          <v-icon color="primary" size="large" class="mr-4 mt-1"
            >mdi-book-open-variant</v-icon
          >
          <div>
            <div class="text-body-1 font-weight-medium mb-2">
              Bạn có {{ dueCardCount }} thẻ ghi nhớ cần ôn tập!
            </div>
            <div class="text-body-2 text-medium-emphasis">
              Ôn tập thường xuyên giúp việc ghi nhớ trở nên hiệu quả hơn. Dành
              vài phút để ôn tập ngay bây giờ nhé?
            </div>
          </div>
        </div>
      </v-card-text>

      <v-card-actions class="pb-4 px-4">
        <v-spacer></v-spacer>
        <v-btn
          variant="outlined"
          color="grey"
          @click="closeDialog"
          size="small"
        >
          Để sau
        </v-btn>
        <v-btn color="primary" @click="goToStudy" size="small" class="ml-2">
          Ôn tập ngay
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits, onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import flashcardService from "@/services/flashcard.service";

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits(["update:modelValue"]);

const router = useRouter();
const dueCardCount = ref(0);
const show = ref(props.modelValue);

// Watch for changes to the model value
watch(
  () => props.modelValue,
  (newValue) => {
    show.value = newValue;
  },
);

// Update the model value when internal show changes
watch(
  () => show.value,
  (newValue) => {
    emit("update:modelValue", newValue);
  },
);

onMounted(async () => {
  await fetchDueCardCount();
});

async function fetchDueCardCount() {
  try {
    const dueCards = await flashcardService.getDueCards();
    dueCardCount.value = dueCards.length;
  } catch {}
}

function closeDialog() {
  show.value = false;
}

function goToStudy() {
  router.push({ name: "flashcardStudy" });
  show.value = false;
}
</script>

<style scoped>
.reminder-dialog {
  border-radius: 12px;
  overflow: hidden;
}
</style>
