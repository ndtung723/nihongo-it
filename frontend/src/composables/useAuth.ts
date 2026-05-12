import { computed } from "vue";
import { useAuthStore } from "@/stores";
import { ROLES } from "@/types/roles";

export function useAuth() {
  const store = useAuthStore();

  const isAuthenticated = computed(() => store.isAuthenticated);
  const user = computed(() => store.user);
  const isAdmin = computed(() => store.user?.roleId === ROLES.ADMIN);

  return {
    isAuthenticated,
    user,
    isAdmin,
    login: store.login,
    logout: store.logout,
    register: store.register,
  };
}
