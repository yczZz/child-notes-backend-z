/* admin/modules/dashboard/index.js */

import { state } from "../core/state.js";

export default {
  template: `
  <div class="metric-row">
    <div class="metric-card" v-for="m in metrics" :key="m.label">
      <span class="mc-icon">{{ m.icon }}</span>
      <span class="mc-label">{{ m.label }}</span>
      <span class="mc-value">{{ m.value }}</span>
    </div>
  </div>
  `,

  setup() {
    const { ref, computed, onMounted } = Vue;

    const metrics = computed(() => {
      const o = state.overview;
      return [
        { icon: "👥", label: "总用户",       value: o.totalUsers ?? 0 },
        { icon: "✨", label: "今日新增用户", value: o.todayUsers ?? 0 },
        { icon: "👶", label: "总宝宝",       value: o.totalBabies ?? 0 },
        { icon: "🎉", label: "今日新增宝宝", value: o.todayBabies ?? 0 },
        { icon: "📝", label: "草稿抽奖",     value: o.draftLotteryCount ?? 0 },
        { icon: "🚀", label: "已发布抽奖",   value: o.publishedLotteryCount ?? 0 },
      ];
    });

    return { metrics };
  },
};
