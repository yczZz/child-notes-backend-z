/* admin/modules/babies/index.js */

import { state, api } from "../core/state.js";
import { toast } from "../core/toast.js";

export default {
  template: `
  <n-card class="section-card" size="small">
    <div class="section-head">
      <h3>宝宝列表</h3>
      <n-space class="section-toolbar">
        <n-input
          v-model:value="state.babies.keyword"
          placeholder="搜索宝宝名"
          clearable
          style="width:200px"
          @keyup.enter="search(1)"
        />
        <n-button type="primary" size="small" @click="search(1)">查询</n-button>
      </n-space>
    </div>

    <n-data-table
      :columns="cols"
      :data="state.babies.records"
      :loading="state.loading.babies"
      :bordered="false"
      :single-line="false"
      :scroll-x="1000"
      size="small"
      :virtual-scroll="false"
    />

    <div class="table-footer">
      <n-pagination
        :page="state.babies.page"
        :page-size="state.babies.pageSize"
        :item-count="state.babies.total"
        :show-size-picker="false"
        @update:page="search"
      />
    </div>
  </n-card>
  `,
  setup() {
    const { onMounted } = Vue;

    async function search(page = 1) {
      state.loading.babies = true;
      try {
        const d = await api.request(
          `/babies?page=${page}&pageSize=${state.babies.pageSize}&keyword=${encodeURIComponent(state.babies.keyword || "")}`
        );
        Object.assign(state.babies, d);
      } catch (e) {
        toast.message?.error(e.message);
      } finally {
        state.loading.babies = false;
      }
    }

    const cols = [
      { title: "ID", key: "id", width: 80 },
      { title: "姓名", key: "name", minWidth: 140, ellipsis: { tooltip: true } },
      { title: "性别", key: "gender", width: 80 },
      { title: "出生日期", key: "birthDate", width: 120 },
      { title: "天龄", key: "ageDays", width: 80 },
      { title: "创建用户", key: "ownerNickName", minWidth: 150, ellipsis: { tooltip: true } },
      { title: "家庭成员", key: "memberCount", width: 100 },
      { title: "创建时间", key: "createdAt", width: 180 },
    ];

    onMounted(() => search(state.babies.page));

    return { state, cols, search };
  },
};
