<template>
	<v-container class="translation-container py-2 px-3">
		<div class="header d-flex align-center mb-2">
			<v-btn icon density="compact" class="mr-2" @click="$router.go(-1)">
				<v-icon>mdi-arrow-left</v-icon>
			</v-btn>
			<h1 class="text-h5 font-weight-bold mb-0">Công cụ dịch thuật</h1>
		</div>

		<v-card class="translation-card">
			<v-card-title class="translation-card-title py-2 px-3">
				<v-icon color="amber-darken-2" size="small" class="mr-1">mdi-translate</v-icon>
				<span class="text-subtitle-1">Dịch Tiếng Nhật - Tiếng Việt</span>
			</v-card-title>

			<v-card-text class="py-2 px-3">
				<div class="d-flex align-center flex-wrap">
					<v-btn-toggle
						v-model="translationDirection"
						mandatory
						color="primary"
						density="compact"
						class="mr-3 mb-1"
					>
						<v-btn value="vn-to-jp" class="translation-toggle-btn px-2" size="small">
							<v-icon size="small" class="mr-1">mdi-arrow-right</v-icon>
							<span class="text-caption">VN → JP</span>
						</v-btn>
						<v-btn value="jp-to-vn" class="translation-toggle-btn px-2" size="small">
							<v-icon size="small" class="mr-1">mdi-arrow-right</v-icon>
							<span class="text-caption">JP → VN</span>
						</v-btn>
					</v-btn-toggle>

					<!-- Model Selection Switch -->
					<div class="d-flex align-center mb-1">
						<v-switch
							v-model="useGpt4"
							:color="useGpt4 ? 'primary' : 'success'"
							hide-details
							density="compact"
							class="mini-switch mx-1"
							inset
						></v-switch>
						<span class="text-caption" :class="{ 'text-primary': useGpt4 }">
							{{ useGpt4 ? 'GPT-4o' : 'GPT-3.5 Turbo' }}
							<v-tooltip activator="parent" location="end">
								{{
									useGpt4
										? 'Độ chính xác cao hơn cho thuật ngữ chuyên ngành nhưng đắt hơn 20 lần'
										: 'Bật để có độ chính xác cao hơn'
								}}
							</v-tooltip>
						</span>
					</div>
				</div>

				<div class="d-flex flex-column flex-md-row mt-2">
					<div class="input-section flex-grow-1 mr-md-2 mb-3 mb-md-0">
						<div class="text-caption font-weight-medium mb-1">
							{{ translationDirection === 'vn-to-jp' ? 'Tiếng Việt' : 'Tiếng Nhật' }}
						</div>
						<v-textarea
							v-model="sourceText"
							:placeholder="
								translationDirection === 'vn-to-jp'
									? 'Nhập văn bản tiếng Việt tại đây...'
									: 'Nhập văn bản tiếng Nhật tại đây...'
							"
							variant="outlined"
							rows="6"
							hide-details
							density="compact"
							bg-color="grey-lighten-5"
							class="source-textarea rounded-lg"
						></v-textarea>
						<div class="d-flex justify-end align-center mt-1">
							<v-btn
								density="compact"
								size="x-small"
								variant="text"
								class="mr-2"
								@click="clearSourceText"
								:disabled="!sourceText"
							>
								<v-icon size="small">mdi-trash-can-outline</v-icon>
							</v-btn>
							<v-btn
								density="compact"
								size="x-small"
								variant="text"
								@click="copyText(sourceText)"
								:disabled="!sourceText"
							>
								<v-icon size="small">mdi-content-copy</v-icon>
							</v-btn>
						</div>
					</div>

					<div class="output-section flex-grow-1 ml-md-2">
						<div class="text-caption font-weight-medium mb-1">
							{{ translationDirection === 'vn-to-jp' ? 'Tiếng Nhật' : 'Tiếng Việt' }}
						</div>
						<div
							class="translation-output rounded-lg"
							:class="{
								'japanese-text': translationDirection === 'vn-to-jp',
								'vietnamese-text': translationDirection === 'jp-to-vn',
							}"
						>
							<div v-if="translating" class="translation-loading pa-2">
								<v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
								<span class="ml-2 text-caption">Đang dịch...</span>
							</div>
							<div v-else-if="translationResult" class="translation-text pa-2">
								{{ translationResult }}
							</div>
							<div v-else class="empty-translation pa-2 text-caption">
								Bản dịch sẽ hiển thị ở đây
							</div>
						</div>
						<div class="d-flex justify-end align-center mt-1">
							<v-btn
								density="compact"
								size="x-small"
								variant="text"
								class="mr-2"
								@click="speakText(translationResult)"
								:disabled="!translationResult"
							>
								<v-icon size="small">mdi-volume-high</v-icon>
							</v-btn>
							<v-btn
								density="compact"
								size="x-small"
								variant="text"
								@click="copyText(translationResult)"
								:disabled="!translationResult"
							>
								<v-icon size="small">mdi-content-copy</v-icon>
							</v-btn>
						</div>
					</div>
				</div>

				<div class="text-center mt-2">
					<v-btn
						color="primary"
						size="small"
						:loading="translating"
						:disabled="!sourceText"
						@click="translateText"
						rounded
						class="px-4"
					>
						<v-icon size="small" class="mr-1">mdi-translate</v-icon>
						Dịch
					</v-btn>
				</div>

				<!-- Translation History -->
				<div v-if="translationHistory.length > 0" class="mt-3 pb-0">
					<div class="d-flex justify-space-between align-center mb-1">
						<div class="text-subtitle-2">Lịch sử dịch</div>
						<v-btn
							density="compact"
							size="x-small"
							variant="text"
							color="error"
							@click="clearHistory"
						>
							<v-icon size="small" class="mr-1">mdi-delete-sweep</v-icon>
							Xóa
						</v-btn>
					</div>
					<v-list density="compact" class="pa-0 history-list">
						<v-list-item
							v-for="(item, index) in translationHistory"
							:key="index"
							@click="loadFromHistory(item)"
							class="history-item mb-1 px-2 py-1"
							rounded="lg"
						>
							<template v-slot:prepend>
								<v-icon :color="item.direction === 'vn-to-jp' ? 'green' : 'blue'" size="small">
									mdi-translate
								</v-icon>
							</template>
							<v-list-item-title class="text-caption d-flex align-center">
								<span class="text-caption text-medium-emphasis">
									{{ item.direction === 'vn-to-jp' ? 'VN → JP' : 'JP → VN' }}
								</span>
								<v-chip
									size="x-small"
									class="ml-1"
									:color="item.isEconomy ? 'success' : 'primary'"
									variant="outlined"
								>
									{{ item.isEconomy ? 'GPT-3.5' : 'GPT-4o' }}
								</v-chip>
								<span class="ml-2 text-truncate history-text">{{ item.source }}</span>
							</v-list-item-title>
							<v-list-item-subtitle class="text-truncate history-text text-caption">
								{{ item.result }}
							</v-list-item-subtitle>
						</v-list-item>
					</v-list>
				</div>
			</v-card-text>
		</v-card>
	</v-container>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import authService from '@/services/auth.service';
import api from '@/utils/api';

interface TranslationHistoryItem {
	direction: string;
	source: string;
	result: string;
	isEconomy: boolean;
	timestamp: number;
}

interface VocabularyTerm {
	japanese: string;
	vietnamese: string;
}

export default defineComponent({
	name: 'TranslationView',

	setup() {
		const route = useRoute();
		const router = useRouter();

		const sourceText = ref('');
		const translationResult = ref('');
		const translating = ref(false);
		const translationDirection = ref('vn-to-jp'); // Default: Vietnamese to Japanese
		const translationHistory = ref<TranslationHistoryItem[]>([]);
		const useGpt4 = ref(false); // Default: Use GPT-3.5 Turbo (false = don't use GPT-4)

		// Mock related vocabulary (could be fetched based on translation content)
		const relatedVocabulary = ref<VocabularyTerm[]>([
			{ japanese: 'プログラム', vietnamese: 'Chương trình' },
			{ japanese: 'データベース', vietnamese: 'Cơ sở dữ liệu' },
			{ japanese: 'ネットワーク', vietnamese: 'Mạng lưới' },
			{ japanese: 'ソフトウェア', vietnamese: 'Phần mềm' },
			{ japanese: 'セキュリティ', vietnamese: 'Bảo mật' },
			{ japanese: 'バグ修正', vietnamese: 'Sửa lỗi' },
		]);

		// Load data from URL if available
		onMounted(() => {
			// Check if there are query parameters
			if (route.query.text) {
				sourceText.value = route.query.text as string;

				if (route.query.dir) {
					translationDirection.value = route.query.dir as string;
				}

				// If there's a result parameter, just display it
				if (route.query.result) {
					translationResult.value = route.query.result as string;
				}
				// If there's text but no result, automatically translate
				else if (route.query.text) {
					// Trigger translation automatically
					translateText();
				}
			}

			// Load translation history from localStorage
			const savedHistory = localStorage.getItem('translationHistory');
			if (savedHistory) {
				try {
					translationHistory.value = JSON.parse(savedHistory);
				} catch (error) {}
			}
		});

		// Save translation history when it changes
		watch(
			translationHistory,
			(newValue) => {
				localStorage.setItem('translationHistory', JSON.stringify(newValue));
			},
			{ deep: true },
		);

		// Methods
		const translateText = async () => {
			if (!sourceText.value.trim()) return;

			translating.value = true;

			try {
				// Choose endpoint based on GPT-4 selection
				const endpoint = useGpt4.value ? 'translate' : 'translate/economy';

				// Call the translation API using axios instance
				const response = await api.post(
					`/api/v1/ai/chat/${endpoint}?direction=${translationDirection.value}`,
					sourceText.value,
					{
						headers: {
							'Content-Type': 'text/plain',
							Accept: 'application/json',
						},
					},
				);

				translationResult.value = response.data.translation;

				// Add to translation history with economy flag
				addToHistory();
			} catch (error: any) {
				if (error.response && (error.response.status === 401 || error.response.status === 403)) {
					alert('Bạn cần đăng nhập để sử dụng dịch vụ dịch thuật.');
				} else {
					alert(error.message || 'Dịch thuật thất bại. Vui lòng thử lại sau.');
				}
			} finally {
				translating.value = false;
			}
		};

		const clearSourceText = () => {
			sourceText.value = '';
			translationResult.value = '';
		};

		const copyText = (text: string) => {
			if (!text) return;

			navigator.clipboard
				.writeText(text)
				.then(() => {
					// Optional: Show a success message
					alert('Đã sao chép vào clipboard!');
				})
				.catch((err) => {});
		};

		const speakText = (text: string) => {
			if (!text) return;

			const utterance = new SpeechSynthesisUtterance(text);

			// Set the language based on the translation direction
			if (translationDirection.value === 'vn-to-jp') {
				utterance.lang = 'ja-JP'; // Japanese
			} else {
				utterance.lang = 'vi-VN'; // Vietnamese
			}

			window.speechSynthesis.speak(utterance);
		};

		const addToHistory = () => {
			// Add to the beginning of the array (most recent first)
			translationHistory.value.unshift({
				direction: translationDirection.value,
				source: sourceText.value,
				result: translationResult.value,
				isEconomy: !useGpt4.value, // false means GPT-4, true means economy
				timestamp: Date.now(),
			});

			// Limit history size
			if (translationHistory.value.length > 10) {
				translationHistory.value = translationHistory.value.slice(0, 10);
			}
		};

		const loadFromHistory = (item: TranslationHistoryItem) => {
			translationDirection.value = item.direction;
			sourceText.value = item.source;
			translationResult.value = item.result;
			useGpt4.value = !item.isEconomy; // Convert from isEconomy to useGpt4
		};

		const clearHistory = () => {
			if (confirm('Bạn có chắc chắn muốn xóa lịch sử dịch?')) {
				translationHistory.value = [];
			}
		};

		return {
			sourceText,
			translationResult,
			translating,
			translationDirection,
			useGpt4,
			relatedVocabulary,
			translationHistory,
			translateText,
			clearSourceText,
			copyText,
			speakText,
			loadFromHistory,
			clearHistory,
		};
	},
});
</script>

<style scoped lang="scss">
.translation-container {
	max-width: 900px;
	margin: 0 auto;
}

.translation-card {
	border-radius: 8px;
	overflow: hidden;
	box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
}

.translation-card-title {
	background-color: rgba(248, 248, 248, 0.8);
	border-bottom: 1px solid #e0e0e0;
	font-weight: 500;
}

.input-section,
.output-section {
	height: 100%;
	display: flex;
	flex-direction: column;
}

.source-textarea {
	flex-grow: 1;
	font-size: 0.9rem;

	:deep(.v-field__input) {
		padding: 8px;
	}
}

.translation-output {
	border: 1px solid #e0e0e0;
	border-radius: 4px;
	min-height: 146px; // Match textarea height
	background-color: rgba(249, 249, 249, 0.5);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-grow: 1;
	overflow: auto;
}

.translation-text {
	width: 100%;
	height: 100%;
	white-space: pre-wrap;
	word-break: break-word;
	min-height: 146px;
	font-size: 0.9rem;
}

.empty-translation {
	color: #9e9e9e;
	font-style: italic;
}

.translation-toggle-btn {
	height: 28px;
	min-width: 80px;
}

.mini-switch {
	margin-top: 0;
	margin-bottom: 0;
}

:deep(.mini-switch .v-switch__track) {
	opacity: 0.5;
	transform: scale(0.75);
}

:deep(.mini-switch .v-switch__thumb) {
	transform: scale(0.75);
}

.japanese-text {
	font-family: 'Noto Sans JP', sans-serif;
	line-height: 1.4;
}

.vietnamese-text {
	line-height: 1.4;
}

.history-list {
	max-height: 300px;
	overflow-y: auto;
	background-color: transparent;
}

.history-item {
	border-radius: 4px;
	transition: background-color 0.2s;
	border: 1px solid rgba(0, 0, 0, 0.05);

	&:hover {
		background-color: rgba(245, 245, 245, 0.8);
	}
}

.history-text {
	max-width: 200px;
	@media (min-width: 600px) {
		max-width: 300px;
	}
}
</style>
