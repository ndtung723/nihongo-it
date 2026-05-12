<template>
  <v-dialog
    v-model="visible"
    max-width="420"
    persistent
    role="alertdialog"
    aria-labelledby="confirm-title"
    aria-describedby="confirm-desc"
  >
    <v-card>
      <v-card-title id="confirm-title" class="text-h6">
        {{ options.title ?? "Xác nhận" }}
      </v-card-title>
      <v-card-text id="confirm-desc">{{ options.message }}</v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="onCancel">
          {{ options.cancelText ?? "Hủy" }}
        </v-btn>
        <v-btn
          :color="options.type === 'error' ? 'error' : 'primary'"
          variant="elevated"
          @click="onConfirm"
        >
          {{ options.confirmText ?? "Xác nhận" }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { useConfirm } from "@/composables/useConfirm";
const { visible, options, onConfirm, onCancel } = useConfirm();
</script>
