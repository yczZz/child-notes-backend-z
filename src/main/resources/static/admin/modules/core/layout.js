/* admin/modules/core/layout.js */

import { state } from "./state.js";
import { toast } from "./toast.js";

import DashboardView from "../dashboard/index.js";
import UsersView from "../users/index.js";
import BabiesView from "../babies/index.js";
import LotteriesView from "../lotteries/index.js";

const { ref, computed } = Vue;

const navItems = [
  { key: "dashboard", label: "📊 概览" },
  { key: "users",     label: "👥 用户统计" },
  { key: "babies",    label: "👶 宝宝统计" },
  { key: "lotteries", label: "🎁 抽奖发布" },
];

export default {
  components: {
    DashboardView,
    UsersView,
    BabiesView,
    LotteriesView,
  },

  template: `
  <n-layout class="layout-shell" has-sider position="absolute">

    <!-- sidebar -->
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="232"
      :collapsed="collapsed"
      @update:collapsed="collapsed = $event"
      :native-scrollbar="false"
    >
      <div class="sidebar-logo">
        <span class="logo-dot">👶</span>
        <span v-if="!collapsed">宝宝手记后台</span>
      </div>
      <n-menu
        :value="activeView"
        :collapsed="collapsed"
        :options="menuOptions"
        @update:value="switchView"
      />
    </n-layout-sider>

    <!-- main area -->
    <n-layout>
      <!-- topbar -->
      <n-layout-header class="topbar" bordered>
        <n-space align="center">
          <n-button size="small" quaternary @click="collapsed = !collapsed">
            {{ collapsed ? '☰' : '✕' }}
          </n-button>
          <h2>{{ activeTitle }}</h2>
        </n-space>

        <n-space align="center" class="topbar-right">
          <n-tag type="success" size="small" round>{{ username }}</n-tag>
          <n-button size="small" text @click="handleLogout">退出登录</n-button>
        </n-space>
      </n-layout-header>

      <!-- content -->
      <n-layout-content class="content-area" :native-scrollbar="false">
        <dashboard-view v-if="activeView === 'dashboard'" />
        <users-view     v-else-if="activeView === 'users'" />
        <babies-view    v-else-if="activeView === 'babies'" />
        <lotteries-view v-else-if="activeView === 'lotteries'" />
      </n-layout-content>
    </n-layout>

  </n-layout>
  `,

  emits: ["viewChange", "logout"],

  setup(props, { emit }) {
    const collapsed = ref(false);
    const activeView = ref("dashboard");

    const activeTitle = computed(() => {
      const item = navItems.find(i => i.key === activeView.value);
      return item ? item.label.replace(/^.\s/, "") : "";
    });

    const menuOptions = navItems.map(item => ({
      label: item.label,
      key: item.key,
    }));

    const username = state.savedUsername();

    function switchView(key) {
      activeView.value = key;
      emit("viewChange", key);
    }

    async function handleLogout() {
      try {
        await fetch("/admin/api/auth/logout", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${state.token.value}`,
          },
        });
      } catch (_) { /* ignore */ }
      state.clearSession();
    }

    return {
      collapsed, activeView, activeTitle, menuOptions, username,
      switchView, handleLogout,
    };
  },
};
