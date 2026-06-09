/* admin/modules/app.js — 应用入口 */

import { state } from "./core/state.js";
import { toast } from "./core/toast.js";
import LoginView from "./auth/login.js";
import LayoutView from "./core/layout.js";
import { fetchOverview } from "./core/data.js";

const { createApp, nextTick } = Vue;

const AdminRoot = {
  components: { LoginView, LayoutView },

  template: `
  <login-view
    v-if="!state.token.value"
    @logged-in="onLoggedIn"
  />
  <layout-view
    v-else
    @view-change="onViewChange"
  />
  `,

  setup() {
    toast.message = naive.useMessage();
    toast.notification = naive.useNotification();

    async function onLoggedIn() {
      await nextTick();
      await onViewChange("dashboard");
    }

    async function onViewChange(key) {
      if (key === "dashboard") {
        await fetchOverview();
      }
    }

    return { state, onLoggedIn, onViewChange };
  },
};

const App = {
  components: { AdminRoot },

  template: `
  <n-message-provider>
    <n-notification-provider>
      <n-dialog-provider>
        <admin-root />
      </n-dialog-provider>
    </n-notification-provider>
  </n-message-provider>
  `,
};

createApp(App).use(naive).mount("#app");
