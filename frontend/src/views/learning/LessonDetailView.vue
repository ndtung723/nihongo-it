<template>
  <div class="lesson-detail-container">
    <!-- Coach Header -->
    <div class="header px-4 py-3 d-flex justify-space-between align-center">
      <div class="d-flex align-center">
        <img
          src="https://via.placeholder.com/40/1abc9c/ffffff?text=TC"
          alt="The Coach"
          class="coach-logo mr-2"
          loading="lazy"
        />
        <div class="text-h6 font-weight-bold text-dark">
          the<span class="primary--text">coach</span>
        </div>
      </div>
      <div class="d-flex align-center">
        <v-btn
          color="warning"
          variant="flat"
          class="upgrade-btn mr-2"
          rounded="pill"
        >
          Nâng cấp Pro <v-icon small class="ml-1">mdi-crown</v-icon>
        </v-btn>
        <v-btn icon variant="text">
          <v-icon>mdi-bell-outline</v-icon>
        </v-btn>
      </div>
    </div>

    <!-- Course Title Header -->
    <div class="px-4 my-4">
      <div class="text-subtitle-1 text-grey text-center">CHỦ ĐỀ 1</div>
      <div class="text-h4 font-weight-bold text-center">Tình huống học thử</div>
    </div>

    <!-- Course Preview Card -->
    <div class="px-4 mb-4">
      <v-card class="course-card" variant="outlined" rounded="lg">
        <v-img
          src="https://via.placeholder.com/600/3f51b5/ffffff?text=Interview"
          height="200"
          cover
          class="position-relative"
        >
          <div
            class="course-overlay d-flex flex-column justify-center align-center"
          >
            <div
              class="level-badge position-absolute"
              style="top: 12px; left: 12px"
            >
              <div class="text-uppercase font-weight-medium">DỄ</div>
            </div>
            <div class="text-h5 font-weight-bold text-white text-center pb-4">
              Vượt qua bài phỏng vấn tuyển dụng
            </div>
            <div class="text-subtitle-1 text-white">16 Bài học</div>
          </div>
        </v-img>
      </v-card>
    </div>

    <!-- Lesson Category Label -->
    <div class="px-4 mb-2 d-flex align-center">
      <div class="d-flex align-center">
        <v-avatar size="30" color="#f5f5f5" class="mr-3">
          <v-icon size="18">mdi-puzzle</v-icon>
        </v-avatar>
        <span class="text-subtitle-1 font-weight-bold">NẠP CỤM</span>
      </div>
    </div>

    <!-- Lessons List -->
    <div class="lessons-list px-4">
      <!-- Lesson 1 -->
      <v-card
        class="lesson-item mb-4"
        variant="outlined"
        rounded="lg"
        elevation="0"
        @click="openLesson('1')"
      >
        <div class="d-flex pa-4">
          <v-avatar color="#f8ecde" size="60" class="lesson-avatar mr-4">
            <img
              src="https://via.placeholder.com/60/ff9955/ffffff?text=A"
              alt="Avatar"
              loading="lazy"
            />
          </v-avatar>
          <div class="flex-grow-1">
            <div class="d-flex">
              <div class="text-subtitle-2 text-grey">01 | HỌC CỤM CHUNKING</div>
            </div>
            <div class="text-h6 font-weight-bold mt-1">
              Which company do you work at?
            </div>
          </div>
        </div>
      </v-card>

      <!-- Lesson 2 -->
      <v-card
        class="lesson-item mb-4"
        variant="outlined"
        rounded="lg"
        elevation="0"
        @click="openLesson('2')"
      >
        <div class="d-flex pa-4">
          <v-avatar color="#e3f2fd" size="60" class="lesson-avatar mr-4">
            <v-icon color="primary" size="24">mdi-video</v-icon>
          </v-avatar>
          <div class="flex-grow-1">
            <div class="d-flex">
              <div class="text-subtitle-2 text-grey">02 | VIDEO HƯỚNG DẪN</div>
            </div>
            <div class="text-h6 font-weight-bold mt-1">
              Tại sao bạn hiểu nhưng không nói được
            </div>
          </div>
        </div>
      </v-card>

      <!-- Lesson 3 -->
      <v-card
        class="lesson-item mb-4"
        variant="outlined"
        rounded="lg"
        elevation="0"
        @click="openLesson('3')"
      >
        <div class="d-flex pa-4">
          <v-avatar color="#f8ecde" size="60" class="lesson-avatar mr-4">
            <img
              src="https://via.placeholder.com/60/ff9955/ffffff?text=A"
              alt="Avatar"
              loading="lazy"
            />
          </v-avatar>
          <div class="flex-grow-1">
            <div class="d-flex">
              <div class="text-subtitle-2 text-grey">03 | HỌC CỤM CHUNKING</div>
            </div>
            <div class="text-h6 font-weight-bold mt-1">
              How long have you been working there?
            </div>
          </div>
        </div>
      </v-card>

      <!-- Lesson 4 -->
      <v-card
        class="lesson-item mb-4"
        variant="outlined"
        rounded="lg"
        elevation="0"
        @click="openLesson('4')"
      >
        <div class="d-flex pa-4">
          <v-avatar color="#f8ecde" size="60" class="lesson-avatar mr-4">
            <img
              src="https://via.placeholder.com/60/ff9955/ffffff?text=A"
              alt="Avatar"
              loading="lazy"
            />
          </v-avatar>
          <div class="flex-grow-1">
            <div class="d-flex">
              <div class="text-subtitle-2 text-grey">04 | HỌC CỤM CHUNKING</div>
            </div>
            <div class="text-h6 font-weight-bold mt-1">
              What do you like about our company?
            </div>
          </div>
        </div>
      </v-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";

const currentTime = ref("21:21");
const dataRate = ref("0.16 KB/s");

// Methods
function updateTime() {
  const date = new Date();
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  currentTime.value = `${hours}:${minutes}`;
}

function updateDataRate() {
  const rates = ["0.16 KB/s", "2.36 KB/s", "23.0 KB/s"];
  dataRate.value = rates[Math.floor(Math.random() * rates.length)];
}

function openLesson(_lessonId: string) {
  // For demonstration, navigation is disabled
  // router.push(`/learning/lesson/${courseId}/${_lessonId}`)
}

// Lifecycle hooks
onMounted(() => {
  updateTime();
  updateDataRate();

  // Update time every minute
  setInterval(updateTime, 60000);

  // Update data rate occasionally (for demonstration only)
  setInterval(updateDataRate, 10000);
});
</script>

<style scoped lang="scss">
.lesson-detail-container {
  background-color: #f8f9fa;
  min-height: 100vh;
  padding-bottom: 64px;
}

.status-bar {
  margin-bottom: 4px;
}

.time-display {
  font-weight: 500;
}

.data-rate {
  font-size: 0.7rem;
}

.status-icons {
  display: flex;
  align-items: center;
}

.battery-status {
  display: flex;
  align-items: center;
}

.coach-logo {
  width: 40px;
  height: 40px;
  border-radius: 8px;
}

.upgrade-btn {
  font-size: 0.9rem;
  font-weight: 600;
  text-transform: none;
  letter-spacing: 0;
}

.course-card {
  overflow: hidden;
}

.course-overlay {
  background: linear-gradient(
    0deg,
    rgba(0, 0, 0, 0.7) 0%,
    rgba(0, 0, 0, 0.3) 100%
  );
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  padding: 20px;
}

.level-badge {
  background: rgba(0, 0, 0, 0.5);
  color: white;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 0.75rem;
}

.lesson-item {
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
  cursor: pointer;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1) !important;
  }
}

.lesson-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
}

.bottom-navigation {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  z-index: 10;
}
</style>
