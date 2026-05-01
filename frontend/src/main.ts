import '@/assets/styles/main.sass';
import 'vuetify/styles';
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import vuetify from './plugins/vuetify';
import App from './App.vue';
import router from './router';
import VueToast from 'vue-toast-notification';
import 'vue-toast-notification/dist/theme-sugar.css';
import { useAuthStore } from './stores';
import vue3GoogleLogin from 'vue3-google-login';

const app = createApp(App);
const pinia = createPinia();

app.use(vuetify);
app.use(pinia);
app.use(router);
app.use(VueToast);
app.use(vue3GoogleLogin, {
	clientId: import.meta.env.VITE_GOOGLE_CLIENT_ID || 'YOUR_GOOGLE_CLIENT_ID_HERE',
});

// Initialize auth state before mounting the app
const authStore = useAuthStore();
authStore.initializeAuth();

app.mount('#app');
