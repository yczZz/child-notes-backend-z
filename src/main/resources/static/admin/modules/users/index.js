/* admin/modules/users/index.js */

import { state, api } from "../core/state.js";
import { toast } from "../core/toast.js";

export default {
  template: `
  <n-card class="section-card" size="small">
    <div class="section-head">
      <h3>用户列表</h3>
      <n-space class="section-toolbar">
        <n-input
          v-model:value="state.users.keyword"
          placeholder="搜索昵称"
          clearable
          style="width:200px"
          @keyup.enter="search(1)"
        />
        <n-button type="primary" size="small" @click="search(1)">查询</n-button>
      </n-space>
    </div>

    <n-data-table
      :columns="cols"
      :data="state.users.records"
      :loading="state.loading.users"
      :bordered="false"
      :single-line="false"
      :scroll-x="900"
      size="small"
      :virtual-scroll="false"
    />

    <div class="table-footer">
      <n-pagination
        :page="state.users.page"
        :page-size="state.users.pageSize"
        :item-count="state.users.total"
        :show-size-picker="false"
        @update:page="search"
      />
    </div>
  </n-card>
  `,
  setup() {
    const { onMounted } = Vue;

    async function search(page = 1) {
      state.loading.users = true;
      try {
        const d = await api.request(
          `/users?page=${page}&pageSize=${state.users.pageSize}&keyword=${encodeURIComponent(state.users.keyword || "")}`
        );
        Object.assign(state.users, d);
      } catch (e) {
        toast.message?.error(e.message);
      } finally {
        state.loading.users = false;
      }
    }

    const cols = [
      { title: "ID", key: "id", width: 80 },
      { title: "昵称", key: "nickName", minWidth: 160, ellipsis: { tooltip: true } },
      { title: "关联宝宝", key: "babyCount", width: 100 },
      { title: "推荐人ID", key: "referrerUserId", width: 110 },
      { title: "注册时间", key: "createdAt", width: 180 },
    ];

    onMounted(() => search(state.users.page));

    return { state, cols, search };
  },
};
