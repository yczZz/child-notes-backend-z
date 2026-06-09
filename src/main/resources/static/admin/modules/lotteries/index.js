/* admin/modules/lotteries/index.js */

import { state, api } from "../core/state.js";
import { toast } from "../core/toast.js";

const { reactive, ref } = Vue;

/* ---- helpers ---- */
function statusLabel(s) {
  return { draft: "草稿", published: "已发布", closed: "已结束" }[s] || s;
}
function toTimestamp(v) {
  if (!v) return null;
  if (typeof v === "string") return new Date(v.replace(" ", "T")).getTime();
  return v instanceof Date ? v.getTime() : new Date(v).getTime();
}
function toDatetimeStr(ts) {
  if (!ts) return "";
  const d = new Date(ts);
  const p = n => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

function emptyForm() {
  const now = new Date();
  const draw = new Date(now.getTime() + 7 * 86400000);
  return {
    id: null, title: "", description: "", coverImage: "",
    startTime: now.getTime(), drawTime: draw.getTime(),
    costPoints: 0, winnerCount: 1, status: "draft",
    prizes: [{ prizeName: "", prizeIntro: "", prizeImage: "", prizeCount: 1 }],
  };
}

const statusOptions = [
  { label: "全部", value: "all" },
  { label: "草稿", value: "draft" },
  { label: "已发布", value: "published" },
  { label: "已结束", value: "closed" },
];
const editStatusOptions = statusOptions.filter(o => o.value !== "all");

export default {
  template: `
  <div>
    <n-card class="section-card" size="small">
      <div class="section-head">
        <h3>抽奖管理</h3>
        <n-space class="section-toolbar">
          <n-select
            v-model:value="state.lotteries.status"
            :options="statusOptions"
            style="width:120px"
            size="small"
            @update:value="fetchList(1)"
          />
          <n-button type="primary" size="small" @click="openDialog(null)">+ 新建抽奖</n-button>
        </n-space>
      </div>

      <n-data-table
        :columns="cols"
        :data="state.lotteries.records"
        :loading="state.loading.lotteries"
        :bordered="false"
        :single-line="false"
        :scroll-x="900"
        size="small"
        :virtual-scroll="false"
        :row-props="rowProps"
      />

      <div class="table-footer">
        <n-pagination
          :page="state.lotteries.page"
          :page-size="state.lotteries.pageSize"
          :item-count="state.lotteries.total"
          :show-size-picker="false"
          @update:page="fetchList"
        />
      </div>
    </n-card>

    <!-- modal -->
    <n-modal
      v-model:show="dialog.visible"
      :title="dialog.form.id ? '编辑抽奖' : '新建抽奖'"
      style="max-width:720px"
      preset="card"
      :segmented="{ content:'soft', footer:'soft' }"
    >
      <n-form :model="dialog.form" label-placement="top" size="medium">
        <n-form-item label="标题" path="title">
          <n-input v-model:value="dialog.form.title" maxlength="128" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="dialog.form.description" type="textarea" :rows="3" maxlength="1000" />
        </n-form-item>
        <n-form-item label="封面图" path="coverImage">
          <n-input v-model:value="dialog.form.coverImage" placeholder="图片URL" />
        </n-form-item>
        <n-form-item label="时间">
          <n-space :size="12">
            <n-date-picker v-model:value="dialog.form.startTime" type="datetime" placeholder="开始时间" style="flex:1" />
            <n-date-picker v-model:value="dialog.form.drawTime" type="datetime" placeholder="开奖时间" style="flex:1" />
          </n-space>
        </n-form-item>
        <n-form-item label="规则">
          <n-space :size="12">
            <n-input-number v-model:value="dialog.form.costPoints" :min="0" :max="999999" style="width:140px" />
            <n-input-number v-model:value="dialog.form.winnerCount" :min="1" :max="9999" style="width:120px" />
            <n-select v-model:value="dialog.form.status" :options="editStatusOptions" style="width:120px" />
          </n-space>
        </n-form-item>
        <n-form-item label="奖品">
          <div class="prize-grid">
            <div v-for="(prize, idx) in dialog.form.prizes" :key="idx" class="prize-row">
              <n-input v-model:value="prize.prizeName" placeholder="奖品名" />
              <n-input v-model:value="prize.prizeIntro" placeholder="说明" />
              <n-input-number v-model:value="prize.prizeCount" :min="1" :max="9999" />
              <n-button type="error" size="small" text @click="removePrize(idx)">删除</n-button>
            </div>
            <n-button dashed size="small" @click="addPrize">+ 添加奖品</n-button>
          </div>
        </n-form-item>
      </n-form>

      <!-- action bar in modal -->
      <template #footer>
        <n-space justify="space-between">
          <n-space v-if="dialog.form.id" :size="8">
            <n-button v-if="dialog.form.status !== 'published'" type="success" size="small" @click="doPublish">发布</n-button>
            <n-button v-if="dialog.form.status !== 'closed'" type="error" size="small" @click="doClose">结束</n-button>
          </n-space>
          <n-space :size="8">
            <n-button @click="dialog.visible = false">取消</n-button>
            <n-button type="primary" :loading="state.loading.save" @click="saveDialog">保存</n-button>
          </n-space>
        </n-space>
      </template>
    </n-modal>
  </div>
  `,
  setup() {
    const { onMounted } = Vue;
    const dialog = reactive({ visible: false, form: emptyForm() });

    async function fetchList(page = 1) {
      state.loading.lotteries = true;
      try {
        const d = await api.request(
          `/lotteries?page=${page}&pageSize=${state.lotteries.pageSize}&status=${encodeURIComponent(state.lotteries.status)}`
        );
        Object.assign(state.lotteries, d);
      } catch (e) {
        toast.message?.error(e.message);
      } finally {
        state.loading.lotteries = false;
      }
    }

    function rowProps(row) {
      return {
        style: "cursor:pointer",
        onClick: () => openDialog(row),
      };
    }

    function openDialog(row) {
      if (row && row.id) {
        dialog.form = {
          id: row.id,
          title: row.title || "",
          description: row.description || "",
          coverImage: row.coverImage || "",
          startTime: toTimestamp(row.startTime),
          drawTime: toTimestamp(row.drawTime),
          costPoints: row.costPoints || 0,
          winnerCount: row.winnerCount || 1,
          status: row.status || "draft",
          prizes: (row.prizes || []).map(p => ({ ...p })),
        };
      } else {
        dialog.form = emptyForm();
      }
      if (!dialog.form.prizes.length) addPrize();
      dialog.visible = true;
    }

    function addPrize() {
      dialog.form.prizes.push({ prizeName: "", prizeIntro: "", prizeImage: "", prizeCount: 1 });
    }
    function removePrize(idx) {
      dialog.form.prizes.splice(idx, 1);
      if (!dialog.form.prizes.length) addPrize();
    }

    async function saveDialog() {
      state.loading.save = true;
      try {
        const f = dialog.form;
        const body = {
          ...f,
          startTime: toDatetimeStr(f.startTime),
          drawTime: toDatetimeStr(f.drawTime),
        };
        await api.request(f.id ? `/lotteries/${f.id}` : "/lotteries", {
          method: f.id ? "PUT" : "POST",
          body: JSON.stringify(body),
        });
        toast.message?.success("已保存");
        dialog.visible = false;
        await fetchList(state.lotteries.page);
      } catch (e) {
        toast.message?.error(e.message);
      } finally {
        state.loading.save = false;
      }
    }

    async function doPublish() {
      if (!dialog.form.id) return;
      try {
        await api.request(`/lotteries/${dialog.form.id}/publish`, { method: "POST" });
        toast.message?.success("已发布");
        dialog.form.status = "published";
        await fetchList(state.lotteries.page);
      } catch (e) { toast.message?.error(e.message); }
    }

    async function doClose() {
      if (!dialog.form.id) return;
      try {
        await api.request(`/lotteries/${dialog.form.id}/close`, { method: "POST" });
        toast.message?.success("已结束");
        dialog.form.status = "closed";
        await fetchList(state.lotteries.page);
      } catch (e) { toast.message?.error(e.message); }
    }

    const cols = [
      { title: "ID", key: "id", width: 70 },
      { title: "标题", key: "title", minWidth: 170, ellipsis: { tooltip: true } },
      { title: "开奖时间", key: "drawTime", width: 180 },
      { title: "积分", key: "costPoints", width: 90 },
      { title: "中奖名额", key: "winnerCount", width: 100 },
      {
        title: "状态", key: "status", width: 100,
        render(row) { return statusLabel(row.status); },
      },
      {
        title: "奖品", key: "prizes", minWidth: 180, ellipsis: { tooltip: true },
        render(row) { return (row.prizes || []).map(p => p.prizeName).join("、"); },
      },
    ];

    onMounted(() => fetchList(state.lotteries.page));

    return {
      state, dialog, statusOptions, editStatusOptions, cols,
      fetchList, rowProps, openDialog, addPrize, removePrize, saveDialog, doPublish, doClose,
    };
  },
};
